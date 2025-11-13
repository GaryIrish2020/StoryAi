package com.example.storyai.data.network

import kotlinx.serialization.Serializable

@Serializable
data class DialogueRequest(
    val prompt_template: String,
    val char1_name: String,
    val char2_name: String,
    val bg_char1_name: String,
    val bg_char2_name: String
)

@Serializable
data class DialogueResponse(
    val dialogue: String
)
