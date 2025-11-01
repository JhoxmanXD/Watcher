package com.example.eyewatch

import android.app.Activity
import kotlinx.coroutines.*
import androidx.compose.runtime.MutableState
import androidx.lifecycle.LifecycleCoroutineScope
import java.lang.Exception
import kotlin.math.abs
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

/**
 * EyeWatchController: centraliza la lógica de detección (start/stop watcher) y el control del overlay.
 * - activity: usado para runOnUiThread cuando hace falta
 * - lifecycleScope: scope para lanzar coroutines ligadas al lifecycle de la Activity
 * - overlayController: controlador que gestiona addView/removeView
 * - isLookingState, detectionEnabledState, overlayFallbackState: estados compartidos con UI Compose
 * - thresholdYaw/thresholdPitch: umbrales (AtomicFloat) provistos por UI
 */
class EyeWatchController(
    private val activity: Activity,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val overlayController: OverlayController,
    private val isLookingState: MutableState<Boolean>,
    private val detectionEnabledState: MutableState<Boolean>,
    private val overlayFallbackState: MutableState<Boolean>,
    private val thresholdYaw: AtomicFloat,
    private val thresholdPitch: AtomicFloat
) {
    // flags internos
    @Volatile
    private var cameraBound = false

    @Volatile
    private var latestIsLooking = false

    @Volatile
    private var overlayIsActive = false

    // tiempos/constantes (ajusta si quieres)
    private val SAMPLE_MS = 1000L
    private val POLL_MS = 250L
    private val REQUIRED_MS_CONTINUOUS = 1000L

    private var overlayMonitorJob: Job? = null
    private var sampleWatcherJob: Job? = null

    fun setCameraBound(bound: Boolean) {
        cameraBound = bound
    }

    fun onFrame(isLooking: Boolean) {
        latestIsLooking = isLooking
        isLookingState.value = isLooking
    }

    fun start() {
        // arrancar watcher si no existe
        if (sampleWatcherJob?.isActive == true) return
        startSampleWatcher()
    }

    fun stop() {
        // parar watcher y monitor, y tratar de quitar overlay
        try {
            sampleWatcherJob?.cancel()
            sampleWatcherJob = null
        } catch (_: Throwable) {}
        cancelOverlayMonitorIfRunning()
        safeHideSystemOverlay()
    }

    fun resetOverlayFlagsOnResume() {
        // usado por MainActivity.onResume()
        overlayIsActive = false
        overlayFallbackState.value = false
        cancelOverlayMonitorIfRunning()
    }

    fun overlayShouldBeActive(): Boolean {
        if (!detectionEnabledState.value) return false
        if (overlayIsActive) return false
        return overlayFallbackState.value
    }

    fun isOverlayActive(): Boolean = overlayIsActive

    private fun startSampleWatcher() {
        sampleWatcherJob = lifecycleScope.launch(Dispatchers.Default) {
            var continuousNotLookMs = 0L
            var lastCheck = System.currentTimeMillis()

            while (isActive) {
                try {
                    val now = System.currentTimeMillis()
                    val delta = now - lastCheck
                    lastCheck = now

                    // Si detección desactivada -> asegurar oculto y reset counters
                    if (!detectionEnabledState.value) {
                        if (overlayIsActive) safeHideSystemOverlay()
                        overlayFallbackState.value = false
                        cancelOverlayMonitorIfRunning()
                        continuousNotLookMs = 0L
                        delay(POLL_MS)
                        continue
                    }

                    // Si cámara no está lista -> no acumulamos ni decidimos
                    if (!cameraBound) {
                        AppLogger.d("SampleWatcher", "cameraBound=false -> omitir muestra")
                        continuousNotLookMs = 0L
                        delay(POLL_MS)
                        continue
                    }

                    val currentlyLooking = latestIsLooking
                    AppLogger.d("SampleWatcher", "Watcher tick -> looking=$currentlyLooking (continuousNotLookMs=$continuousNotLookMs)")

                    if (currentlyLooking) {
                        // Si está mirando, reset contador y quitar overlays si están activos
                        continuousNotLookMs = 0L
                        if (overlayIsActive) {
                            safeHideSystemOverlay()
                        }
                        if (overlayFallbackState.value) {
                            activity.runOnUiThread { overlayFallbackState.value = false }
                        }
                        cancelOverlayMonitorIfRunning()
                    } else {
                        // No mirando: acumular tiempo de no-mirada
                        continuousNotLookMs += delta
                        // Solo cuando llevemos NO_MIRANDO >= REQUIRED_MS_CONTINUOUS ejecutar la acción
                        if (continuousNotLookMs >= REQUIRED_MS_CONTINUOUS) {
                            AppLogger.d("SampleWatcher", "No-mirada continua >= ${REQUIRED_MS_CONTINUOUS}ms -> intentar mostrar apagado")
                            // reset para evitar reentradas rápidas
                            continuousNotLookMs = 0L

                            val hasPerm = overlayController.hasPermission()
                            if (hasPerm) {
                                val ok = overlayController.showOverlay()
                                if (ok) {
                                    overlayIsActive = true
                                    overlayFallbackState.value = false
                                    AppLogger.d("SampleWatcher", "Mostrando overlay sistema por muestra")
                                    startOverlayMonitor()
                                } else {
                                    overlayIsActive = false
                                    overlayFallbackState.value = true
                                    AppLogger.w("SampleWatcher", "showOverlay() falló -> fallback in-app activado por muestra")
                                    startOverlayMonitor()
                                }
                            } else {
                                overlayIsActive = false
                                overlayFallbackState.value = true
                                AppLogger.d("SampleWatcher", "Sin permiso -> fallback in-app activado por muestra")
                                startOverlayMonitor()
                            }
                        }
                    }

                    delay(POLL_MS)
                } catch (e: Exception) {
                    AppLogger.e("SampleWatcher", "Error en sample watcher", e)
                }
            }
        }
    }

    private fun startOverlayMonitor() {
        // si ya hay monitor activo no iniciar otro
        if (overlayMonitorJob?.isActive == true) return

        overlayMonitorJob = lifecycleScope.launch(Dispatchers.Default) {
            try {
                var continuousLookMs = 0L
                var lastCheck = System.currentTimeMillis()

                while (isActive && (overlayIsActive || overlayFallbackState.value)) {
                    val now = System.currentTimeMillis()
                    val delta = now - lastCheck
                    lastCheck = now

                    if (latestIsLooking) {
                        continuousLookMs += delta
                    } else {
                        continuousLookMs = 0L
                    }

                    if (continuousLookMs >= REQUIRED_MS_CONTINUOUS && cameraBound) {
                        AppLogger.d("OverlayMonitor", "Mirada continua detectada -> quitar apagado (continuousMs=$continuousLookMs)")
                        activity.runOnUiThread {
                            try {
                                if (overlayIsActive) {
                                    val ok = overlayController.hideOverlay()
                                    overlayIsActive = !ok
                                    if (!ok) overlayFallbackState.value = true else overlayFallbackState.value = false
                                    AppLogger.d("OverlayMonitor", "hideOverlay() desde monitor -> ok=$ok")
                                }
                                if (overlayFallbackState.value) {
                                    overlayFallbackState.value = false
                                    AppLogger.d("OverlayMonitor", "fallback in-app quitado desde monitor")
                                }
                            } catch (t: Throwable) {
                                AppLogger.e("OverlayMonitor", "Error quitando overlay desde monitor", t)
                            } finally {
                                cancelOverlayMonitorIfRunning()
                            }
                        }
                        break
                    }
                    delay(POLL_MS)
                }
            } catch (ex: Exception) {
                AppLogger.e("OverlayMonitor", "Error en overlay monitor", ex)
            }
        }
    }

    private fun cancelOverlayMonitorIfRunning() {
        try {
            overlayMonitorJob?.cancel()
            overlayMonitorJob = null
        } catch (t: Throwable) {
            AppLogger.w("OverlayMonitor", "cancel error: ${t.message}")
        }
    }

    private fun safeHideSystemOverlay() {
        activity.runOnUiThread {
            try {
                val ok = overlayController.hideOverlay()
                overlayIsActive = !ok
                if (!ok) overlayFallbackState.value = true else overlayFallbackState.value = false
                AppLogger.d("OverlayAction", "safeHideSystemOverlay -> ok=$ok")
            } catch (t: Throwable) {
                AppLogger.e("OverlayAction", "safeHideSystemOverlay error", t)
                overlayIsActive = false
                overlayFallbackState.value = false
            } finally {
                cancelOverlayMonitorIfRunning()
            }
        }
    }
}

