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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // We need the activity to handle full-screen mode if required, 
    // although for purely background playback strictly, fullscreen logic 
    // might be secondary, but we keep it to match original capabilities.
    val activity = remember(context) { context.findActivity() }

    var youTubePlayer by remember { mutableStateOf<YouTubePlayer?>(null) }
    var playerView by remember { mutableStateOf<YouTubePlayerView?>(null) }
    var fullScreenView by remember { mutableStateOf<View?>(null) }

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
                        // Initial load
                        loadSource(player, playerSource)
                    }
                }

                // Initialize without passing the lifecycle to the view itself.
                // This prevents the view from automatically pausing the player onStop.
                initialize(listener, options)

                // Optional: Restore fullscreen listener if needed
                addFullscreenListener(object : FullscreenListener {
                    override fun onEnterFullscreen(view: View, exitFullscreen: () -> Unit) {
                        val decor = activity?.window?.decorView as? ViewGroup
                        decor?.addView(
                            view, ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        )
                        fullScreenView = view
                    }

                    override fun onExitFullscreen() {
                        val decor = activity?.window?.decorView as? ViewGroup
                        fullScreenView?.let { decor?.removeView(it) }
                        fullScreenView = null
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

    // React to source changes
    LaunchedEffect(playerSource) {
        youTubePlayer?.let { player ->
            loadSource(player, playerSource)
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

