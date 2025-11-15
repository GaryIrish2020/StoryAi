package com.GirishDevelopment.storyai.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

// --- NEW DATA MODELS TO MATCH PYTHON BACKEND --- 

// Represents a single text part within a content message
@Serializable
data class Part(
    @SerialName("text") val text: String
)

// Represents a single turn in the conversation history (system, user, or model)
@Serializable
data class Content(
    @SerialName("role") val role: String, // "system", "user", or "model"
    @SerialName("parts") val parts: List<Part>
)

// --- API Request/Response Models ---

// Used for the initial /start_or_continue_story POST request for a new story
@Serializable
data class StartStoryRequest(
    @SerialName("userId") val userId: String,
    @SerialName("storyId") val storyId: String,
    @SerialName("systemPrompt") val systemPrompt: String? = null, // Only for NEW stories
    @SerialName("initialHistory") val initialHistory: List<Content>? = null, // Only for NEW stories
    @SerialName("characterRoles") val characterRoles: Map<String, String>? = null // Only for NEW stories
)

// Used for the /get_next_dialogue POST request
@Serializable
data class GetDialogueRequest(
    @SerialName("userId") val userId: String,
    @SerialName("storyId") val storyId: String,
    @SerialName("conversationHistory") val conversationHistory: List<Content>,
    @SerialName("userMessage") val userMessage: String,
    @SerialName("characterRoles") val characterRoles: Map<String, String>
)

// Used for the /generate_choices POST request
@Serializable
data class GetChoicesRequest(
    @SerialName("userId") val userId: String,
    @SerialName("storyId") val storyId: String,
    @SerialName("conversationHistory") val conversationHistory: List<Content>
)

// General Response Structure for most endpoints
@Serializable
data class ApiResponse(
    @SerialName("status") val status: String,
    @SerialName("newDialogue") val newDialogue: String? = null, // From /get_next_dialogue
    @SerialName("fullHistory") val fullHistory: List<Content>? = null, // From /get_next_dialogue (used for state sync)
    @SerialName("conversationHistory") val conversationHistory: List<Content>? = null, // From /start_or_continue_story
    @SerialName("characterRoles") val characterRoles: Map<String, String>? = null, // From /start_or_continue_story
    @SerialName("choices") val choices: List<String>? = null, // From /generate_choices
    @SerialName("error") val error: String? = null
)

// --- Retrofit API Service Interface ---

interface StoryService {

    @POST("/start_or_continue_story")
    suspend fun startOrContinueStory(@Body request: StartStoryRequest): ApiResponse

    @POST("/get_next_dialogue")
    suspend fun getNextDialogueLine(@Body request: GetDialogueRequest): ApiResponse
    
    @POST("/generate_choices")
    suspend fun getChoices(@Body request: GetChoicesRequest): ApiResponse
}
