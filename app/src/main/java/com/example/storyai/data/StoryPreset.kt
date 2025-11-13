package com.example.storyai.data

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
    val prompt_template: String,
    val cover_image_url: String,
    val genres: List<String>,

    // NEW FIELDS: Make them optional with default values
    val char1_name: String = "",
    val char2_name: String = "",
    val bg_char1_name: String = "",
    val bg_char2_name: String = ""
)
