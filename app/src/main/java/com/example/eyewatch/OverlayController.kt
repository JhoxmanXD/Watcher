package com.example.eyewatch

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * OverlayController robusto:
 * - Intenta varios tipos de WindowManager.LayoutParams según la API.
 * - Usa AppLogger para logging centralizado (si AppLogger.enabled==true).
 * - showOverlay/hideOverlay bloquean hasta que la UI thread completa la operación o timeout.
 */
class OverlayController(private val context: Context) {

    private val appContext: Context = context.applicationContext
    private val windowManager: WindowManager =
        appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var overlayView: FrameLayout? = null
    @Volatile private var isShown = false

    private fun logD(tag: String, msg: String) {
        try {
            AppLogger.d(tag, msg)
        } catch (t: Throwable) {
            Log.d(tag, msg)
        }
    }

    private fun logE(tag: String, msg: String, t: Throwable? = null) {
        try {
            AppLogger.e(tag, msg, t)
        } catch (_: Throwable) {
            if (t != null) Log.e(tag, msg, t) else Log.e(tag, msg)
        }
    }

    fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Settings.canDrawOverlays(appContext)
            } catch (t: Throwable) {
                logE("OverlayController", "Settings.canDrawOverlays threw", t)
                false
            }
        } else true
    }

    fun requestOverlayPermission(fromActivity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${fromActivity.packageName}")
                ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                fromActivity.startActivity(intent)
            } catch (t: Exception) {
                try {
                    val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    fromActivity.startActivity(i)
                } catch (e: Exception) {
                    logE("OverlayController", "No se puede abrir settings overlay", e)
                }
            }
        }
    }

    /**
     * Crea LayoutParams para un tipo dado
     */
    private fun buildLayoutParams(type: Int): WindowManager.LayoutParams {
        val flags = (WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                or WindowManager.LayoutParams.FLAG_FULLSCREEN
                // permitir que eventos sigan a la app; quita FLAG_NOT_FOCUSABLE si quieres bloquear input
                or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            flags,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            } catch (_: Throwable) {}
        }
        return params
    }

    /**
     * Intenta crear el overlay en UI thread. Devuelve true si pudo añadir.
     * Intentará varios tipos (API >= 26: TYPE_APPLICATION_OVERLAY; legacy: TYPE_SYSTEM_ALERT/TYPE_PHONE).
     */
    fun showOverlay(timeoutMs: Long = 700L): Boolean {
        if (isShown) return true
        if (!hasPermission()) {
            logD("OverlayController", "No permission to draw overlays")
            return false
        }

        val latch = CountDownLatch(1)
        val result = BooleanArray(1) { false }

        val runnable = Runnable {
            try {
                // limpiar si hay algo previo
                overlayView?.let { try { windowManager.removeView(it) } catch (_: Exception) {} }
                overlayView = null
                // Intentamos tipos en orden de preferencia
                val triedTypes = mutableListOf<String>()
                val typeCandidates = mutableListOf<Int>()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    typeCandidates.add(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
                }
                // legacy fallbacks: añadir en orden
                // TYPE_SYSTEM_ERROR/System_ALERT están protegidos en algunas ROMs, pero los probamos como fallback
                typeCandidates.add(WindowManager.LayoutParams.TYPE_APPLICATION_PANEL)
                typeCandidates.add(WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG)
                // Add PHONE and SYSTEM_ALERT as older fallbacks if constants exist
                try { typeCandidates.add(WindowManager.LayoutParams.TYPE_PHONE) } catch (_: Throwable) {}
                try { typeCandidates.add(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT) } catch (_: Throwable) {}

                var added = false
                var lastEx: Throwable? = null

                for (t in typeCandidates) {
                    try {
                        val params = buildLayoutParams(t)
                        val root = FrameLayout(appContext).apply {
                            setBackgroundColor(0xFF000000.toInt())
                            // capture touch to avoid passing touches if you want blocking overlay:
                            setOnTouchListener { _, _ -> true }
                        }
                        windowManager.addView(root, params)
                        overlayView = root
                        isShown = true
                        result[0] = true
                        added = true
                        logD("OverlayController", "addView succeeded with type=$t")
                        break
                    } catch (ex: Throwable) {
                        lastEx = ex
                        triedTypes.add("type=$t -> ${ex.message}")
                        // limpiar cualquier view parcial
                        try { overlayView?.let { windowManager.removeView(it) } } catch (_: Exception) {}
                        overlayView = null
                    }
                }

                if (!added) {
                    logE("OverlayController", "Failed to add overlay; tried: ${triedTypes.joinToString("; ")}", lastEx)
                    result[0] = false
                    isShown = false
                } else {
                    logD("OverlayController", "Overlay añadido correctamente (UI thread)")
                }
            } catch (t: Throwable) {
                logE("OverlayController", "addView fallo (outer)", t)
                try { overlayView?.let { windowManager.removeView(it) } } catch (_: Exception) {}
                overlayView = null
                isShown = false
                result[0] = false
            } finally {
                latch.countDown()
            }
        }

        // Ejecutar en main thread
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run()
        } else {
            Handler(Looper.getMainLooper()).post(runnable)
        }

        try {
            latch.await(timeoutMs, TimeUnit.MILLISECONDS)
        } catch (ie: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        return result[0]
    }

    /**
     * Oculta el overlay en UI thread y devuelve true si se quitó (o false si no había).
     */
    fun hideOverlay(timeoutMs: Long = 700L): Boolean {
        if (!isShown && overlayView == null) return false

        val latch = CountDownLatch(1)
        val result = BooleanArray(1) { false }

        val runnable = Runnable {
            try {
                overlayView?.let { windowManager.removeView(it) }
                overlayView = null
                isShown = false
                result[0] = true
                logD("OverlayController", "Overlay quitado correctamente (UI thread)")
            } catch (t: Throwable) {
                logE("OverlayController", "removeView fallo", t)
                overlayView = null
                isShown = false
                result[0] = false
            } finally {
                latch.countDown()
            }
        }

        if (Looper.myLooper() == Looper.getMainLooper()) runnable.run() else Handler(Looper.getMainLooper()).post(runnable)

        try {
            latch.await(timeoutMs, TimeUnit.MILLISECONDS)
        } catch (ie: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        return result[0]
    }

    fun isOverlayShowing(): Boolean = isShown
}
