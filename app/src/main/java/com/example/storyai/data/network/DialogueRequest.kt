package com.example.storyai.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DialogueRequest(
    @SerialName("prompt_template")
    val promptTemplate: String,
    
    @SerialName("char1_name")
    val char1Name: String,
    
    @SerialName("char2_name")
    val char2Name: String,
    
    @SerialName("bg_char1_name")
    val bgChar1Name: String,
    
    @SerialName("bg_char2_name")
    val bgChar2Name: String
)
