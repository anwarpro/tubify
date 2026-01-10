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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.FullscreenListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

sealed interface PlayerSource {
    data class Video(val videoId: String) : PlayerSource
    data class Playlist(val playlistId: String) : PlayerSource
}

@Composable
fun YouTubePlayer(
    playerSource: PlayerSource,
    modifier: Modifier = Modifier,
    playerController: PlayerController? = null,
    onStateChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current

    // We need the activity to handle full-screen mode if required, 
    // although for purely background playback strictly, fullscreen logic 
    // might be secondary, but we keep it to match original capabilities.
    val activity = remember(context) { context.findActivity() }

    var youTubePlayer by remember { mutableStateOf<YouTubePlayer?>(null) }
    var playerView by remember { mutableStateOf<YouTubePlayerView?>(null) }
    var fullScreenView by remember { mutableStateOf<View?>(null) }

    // Force recreation when switching to/from a playlist or between playlists
    // This allows IFramePlayerOptions to be re-applied correctly
    val key = remember(playerSource) {
        if (playerSource is PlayerSource.Playlist) "playlist_${playerSource.playlistId}"
        else "video_player" // Keep same player for video-to-video transitions if desired
    }

    androidx.compose.runtime.key(key) {
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                YouTubePlayerView(ctx).apply {
                    playerView = this
                    // CRITICAL: Disable automatic initialization to manually control lifecycle
                    enableAutomaticInitialization = false
                    // Enable background playback
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
                            playerController?.setPlayer(player)
                            // Initial load only if it's a video. 
                            // Playlist is handled by IFramePlayerOptions.
                            if (playerSource is PlayerSource.Video) {
                                loadSource(player, playerSource)
                            }
                        }

                        override fun onStateChange(
                            player: YouTubePlayer,
                            state: com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants.PlayerState
                        ) {
                            when (state) {
                                com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants.PlayerState.PLAYING -> onStateChange(true)
                                com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants.PlayerState.PAUSED,
                                com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants.PlayerState.ENDED -> onStateChange(false)
                                else -> {} 
                            }
                        }
                    }

                    // Initialize without passing the lifecycle to the view itself.
                    // This prevents the view from automatically pausing the player onStop.
                    initialize(listener, options)

                    // Optional: Restore fullscreen listener if needed
                    addFullscreenListener(object : FullscreenListener {
                        override fun onEnterFullscreen(view: View, exitFullscreen: () -> Unit) {
                            activity?.let { act ->
                                val decor = act.window.decorView as? ViewGroup
                                decor?.addView(
                                    view, ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                )
                                fullScreenView = view

                                // Set orientation to landscape
                                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

                                // Hide status and navigation bars
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

                                // Set orientation back to portrait
                                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

                                // Show status and navigation bars
                                WindowCompat.getInsetsController(act.window, act.window.decorView).apply {
                                    show(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
                                }
                            }
                        }
                    })
                }
            },
            onRelease = {
                // AndroidView's onRelease is called when the View is detached.
                // equivalent to onDispose logic often.
                it.release()
            }
        )
    }

    // React to source changes ONLY for Video-to-Video
    // (Playlist changes will trigger outer key recreation)
    LaunchedEffect(playerSource) {
        if (playerSource is PlayerSource.Video) {
             youTubePlayer?.let { player ->
                loadSource(player, playerSource)
            }
        }
    }

    // Handle cleanup when Composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            playerView?.release()

            // Clean up fullscreen view if active
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


