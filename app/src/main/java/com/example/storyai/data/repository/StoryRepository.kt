package com.example.storyai.data.repository

import com.example.storyai.data.Character
import com.example.storyai.data.Message
import com.example.storyai.data.StoryPreset
import com.example.storyai.data.StoryPresetsResponse
import com.example.storyai.data.network.DialogueRequest
import com.example.storyai.data.network.StoryService
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import javax.inject.Inject

class StoryRepository @Inject constructor(
    private val storyService: StoryService,
    private val remoteConfig: FirebaseRemoteConfig,
    private val json: Json
) {

    private var storyPresets: List<StoryPreset>? = null

    suspend fun generateDialogue(preset: StoryPreset): Message {
        val fullPrompt = """
Template: ${preset.prompt_template}
Main Char 1: ${preset.char1_name}
Main Char 2: ${preset.char2_name}
Background 1: ${preset.bg_char1_name}
Background 2: ${preset.bg_char2_name}
""".trimIndent()

        val request = DialogueRequest(
            prompt_template = fullPrompt,
            char1_name = preset.char1_name,
            char2_name = preset.char2_name,
            bg_char1_name = preset.bg_char1_name,
            bg_char2_name = preset.bg_char2_name
        )
        val response = storyService.generateDialogue(request)

        val authorName = when {
            response.dialogue.startsWith(preset.char1_name) -> preset.char1_name
            response.dialogue.startsWith(preset.char2_name) -> preset.char2_name
            response.dialogue.startsWith(preset.bg_char1_name) -> preset.bg_char1_name
            response.dialogue.startsWith(preset.bg_char2_name) -> preset.bg_char2_name
            else -> "Narrator"
        }

        return Message(
            author = Character("", authorName, ""),
            content = response.dialogue,
            timestamp = System.currentTimeMillis()
        )
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
