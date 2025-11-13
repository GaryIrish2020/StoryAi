package com.example.storyai.data.network

import com.example.storyai.data.Message
import retrofit2.http.Body
import retrofit2.http.POST

interface StoryApiService {

    @POST("generate_dialogue")
    suspend fun getStory(@Body requestBody: Map<String, String> = emptyMap()): List<Message>

    @POST("make_choice") // Please replace with your actual endpoint
    suspend fun makeChoice(@Body choice: Map<String, String>)
}
