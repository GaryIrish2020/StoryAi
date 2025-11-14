package com.example.storyai.ui

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storyai.data.Message
import com.example.storyai.data.StoryPreset
import com.example.storyai.data.repository.StoryRepository
import com.example.storyai.data.network.HistoryRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject
import java.util.UUID

sealed interface ChatUiState {
    object Loading : ChatUiState
    object Success : ChatUiState
    data class Error(val message: String) : ChatUiState
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val storyRepository: StoryRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _choices = MutableStateFlow<List<String>>(emptyList())
    val choices: StateFlow<List<String>> = _choices.asStateFlow()

    private val conversationHistory = mutableListOf<String>()
    private var messageCount = 0
    private val maxMessages = 10

    private var isGenerating = false
    private var currentPreset: StoryPreset? = null

    init {
        loadStoryPresetAndStart()
    }

    private fun loadStoryPresetAndStart() {
        val storyId: String? = savedStateHandle.get<String>("storyId")
        if (storyId == null) {
            Log.e("ChatViewModel", "StoryId not found in SavedStateHandle")
            return
        }

        viewModelScope.launch {
            try {
                val preset = storyRepository.getStoryPreset(storyId)
                if (preset == null) {
                    Log.e("ChatViewModel", "Story preset not found for ID: $storyId")
                    return@launch
                }
                currentPreset = preset
                startStory()
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error loading initial story preset: ", e)
            }
        }
    }

    private fun startStory() {
        if (isGenerating || currentPreset == null) return
        isGenerating = true

        _uiState.value = ChatUiState.Loading
        _messages.value = emptyList()
        _choices.value = emptyList()
        conversationHistory.clear()
        messageCount = 0

        viewModelScope.launch {
            val preset = currentPreset!!
            val initialPrompt = preset.prompt_template
                .replace("{char1_name}", preset.char1_name)
                .replace("{char2_name}", preset.char2_name)
                .replace("{bg_char1_name}", preset.bg_char1_name)
                .replace("{bg_char2_name}", preset.bg_char2_name)
            
            // --- CRITICAL FIX: Add the INITIAL PROMPT AS A SYSTEM MESSAGE ---
            conversationHistory.add("SYSTEM_CONTEXT: $initialPrompt")
            
            // 2. Start the conversation loop
            fetchNextDialogueLine(isFirstAttempt = true)
        }
    }

    private fun fetchNextDialogueLine(isFirstAttempt: Boolean = false) {
        if (currentPreset == null) return

        if (messageCount >= maxMessages) {
            generateChoices()
            return
        }

        viewModelScope.launch {
            try {
                val requestBody = HistoryRequest(history = conversationHistory)
                val response = storyRepository.generateDialogue(requestBody)

                val aiResponseText = "${response.dialogue.characterName}: ${response.dialogue.dialogueLine}"
                conversationHistory.add(aiResponseText)

                val newMessage = Message(
                    author = response.dialogue.characterName,
                    text = response.dialogue.dialogueLine,
                    timestamp = System.currentTimeMillis(),
                    isAnimated = false
                )

                _messages.update { it + newMessage }
                messageCount++

            } catch (e: HttpException) {
                if (e.code() == 503 && isFirstAttempt) {
                    Log.w("ChatViewModel", "Server busy (503), retrying after delay...")
                    delay(2000)
                    fetchNextDialogueLine(isFirstAttempt = false)
                } else {
                    Log.e("ChatViewModel", "Error fetching next dialogue line: ", e)
                    isGenerating = false
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error fetching next dialogue line: ", e)
                isGenerating = false
            }
        }
    }

    private fun generateChoices() {
        isGenerating = false
        viewModelScope.launch {
            try {
                val requestBody = HistoryRequest(history = conversationHistory)
                val choiceResponse = storyRepository.getChoices(requestBody)
                _choices.value = choiceResponse.choices
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error generating choices: ", e)
                _choices.value = listOf("Error generating choices.")
            }
        }
    }

    fun onAnimationFinished(messageId: String) {
        _messages.update { currentMessages ->
            currentMessages.map { m ->
                if (m.id == messageId) m.copy(isAnimated = true) else m
            }
        }
        if (isGenerating) {
            viewModelScope.launch {
                delay(1000)
                fetchNextDialogueLine()
            }
        }
    }

    fun onUserChoiceSelected(choiceText: String) {
        if (currentPreset == null) return

        conversationHistory.add("USER: $choiceText")
        _messages.update { it + Message(
            author = "You",
            text = choiceText,
            timestamp = System.currentTimeMillis(),
            isAnimated = true
        ) }

        _choices.value = emptyList()

        messageCount = 0
        isGenerating = true
        fetchNextDialogueLine()
    }
}
