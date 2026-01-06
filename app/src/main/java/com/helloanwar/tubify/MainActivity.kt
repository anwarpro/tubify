package com.helloanwar.tubify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.helloanwar.tubify.ui.theme.TubifyTheme
import com.helloanwar.tubify.utils.VideoIdsProvider
import com.helloanwar.tubify.ui.components.YouTubePlayer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TubifyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    var videoId by remember { mutableStateOf(VideoIdsProvider.nextVideoId) }
                    Column(modifier = Modifier.padding(innerPadding)) {
                        YouTubePlayer(videoId = videoId)

                        Button(onClick = { videoId = VideoIdsProvider.nextVideoId }) {
                            Text("Play next video")
                        }
                    }
                }
            }
        }
    }
}





@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TubifyTheme {
        YouTubePlayer(videoId = "")
    }
}