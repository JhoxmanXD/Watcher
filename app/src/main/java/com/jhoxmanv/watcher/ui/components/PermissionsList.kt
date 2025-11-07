package com.jhoxmanv.watcher.ui.components

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.jhoxmanv.watcher.service.DeviceAdmin

@Composable
fun PermissionsList(
    onGrantCameraPermission: () -> Unit,
    onGrantOverlayPermission: () -> Unit,
    onGrantDeviceAdminPermission: () -> Unit
) {
    val context = LocalContext.current
    val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val componentName = ComponentName(context, DeviceAdmin::class.java)

    val permissions = listOf(
        "Camera" to Manifest.permission.CAMERA,
        "Draw Overlays" to Manifest.permission.SYSTEM_ALERT_WINDOW,
        "Device Admin" to "Device Admin"
    )

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Permissions needed:", modifier = Modifier.padding(bottom = 8.dp))

        permissions.forEach { (name, permission) ->
            val isGranted = when (permission) {
                Manifest.permission.SYSTEM_ALERT_WINDOW -> Settings.canDrawOverlays(context)
                "Device Admin" -> devicePolicyManager.isAdminActive(componentName)
                else -> ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }

            Row(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(text = "$name: ${if (isGranted) "Granted" else "Not Granted"}", modifier = Modifier.weight(1f))
                if (!isGranted) {
                    Button(onClick = {
                        when (permission) {
                            Manifest.permission.CAMERA -> onGrantCameraPermission()
                            Manifest.permission.SYSTEM_ALERT_WINDOW -> onGrantOverlayPermission()
                            "Device Admin" -> onGrantDeviceAdminPermission()
                        }
                    }) {
                        Text("Grant")
                    }
                }
            }
        }
    }
}