package com.jhoxmanv.watcher.ui.screens

import android.content.Intent
import android.content.res.Configuration
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.mlkit.vision.face.Face
import com.jhoxmanv.watcher.EyeWatchController
import com.jhoxmanv.watcher.FaceDetectionResult
import com.jhoxmanv.watcher.WatcherStateHolder
import com.jhoxmanv.watcher.service.WatcherService
import com.jhoxmanv.watcher.ui.components.SettingItem
import com.jhoxmanv.watcher.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GazeConfigScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val settingsViewModel: SettingsViewModel = viewModel()

    val eyeWatchController = remember { EyeWatchController(context, lifecycleOwner) }
    val previewView = remember { PreviewView(context) }

    var largestFace by remember { mutableStateOf<Face?>(null) }
    var sourceSize by remember { mutableStateOf(IntSize.Zero) }
    var previewSize by remember { mutableStateOf(IntSize.Zero) }
    val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT

    // The UI slider works with sensitivity (0.0 to 1.0), which is the inverse of the threshold.
    var sliderSensitivity by remember { mutableFloatStateOf(1.0f - settingsViewModel.tempGazeThreshold.floatValue) }

    // This effect correctly starts/restarts the camera when the slider value changes.
    LaunchedEffect(Unit) {
        eyeWatchController.setOnFacesDetectedListener { result ->
            largestFace = result.faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
            sourceSize = IntSize(result.sourceWidth, result.sourceHeight)
        }
        // Start with a fixed face size of 50%
        eyeWatchController.startCamera(
            minFaceSize = 0.5f,
            surfaceProvider = previewView.surfaceProvider
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            eyeWatchController.stopCamera()
            settingsViewModel.resetTempGazeConfig()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gaze Detection Setup") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { // Discards changes
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back (Discard Changes)")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier
                    .weight(0.7f) // Give more space to the camera
                    .onSizeChanged { previewSize = it }
            ) {
                AndroidView({ previewView }, modifier = Modifier.fillMaxSize())

                Canvas(modifier = Modifier.fillMaxSize()) { ->
                    largestFace?.let { face ->
                        if (previewSize == IntSize.Zero || sourceSize == IntSize.Zero) return@let

                        val (scaleX, scaleY) = calculateScaleFactors(
                            sourceWidth = sourceSize.width, sourceHeight = sourceSize.height,
                            previewWidth = previewSize.width, previewHeight = previewSize.height,
                            isPortrait = isPortrait
                        )

                        val bounds = face.boundingBox
                        val transformedLeft = size.width - (bounds.right * scaleX)
                        val transformedTop = bounds.top * scaleY

                        drawRect(
                            color = Color.Green,
                            topLeft = Offset(transformedLeft, transformedTop),
                            size = Size(bounds.width() * scaleX, bounds.height() * scaleY),
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // Use the temporary threshold for real-time feedback
                        val isLooking = (face.leftEyeOpenProbability ?: 0f) > settingsViewModel.tempGazeThreshold.floatValue &&
                                      (face.rightEyeOpenProbability ?: 0f) > settingsViewModel.tempGazeThreshold.floatValue

                        if (isLooking) {
                            drawRect(
                                color = Color.Blue,
                                topLeft = Offset(transformedLeft + 5, transformedTop + 5),
                                size = Size((bounds.width() * scaleX) - 10, (bounds.height() * scaleY) - 10),
                                style = Stroke(width = 3.dp.toPx())
                            )
                        }
                    }
                }
            }

            // Controls section
            Column(
                modifier = Modifier
                    .weight(0.3f) // Less space for controls
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                SettingItem(
                    icon = Icons.Default.Visibility,
                    title = "Gaze Sensitivity",
                    description = "Higher values are more sensitive.",
                    valueLabel = { Text("${(sliderSensitivity * 100).toInt()}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                ) {
                    Slider(
                        value = sliderSensitivity,
                        onValueChange = {
                            sliderSensitivity = it
                            settingsViewModel.onTempGazeSensitivityChanged(it)
                        },
                        valueRange = 0.1f..1.0f,
                        steps = 9
                    )
                }

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = {
                        settingsViewModel.saveGazeConfig()
                        if (WatcherStateHolder.isServiceRunning.value) {
                            val intent = Intent(context, WatcherService::class.java)
                            context.stopService(intent)
                            context.startService(intent)
                        }
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apply and Exit")
                }
            }
        }
    }
}

private fun calculateScaleFactors(
    sourceWidth: Int, sourceHeight: Int,
    previewWidth: Int, previewHeight: Int,
    isPortrait: Boolean
): Pair<Float, Float> {
    val (width, height) = if (isPortrait) sourceHeight to sourceWidth else sourceWidth to sourceHeight
    val scaleX = previewWidth.toFloat() / width.toFloat()
    val scaleY = previewHeight.toFloat() / height.toFloat()
    return scaleX to scaleY
}
