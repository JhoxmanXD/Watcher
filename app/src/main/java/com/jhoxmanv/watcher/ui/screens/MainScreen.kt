package com.jhoxmanv.watcher.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jhoxmanv.watcher.service.WatcherService
import com.jhoxmanv.watcher.ui.components.InfoTooltip
import com.jhoxmanv.watcher.ui.components.SettingItem
import com.jhoxmanv.watcher.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onShowTutorial: () -> Unit) {
    val context = LocalContext.current
    val settingsViewModel: SettingsViewModel = viewModel()

    var faceDetectionThreshold by settingsViewModel.faceDetectionThreshold
    var screenOffTime by settingsViewModel.screenOffTime
    var showSettings by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("watcherjx", fontWeight = FontWeight.Bold, fontSize = 24.sp) },
                actions = {
                    IconButton(onClick = onShowTutorial) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "Tutorial")
                    }
                    IconButton(onClick = { showSettings = !showSettings }) {
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showSettings) {
                SettingsScreen(settingsViewModel = settingsViewModel)
            } else {
                Button(
                    onClick = {
                        val intent = Intent(context, WatcherService::class.java)
                        context.startService(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(60.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Start Service")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Service", fontSize = 18.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val intent = Intent(context, WatcherService::class.java)
                        context.stopService(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop Service")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stop Service", fontSize = 18.sp)
                }

                Spacer(modifier = Modifier.height(32.dp))

                LazyColumn {
                    item {
                        SettingItem(
                            icon = Icons.Default.Face,
                            title = "Face Detection Threshold",
                            description = "Controls the sensitivity of the face detection."
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Slider(
                                    value = faceDetectionThreshold,
                                    onValueChange = { faceDetectionThreshold = it },
                                    valueRange = 0.1f..0.9f,
                                    steps = 8,
                                    modifier = Modifier.weight(1f)
                                )
                                InfoTooltip("A lower value makes the detection more sensitive, but may increase false positives.")
                            }
                        }
                    }

                    item {
                        SettingItem(
                            icon = Icons.Default.Timer,
                            title = "Screen Off Time",
                            description = "Time to wait before turning off the screen."
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Slider(
                                    value = screenOffTime,
                                    onValueChange = { screenOffTime = it },
                                    valueRange = 1f..60f,
                                    steps = 59,
                                    modifier = Modifier.weight(1f)
                                )
                                InfoTooltip("The time in seconds to wait before locking the screen when no face is detected.")
                            }
                        }
                    }
                }
            }
        }
    }
}
