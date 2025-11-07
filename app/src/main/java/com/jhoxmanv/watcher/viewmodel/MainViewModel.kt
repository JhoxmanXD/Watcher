
package com.jhoxmanv.watcher.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

    val allPermissionsGranted = mutableStateOf(false)

    fun checkPermissions(context: Context) {
        val requiredPermissions = arrayOf(
            Manifest.permission.CAMERA,
        )

        val hasCameraPermission = requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

        val hasOverlayPermission = Settings.canDrawOverlays(context)

        allPermissionsGranted.value = hasCameraPermission && hasOverlayPermission
    }
}
