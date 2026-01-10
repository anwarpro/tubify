package com.helloanwar.tubify.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.helloanwar.tubify.utils.MediaSessionManager
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.utils.YouTubePlayerTracker
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PlaybackService : Service() {

    private val binder = LocalBinder()
    
    // Manage YouTubePlayerView here to keep it alive
    var playerView: YouTubePlayerView? = null
    var youTubePlayer: YouTubePlayer? = null
    private val tracker = YouTubePlayerTracker()
    
    private var mediaSessionManager: MediaSessionManager? = null

    // Callbacks from MediaSession to UI (delegated through this service)
    var onPlay: (() -> Unit)? = null
    var onPause: (() -> Unit)? = null
    var onNext: (() -> Unit)? = null
    var onPrevious: (() -> Unit)? = null
    var onSeekTo: ((Long) -> Unit)? = null
    
    // Playback state for UI
    private val _playbackState = MutableStateFlow(PlaybackInfo())
    val playbackState: StateFlow<PlaybackInfo> = _playbackState

    data class PlaybackInfo(
        val videoId: String = "",
        val isPlaying: Boolean = false,
        val currentSecond: Float = 0f,
        val duration: Float = 0f,
        val title: String = ""
    )

    inner class LocalBinder : Binder() {
        fun getService(): PlaybackService = this@PlaybackService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        initMediaSession()
    }

    private fun initMediaSession() {
        mediaSessionManager = MediaSessionManager(
            context = this,
            isService = true,
            onPlayCallback = { onPlay?.invoke() },
            onPauseCallback = { onPause?.invoke() },
            onNextCallback = { onNext?.invoke() },
            onPreviousCallback = { onPrevious?.invoke() },
            onSeekToCallback = { pos -> onSeekTo?.invoke(pos) }
        )
        mediaSessionManager?.startSession()
    }

    fun updateMetadata(title: String, duration: Long) {
        mediaSessionManager?.updateMetadata(title, duration)
        _playbackState.value = _playbackState.value.copy(title = title, duration = duration / 1000f)
    }

    fun updateState(isPlaying: Boolean, positionMs: Long) {
        mediaSessionManager?.updateState(isPlaying, positionMs)
        _playbackState.value = _playbackState.value.copy(
            isPlaying = isPlaying, 
            currentSecond = positionMs / 1000f
        )
    }

    fun setPlayerState(videoId: String, isPlaying: Boolean, currentSecond: Float, duration: Float) {
        _playbackState.value = _playbackState.value.copy(
            videoId = videoId,
            isPlaying = isPlaying,
            currentSecond = currentSecond,
            duration = duration
        )
    }

    override fun onDestroy() {
        mediaSessionManager?.release()
        playerView?.release()
        super.onDestroy()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Return START_STICKY to ensure service is restarted if killed
        return START_STICKY
    }
}
