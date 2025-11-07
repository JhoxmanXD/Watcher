package com.jhoxmanv.watcher

import android.annotation.SuppressLint
import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class EyeWatchController(private val context: Context, private val lifecycleOwner: LifecycleOwner) {

    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var imageAnalyzer: ImageAnalysis? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var onFaceDetected: (Boolean) -> Unit = {}

    // Define un TAG constante para los logs de esta clase
    companion object {
        private const val TAG = "EyeWatchController"
    }

    fun setOnFaceDetectedListener(listener: (Boolean) -> Unit) {
        onFaceDetected = listener
    }

    @SuppressLint("UnsafeOptInUsageError")
    fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val faceDetectorOptions = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build()

            val faceDetector = FaceDetection.getClient(faceDetectorOptions)

            imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        val image = imageProxy.image
                        if (image != null) {
                            val inputImage = InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)
                            faceDetector.process(inputImage)
                                .addOnSuccessListener { faces ->
                                    // Notifica si se detectó al menos un rostro
                                    onFaceDetected(faces.isNotEmpty())
                                }
                                .addOnFailureListener { e ->
                                    // Usa AppLogger para registrar el error
                                    AppLogger.e(TAG, "La detección de rostros falló", e)
                                }
                                .addOnCompleteListener {
                                    // ¡Muy importante! Cierra el proxy para recibir el siguiente frame
                                    imageProxy.close()
                                }
                        } else {
                            // Cierra el proxy si la imagen es nula para evitar bloqueos
                            imageProxy.close()
                        }
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    imageAnalyzer
                )
                // Usa AppLogger para un log de depuración
                AppLogger.d(TAG, "Cámara iniciada y enlazada al ciclo de vida.")

            } catch (e: Exception) {
                // Usa AppLogger para registrar el error
                AppLogger.e(TAG, "Error al enlazar los casos de uso de la cámara", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun stopCamera() {
        if (cameraExecutor.isShutdown) return
        try {
            cameraProvider?.unbindAll()
            AppLogger.d(TAG, "Cámara detenida y desenlazada.")
        } finally {
            cameraExecutor.shutdown()
        }
    }
}
