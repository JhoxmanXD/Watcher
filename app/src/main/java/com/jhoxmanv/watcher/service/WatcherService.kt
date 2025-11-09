package com.jhoxmanv.watcher.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.google.mlkit.vision.face.Face
import com.jhoxmanv.watcher.EyeWatchController
import com.jhoxmanv.watcher.MainActivity
import com.jhoxmanv.watcher.OverlayController
import com.jhoxmanv.watcher.R
import com.jhoxmanv.watcher.WatcherStateHolder
import kotlin.math.abs


class WatcherService : Service(), LifecycleOwner {

    private val channelId = "WatcherServiceChannel"
    private lateinit var eyeWatchController: EyeWatchController
    private lateinit var overlayController: OverlayController
    private lateinit var overlayAudioGrip: OverlayAudioGrip
    private val lifecycleRegistry = LifecycleRegistry(this)
    private lateinit var sharedPreferences: SharedPreferences


    private val lockHandler = Handler(Looper.getMainLooper())
    private var lockRunnable: Runnable? = null

    companion object {
        private const val TAG = "WatcherService"
    }

    override fun onCreate() {
        super.onCreate()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        WatcherStateHolder.isServiceRunning.value = true

        sharedPreferences = getSharedPreferences("watcher_settings", MODE_PRIVATE)
        eyeWatchController = EyeWatchController(this, this)
        overlayController = OverlayController(this, lifecycle)
        overlayAudioGrip = OverlayAudioGrip(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_SERVICE") {
            stopSelf()
            return START_NOT_STICKY
        }

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        createNotificationChannel()
        startForeground(1, createNotification())

        val gazeThreshold = sharedPreferences.getFloat("gaze_threshold", 0.4f)
        val yawThreshold = sharedPreferences.getFloat("yaw_threshold", 20f)
        val pitchThreshold = sharedPreferences.getFloat("pitch_threshold", 20f)
        val minFaceSize = 0.5f

        eyeWatchController.setOnFacesDetectedListener { result ->
            val screenOffTime = sharedPreferences.getFloat("screen_off_time", 10f).toLong() * 1000
            val pauseMedia = sharedPreferences.getBoolean("pause_media", true)

            val largestFace = result.faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
            val isLooking = largestFace != null && isUserLooking(largestFace, gazeThreshold, yawThreshold, pitchThreshold)

            if (isLooking) {
                cancelScreenLock()
            } else {
                if (!overlayController.isOverlayShowing() && lockRunnable == null) {
                    scheduleScreenLock(screenOffTime, pauseMedia)
                }
            }
        }
        eyeWatchController.startCamera(minFaceSize = minFaceSize)

        return START_STICKY
    }

    private fun isUserLooking(face: Face, eyeOpenThreshold: Float, yawThreshold: Float, pitchThreshold: Float): Boolean {
        val eyesAreOpen = (face.leftEyeOpenProbability ?: 0f) > eyeOpenThreshold &&
                          (face.rightEyeOpenProbability ?: 0f) > eyeOpenThreshold

        val headIsFacingForward = abs(face.headEulerAngleY) < yawThreshold &&
                                  abs(face.headEulerAngleX) < pitchThreshold

        return eyesAreOpen && headIsFacingForward
    }

    private fun scheduleScreenLock(delay: Long, pauseMedia: Boolean) {
        Log.d(TAG, "Scheduling screen lock in ${delay / 1000} seconds.")
        lockRunnable = Runnable {
            overlayController.showOverlay(pauseMedia)
            if (pauseMedia) {
                overlayAudioGrip.acquire()
            }
            lockRunnable = null
        }
        lockHandler.postDelayed(lockRunnable!!, delay)
    }

    private fun cancelScreenLock() {
        lockRunnable?.let {
            Log.d(TAG, "Face detected, cancelling screen lock schedule.")
            lockHandler.removeCallbacks(it)
            lockRunnable = null
        }

        if (overlayController.isOverlayShowing()) {
            val pauseMedia = sharedPreferences.getBoolean("pause_media", true)
            if (pauseMedia) {
                overlayAudioGrip.release()
            }
            overlayController.hideOverlay()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        WatcherStateHolder.isServiceRunning.value = false
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        cancelScreenLock()

        if (overlayController.isOverlayShowing()) {
            overlayController.hideOverlay()
        }

        eyeWatchController.stopCamera()
        overlayAudioGrip.release() // Make sure to release the grip on destroy
        Log.d(TAG, "Service destroyed.")
    }


    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent(this, WatcherService::class.java).apply { action = "STOP_SERVICE" }
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Watcher is Active")
            .setContentText("Protecting your privacy by monitoring your presence.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Stop", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(channelId, "Watcher Service Channel", NotificationManager.IMPORTANCE_DEFAULT)
        getSystemService(NotificationManager::class.java).createNotificationChannel(serviceChannel)
    }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
}
