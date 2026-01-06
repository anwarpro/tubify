package com.helloanwar.tubify.data.local

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var lastPlayedType: String
        get() = prefs.getString(KEY_LAST_PLAYED_TYPE, TYPE_VIDEO) ?: TYPE_VIDEO
        set(value) = prefs.edit().putString(KEY_LAST_PLAYED_TYPE, value).apply()

    var lastPlayedId: String
        get() = prefs.getString(KEY_LAST_PLAYED_ID, DEFAULT_VIDEO_ID) ?: DEFAULT_VIDEO_ID
        set(value) = prefs.edit().putString(KEY_LAST_PLAYED_ID, value).apply()

    companion object {
        private const val PREFS_NAME = "tubify_prefs"
        private const val KEY_LAST_PLAYED_TYPE = "last_played_type"
        private const val KEY_LAST_PLAYED_ID = "last_played_id"

        const val TYPE_VIDEO = "video"
        const val TYPE_PLAYLIST = "playlist"
        // Starting video ID if nothing is saved
        const val DEFAULT_VIDEO_ID = "dQw4w9WgXcQ" 
    }
}
