package com.helloanwar.tubify.utils

import java.util.Random


object VideoIdsProvider {
    private val videoIds =
        arrayOf<String>("-e_3Cg9GZFU", "LvetJ9U_tVY", "S0Q4gqBUs7c", "kqSdQq5bklE", "n365C9NbbC4")
    private val liveVideoIds = arrayOf<String>("hHW1oY26kxQ")
    private val random: Random = Random()

    val nextVideoId: String
        get() = videoIds[random.nextInt(videoIds.size)]

    val nextLiveVideoId: String
        get() = liveVideoIds[random.nextInt(liveVideoIds.size)]
}