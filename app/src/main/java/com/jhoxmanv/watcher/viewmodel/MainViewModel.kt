package com.jhoxmanv.watcher.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

    val allPermissionsGranted = mutableStateOf(false)

    fun checkPermissions(context: Context) {
        val basePermissions = listOf(
            Manifest.permission.CAMERA
        )

        val hasBasePermissions = basePermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

        val hasOverlayPermission = Settings.canDrawOverlays(context)

        // Notification permission is only required for Android 13 (API 33) and above
        val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Always granted on older versions
        }

        allPermissionsGranted.value = hasBasePermissions && hasOverlayPermission && hasNotificationPermission
    }
}
