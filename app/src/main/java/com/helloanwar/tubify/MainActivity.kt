package com.helloanwar.tubify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.helloanwar.tubify.ui.theme.TubifyTheme
import com.helloanwar.tubify.utils.VideoIdsProvider
import com.helloanwar.tubify.ui.components.YouTubePlayer
import com.helloanwar.tubify.ui.components.PlayerSource
import com.helloanwar.tubify.data.local.database.AppDatabase
import com.helloanwar.tubify.data.repository.VideoRepository
import com.helloanwar.tubify.data.local.entity.VideoEntity
import com.helloanwar.tubify.data.local.entity.PlaylistEntity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.helloanwar.tubify.ui.screens.LibraryScreen
import com.helloanwar.tubify.ui.viewmodel.MainViewModel
import com.helloanwar.tubify.ui.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity() {

    private val _playerSource = mutableStateOf<PlayerSource>(PlayerSource.Video(VideoIdsProvider.nextVideoId))
    private val _playerController = com.helloanwar.tubify.ui.components.PlayerController()
    private var mediaSessionManager: com.helloanwar.tubify.utils.MediaSessionManager? = null
    
    private lateinit var database: AppDatabase
    private lateinit var repository: VideoRepository
    
    private val mainViewModel: MainViewModel by viewModels {
        MainViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        database = AppDatabase.getDatabase(this)
        repository = VideoRepository(database.videoDao(), database.playlistDao())

        mediaSessionManager = com.helloanwar.tubify.utils.MediaSessionManager(
            context = this,
            onPlayCallback = { _playerController.play() },
            onPauseCallback = { _playerController.pause() },
            onNextCallback = {
                _playerSource.value = PlayerSource.Video(VideoIdsProvider.nextVideoId)
            },
            onPreviousCallback = { 
                _playerController.seekTo(0f)
            }
        )
        mediaSessionManager?.startSession()

        // Handle initial intent
        handleIntent(intent)

        setContent {
            TubifyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val playerSource by remember { _playerSource }
                    
                    Column(modifier = Modifier.padding(innerPadding)) {
                        YouTubePlayer(
                            playerSource = playerSource,
                            playerController = _playerController,
                            onStateChange = { isPlaying ->
                                mediaSessionManager?.updateState(isPlaying)
                                mediaSessionManager?.updateMetadata("Video Playing") // Update with actual title if available
                            }
                        )

                        LibraryScreen(
                            viewModel = mainViewModel,
                            onVideoClick = { videoId ->
                                _playerSource.value = PlayerSource.Video(videoId)
                            },
                            onPlaylistClick = { playlistId ->
                                _playerSource.value = PlayerSource.Playlist(playlistId)
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSessionManager?.release()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        if (intent?.action == android.content.Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(android.content.Intent.EXTRA_TEXT)?.let { sharedText ->
                val videoId = com.helloanwar.tubify.utils.YouTubeUrlParser.getVideoId(sharedText)
                val playlistId = com.helloanwar.tubify.utils.YouTubeUrlParser.getPlaylistId(sharedText)

                if (videoId != null) {
                    _playerSource.value = PlayerSource.Video(videoId)
                    lifecycleScope.launch {
                        repository.insertVideo(VideoEntity(id = videoId, title = "Shared Video"))
                        android.util.Log.d("TubifyDB", "Saved video: $videoId")
                    }
                } else if (playlistId != null) {
                    _playerSource.value = PlayerSource.Playlist(playlistId)
                    lifecycleScope.launch {
                        repository.insertPlaylist(PlaylistEntity(id = playlistId, title = "Shared Playlist"))
                        android.util.Log.d("TubifyDB", "Saved playlist: $playlistId")
                    }
                }
            }
        }
    }
}





@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TubifyTheme {
        YouTubePlayer(playerSource = com.helloanwar.tubify.ui.components.PlayerSource.Video(""))
    }
}