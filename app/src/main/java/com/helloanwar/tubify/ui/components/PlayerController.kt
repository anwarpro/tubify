package com.helloanwar.tubify.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer

class PlayerController {
    private var player: YouTubePlayer? = null

    fun setPlayer(player: YouTubePlayer) {
        this.player = player
    }

    fun play() {
        player?.play()
    }

    fun pause() {
        player?.pause()
    }

    fun seekTo(seconds: Float) {
        player?.seekTo(seconds)
    }

    fun nextVideo() {
        player?.nextVideo()
    }

    fun previousVideo() {
        player?.previousVideo()
    }
}

@Composable
fun rememberPlayerController(): PlayerController {
    return remember { PlayerController() }
}
