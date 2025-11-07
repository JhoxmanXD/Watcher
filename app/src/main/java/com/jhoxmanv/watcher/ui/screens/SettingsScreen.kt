package com.jhoxmanv.watcher.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jhoxmanv.watcher.ui.components.InfoTooltip
import com.jhoxmanv.watcher.ui.components.SettingItem
import com.jhoxmanv.watcher.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(settingsViewModel: SettingsViewModel) {
    val faceDetectionThreshold by settingsViewModel.faceDetectionThreshold
    val screenOffTime by settingsViewModel.screenOffTime

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        SettingItem(
            icon = Icons.Default.Face,
            title = "Face Detection Threshold",
            description = "Controls the sensitivity of the face detection.",
            valueLabel = {
                Text("${(faceDetectionThreshold * 100).toInt()}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = faceDetectionThreshold,
                    onValueChange = { settingsViewModel.onFaceThresholdChanged(it) },
                    valueRange = 0.1f..0.9f,
                    steps = 8,
                    modifier = Modifier.weight(1f)
                )
                InfoTooltip("A lower value makes the detection more sensitive, but may increase false positives.")
            }
        }

        Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

        SettingItem(
            icon = Icons.Default.Timer,
            title = "Screen Off Time",
            description = "Time to wait before turning off the screen.",
            valueLabel = {
                Text("${screenOffTime.toInt()}s", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = screenOffTime,
                    onValueChange = { settingsViewModel.onScreenOffTimeChanged(it) },
                    valueRange = 1f..60f,
                    steps = 59,
                    modifier = Modifier.weight(1f)
                )
                InfoTooltip("The time in seconds to wait before locking the screen when no face is detected.")
            }
        }
    }
}
