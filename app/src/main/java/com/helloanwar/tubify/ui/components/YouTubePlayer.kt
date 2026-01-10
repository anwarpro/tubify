package com.helloanwar.tubify.ui.components

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import android.content.pm.ActivityInfo
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.FullscreenListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.utils.YouTubePlayerTracker
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface PlayerSource {
    data class Video(val videoId: String) : PlayerSource
    data class Playlist(val playlistId: String) : PlayerSource
}

@Composable
fun YouTubePlayer(
    playerSource: PlayerSource,
    modifier: Modifier = Modifier,
    playerController: PlayerController? = null,
    playbackService: com.helloanwar.tubify.service.PlaybackService? = null,
    onPlaybackUpdate: (videoId: String, isPlaying: Boolean, currentSecond: Float, duration: Float) -> Unit = { _, _, _, _ -> }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activity = remember(context) { context.findActivity() }

    // Use persistence from service if available
    var youTubePlayer by remember { mutableStateOf<YouTubePlayer?>(playbackService?.youTubePlayer) }
    var playerView by remember { mutableStateOf<YouTubePlayerView?>(playbackService?.playerView) }
    val tracker = remember { YouTubePlayerTracker() }
    var fullScreenView by remember { mutableStateOf<View?>(null) }

    // Link service callbacks
    DisposableEffect(playbackService) {
        playbackService?.let { service ->
            service.onPlay = { playerController?.play() }
            service.onPause = { playerController?.pause() }
            service.onNext = { /* handled in MainActivity */ }
            service.onPrevious = { /* handled in MainActivity */ }
            service.onSeekTo = { pos -> playerController?.seekTo(pos / 1000f) }
        }
        onDispose {
            playbackService?.onPlay = null
            playbackService?.onPause = null
            playbackService?.onNext = null
            playbackService?.onPrevious = null
            playbackService?.onSeekTo = null
        }
    }

    val key = remember(playerSource) {
        if (playerSource is PlayerSource.Playlist) "playlist_${playerSource.playlistId}"
        else "video_player"
    }

    androidx.compose.runtime.key(key) {
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                val view = playbackService?.playerView ?: YouTubePlayerView(ctx).apply {
                    enableAutomaticInitialization = false
                    enableBackgroundPlayback(true)
                    
                    val options = IFramePlayerOptions.Builder(ctx)
                        .controls(1)
                        .fullscreen(1)
                        .autoplay(1)
                        .ivLoadPolicy(3)
                        .apply {
                            if (playerSource is PlayerSource.Playlist) {
                                this.listType("playlist")
                                    .list(playerSource.playlistId)
                            }
                        }
                        .build()

                    val listener = object : AbstractYouTubePlayerListener() {
                        override fun onReady(player: YouTubePlayer) {
                            youTubePlayer = player
                            playbackService?.youTubePlayer = player
                            playerController?.setPlayer(player)
                            player.addListener(tracker)
                            if (playerSource is PlayerSource.Video) {
                                loadSource(player, playerSource)
                            }
                        }

                        override fun onStateChange(player: YouTubePlayer, state: PlayerConstants.PlayerState) {
                            val isPlaying = state == PlayerConstants.PlayerState.PLAYING
                            onPlaybackUpdate(tracker.videoId ?: "", isPlaying, tracker.currentSecond, tracker.videoDuration)
                            playbackService?.setPlayerState(tracker.videoId ?: "", isPlaying, tracker.currentSecond, tracker.videoDuration)
                        }

                        override fun onCurrentSecond(player: YouTubePlayer, second: Float) {
                            onPlaybackUpdate(tracker.videoId ?: "", tracker.state == PlayerConstants.PlayerState.PLAYING, second, tracker.videoDuration)
                            playbackService?.setPlayerState(tracker.videoId ?: "", tracker.state == PlayerConstants.PlayerState.PLAYING, second, tracker.videoDuration)
                        }

                        override fun onVideoDuration(player: YouTubePlayer, duration: Float) {
                            onPlaybackUpdate(tracker.videoId ?: "", tracker.state == PlayerConstants.PlayerState.PLAYING, tracker.currentSecond, duration)
                            playbackService?.setPlayerState(tracker.videoId ?: "", tracker.state == PlayerConstants.PlayerState.PLAYING, tracker.currentSecond, duration)
                        }
                    }

                    initialize(listener, options)

                    addFullscreenListener(object : FullscreenListener {
                        override fun onEnterFullscreen(view: View, exitFullscreen: () -> Unit) {
                            activity?.let { act ->
                                val decor = act.window.decorView as? ViewGroup
                                decor?.addView(view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                                fullScreenView = view
                                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                WindowCompat.getInsetsController(act.window, act.window.decorView).apply {
                                    hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
                                    systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                                }
                            }
                        }

                        override fun onExitFullscreen() {
                            activity?.let { act ->
                                val decor = act.window.decorView as? ViewGroup
                                fullScreenView?.let { decor?.removeView(it) }
                                fullScreenView = null
                                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                WindowCompat.getInsetsController(act.window, act.window.decorView).apply {
                                    show(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
                                }
                            }
                        }
                    })
                }
                playbackService?.playerView = view
                playerView = view
                view
            },
            update = { view ->
                // Ensure playerController is updated if view/player already exists
                youTubePlayer?.let { playerController?.setPlayer(it) }
            },
            onRelease = {
                // Do NOT release if we want it to persist in service
                if (playbackService == null) {
                    it.release()
                } else {
                    // Detach from parent to allow re-attachment
                    (it.parent as? ViewGroup)?.removeView(it)
                }
            }
        )
    }

    LaunchedEffect(playerSource) {
        if (playerSource is PlayerSource.Video) {
            youTubePlayer?.let { player ->
                loadSource(player, playerSource)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (playbackService == null) {
                playerView?.release()
            }
            fullScreenView?.let { view ->
                val decor = activity?.window?.decorView as? ViewGroup
                decor?.removeView(view)
            }
            fullScreenView = null
        }
    }
}

private fun loadSource(player: YouTubePlayer, source: PlayerSource) {
    when (source) {
        is PlayerSource.Video -> player.loadVideo(source.videoId, 0f)
        is PlayerSource.Playlist -> {

        }
    }
}

// Helper extension to find Activity from Context
private tailrec fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}


