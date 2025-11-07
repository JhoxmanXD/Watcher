package com.jhoxmanv.watcher

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.FrameLayout

class OverlayController(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: FrameLayout? = null

    companion object {
        private const val TAG = "OverlayController"
    }

    fun isOverlayShowing(): Boolean = overlayView != null && overlayView?.isAttachedToWindow == true

    @SuppressLint("ClickableViewAccessibility")
    fun showOverlay() {
        if (isOverlayShowing()) {
            Log.d(TAG, "Overlay is already showing.")
            return
        }

        Handler(Looper.getMainLooper()).post {
            try {
                // Modern, simple, and safe WindowManager parameters.
                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    0, // Setting flags to 0 makes the window modal and blocks all touches below it.
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                }

                overlayView = FrameLayout(context).apply {
                    setBackgroundColor(0xFF000000.toInt()) // Black background
                    // Consume all touch events and satisfy the accessibility lint check by calling performClick.
                    setOnTouchListener { v, event ->
                        if (event.action == MotionEvent.ACTION_DOWN) {
                            v.performClick()
                        }
                        true
                    }
                }

                windowManager.addView(overlayView, params)
                Log.d(TAG, "Overlay shown successfully.")

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
