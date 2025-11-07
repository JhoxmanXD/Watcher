package com.jhoxmanv.watcher.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableFloatStateOf
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreferences = application.getSharedPreferences("watcher_settings", Context.MODE_PRIVATE)

    // Default gaze sensitivity is 80% (threshold of 0.2f). Min face size is fixed at 50% (0.5f).
    private val initialGazeThreshold = sharedPreferences.getFloat("gaze_threshold", 0.2f)
    private val initialScreenOffTime = sharedPreferences.getFloat("screen_off_time", 10f)

    // --- Live, saved values --- //
    var gazeThreshold = mutableFloatStateOf(initialGazeThreshold)
    var screenOffTime = mutableFloatStateOf(initialScreenOffTime)

    // --- Temporary value for the config screen --- //
    var tempGazeThreshold = mutableFloatStateOf(gazeThreshold.floatValue)

    /**
     * The UI slider represents sensitivity from 0.0 to 1.0.
     * We convert it to a threshold where high sensitivity means a low threshold.
     * e.g., 80% sensitivity (0.8) -> 20% threshold (0.2)
     */
    fun onTempGazeSensitivityChanged(newSliderValue: Float) {
        tempGazeThreshold.floatValue = 1.0f - newSliderValue
    }

    fun saveGazeConfig() {
        gazeThreshold.floatValue = tempGazeThreshold.floatValue
        sharedPreferences.edit {
            putFloat("gaze_threshold", gazeThreshold.floatValue)
        }
    }

    fun resetTempGazeConfig() {
        tempGazeThreshold.floatValue = gazeThreshold.floatValue
    }

    fun onScreenOffTimeChanged(newValue: Float) {
        screenOffTime.floatValue = newValue
        sharedPreferences.edit { putFloat("screen_off_time", newValue) }
    }
}
