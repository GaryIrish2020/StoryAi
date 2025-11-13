package com.example.storyai.data

import kotlinx.serialization.Serializable

@Serializable
data class Story(
    val id: String,
    val title: String,
    val genre: String,
    val coverImageUrl: String // URL for the story's cover image
)
