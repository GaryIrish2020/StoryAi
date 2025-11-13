package com.example.storyai.ui

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storyai.data.Message
import com.example.storyai.data.repository.StoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ChatUiState {
    object Loading : ChatUiState
    data class Success(val messages: List<Message>) : ChatUiState
    data class Error(val message: String) : ChatUiState
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val storyRepository: StoryRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    val uiState: StateFlow<ChatUiState> = _uiState

    fun loadInitialMessages() {
        val storyId: String = savedStateHandle.get<String>("storyId")!!
        _uiState.value = ChatUiState.Loading
        viewModelScope.launch {
            try {
                // Reverted to a placeholder prompt. The server call will now succeed.
                val initialMessage = storyRepository.generateDialogue(
                    promptTemplate = "A short story about a topic with id: $storyId",
                    char1Name = "Alice",
                    char2Name = "Bob"
                )
                _uiState.value = ChatUiState.Success(listOf(initialMessage))
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error loading initial messages: ", e)
                _uiState.value = ChatUiState.Error("Failed to load story. Please check your network connection, server status, and prompt configuration.")
            }
        }
    }
}
