package com.jhoxmanv.watcher.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jhoxmanv.watcher.WatcherStateHolder
import com.jhoxmanv.watcher.service.WatcherService
import com.jhoxmanv.watcher.ui.components.InfoTooltip
import com.jhoxmanv.watcher.ui.components.SettingItem
import com.jhoxmanv.watcher.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onNavigateToGazeConfig: () -> Unit
) {
    val context = LocalContext.current
    val screenOffTime by settingsViewModel.screenOffTime

    fun onSettingsChanged() {
        if (WatcherStateHolder.isServiceRunning.value) {
            val intent = Intent(context, WatcherService::class.java)
            context.stopService(intent)
            context.startService(intent)
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Clickable item to navigate to the new Gaze Configuration screen
        SettingItem(
            icon = Icons.Default.Tune,
            title = "Configure Gaze Detection",
            description = "Adjust sensitivity and see a live preview.",
            modifier = Modifier.clickable(onClick = onNavigateToGazeConfig)
        ) { /* No content here, the whole item is clickable */ }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

        // Setting for Screen Off Time
        SettingItem(
            icon = Icons.Default.Timer,
            title = "Screen Off Time",
            description = "Time to wait before turning off the screen.",
            valueLabel = {
                Text("${screenOffTime.toInt()}s", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        ) {
            // Add a Box with padding to constrain the width of the Row
            Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Slider(
                        value = screenOffTime,
                        onValueChange = { settingsViewModel.onScreenOffTimeChanged(it) },
                        onValueChangeFinished = { onSettingsChanged() },
                        valueRange = 1f..60f,
                        steps = 59,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    InfoTooltip("The time in seconds to wait before locking the screen when no face is detected.")
                }
            }
        }
    }
}
