package com.helloanwar.tubify.data.repository

import com.helloanwar.tubify.data.local.dao.PlaylistDao
import com.helloanwar.tubify.data.local.dao.VideoDao
import com.helloanwar.tubify.data.local.entity.PlaylistEntity
import com.helloanwar.tubify.data.local.entity.VideoEntity
import kotlinx.coroutines.flow.Flow

class VideoRepository(
    private val videoDao: VideoDao,
    private val playlistDao: PlaylistDao
) {
    val allVideos: Flow<List<VideoEntity>> = videoDao.getAllVideos()
    val allPlaylists: Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()

    suspend fun insertVideo(video: VideoEntity) {
        videoDao.insertVideo(video)
    }

    suspend fun insertPlaylist(playlist: PlaylistEntity) {
        playlistDao.insertPlaylist(playlist)
    }

    suspend fun getVideoById(id: String): VideoEntity? {
        return videoDao.getVideoById(id)
    }

    suspend fun getPlaylistById(id: String): PlaylistEntity? {
        return playlistDao.getPlaylistById(id)
    }
}
