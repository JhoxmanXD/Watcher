package com.jhoxmanv.watcher.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableFloatStateOf
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreferences = application.getSharedPreferences("watcher_settings", Context.MODE_PRIVATE)

    // --- Defaults as requested by the user --- //
    private val defaultGazeThreshold = 0.2f      // Corresponds to 80% sensitivity (1.0 - 0.8)
    private val defaultYawThreshold = 20f
    private val defaultPitchThreshold = 20f
    private val defaultScreenOffTime = 1f

    // --- Live, saved values --- //
    var gazeThreshold = mutableFloatStateOf(sharedPreferences.getFloat("gaze_threshold", defaultGazeThreshold))
    var yawThreshold = mutableFloatStateOf(sharedPreferences.getFloat("yaw_threshold", defaultYawThreshold))
    var pitchThreshold = mutableFloatStateOf(sharedPreferences.getFloat("pitch_threshold", defaultPitchThreshold))
    var screenOffTime = mutableFloatStateOf(sharedPreferences.getFloat("screen_off_time", defaultScreenOffTime))

    // --- Temporary values for the config screen --- //
    var tempGazeThreshold = mutableFloatStateOf(gazeThreshold.floatValue)
    var tempYawThreshold = mutableFloatStateOf(yawThreshold.floatValue)
    var tempPitchThreshold = mutableFloatStateOf(pitchThreshold.floatValue)

    // --- Functions to update temporary values --- //
    fun onTempGazeSensitivityChanged(newSliderValue: Float) {
        // UI slider is 0.0 to 1.0. High sensitivity means a low threshold.
        tempGazeThreshold.floatValue = 1.0f - newSliderValue
    }

    fun onTempYawThresholdChanged(newValue: Float) {
        tempYawThreshold.floatValue = newValue
    }

    fun onTempPitchThresholdChanged(newValue: Float) {
        tempPitchThreshold.floatValue = newValue
    }

    // --- Functions to save or discard changes --- //
    fun saveGazeConfig() {
        gazeThreshold.floatValue = tempGazeThreshold.floatValue
        yawThreshold.floatValue = tempYawThreshold.floatValue
        pitchThreshold.floatValue = tempPitchThreshold.floatValue
        sharedPreferences.edit {
            putFloat("gaze_threshold", gazeThreshold.floatValue)
            putFloat("yaw_threshold", yawThreshold.floatValue)
            putFloat("pitch_threshold", pitchThreshold.floatValue)
        }
    }

    fun resetTempGazeConfig() {
        tempGazeThreshold.floatValue = gazeThreshold.floatValue
        tempYawThreshold.floatValue = yawThreshold.floatValue
        tempPitchThreshold.floatValue = pitchThreshold.floatValue
    }

    // --- Function for the main screen slider --- //
    fun onScreenOffTimeChanged(newValue: Float) {
        screenOffTime.floatValue = newValue
        sharedPreferences.edit { putFloat("screen_off_time", newValue) }
    }
}
