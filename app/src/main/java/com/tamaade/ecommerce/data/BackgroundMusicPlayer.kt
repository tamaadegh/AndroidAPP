package com.tamaade.ecommerce.data

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import com.tamaade.ecommerce.data.model.MusicTrack

/**
 * Looping background music driven by three switches: a track from the backend,
 * the user's preference, and whether the app is in the foreground.
 * Must be used from the main thread (MediaPlayer callbacks arrive there too).
 */
class BackgroundMusicPlayer {
    private var player: MediaPlayer? = null
    private var prepared = false
    private var track: MusicTrack? = null
    private var userEnabled = true
    private var inForeground = false

    fun setTrack(track: MusicTrack?) {
        if (track?.url != this.track?.url) releasePlayer()
        this.track = track
        sync()
    }

    fun setUserEnabled(enabled: Boolean) {
        userEnabled = enabled
        // Switching off releases the player so nothing keeps streaming in the background.
        if (!enabled) releasePlayer()
        sync()
    }

    fun onForeground() {
        inForeground = true
        sync()
    }

    fun onBackground() {
        inForeground = false
        sync()
    }

    fun release() {
        track = null
        releasePlayer()
    }

    private fun sync() {
        val current = track
        val shouldPlay = current != null && userEnabled && inForeground
        val existing = player
        when {
            shouldPlay && existing == null -> createPlayer(current!!)
            shouldPlay && prepared && existing?.isPlaying == false -> existing.start()
            !shouldPlay && prepared && existing?.isPlaying == true -> existing.pause()
        }
    }

    private fun createPlayer(track: MusicTrack) {
        val mp = MediaPlayer()
        player = mp
        prepared = false
        try {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            mp.isLooping = true
            mp.setVolume(track.volume, track.volume)
            mp.setOnPreparedListener {
                if (player === it) {
                    prepared = true
                    sync()
                }
            }
            mp.setOnErrorListener { failed, what, extra ->
                Log.w(TAG, "Background music error what=$what extra=$extra")
                if (player === failed) releasePlayer()
                true
            }
            mp.setDataSource(track.url)
            mp.prepareAsync()
        } catch (e: Exception) {
            Log.w(TAG, "Could not start background music", e)
            releasePlayer()
        }
    }

    private fun releasePlayer() {
        player?.let {
            try {
                it.reset()
            } catch (_: IllegalStateException) {
            }
            it.release()
        }
        player = null
        prepared = false
    }

    private companion object {
        const val TAG = "BackgroundMusic"
    }
}
