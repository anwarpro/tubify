package com.helloanwar.tubify.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.helloanwar.tubify.data.local.entity.PlaylistEntity
import com.helloanwar.tubify.data.local.entity.VideoEntity
import com.helloanwar.tubify.data.remote.model.PlaylistItem
import com.helloanwar.tubify.data.repository.VideoRepository
import com.helloanwar.tubify.data.repository.YouTubeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: VideoRepository,
    private val youtubeRepository: YouTubeRepository
) : ViewModel() {

    val videos: StateFlow<List<VideoEntity>> = repository.allVideos
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val playlists: StateFlow<List<PlaylistEntity>> = repository.allPlaylists
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        
    private val _userPlaylists = MutableStateFlow<List<PlaylistItem>>(emptyList())
    val userPlaylists: StateFlow<List<PlaylistItem>> = _userPlaylists.asStateFlow()
    
    fun fetchUserPlaylists() {
        viewModelScope.launch {
            youtubeRepository.getUserPlaylists().collect { result ->
                result.onSuccess { response ->
                    _userPlaylists.value = response.items
                }.onFailure { e ->
                    // Handle error (e.g. show toast or log)
                    e.printStackTrace()
                }
            }
        }
    }
}

class MainViewModelFactory(
    private val repository: VideoRepository,
    private val youtubeRepository: YouTubeRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, youtubeRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
