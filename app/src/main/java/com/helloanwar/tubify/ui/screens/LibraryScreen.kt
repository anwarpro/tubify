package com.helloanwar.tubify.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
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
    val searchQuery by viewModel.searchQuery.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(end = 16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                placeholder = { Text("Search your library...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )
            
            IconButton(onClick = onSignInClick) {
                Icon(Icons.Default.AccountCircle, contentDescription = "Login to YouTube")
            }
        }

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

    if (videos.isEmpty()) {
        EmptyLibraryContent(type = "videos")
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(videos) { video ->
                VideoItem(
                    video = video,
                    onClick = { onVideoClick(video.id) },
                    onDelete = { viewModel.deleteVideo(video) }
                )
            }
        }
    }
}

@Composable
fun PlaylistList(
    viewModel: MainViewModel,
    onPlaylistClick: (String) -> Unit
) {
    val playlists by viewModel.playlists.collectAsState()

    if (playlists.isEmpty()) {
        EmptyLibraryContent(type = "playlists")
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(playlists) { playlist ->
                PlaylistItem(
                    playlist = playlist,
                    onClick = { onPlaylistClick(playlist.id) },
                    onDelete = { viewModel.deletePlaylist(playlist) }
                )
            }
        }
    }
}

@Composable
fun EmptyLibraryContent(type: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Your $type library is empty",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "To add $type, open YouTube, click on Share, and choose Tubify.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
fun VideoItem(video: VideoEntity, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = video.title, style = MaterialTheme.typography.titleMedium)
                Text(text = "ID: ${video.id}", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun PlaylistItem(playlist: PlaylistEntity, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = playlist.title, style = MaterialTheme.typography.titleMedium)
                Text(text = "ID: ${playlist.id}", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
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
