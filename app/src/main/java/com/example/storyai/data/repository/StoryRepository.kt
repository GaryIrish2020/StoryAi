package com.example.storyai.data.repository

import com.example.storyai.data.StoryPreset
import com.example.storyai.data.StoryPresetsResponse
import com.example.storyai.data.network.ApiResponse
import com.example.storyai.data.network.GetChoicesRequest
import com.example.storyai.data.network.GetDialogueRequest
import com.example.storyai.data.network.StartStoryRequest
import com.example.storyai.data.network.StoryService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import javax.inject.Inject

@OptIn(InternalSerializationApi::class)
class StoryRepository @Inject constructor(
    private val apiService: StoryService,
    private val remoteConfig: FirebaseRemoteConfig,
    private val firestore: FirebaseFirestore,
    private val json: Json
) {

    private var storyPresets: List<StoryPreset>? = null
    
    init {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 0
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
    }

    suspend fun checkIfStoryExists(storyId: String): Boolean {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return false
        val docId = "${userId}_${storyId}"
        return try {
            firestore.collection("saved_stories").document(docId).get().await().exists()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun startOrContinueStory(request: StartStoryRequest): ApiResponse {
        return apiService.startOrContinueStory(request)
    }

    suspend fun getNextDialogueLine(request: GetDialogueRequest): ApiResponse {
        return apiService.getNextDialogueLine(request)
    }

    suspend fun getChoices(request: GetChoicesRequest): ApiResponse {
        return apiService.getChoices(request)
    }
    
    suspend fun getStoryPresets(): List<StoryPreset> {
        if (storyPresets != null) return storyPresets!!

        remoteConfig.fetchAndActivate().await()
        val jsonString = remoteConfig.getString("story_presets")
        val response = json.decodeFromString<StoryPresetsResponse>(jsonString)
        storyPresets = response.story_presets
        return response.story_presets
    }

    suspend fun getStoryPreset(id: String): StoryPreset? {
        val presets = getStoryPresets()
        return presets.find { it.id == id }
    }
}
