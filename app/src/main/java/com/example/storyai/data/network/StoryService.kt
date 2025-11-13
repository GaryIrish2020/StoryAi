package com.example.storyai.data.network

import retrofit2.http.Body
import retrofit2.http.POST

interface StoryService {

    @POST("dialogue_generator")
    suspend fun generateDialogue(@Body request: DialogueRequest): DialogueResponse
}
