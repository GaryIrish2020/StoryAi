package com.example.storyai.data.repository

import com.example.storyai.data.StoryPreset
import com.example.storyai.data.StoryPresetsResponse
import com.example.storyai.data.network.ChoiceResponse
import com.example.storyai.data.network.DialogueResponse
import com.example.storyai.data.network.HistoryRequest
import com.example.storyai.data.network.StoryService
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import javax.inject.Inject

@OptIn(InternalSerializationApi::class)
class StoryRepository @Inject constructor(
    private val apiService: StoryService,
    private val remoteConfig: FirebaseRemoteConfig,
    private val json: Json
) {

    private var storyPresets: List<StoryPreset>? = null

    /**
     * Sends the current conversation history to the server to generate the next dialogue line.
     * @param request Contains the List<String> history. (Uses concrete HistoryRequest)
     */
    suspend fun generateDialogue(request: HistoryRequest): DialogueResponse {
        return apiService.generateDialogue(request)
    }

    /**
     * Sends the current conversation history to the server to generate story choices.
     * @param request Contains the List<String> history. (Uses concrete HistoryRequest)
     */
    suspend fun getChoices(request: HistoryRequest): ChoiceResponse {
        return apiService.getChoices(request)
    }
    
    suspend fun getStoryPresets(): List<StoryPreset> {
        if (storyPresets != null) return storyPresets!!

        // Assume fetchAndActivate is handled elsewhere, but perform a quick fetch if needed
        remoteConfig.fetchAndActivate().await() 
        
        val jsonString = remoteConfig.getString("story_presets")
        // NOTE: This assumes StoryPresetsResponse has been defined and includes
        // the list of StoryPreset objects.
        val response = json.decodeFromString<StoryPresetsResponse>(jsonString)
        storyPresets = response.story_presets
        return response.story_presets
    }

    suspend fun getStoryPreset(id: String): StoryPreset? {
        val presets = getStoryPresets()
        return presets.find { it.id == id }
    }
}
