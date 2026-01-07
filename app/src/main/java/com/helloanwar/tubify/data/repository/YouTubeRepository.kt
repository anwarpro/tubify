package com.helloanwar.tubify.data.repository

import com.helloanwar.tubify.data.auth.GoogleAuthClient
import com.helloanwar.tubify.data.remote.YouTubeApiService
import com.helloanwar.tubify.data.remote.model.PlaylistListResponse
import com.helloanwar.tubify.data.remote.model.VideoListResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class YouTubeRepository(
    private val apiService: YouTubeApiService,
    private val authClient: GoogleAuthClient
) {
    
    fun getVideoDetails(videoId: String): Flow<Result<VideoListResponse>> = flow {
        try {
            val response = apiService.getVideoDetails(videoId)
            emit(Result.success(response))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun getPlaylistDetails(playlistId: String): Flow<Result<PlaylistListResponse>> = flow {
        try {
            val response = apiService.getPlaylistDetails(playlistId)
            emit(Result.success(response))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun getUserPlaylists(): Flow<Result<PlaylistListResponse>> = flow {
        val account = authClient.getSignedInAccount()
        if (account == null) {
            emit(Result.failure(Exception("User not signed in")))
            return@flow
        }
        
        val token = authClient.getAccessToken(account)
        if (token == null) {
            emit(Result.failure(Exception("Failed to retrieve access token")))
            return@flow
        }

        try {
            val response = apiService.getUserPlaylists(token)
            emit(Result.success(response))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
