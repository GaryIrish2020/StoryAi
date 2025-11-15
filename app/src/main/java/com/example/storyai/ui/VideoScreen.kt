package com.example.storyai.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController

@OptIn(UnstableApi::class)
@Composable
fun VideoScreen(
    navController: NavController,
    storyId: String,
    viewModel: VideoViewModel = hiltViewModel()
) {
    val storyPreset by viewModel.storyPreset.collectAsState()
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }

    LaunchedEffect(storyPreset) {
        val videoUrl = storyPreset?.intro_video_url
        Log.d("VideoScreen", "Story Preset loaded. Video URL: $videoUrl")

        if (!videoUrl.isNullOrBlank()) {
            val mediaItem = MediaItem.fromUri(videoUrl)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        } else if (storyPreset != null) {
            Log.d("VideoScreen", "No video URL found. Navigating to chat screen.")
            navController.navigate("chat/$storyId/true") {
                popUpTo("video/$storyId") { inclusive = true }
            }
        }
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    navController.navigate("chat/$storyId/true") {
                        popUpTo("video/$storyId") { inclusive = true }
                    }
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (!storyPreset?.intro_video_url.isNullOrBlank()) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        // --- FIX: Change resize mode to FIT to prevent cropping ---
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            CircularProgressIndicator()
        }
    }
}
