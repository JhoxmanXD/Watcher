package com.jhoxmanv.watcher.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel

// Data class to hold the state of all permissions
data class PermissionState(
    val hasCamera: Boolean = false,
    val hasOverlay: Boolean = false,
    val hasNotifications: Boolean = false
) {
    fun allGranted(): Boolean {
        // Notification permission is only checked on Android 13+
        val notificationsOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) hasNotifications else true
        return hasCamera && hasOverlay && notificationsOk
    }
}

class MainViewModel : ViewModel() {

    private val _permissionState = mutableStateOf(PermissionState())
    val permissionState: State<PermissionState> = _permissionState

    fun checkPermissions(context: Context) {
        val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        _permissionState.value = PermissionState(
            hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
            hasOverlay = Settings.canDrawOverlays(context),
            hasNotifications = hasNotificationPermission
        )
    }
}
