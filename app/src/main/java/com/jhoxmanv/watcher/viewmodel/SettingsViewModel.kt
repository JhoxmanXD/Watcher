package com.jhoxmanv.watcher.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableFloatStateOf
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreferences = application.getSharedPreferences("watcher_settings", Context.MODE_PRIVATE)

    // --- Live, saved values --- //
    val minFaceSize = mutableFloatStateOf(sharedPreferences.getFloat("min_face_size", 0.4f))
    val eyeOpenProbability = mutableFloatStateOf(sharedPreferences.getFloat("eye_open_prob", 0.6f))
    val screenOffTime = mutableFloatStateOf(sharedPreferences.getFloat("screen_off_time", 10f))

    // --- Temporary values for the config screen --- //
    var tempMinFaceSize = mutableFloatStateOf(minFaceSize.floatValue)
    var tempEyeOpenProbability = mutableFloatStateOf(eyeOpenProbability.floatValue)

    // --- Functions to update temporary values --- //
    fun onTempMinFaceSizeChanged(newValue: Float) {
        tempMinFaceSize.floatValue = newValue
    }

    fun onTempEyeOpenProbabilityChanged(newValue: Float) {
        tempEyeOpenProbability.floatValue = newValue
    }

    // --- Functions to save or discard changes --- //
    fun saveGazeConfig() {
        minFaceSize.floatValue = tempMinFaceSize.floatValue
        eyeOpenProbability.floatValue = tempEyeOpenProbability.floatValue
        sharedPreferences.edit {
            putFloat("min_face_size", minFaceSize.floatValue)
            putFloat("eye_open_prob", eyeOpenProbability.floatValue)
        }
    }

    fun resetTempGazeConfig() {
        tempMinFaceSize.floatValue = minFaceSize.floatValue
        tempEyeOpenProbability.floatValue = eyeOpenProbability.floatValue
    }

    // --- Function for the main screen slider --- //
    fun onScreenOffTimeChanged(newValue: Float) {
        screenOffTime.floatValue = newValue
        sharedPreferences.edit { putFloat("screen_off_time", newValue) }
    }
}
