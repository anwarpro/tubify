package com.helloanwar.tubify.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class VideoListResponse(
    val items: List<VideoItem> = emptyList()
)

@Serializable
data class VideoItem(
    val id: String,
    val snippet: VideoSnippet,
    val statistics: VideoStatistics? = null
)

@Serializable
data class VideoSnippet(
    val title: String,
    val description: String,
    val thumbnails: Thumbnails,
    val channelTitle: String
)

@Serializable
data class VideoStatistics(
    val viewCount: String? = null,
    val likeCount: String? = null
)

@Serializable
data class PlaylistListResponse(
    val items: List<PlaylistItem> = emptyList()
)

@Serializable
data class PlaylistItem(
    val id: String,
    val snippet: PlaylistSnippet,
    val contentDetails: PlaylistContentDetails? = null
)

@Serializable
data class PlaylistSnippet(
    val title: String,
    val description: String,
    val thumbnails: Thumbnails,
    val channelTitle: String
)

@Serializable
data class PlaylistContentDetails(
    val itemCount: Int
)

@Serializable
data class Thumbnails(
    val default: Thumbnail? = null,
    val medium: Thumbnail? = null,
    val high: Thumbnail? = null
)

@Serializable
data class Thumbnail(
    val url: String
)
