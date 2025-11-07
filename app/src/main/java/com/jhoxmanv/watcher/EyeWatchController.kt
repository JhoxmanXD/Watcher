package com.jhoxmanv.watcher

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// Data class to hold the complete result of a face detection pass
data class FaceDetectionResult(
    val faces: List<Face>,
    val sourceWidth: Int,
    val sourceHeight: Int
)

class EyeWatchController(private val context: Context, private val lifecycleOwner: LifecycleOwner) {

    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private var onFacesDetected: (FaceDetectionResult) -> Unit = {}

    companion object {
        private const val TAG = "EyeWatchController"
    }

    fun setOnFacesDetectedListener(listener: (FaceDetectionResult) -> Unit) {
        onFacesDetected = listener
    }

    @SuppressLint("UnsafeOptInUsageError")
    fun startCamera(minFaceSize: Float, surfaceProvider: Preview.SurfaceProvider? = null) {
        cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val faceDetectorOptions = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setMinFaceSize(minFaceSize)
                .build()

            val faceDetector = FaceDetection.getClient(faceDetectorOptions)

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        val image = imageProxy.image
                        if (image != null) {
                            val inputImage = InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)
                            faceDetector.process(inputImage)
                                .addOnSuccessListener { faces ->
                                    val result = FaceDetectionResult(faces, image.width, image.height)
                                    onFacesDetected(result)
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
                cameraProvider.unbindAll()
                val useCases = mutableListOf<androidx.camera.core.UseCase>(imageAnalyzer)

                if (surfaceProvider != null) {
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(surfaceProvider)
                    }
                    useCases.add(preview)
                }

                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    *useCases.toTypedArray()
                )
                Log.d(TAG, "Camera started. Preview enabled: ${surfaceProvider != null}")

            } catch (e: Exception) {
                Log.e(TAG, "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun stopCamera() {
        if (::cameraProviderFuture.isInitialized) {
            cameraProviderFuture.get().unbindAll()
        }
        if (!cameraExecutor.isShutdown) {
            cameraExecutor.shutdown()
        }
    }
}
