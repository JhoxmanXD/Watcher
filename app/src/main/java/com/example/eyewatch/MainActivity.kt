package com.example.eyewatch

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * AppLogger centralizado: logs solo si BuildConfig.DEBUG && AppLogger.enabled == true.
 */
object AppLogger {
    @JvmStatic
    var enabled: Boolean = BuildConfig.DEBUG

    @JvmStatic
    fun d(tag: String, msg: String) {
        if (enabled) android.util.Log.d(tag, msg)
    }

    @JvmStatic
    fun w(tag: String, msg: String) {
        if (enabled) android.util.Log.w(tag, msg)
    }

    @JvmStatic
    fun e(tag: String, msg: String, t: Throwable? = null) {
        if (enabled) {
            if (t != null) android.util.Log.e(tag, msg, t) else android.util.Log.e(tag, msg)
        }
    }
}

/**
 * MainActivity: UI + Camera binding. La lógica de detección/apagado está en EyeWatchController.
 */
class MainActivity : ComponentActivity() {

    private lateinit var cameraExecutor: ExecutorService

    // UI states (Compose)
    private val isLookingState = mutableStateOf(false)
    private val cameraPermissionState = mutableStateOf(false)
    private val overlayPermissionState = mutableStateOf(false)

    // Toggle para activar/desactivar detección (controla AppLogger.enabled)
    private val detectionEnabledState = mutableStateOf(true)

    // Fallback in-app si el overlay sistema no se puede añadir
    private val overlayFallbackState = mutableStateOf(false)

    // Umbrales
    private val thresholdYaw = AtomicFloat(9f)
    private val thresholdPitch = AtomicFloat(20f)

    // Overlay controller
    private lateinit var overlayController: OverlayController

    // Controlador que agrupa la lógica de muestreo/monitor/overlay
    private lateinit var eyeWatchController: EyeWatchController

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            cameraPermissionState.value = isGranted
            if (isGranted) startCameraIfReady()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        overlayController = OverlayController(this)

        overlayPermissionState.value = overlayController.hasPermission()
        cameraPermissionState.value = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        // Inicializar controlador de lógica (le pasamos los estados que necesita)
        eyeWatchController = EyeWatchController(
            activity = this,
            lifecycleScope = lifecycleScope,
            overlayController = overlayController,
            isLookingState = isLookingState,
            detectionEnabledState = detectionEnabledState,
            overlayFallbackState = overlayFallbackState,
            thresholdYaw = thresholdYaw,
            thresholdPitch = thresholdPitch
        )

        // Sincronizar logger con el estado inicial del switch
        AppLogger.enabled = BuildConfig.DEBUG && detectionEnabledState.value

