package com.jhoxmanv.watcher

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle

class OverlayController(private val context: Context, private val lifecycle: Lifecycle) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: FrameLayout? = null
    private val sharedPreferences = context.getSharedPreferences("watcher_settings", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "OverlayController"
    }

    fun isOverlayShowing(): Boolean = overlayView != null && overlayView?.isAttachedToWindow == true

    @SuppressLint("ClickableViewAccessibility")
    fun showOverlay(pauseMedia: Boolean) {
        if (isOverlayShowing()) {
            Log.d(TAG, "Overlay is already showing.")
            return
        }

        Handler(Looper.getMainLooper()).post {
            if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                Log.d(TAG, "Not showing overlay as lifecycle is not active.")
                return@post
            }
            try {
                // If pauseMedia is true, the window must be focusable to steal audio focus.
                // If pauseMedia is false, the window must NOT be focusable.
                val flags = if (pauseMedia) {
                    0 // Default flags: focusable, modal, blocks touches.
                } else {
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                }

                // Read opacity from shared preferences, default to 1f (100% solid).
                val overlayOpacity = sharedPreferences.getFloat("overlay_opacity", 1f)
                // Calculate color with alpha component.
                val backgroundColor = Color.argb((255 * overlayOpacity).toInt(), 0, 0, 0)

                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    flags,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                }

                overlayView = FrameLayout(context).apply {
                    setBackgroundColor(backgroundColor)
                    // Always consume touch events to block interaction with the app below.
                    setOnTouchListener { v, event ->
                        if (event.action == MotionEvent.ACTION_DOWN) {
                            v.performClick()
                        }
                        true
                    }
                }

                windowManager.addView(overlayView, params)
                Log.d(TAG, "Overlay shown with pauseMedia=$pauseMedia, opacity=$overlayOpacity")

            } catch (e: Exception) {
                Log.e(TAG, "Error showing overlay", e)
                overlayView = null
            }
        }
    }

    fun hideOverlay() {
        if (!isOverlayShowing()) {
            Log.d(TAG, "Overlay is already hidden.")
            return
        }

        Handler(Looper.getMainLooper()).post {
            try {
                overlayView?.let {
                    windowManager.removeView(it)
                    overlayView = null
                    Log.d(TAG, "Overlay hidden successfully.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error hiding overlay", e)
                overlayView = null
            }
        }
    }
}
