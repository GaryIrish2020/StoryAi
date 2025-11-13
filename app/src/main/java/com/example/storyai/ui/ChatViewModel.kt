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
                val preset = storyRepository.getStoryPreset(storyId)
                if (preset == null) {
                    _uiState.value = ChatUiState.Error("Story preset not found.")
                    return@launch
                }

                val initialMessage = storyRepository.generateDialogue(preset)
                _uiState.value = ChatUiState.Success(listOf(initialMessage))
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error loading initial messages: ", e)
                _uiState.value = ChatUiState.Error("Failed to load story. Please check your network connection, server status, and prompt configuration.")
            }
        }
    }
}
