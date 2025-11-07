package com.jhoxmanv.watcher.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreferences = application.getSharedPreferences("watcher_settings", Context.MODE_PRIVATE)

    // Load initial values from SharedPreferences, with defaults
    private val initialThreshold = sharedPreferences.getFloat("face_threshold", 0.4f)
    private val initialScreenOffTime = sharedPreferences.getFloat("screen_off_time", 10f)

    // State holders for the UI
    var faceDetectionThreshold = mutableStateOf(initialThreshold)
    var screenOffTime = mutableStateOf(initialScreenOffTime)

    fun onFaceThresholdChanged(newValue: Float) {
        faceDetectionThreshold.value = newValue
        sharedPreferences.edit {
            putFloat("face_threshold", newValue)
        }
    }

    fun onScreenOffTimeChanged(newValue: Float) {
        screenOffTime.value = newValue
        sharedPreferences.edit {
            putFloat("screen_off_time", newValue)
        }
    }
}
