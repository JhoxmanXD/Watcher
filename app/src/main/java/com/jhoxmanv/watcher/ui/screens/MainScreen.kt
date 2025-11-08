package com.jhoxmanv.watcher.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhoxmanv.watcher.WatcherStateHolder
import com.jhoxmanv.watcher.service.WatcherService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onShowTutorial: () -> Unit,
    onShowSettings: () -> Unit
) {
    val context = LocalContext.current
    // Observe the service running state from the global state holder
    val isServiceRunning by WatcherStateHolder.isServiceRunning.collectAsState()

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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
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
                    // No need to manually toggle the state here anymore
                },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(60.dp),
                colors = buttonColors
            ) {
                Icon(buttonIcon, contentDescription = buttonText)
                Spacer(modifier = Modifier.width(8.dp))
                Text(buttonText, fontSize = 18.sp)
            }
        }
    }
}
