package com.example.storyai.data.network

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

@OptIn(InternalSerializationApi::class)
// 1. The Request body for both endpoints
// This is what you send TO the server
@Serializable
data class HistoryRequest(
    val history: List<String>
)

@OptIn(InternalSerializationApi::class)
// 2. The nested Dialogue object
// This is what the server sends back INSIDE the DialogueResponse
@Serializable
data class DialogueLine(
    @SerialName("character_name")
    val characterName: String,
    @SerialName("dialogue_line")
    val dialogueLine: String
)

@OptIn(InternalSerializationApi::class)
// 3. The Response from /dialogue_generator
@Serializable
data class DialogueResponse(
    val status: String,
    val dialogue: DialogueLine // <-- This is now an object, not a String
)

@OptIn(InternalSerializationApi::class)
// 4. The Response from /generate_choices
@Serializable
data class ChoiceResponse(
    val status: String,
    val choices: List<String>
)

// 5. Your updated Retrofit API service interface
interface StoryService {

    @POST("/dialogue_generator")
    suspend fun generateDialogue(@Body request: HistoryRequest): DialogueResponse
    
    @POST("/generate_choices")
    suspend fun getChoices(@Body request: HistoryRequest): ChoiceResponse
}
