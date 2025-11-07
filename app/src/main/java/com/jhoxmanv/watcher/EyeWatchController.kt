package com.jhoxmanv.watcher

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
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

    companion object {
        private const val TAG = "EyeWatchController"
    }

    fun setOnFaceDetectedListener(listener: (Boolean) -> Unit) {
        onFaceDetected = listener
    }

    @SuppressLint("UnsafeOptInUsageError")
    fun startCamera(minFaceSize: Float) { // Parameter for sensitivity
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            // Configure the face detector with the provided sensitivity
            val faceDetectorOptions = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE) // We only need detection
                .setMinFaceSize(minFaceSize) // Use the parameter here
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
                                    onFaceDetected(faces.isNotEmpty())
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Face detection failed", e)
                                }
                                .addOnCompleteListener { _ ->
                                    imageProxy.close()
                                }
                        } else {
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
                Log.d(TAG, "Camera started and bound to lifecycle.")

            } catch (e: Exception) {
                Log.e(TAG, "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun stopCamera() {
        if (cameraExecutor.isShutdown) return
        try {
            cameraProvider?.unbindAll()
            Log.d(TAG, "Camera stopped and unbound.")
        } finally {
            cameraExecutor.shutdown()
        }
    }
}
