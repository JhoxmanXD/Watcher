package com.jhoxmanv.watcher.ui.components

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jhoxmanv.watcher.viewmodel.PermissionState

@Composable
fun PermissionsList(
    permissionState: PermissionState,
    onGrantCameraPermission: () -> Unit,
    onGrantOverlayPermission: () -> Unit,
    onGrantNotificationPermission: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Permissions needed:", modifier = Modifier.padding(bottom = 8.dp))

        PermissionRow(
            name = "Camera",
            isGranted = permissionState.hasCamera,
            onClick = onGrantCameraPermission
        )
        PermissionRow(
            name = "Draw Overlays",
            isGranted = permissionState.hasOverlay,
            onClick = onGrantOverlayPermission
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionRow(
                name = "Notifications",
                isGranted = permissionState.hasNotifications,
                onClick = onGrantNotificationPermission
            )
        }
    }
}

@Composable
private fun PermissionRow(name: String, isGranted: Boolean, onClick: () -> Unit) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = "$name: ${if (isGranted) "Granted" else "Not Granted"}", modifier = Modifier.weight(1f))
        if (!isGranted) {
            Button(onClick = onClick) {
                Text("Grant")
            }
        }
    }
}
