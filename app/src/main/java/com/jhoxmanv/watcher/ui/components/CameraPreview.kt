package com.jhoxmanv.watcher.ui.components

import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.jhoxmanv.watcher.EyeWatchController

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    eyeWatchController: EyeWatchController,
    minFaceSize: Float
) {
    AndroidView(
        factory = { context ->
            PreviewView(context)
        },
        modifier = modifier,
        update = { previewView ->
            // Every time the preview is updated, we restart the camera with the current settings.
            // This is how the live preview works.
            eyeWatchController.startCamera(
                minFaceSize = minFaceSize,
                surfaceProvider = previewView.surfaceProvider
            )
        }
    )
}
