package com.jhoxmanv.watcher.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import com.jhoxmanv.watcher.R

class OverlayAudioGrip(private val ctx: Context) {
    private val audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var mediaPlayer: MediaPlayer? = null
    private var focusRequest: AudioFocusRequest? = null
    private var afChangeListener: AudioManager.OnAudioFocusChangeListener? = null

    companion object {
        private const val TAG = "OverlayAudioGrip"
    }

    fun acquire() {
        if (focusRequest != null || afChangeListener != null) return
        Log.d(TAG, "Acquiring audio grip...")

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        afChangeListener = AudioManager.OnAudioFocusChangeListener { }

        val result: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attrs)
                .setOnAudioFocusChangeListener(afChangeListener!!)
                .build()
            result = audioManager.requestAudioFocus(focusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            result = audioManager.requestAudioFocus(
                afChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )
        }

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.d(TAG, "Audio focus granted. Starting silent media player.")
            mediaPlayer = MediaPlayer.create(ctx, R.raw.silence)
            mediaPlayer?.isLooping = true
            mediaPlayer?.setVolume(0f, 0f)
            try {
                mediaPlayer?.start()
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Error starting silent media player", e)
                release()
            }
        } else {
            Log.w(TAG, "Audio focus request denied.")
            afChangeListener = null
        }
    }

    fun release() {
        Log.d(TAG, "Releasing audio grip...")
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Error stopping media player", e)
        }
        mediaPlayer?.release()
        mediaPlayer = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            afChangeListener?.let {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(it)
            }
        }
        focusRequest = null
        afChangeListener = null
    }
}