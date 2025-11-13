package com.example.storyai.data.repository

import com.example.storyai.data.Character
import com.example.storyai.data.Message
import com.example.storyai.data.network.DialogueRequest
import com.example.storyai.data.network.StoryService
import javax.inject.Inject

class StoryRepository @Inject constructor(
    private val storyService: StoryService
) {

    suspend fun generateDialogue(promptTemplate: String, char1Name: String, char2Name: String): Message {
        val request = DialogueRequest(
            prompt_template = promptTemplate,
            char1_name = char1Name,
            char2_name = char2Name
        )
        val response = storyService.generateDialogue(request)

        val authorName = if (response.dialogue.startsWith(char1Name)) char1Name else char2Name

        return Message(
            author = Character("", authorName, ""),
            content = response.dialogue,
            timestamp = System.currentTimeMillis()
        )
    }
}
