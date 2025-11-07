package com.jhoxmanv.watcher.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FaceRetouchingNatural
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.jhoxmanv.watcher.EyeWatchController
import com.jhoxmanv.watcher.FaceDetectionResult
import com.jhoxmanv.watcher.ui.components.CameraPreview
import com.jhoxmanv.watcher.ui.components.SettingItem
import com.jhoxmanv.watcher.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GazeConfigScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val settingsViewModel: SettingsViewModel = viewModel()

    var detectionResult by remember { mutableStateOf<FaceDetectionResult?>(null) }
    val eyeWatchController = remember { EyeWatchController(context, lifecycleOwner) }

    // Get configuration in the Composable context
    val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT

    val tempMinFaceSize by settingsViewModel.tempMinFaceSize
    val tempEyeOpenProb by settingsViewModel.tempEyeOpenProbability

    var previewSize by remember { mutableStateOf(IntSize.Zero) }

    fun handleBackNavigation() {
        settingsViewModel.saveGazeConfig()
        navController.popBackStack()
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
                    IconButton(onClick = { handleBackNavigation() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                    .weight(0.6f)
                    .onSizeChanged { previewSize = it }
            ) {
                LaunchedEffect(tempMinFaceSize) {
                    eyeWatchController.setOnFacesDetectedListener { result ->
                        detectionResult = result
                    }
                }

                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    eyeWatchController = eyeWatchController,
                    minFaceSize = tempMinFaceSize
                )

                Canvas(modifier = Modifier.fillMaxSize()) { ->
                    detectionResult?.let { result ->
                        val (scaleX, scaleY) = calculateScaleFactors(
                            sourceWidth = result.sourceWidth,
                            sourceHeight = result.sourceHeight,
                            previewWidth = previewSize.width,
                            previewHeight = previewSize.height,
                            isPortrait = isPortrait // Pass the value here
                        )

                        result.faces.forEach { face ->
                            val bounds = face.boundingBox
                            // Adjust coordinates for front camera mirroring
                            val transformedLeft = size.width - (bounds.right * scaleX)
                            val transformedTop = bounds.top * scaleY

                            drawRect(
                                color = Color.Green,
                                topLeft = Offset(transformedLeft, transformedTop),
                                size = Size(bounds.width() * scaleX, bounds.height() * scaleY),
                                style = Stroke(width = 2.dp.toPx())
                            )

                            val isLooking = (face.leftEyeOpenProbability ?: 0f) > tempEyeOpenProb &&
                                          (face.rightEyeOpenProbability ?: 0f) > tempEyeOpenProb

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
            }

            // Controls section
            Column(
                modifier = Modifier
                    .weight(0.4f)
                    .padding(16.dp)
            ) {
                SettingItem(
                    icon = Icons.Default.FaceRetouchingNatural,
                    title = "Minimum Face Size",
                    description = "Ignores faces that are too small or far away.",
                    valueLabel = { Text("${(tempMinFaceSize * 100).toInt()}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                ) {
                    Slider(
                        value = tempMinFaceSize,
                        onValueChange = { settingsViewModel.onTempMinFaceSizeChanged(it) },
                        valueRange = 0.1f..1.0f,
                        steps = 9
                    )
                }

                SettingItem(
                    icon = Icons.Default.Visibility,
                    title = "Gaze Detection Sensitivity",
                    description = "Ensures the user is looking at the screen.",
                    valueLabel = { Text("${(tempEyeOpenProb * 100).toInt()}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                ) {
                    Slider(
                        value = tempEyeOpenProb,
                        onValueChange = { settingsViewModel.onTempEyeOpenProbabilityChanged(it) },
                        valueRange = 0.1f..1.0f,
                        steps = 9
                    )
                }

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = { handleBackNavigation() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save and Exit")
                }
            }
        }
    }
}

private fun calculateScaleFactors(
    sourceWidth: Int,
    sourceHeight: Int,
    previewWidth: Int,
    previewHeight: Int,
    isPortrait: Boolean
): Pair<Float, Float> {
    val (width, height) = if (isPortrait) {
        sourceHeight to sourceWidth // In portrait, the camera image is landscape
    } else {
        sourceWidth to sourceHeight
    }
    val scaleX = previewWidth.toFloat() / width.toFloat()
    val scaleY = previewHeight.toFloat() / height.toFloat()
    return scaleX to scaleY
}
