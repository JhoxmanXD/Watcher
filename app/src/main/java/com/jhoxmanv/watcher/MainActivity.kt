package com.jhoxmanv.watcher

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jhoxmanv.watcher.service.DeviceAdmin
import com.jhoxmanv.watcher.service.WatcherService
import com.jhoxmanv.watcher.ui.components.PermissionsList
import com.jhoxmanv.watcher.ui.screens.MainScreen
import com.jhoxmanv.watcher.ui.screens.SettingsScreen
import com.jhoxmanv.watcher.ui.screens.TutorialScreen
import com.jhoxmanv.watcher.ui.theme.WatcherTheme
import com.jhoxmanv.watcher.viewmodel.MainViewModel
import com.jhoxmanv.watcher.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { mainViewModel.checkPermissions(this) }

    private val requestOverlayLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { mainViewModel.checkPermissions(this) }

    private val requestDeviceAdminLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { mainViewModel.checkPermissions(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = getPreferences(Context.MODE_PRIVATE)
        val tutorialShown = sharedPreferences.getBoolean("tutorial_shown", false)

        setContent {
            WatcherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val startDestination = if (tutorialShown) "main" else "tutorial"

                    NavHost(navController = navController, startDestination = startDestination) {
                        composable("tutorial") {
                            TutorialScreen(onFinished = {
                                sharedPreferences.edit { putBoolean("tutorial_shown", true) }
                                navController.navigate("main") { popUpTo("tutorial") { inclusive = true } }
                            })
                        }
                        composable("main") {
                            val allPermissionsGranted by mainViewModel.allPermissionsGranted
                            if (allPermissionsGranted) {
                                MainScreen(
                                    onShowTutorial = { navController.navigate("tutorial") },
                                    onShowSettings = { navController.navigate("settings") }
                                )
                            } else {
                                PermissionsList(
                                    onGrantCameraPermission = { requestPermissionLauncher.launch(Manifest.permission.CAMERA) },
                                    onGrantOverlayPermission = { requestOverlayLauncher.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri())) },
                                    onGrantDeviceAdminPermission = {
                                        val componentName = ComponentName(this@MainActivity, DeviceAdmin::class.java)
                                        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                                            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "This app needs device admin permission to turn off the screen.")
                                        }
                                        requestDeviceAdminLauncher.launch(intent)
                                    },
                                    onGrantNotificationPermission = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    }
                                )
                            }
                        }
                        composable("settings") {
                            val settingsViewModel: SettingsViewModel = viewModel()
                            SettingsScreen(settingsViewModel = settingsViewModel)
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mainViewModel.checkPermissions(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        val intent = Intent(this, WatcherService::class.java)
        stopService(intent)
    }
}
