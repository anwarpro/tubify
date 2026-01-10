package com.helloanwar.tubify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.helloanwar.tubify.data.local.database.AppDatabase
import com.helloanwar.tubify.data.local.entity.PlaylistEntity
import com.helloanwar.tubify.data.local.entity.VideoEntity
import com.helloanwar.tubify.data.repository.VideoRepository
import com.helloanwar.tubify.ui.components.PlayerSource
import com.helloanwar.tubify.ui.components.YouTubePlayer
import com.helloanwar.tubify.ui.screens.LibraryScreen
import com.helloanwar.tubify.ui.theme.TubifyTheme
import com.helloanwar.tubify.ui.viewmodel.MainViewModel
import com.helloanwar.tubify.ui.viewmodel.MainViewModelFactory
import com.helloanwar.tubify.utils.VideoIdsProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.helloanwar.tubify.service.PlaybackService
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {

    private val _playerSource = mutableStateOf<PlayerSource>(PlayerSource.Video(VideoIdsProvider.nextVideoId))
    private val _playerController = com.helloanwar.tubify.ui.components.PlayerController()
    private var lastVideoId: String? = null
    private var lastSeekTime: Long = 0
    private var playbackService = mutableStateOf<PlaybackService?>(null)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as PlaybackService.LocalBinder
            playbackService.value = binder.getService()
            setupServiceCallbacks(binder.getService())
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playbackService.value = null
        }
    }
    
    private lateinit var database: AppDatabase
    private lateinit var repository: VideoRepository
    
    private lateinit var ktorClient: com.helloanwar.tubify.data.remote.KtorClient
    private lateinit var apiService: com.helloanwar.tubify.data.remote.YouTubeApiService
    private lateinit var authClient: com.helloanwar.tubify.data.auth.GoogleAuthClient
    private lateinit var youtubeRepository: com.helloanwar.tubify.data.repository.YouTubeRepository
    
    private val mainViewModel: MainViewModel by viewModels {
        MainViewModelFactory(repository, youtubeRepository)
    }

    private lateinit var userPreferences: com.helloanwar.tubify.data.local.UserPreferences
    
    private val signInLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
            lifecycleScope.launch {
                 try {
                     val account = task.await()
                     // Auth successful, fetch playlists or token
//                     mainViewModel.fetchUserPlaylists()
                 } catch (e: Exception) {
                     android.util.Log.e("Auth", "Sign-in failed", e)
                 }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        database = AppDatabase.getDatabase(this)
        repository = VideoRepository(database.videoDao(), database.playlistDao())
        
        // Manual DI
        val httpClient = com.helloanwar.tubify.data.remote.KtorClient.client
        apiService = com.helloanwar.tubify.data.remote.YouTubeApiService(httpClient)
        authClient = com.helloanwar.tubify.data.auth.GoogleAuthClient(this)
        youtubeRepository = com.helloanwar.tubify.data.repository.YouTubeRepository(apiService, authClient)
        
        userPreferences = com.helloanwar.tubify.data.local.UserPreferences(this)

        // Load initial state from preferences
        val lastType = userPreferences.lastPlayedType
        val lastId = userPreferences.lastPlayedId
        if (lastType == com.helloanwar.tubify.data.local.UserPreferences.TYPE_PLAYLIST) {
            _playerSource.value = PlayerSource.Playlist(lastId)
        } else {
            _playerSource.value = PlayerSource.Video(lastId)
        }

        // Bind to PlaybackService
        val intent = Intent(this, PlaybackService::class.java)
        startService(intent) // Ensure service stays alive
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        // Handle initial intent
        handleIntent(intent)

        setContent {
            TubifyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val playerSource by remember { _playerSource }
                    val service by remember { playbackService }
                    Column(modifier = Modifier.padding(innerPadding)) {
                        YouTubePlayer(
                            playerSource = playerSource,
                            playerController = _playerController,
                            playbackService = service,
                            onPlaybackUpdate = { videoId, isPlaying, currentSecond, duration ->
                                if (System.currentTimeMillis() - lastSeekTime > 1000L) {
                                    service?.updateState(isPlaying, (currentSecond * 1000).toLong())
                                }
                                
                                // Update metadata (title and duration)
                                val currentTitle = mainViewModel.videos.value.find { it.id == videoId }?.title 
                                    ?: (if (playerSource is PlayerSource.Playlist) "Playlist Playing" else "Video Playing")
                                
                                service?.updateMetadata(currentTitle, (duration * 1000).toLong())
                                
                                // If title is generic and we have a new videoId, try to fetch real title
                                if (videoId.isNotEmpty() && videoId != lastVideoId && (currentTitle == "Video Playing" || currentTitle == "Playlist Playing")) {
                                    lastVideoId = videoId
                                    lifecycleScope.launch {
                                        youtubeRepository.getVideoDetails(videoId).collect { result ->
                                            result.onSuccess { response ->
                                                val realTitle = response.items.firstOrNull()?.snippet?.title
                                                if (realTitle != null) {
                                                    service?.updateMetadata(realTitle, (duration * 1000).toLong())
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        )

                        LibraryScreen(
                            viewModel = mainViewModel,
                            onVideoClick = { videoId ->
                                _playerSource.value = PlayerSource.Video(videoId)
                                userPreferences.lastPlayedType = com.helloanwar.tubify.data.local.UserPreferences.TYPE_VIDEO
                                userPreferences.lastPlayedId = videoId
                                mainViewModel.viewModelScope.launch {
                                    // Optionally fetch video details here if needed
                                     // youtubeRepository.getVideoDetails(videoId)...
                                }
                            },
                            onPlaylistClick = { playlistId ->
                                _playerSource.value = PlayerSource.Playlist(playlistId)
                                userPreferences.lastPlayedType = com.helloanwar.tubify.data.local.UserPreferences.TYPE_PLAYLIST
                                userPreferences.lastPlayedId = playlistId
                            },
                            onSignInClick = {
                                signInLauncher.launch(authClient.getSignInIntent())
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)
    }

    private fun setupServiceCallbacks(service: PlaybackService) {
        service.onPlay = { _playerController.play() }
        service.onPause = { _playerController.pause() }
        service.onSeekTo = { pos -> _playerController.seekTo(pos / 1000f) }
        service.onNext = {
            val currentSource = _playerSource.value
            if (currentSource is PlayerSource.Playlist) {
                _playerController.nextVideo()
            } else if (currentSource is PlayerSource.Video) {
                val videos = mainViewModel.videos.value
                if (videos.isNotEmpty()) {
                    val currentIndex = videos.indexOfFirst { it.id == currentSource.videoId }
                    val nextIndex = if (currentIndex != -1 && currentIndex < videos.size - 1) currentIndex + 1 else 0
                    val nextVideo = videos[nextIndex]
                    _playerSource.value = PlayerSource.Video(nextVideo.id)
                    userPreferences.lastPlayedType = com.helloanwar.tubify.data.local.UserPreferences.TYPE_VIDEO
                    userPreferences.lastPlayedId = nextVideo.id
                } else {
                    _playerSource.value = PlayerSource.Video(VideoIdsProvider.nextVideoId)
                }
            }
        }
        service.onPrevious = {
            val currentSource = _playerSource.value
            if (currentSource is PlayerSource.Playlist) {
                _playerController.previousVideo()
            } else if (currentSource is PlayerSource.Video) {
                val videos = mainViewModel.videos.value
                if (videos.isNotEmpty()) {
                    val currentIndex = videos.indexOfFirst { it.id == currentSource.videoId }
                    val prevIndex = if (currentIndex > 0) currentIndex - 1 else videos.size - 1
                    val prevVideo = videos[prevIndex]
                    _playerSource.value = PlayerSource.Video(prevVideo.id)
                    userPreferences.lastPlayedType = com.helloanwar.tubify.data.local.UserPreferences.TYPE_VIDEO
                    userPreferences.lastPlayedId = prevVideo.id
                } else {
                    _playerController.seekTo(0f)
                }
            }
        }
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
                    userPreferences.lastPlayedType = com.helloanwar.tubify.data.local.UserPreferences.TYPE_VIDEO
                    userPreferences.lastPlayedId = videoId
                    lifecycleScope.launch {
                        youtubeRepository.getVideoDetails(videoId).collect { result ->
                            val title = result.map { it.items.firstOrNull()?.snippet?.title }.getOrNull() ?: "Shared Video"
                            repository.insertVideo(VideoEntity(id = videoId, title = title))
                            android.util.Log.d("TubifyDB", "Saved video: $videoId ($title)")
                        }
                    }
                } else if (playlistId != null) {
                    _playerSource.value = PlayerSource.Playlist(playlistId)
                    userPreferences.lastPlayedType = com.helloanwar.tubify.data.local.UserPreferences.TYPE_PLAYLIST
                    userPreferences.lastPlayedId = playlistId
                    lifecycleScope.launch {
                        youtubeRepository.getPlaylistDetails(playlistId).collect { result ->
                            val title = result.map { it.items.firstOrNull()?.snippet?.title }.getOrNull() ?: "Shared Playlist"
                            repository.insertPlaylist(PlaylistEntity(id = playlistId, title = title))
                            android.util.Log.d("TubifyDB", "Saved playlist: $playlistId ($title)")
                        }
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