        setContent {
            val backgroundBrush = Brush.verticalGradient(listOf(Color(0xFFF3F6FB), Color(0xFFEAF0F7)))
            var uiYaw by remember { mutableStateOf(thresholdYaw.get()) }
            var uiPitch by remember { mutableStateOf(thresholdPitch.get()) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundBrush)
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "EyeWatch — Detector de Mirada (Beta)",
                        fontSize = 18.sp,
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF0F172A)
                    )

                    if (!cameraPermissionState.value) {
                        Button(onClick = { requestCameraPermission() }) { Text("Permiso Cámara") }
                    } else {
                        Text("Cámara: OK", color = Color(0xFF065F46))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    if (!overlayPermissionState.value) {
                        Button(onClick = { tryOpenOverlaySettings(this@MainActivity) }) { Text("Permiso Overlay") }
                    } else {
                        Text("Overlay: OK", color = Color(0xFF065F46))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Switch animado para habilitar/deshabilitar detección
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Detección:", modifier = Modifier.padding(end = 8.dp))

                    val enabled = detectionEnabledState.value

                    Switch(
                        checked = enabled,
                        onCheckedChange = {
                            detectionEnabledState.value = it
                            AppLogger.enabled = BuildConfig.DEBUG && detectionEnabledState.value
                            onDetectionToggled(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF065F46),
                            checkedTrackColor = Color(0xFF9AE6B4),
                            uncheckedThumbColor = Color(0xFFB91C1C),
                            uncheckedTrackColor = Color(0xFFF9B6B6)
                        )
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    val stateColor by animateColorAsState(
                        targetValue = if (enabled) Color(0xFF065F46) else Color(0xFFE74C3C),
                        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                    )
                    Text(
                        text = if (enabled) "ACTIVA" else "INACTIVA",
                        color = stateColor,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Card con animación de color según isLookingState
                val cardColor by animateColorAsState(
                    targetValue = if (isLookingState.value) Color(0xFF2ECC71) else Color(0xFFE74C3C),
                    animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing)
                )

                Card(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(cardColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = if (isLookingState.value) "MIRANDO" else "NO MIRANDO", fontSize = 28.sp, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "Umbral Yaw (horiz): ${uiYaw.toInt()}°", fontSize = 14.sp)
                Slider(value = uiYaw, onValueChange = {
                    uiYaw = it
                    thresholdYaw.set(it)
                }, valueRange = 0f..45f, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Umbral Pitch (vert): ${uiPitch.toInt()}°", fontSize = 14.sp)
                Slider(value = uiPitch, onValueChange = {
                    uiPitch = it
                    thresholdPitch.set(it)
                }, valueRange = 0f..45f, modifier = Modifier.fillMaxWidth())
            }

            // fallback in-app: si no hay permiso o showOverlay falló y la lógica pide mostrar apagado
            if ((!overlayPermissionState.value || overlayFallbackState.value) && eyeWatchController.overlayShouldBeActive()) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black))
            }
        }

        // arrancar la lógica (watcher + monitor) — el controlador internamente vigila detectionEnabledState
        eyeWatchController.start()
    }

    override fun onResume() {
        super.onResume()
        if (cameraPermissionState.value) startCameraIfReady()

        overlayPermissionState.value = overlayController.hasPermission()

        // quitamos overlays al reanudar; watcher/monitor decidirán luego
        try { overlayController.hideOverlay() } catch (_: Throwable) {}
        eyeWatchController.resetOverlayFlagsOnResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        eyeWatchController.stop()
    }

    private fun requestCameraPermission() {
        requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun onDetectionToggled(enabled: Boolean) {
        // actualizar UI y avisar al controller
        if (!enabled) {
            // desactivar: dejar todo en estado seguro
            eyeWatchController.stop()
            isLookingState.value = false
        } else {
            // activar: asumimos mirando hasta primera muestra
            isLookingState.value = true
            // el controller ya escucha detectionEnabledState así que retomará
        }
    }

    private fun startCameraIfReady() {
        if (!cameraPermissionState.value) return

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                val analyzer = DynamicFaceAnalyzer(
                    getYaw = { thresholdYaw.get() },
                    getPitch = { thresholdPitch.get() },
                    onFrame = { isLooking ->
                        // delegar al controlador
                        eyeWatchController.onFrame(isLooking)
                    }
                )

                imageAnalysis.setAnalyzer(cameraExecutor, analyzer)

                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis)

                // notificar al controlador que la cámara quedó ligada
                eyeWatchController.setCameraBound(true)
                AppLogger.d("EyeWatch", "Camera bound and analyzer running")
            } catch (e: Exception) {
                AppLogger.e("EyeWatchApp", "Error al iniciar la cámara", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun tryOpenOverlaySettings(activity: Activity) {
        try {
            val intent = Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${activity.packageName}"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            activity.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            try {
                val i = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                i.data = Uri.parse("package:${activity.packageName}")
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                activity.startActivity(i)
            } catch (t: Exception) {
                AppLogger.e("OverlayPerm", "No se pudo abrir settings de overlay", t)
            }
        }
    }
}

/**
 * AtomicFloat ligero implementado sobre AtomicInteger.
 */
class AtomicFloat(initial: Float = 0f) {
    private val bits = java.util.concurrent.atomic.AtomicInteger(java.lang.Float.floatToIntBits(initial))
    fun get(): Float = java.lang.Float.intBitsToFloat(bits.get())
    fun set(value: Float) = bits.set(java.lang.Float.floatToIntBits(value))
}
