package com.jhoxmanv.watcher.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
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

class WatcherService : Service(), LifecycleOwner {

    private val CHANNEL_ID = "WatcherServiceChannel"
    private lateinit var eyeWatchController: EyeWatchController
    private lateinit var overlayController: OverlayController
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

        sharedPreferences = getSharedPreferences("watcher_settings", Context.MODE_PRIVATE)
        eyeWatchController = EyeWatchController(this, this)
        overlayController = OverlayController(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_SERVICE") {
            stopSelf()
            return START_NOT_STICKY
        }

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        createNotificationChannel()
        startForeground(1, createNotification())

        // Load all settings
        val screenOffTime = sharedPreferences.getFloat("screen_off_time", 10f).toLong() * 1000
        val minFaceSize = sharedPreferences.getFloat("min_face_size", 0.4f)
        val eyeOpenProb = sharedPreferences.getFloat("eye_open_prob", 0.6f)

        // Configure and start camera for background analysis (no preview)
        eyeWatchController.setOnFacesDetectedListener { result ->
            val faces = result.faces
            val isSomeoneLooking = isUserLooking(faces, eyeOpenProb)
            if (isSomeoneLooking) {
                overlayController.hideOverlay()
                cancelScreenLock()
            } else {
                scheduleScreenLock(screenOffTime)
            }
        }
        eyeWatchController.startCamera(minFaceSize = minFaceSize)

        return START_STICKY
    }

    // ===================================================================
    // INICIO DE LA MODIFICACIÓN
    // ===================================================================
    private fun isUserLooking(faces: List<Face>, eyeOpenThreshold: Float): Boolean {
        // Si no hay rostros, definitivamente no está mirando.
        if (faces.isEmpty()) {
            return false
        }

        // Encuentra el rostro más grande basándose en el área de su cuadro delimitador (bounding box).
        // `maxByOrNull` es una forma segura y concisa de encontrar el máximo en una colección.
        val largestFace = faces.maxByOrNull {
            val bounds = it.boundingBox
            bounds.width() * bounds.height()
        }

        // Si por alguna razón no se pudo determinar el rostro más grande (aunque no debería pasar si la lista no está vacía),
        // consideramos que no está mirando.
        if (largestFace == null) {
            return false
        }

        // Ahora, aplica la lógica de los ojos abiertos SOLO al rostro más grande.
        val leftEyeOpenProb = largestFace.leftEyeOpenProbability
        val rightEyeOpenProb = largestFace.rightEyeOpenProbability

        // Devuelve `true` solo si ambos ojos del rostro más grande están abiertos por encima del umbral.
        return (leftEyeOpenProb != null && leftEyeOpenProb > eyeOpenThreshold) &&
                (rightEyeOpenProb != null && rightEyeOpenProb > eyeOpenThreshold)
    }
    // ===================================================================
    // FIN DE LA MODIFICACIÓN
    // ===================================================================

    private fun scheduleScreenLock(delay: Long) {
        if (lockRunnable == null && !overlayController.isOverlayShowing()) {
            Log.d(TAG, "Scheduling screen lock in ${delay / 1000} seconds.")
            lockRunnable = Runnable {
                overlayController.showOverlay()
                lockRunnable = null
            }
            lockHandler.postDelayed(lockRunnable!!, delay)
        }
    }

    private fun cancelScreenLock() {
        lockRunnable?.let {
            Log.d(TAG, "Face detected, cancelling screen lock.")
            lockHandler.removeCallbacks(it)
            lockRunnable = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        WatcherStateHolder.isServiceRunning.value = false
        cancelScreenLock()
        overlayController.hideOverlay()
        eyeWatchController.stopCamera()
        Log.d(TAG, "Service destroyed.")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent(this, WatcherService::class.java).apply { action = "STOP_SERVICE" }
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Watcher is Active")
            .setContentText("Protecting your privacy by monitoring your presence.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Stop", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(CHANNEL_ID, "Watcher Service Channel", NotificationManager.IMPORTANCE_DEFAULT)
            getSystemService(NotificationManager::class.java).createNotificationChannel(serviceChannel)
        }
    }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
}
