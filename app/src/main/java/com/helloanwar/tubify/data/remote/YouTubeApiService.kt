package com.helloanwar.tubify.data.remote

import com.helloanwar.tubify.data.remote.model.PlaylistListResponse
import com.helloanwar.tubify.data.remote.model.VideoListResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.header

class YouTubeApiService(private val client: HttpClient) {

    suspend fun getVideoDetails(videoId: String): VideoListResponse {
        return client.get("videos") {
            parameter("part", "snippet,contentDetails,statistics")
            parameter("id", videoId)
        }.body()
    }

    suspend fun getPlaylistDetails(playlistId: String): PlaylistListResponse {
        return client.get("playlists") {
            parameter("part", "snippet,contentDetails")
            parameter("id", playlistId)
        }.body()
    }

    suspend fun getUserPlaylists(accessToken: String): PlaylistListResponse {
        return client.get("playlists") {
            parameter("part", "snippet,contentDetails")
            parameter("mine", "true")
            header("Authorization", "Bearer $accessToken")
        }.body()
    }
}
