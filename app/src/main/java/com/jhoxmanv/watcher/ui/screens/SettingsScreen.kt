package com.jhoxmanv.watcher.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jhoxmanv.watcher.ui.components.SettingItem

@Composable
fun SettingsScreen() {
    var faceDetectionThreshold by remember { mutableStateOf(0.5f) }
    var screenOffTime by remember { mutableStateOf(30f) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        SettingItem(
            icon = Icons.Default.Face,
            title = "Face Detection Threshold",
            description = "Controls the sensitivity of the face detection."
        ) {
            Slider(
                value = faceDetectionThreshold,
                onValueChange = { faceDetectionThreshold = it },
                valueRange = 0.1f..0.9f,
                steps = 8
            )
        }

        SettingItem(
            icon = Icons.Default.Timer,
            title = "Screen Off Time",
            description = "Time to wait before turning off the screen."
        ) {
            Slider(
                value = screenOffTime,
                onValueChange = { screenOffTime = it },
                valueRange = 1f..60f,
                steps = 59
            )
        }
    }
}
