package com.example.storyai.data

import com.example.storyai.data.network.Content
import kotlinx.serialization.Serializable

@Serializable
data class StoryPresetsResponse(
    val story_presets: List<StoryPreset>
)

@Serializable
data class StoryPreset(
    val id: String,
    val title: String,
    val description: String,
    val systemPrompt: String,
    val cover_image_url: String,
    val genres: List<String>,
    
    // NEW FIELD for the introductory video
    val intro_video_url: String? = null, 

    val characterRoles: Map<String, String>,
    val initialHistory: List<Content>
)
