package com.jhoxmanv.watcher.ui.screens

import android.content.Intent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.google.mlkit.vision.face.Face
import com.jhoxmanv.watcher.EyeWatchController
import com.jhoxmanv.watcher.WatcherStateHolder
import com.jhoxmanv.watcher.service.WatcherService
import com.jhoxmanv.watcher.ui.components.SettingItem
import com.jhoxmanv.watcher.viewmodel.SettingsViewModel
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GazeConfigScreen(
    navController: NavController,
    settingsViewModel: SettingsViewModel // Receive the shared ViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val eyeWatchController = remember { EyeWatchController(context, lifecycleOwner) }
    val previewView = remember { PreviewView(context) }

    var largestFace by remember { mutableStateOf<Face?>(null) }
    var sourceSize by remember { mutableStateOf(IntSize.Zero) }
    var previewSize by remember { mutableStateOf(IntSize.Zero) }

    var sliderSensitivity by remember { mutableFloatStateOf(1.0f - settingsViewModel.tempGazeThreshold.floatValue) }
    var sliderYawThreshold by remember { mutableFloatStateOf(settingsViewModel.tempYawThreshold.floatValue) }
    var sliderPitchThreshold by remember { mutableFloatStateOf(settingsViewModel.tempPitchThreshold.floatValue) }

    LaunchedEffect(Unit) {
        eyeWatchController.setOnFacesDetectedListener { result ->
            largestFace = result.faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
            sourceSize = IntSize(result.sourceWidth, result.sourceHeight)
        }
        eyeWatchController.startCamera(
            minFaceSize = 0.5f, // Fixed value
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
            // Top 50%: Camera Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.5f)
                    .onSizeChanged { previewSize = it }
            ) {
                AndroidView({ previewView }, modifier = Modifier.fillMaxSize())
                Canvas(modifier = Modifier.fillMaxSize()) {
                    largestFace?.let { face ->
                        if (previewSize == IntSize.Zero || sourceSize == IntSize.Zero) return@let

                        val (scaleX, scaleY) = calculateScaleFactors(
                            sourceWidth = sourceSize.width, sourceHeight = sourceSize.height,
                            previewWidth = previewSize.width, previewHeight = previewSize.height
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

                        val eyesAreOpen = (face.leftEyeOpenProbability ?: 0f) > settingsViewModel.tempGazeThreshold.floatValue &&
                                      (face.rightEyeOpenProbability ?: 0f) > settingsViewModel.tempGazeThreshold.floatValue

                        val headIsFacingForward = abs(face.headEulerAngleY) < sliderYawThreshold &&
                                abs(face.headEulerAngleX) < sliderPitchThreshold

                        if (eyesAreOpen && headIsFacingForward) {
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

            // Bottom 50%: Controls with a solid background
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.5f),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // This inner column contains only the sliders and is scrollable
                    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        SettingItem(
                            icon = Icons.Default.Visibility,
                            title = "Eye Open Sensitivity",
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

                        SettingItem(
                            icon = Icons.Default.ScreenRotation,
                            title = "Head Yaw Threshold (Left/Right)",
                            description = "Max angle to be considered 'looking forward'.",
                            valueLabel = { Text("${sliderYawThreshold.toInt()}°", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                        ) {
                            Slider(
                                value = sliderYawThreshold,
                                onValueChange = { 
                                    sliderYawThreshold = it
                                    settingsViewModel.onTempYawThresholdChanged(it) 
                                },
                                valueRange = 5f..45f
                            )
                        }

                        SettingItem(
                            icon = Icons.Default.ScreenRotation,
                            title = "Head Pitch Threshold (Up/Down)",
                            description = "Max angle to be considered 'looking forward'.",
                            valueLabel = { Text("${sliderPitchThreshold.toInt()}°", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                        ) {
                            Slider(
                                value = sliderPitchThreshold,
                                onValueChange = { 
                                    sliderPitchThreshold = it
                                    settingsViewModel.onTempPitchThresholdChanged(it) 
                                },
                                valueRange = 5f..45f
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp)) // Fixed spacer

                    // Button is outside the scrollable column, anchored to the bottom
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
}

private fun calculateScaleFactors(
    sourceWidth: Int, sourceHeight: Int,
    previewWidth: Int, previewHeight: Int
): Pair<Float, Float> {
    // Since the app is locked to portrait, the camera source is landscape.
    val width = sourceHeight.toFloat()
    val height = sourceWidth.toFloat()
    val scaleX = previewWidth.toFloat() / width
    val scaleY = previewHeight.toFloat() / height
    return scaleX to scaleY
}
