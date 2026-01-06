package com.helloanwar.tubify.utils

import java.util.regex.Pattern

object YouTubeUrlParser {
    // Regex for Video IDs
    // Supports:
    // youtube.com/watch?v=VIDEO_ID
    // youtube.com/embed/VIDEO_ID
    // youtube.com/v/VIDEO_ID
    // youtu.be/VIDEO_ID
    // youtube.com/shorts/VIDEO_ID
    private const val VIDEO_ID_REGEX =
        "(?:youtube(?:-nocookie)?\\.com/(?:[^/\\n\\s]+/.+/|(?:v|e(?:mbed)?)/|.*[?&]v=|shorts/)|youtu\\.be/)([a-zA-Z0-9_-]{11})"
    
    // Regex for Playlist IDs
    // Supports:
    // youtube.com/playlist?list=PLAYLIST_ID
    // youtube.com/watch?v=VIDEO_ID&list=PLAYLIST_ID
    private const val PLAYLIST_ID_REGEX = "[?&]list=([a-zA-Z0-9_-]+)"

    fun getVideoId(url: String): String? {
        val pattern = Pattern.compile(VIDEO_ID_REGEX, Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(url)
        return if (matcher.find()) matcher.group(1) else null
    }

    fun getPlaylistId(url: String): String? {
        val pattern = Pattern.compile(PLAYLIST_ID_REGEX, Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(url)
        return if (matcher.find()) matcher.group(1) else null
    }
}
