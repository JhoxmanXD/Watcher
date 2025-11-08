package com.jhoxmanv.watcher.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhoxmanv.watcher.WatcherStateHolder
import com.jhoxmanv.watcher.service.WatcherService
import com.jhoxmanv.watcher.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    settingsViewModel: SettingsViewModel, // Receive the shared ViewModel
    onShowTutorial: () -> Unit,
    onShowSettings: () -> Unit,
    onNavigateToGazeConfig: () -> Unit
) {
    val context = LocalContext.current
    val isServiceRunning by WatcherStateHolder.isServiceRunning.collectAsState()

    // Observe current settings from the shared ViewModel
    val gazeThreshold by settingsViewModel.gazeThreshold
    val screenOffTime by settingsViewModel.screenOffTime
    val sensitivityPercentage = (1.0f - gazeThreshold) * 100

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("watcherjx", fontWeight = FontWeight.Bold, fontSize = 24.sp) },
                actions = {
                    IconButton(onClick = onShowTutorial) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Tutorial")
                    }
                    IconButton(onClick = onShowSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Your screen stays on only when you look at it.",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Privacy, simplified.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Main action button
            val buttonText = if (isServiceRunning) "Stop Service" else "Start Service"
            val buttonIcon = if (isServiceRunning) Icons.Default.Stop else Icons.Default.PlayArrow
            val buttonColors = if (isServiceRunning) {
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            } else {
                ButtonDefaults.buttonColors()
            }

            Button(
                onClick = {
                    val intent = Intent(context, WatcherService::class.java)
                    if (isServiceRunning) {
                        context.stopService(intent)
                    } else {
                        context.startService(intent)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(60.dp),
                colors = buttonColors,
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(buttonIcon, contentDescription = buttonText)
                Spacer(modifier = Modifier.width(8.dp))
                Text(buttonText, fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Status Panel
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Current Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Gaze Sensitivity")
                        Text("${sensitivityPercentage.toInt()}%", fontWeight = FontWeight.Bold)
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Screen Off Timer")
                        Text("${screenOffTime.toInt()}s", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f)) // Pushes the next item to the bottom

            // Direct link to Gaze Configuration
            TextButton(onClick = onNavigateToGazeConfig) {
                Text("Calibrate Gaze Detection")
            }
        }
    }
}