/**
 * DynamicFaceAnalyzer: mantiene detector ML Kit y llama onFrame(isLooking:Boolean) por cada frame.
 * Está aquí porque forma parte de la lógica de detección.
 */
class DynamicFaceAnalyzer(
    private val getYaw: () -> Float,
    private val getPitch: () -> Float,
    private val onFrame: (isLooking: Boolean) -> Unit
) : ImageAnalysis.Analyzer {

    private val detectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
        .enableTracking()
        .build()

    private val detector = FaceDetection.getClient(detectorOptions)

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val rotation = imageProxy.imageInfo.rotationDegrees
        val inputImage = InputImage.fromMediaImage(mediaImage, rotation)

        detector.process(inputImage)
            .addOnSuccessListener { faces: List<Face> ->
                var isLookingNow = false
                if (faces.isNotEmpty()) {
                    val yawThresh = getYaw()
                    val pitchThresh = getPitch()
                    for (face in faces) {
                        val rotY = face.headEulerAngleY
                        val rotX = face.headEulerAngleX
                        if (abs(rotY) < yawThresh && abs(rotX) < pitchThresh) {
                            isLookingNow = true
                            break
                        }
                    }
                }
                onFrame(isLookingNow)
            }
            .addOnFailureListener { e ->
                AppLogger.e("DynamicFaceAnalyzer", "Error ML Kit", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}
