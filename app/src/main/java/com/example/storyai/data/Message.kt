package com.example.storyai.data

import java.util.UUID

data class Message(
    val id: String = UUID.randomUUID().toString(), // Unique ID for state management
    val author: String,
    val text: String,
    val timestamp: Long,
    var isAnimated: Boolean = false // Tracks if the typewriter effect has finished
)
