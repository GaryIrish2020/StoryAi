package com.GirishDevelopment.storyai.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.GirishDevelopment.storyai.data.StoryPreset
import com.GirishDevelopment.storyai.R
import kotlinx.coroutines.flow.collectLatest

@Composable
fun HomeScreen(navController: NavController, viewModel: HomeViewModel = hiltViewModel()) {
    val groupedStories by viewModel.groupedStories.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collectLatest { event ->
            when (event) {
                is NavigationEvent.NavigateToVideo -> navController.navigate("video/${event.storyId}")
                is NavigationEvent.NavigateToChat -> navController.navigate("chat/${event.storyId}/false")
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background), // Corrected filename
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Scaffold(
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                groupedStories.forEach { (genre, storiesInGenre) ->
                    item {
                        Text(
                            text = genre,
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(storiesInGenre, key = { it.id }) { story ->
                                StoryCard(story) {
                                    viewModel.onStorySelected(story.id)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StoryCard(story: StoryPreset, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column {
            Image(
                painter = rememberAsyncImagePainter(story.cover_image_url),
                contentDescription = story.title,
                modifier = Modifier
                    .height(140.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                contentScale = ContentScale.Crop
            )
            Text(
                text = story.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
