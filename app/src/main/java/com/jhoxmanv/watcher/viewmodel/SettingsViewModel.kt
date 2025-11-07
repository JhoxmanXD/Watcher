package com.jhoxmanv.watcher.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class SettingsViewModel : ViewModel() {
    val faceDetectionThreshold = mutableStateOf(0.5f)
    val screenOffTime = mutableStateOf(30f)
}
