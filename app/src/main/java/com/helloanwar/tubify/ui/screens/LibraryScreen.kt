package com.helloanwar.tubify.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.helloanwar.tubify.data.local.entity.PlaylistEntity
import com.helloanwar.tubify.data.local.entity.VideoEntity
import com.helloanwar.tubify.ui.viewmodel.MainViewModel

@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    onVideoClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onSignInClick: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Videos", "Playlists")

    Column(modifier = Modifier.fillMaxSize()) {
        SecondaryTabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }

        when (selectedTabIndex) {
            0 -> VideoList(viewModel = viewModel, onVideoClick = onVideoClick)
            1 -> PlaylistList(viewModel = viewModel, onPlaylistClick = onPlaylistClick)
        }
    }
}

@Composable
fun VideoList(
    viewModel: MainViewModel,
    onVideoClick: (String) -> Unit
) {
    val videos by viewModel.videos.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(videos) { video ->
            VideoItem(video = video, onClick = { onVideoClick(video.id) })
        }
    }
}

@Composable
fun PlaylistList(
    viewModel: MainViewModel,
    onPlaylistClick: (String) -> Unit
) {
    val playlists by viewModel.playlists.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(playlists) { playlist ->
            PlaylistItem(playlist = playlist, onClick = { onPlaylistClick(playlist.id) })
        }
    }
}

@Composable
fun UserPlaylistList(
    viewModel: MainViewModel,
    onPlaylistClick: (String) -> Unit,
    onSignInClick: () -> Unit
) {
    val userPlaylists by viewModel.userPlaylists.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        if (userPlaylists.isEmpty()) {
            androidx.compose.material3.Button(
                onClick = onSignInClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Sign In to view your playlists")
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(userPlaylists) { playlist ->
                UserPlaylistItem(playlist = playlist, onClick = { onPlaylistClick(playlist.id) })
            }
        }
    }
}

@Composable
fun VideoItem(video: VideoEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = video.title, style = MaterialTheme.typography.titleMedium)
            Text(text = "ID: ${video.id}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun PlaylistItem(playlist: PlaylistEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = playlist.title, style = MaterialTheme.typography.titleMedium)
            Text(text = "ID: ${playlist.id}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun UserPlaylistItem(
    playlist: com.helloanwar.tubify.data.remote.model.PlaylistItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = playlist.snippet.title, style = MaterialTheme.typography.titleMedium)
            Text(text = "ID: ${playlist.id}", style = MaterialTheme.typography.bodySmall)
            Text(
                text = "Items: ${playlist.contentDetails?.itemCount ?: 0}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
