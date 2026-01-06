import java.util.regex.Pattern

object YouTubeUrlParser {
    private const val VIDEO_ID_REGEX =
        "(?:youtube(?:-nocookie)?\\.com/(?:[^/\\n\\s]+/.+/|(?:v|e(?:mbed)?)/|.*[?&]v=|shorts/)|youtu\\.be/)([a-zA-Z0-9_-]{11})"
    
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

fun main() {
    val testCases = listOf(
        "https://www.youtube.com/watch?v=dQw4w9WgXcQ" to "dQw4w9WgXcQ",
        "https://youtu.be/dQw4w9WgXcQ" to "dQw4w9WgXcQ",
        "https://www.youtube.com/shorts/dQw4w9WgXcQ" to "dQw4w9WgXcQ",
        "https://youtube.com/playlist?list=PL1234567890" to null,
    )

    val playlistCases = listOf(
        "https://youtube.com/playlist?list=PL1234567890" to "PL1234567890",
        "https://www.youtube.com/watch?v=dQw4w9WgXcQ&list=PL1234567890" to "PL1234567890"
    )

    println("Testing Video IDs:")
    testCases.forEach { (url, expected) ->
        val actual = YouTubeUrlParser.getVideoId(url)
        if (actual == expected) {
            println("PASS: $url -> $actual")
        } else {
            println("FAIL: $url -> Expected $expected, got $actual")
        }
    }

    println("\nTesting Playlist IDs:")
    playlistCases.forEach { (url, expected) ->
        val actual = YouTubeUrlParser.getPlaylistId(url)
        if (actual == expected) {
            println("PASS: $url -> $actual")
        } else {
            println("FAIL: $url -> Expected $expected, got $actual")
        }
    }
}
