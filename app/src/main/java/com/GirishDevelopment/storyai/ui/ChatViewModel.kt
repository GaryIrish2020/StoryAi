package com.GirishDevelopment.storyai.ui

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.GirishDevelopment.storyai.data.Message
import com.GirishDevelopment.storyai.data.StoryPreset
import com.GirishDevelopment.storyai.data.network.Content
import com.GirishDevelopment.storyai.data.network.GetChoicesRequest
import com.GirishDevelopment.storyai.data.network.GetDialogueRequest
import com.GirishDevelopment.storyai.data.network.StartStoryRequest
import com.GirishDevelopment.storyai.data.repository.StoryRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.HttpException
import retrofit2.Response
import javax.inject.Inject
import kotlin.collections.plus

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val storyRepository: StoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _choices = MutableStateFlow<List<String>>(emptyList())
    val choices: StateFlow<List<String>> = _choices.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _isNewStory = MutableStateFlow(false)
    val isNewStory: StateFlow<Boolean> = _isNewStory.asStateFlow()

    private val _characterRoles = MutableStateFlow<Map<String, String>>(emptyMap())
    val characterRoles: StateFlow<Map<String, String>> = _characterRoles.asStateFlow()

    private val conversationHistory = mutableListOf<Content>()
    private val storyId: String = savedStateHandle.get<String>("storyId")!!
    private val userId: String = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"

    private var isGenerating = false
    private var currentPreset: StoryPreset? = null
    private var messageCount = 0
    private val maxMessages = 10
    private val INTER_MESSAGE_BUFFER_MS = 50L

    init {
        _isNewStory.value = savedStateHandle.get<Boolean>("isNewStory") ?: false
        if (_isNewStory.value) {
            beginStory()
        }
    }

    fun beginStory() {
        if (_messages.value.isNotEmpty() && !_isNewStory.value) return

        if (userId == "anonymous") {
            handleError("Authentication Required", Exception("Please log in to begin a story."))
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val preset = storyRepository.getStoryPreset(storyId)
                    ?: throw Exception("Story preset not found for ID: $storyId")
                currentPreset = preset
                _characterRoles.value = preset.characterRoles

                val request = StartStoryRequest(
                    userId = userId,
                    storyId = storyId,
                    systemPrompt = preset.systemPrompt,
                    initialHistory = preset.initialHistory,
                    characterRoles = preset.characterRoles
                )
                val response = storyRepository.startOrContinueStory(request)

                if (response.error != null) {
                    throw Exception(response.error)
                }

                conversationHistory.clear()
                response.conversationHistory?.let { history ->
                    conversationHistory.addAll(history)
                }

                val initialHistorySize = if (_isNewStory.value) preset.initialHistory.size else 0
                val uiMessages = convertHistoryToMessages(conversationHistory, initialHistorySize)
                _messages.value = uiMessages
                
                isGenerating = true
                
                if (_isNewStory.value) {
                    messageCount = 0
                    generateChoices()
                } else {
                    messageCount = uiMessages.size
                    val lastMessageRole = conversationHistory.lastOrNull()?.role
                    if (lastMessageRole == "model") {
                        generateChoices()
                    } else if (lastMessageRole == "user") {
                        val lastUserMessage = conversationHistory.last().parts.firstOrNull()?.text?.removePrefix("USER: ") ?: ""
                        fetchNextDialogueLine(userMessage = lastUserMessage, showLoading = true)
                    } else {
                        generateChoices()
                    }
                }

            } catch (e: Exception) {
                handleError("Failed to begin story", e)
                isGenerating = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun fetchNextDialogueLine(userMessage: String, isFirstAttempt: Boolean = false, showLoading: Boolean = true) {
        if (userId == "anonymous") return
        if (showLoading) _isLoading.value = true

        viewModelScope.launch {
            try {
                val request = GetDialogueRequest(
                    userId = userId,
                    storyId = storyId,
                    conversationHistory = conversationHistory,
                    userMessage = userMessage,
                    characterRoles = currentPreset?.characterRoles ?: emptyMap()
                )
                val response = storyRepository.getNextDialogueLine(request)

                if (response.error != null) throw HttpException(Response.error<Any>(response.status.toInt(), ResponseBody.create(null, response.error)))

                response.fullHistory?.let { history ->
                    conversationHistory.clear()
                    conversationHistory.addAll(history)
                }

                val newDialogueText = response.newDialogue ?: throw Exception("Server did not return newDialogue.")

                val newMessage = Message(
                    author = extractAuthorFromDialogueLine(newDialogueText),
                    text = extractTextFromDialogueLine(newDialogueText),
                    timestamp = System.currentTimeMillis(),
                    isAnimated = false
                )
                _messages.update { it + newMessage }
                messageCount++

            } catch (e: HttpException) {
                if (e.code() == 503 && isFirstAttempt) {
                    delay(2000)
                    fetchNextDialogueLine(userMessage, isFirstAttempt = false, showLoading = showLoading)
                } else {
                    handleError("Failed to generate next line", e)
                    isGenerating = false
                }
            } catch (e: Exception) {
                handleError("Failed to generate next line", e)
                isGenerating = false
            } finally {
                if (showLoading) _isLoading.value = false
            }
        }
    }

    fun togglePause() {
        _isPaused.value = !_isPaused.value
        isGenerating = !_isPaused.value
        if (!_isPaused.value) {
            if (messageCount > 0 && messageCount < maxMessages) {
                onAnimationFinished(_messages.value.last().id)
            }
        }
    }

    private fun convertHistoryToMessages(history: List<Content>, initialHistorySize: Int): List<Message> {
        val messagesToSkip = if (initialHistorySize > 0) 1 + initialHistorySize else 0

        return history
            .drop(messagesToSkip)
            .mapNotNull { content ->
                val rawText = content.parts.firstOrNull()?.text?.trim() ?: return@mapNotNull null

                when (content.role) {
                    "user" -> {
                        val userText = rawText.removePrefix("USER:").trim()
                        if (userText.isNotBlank()) {
                            Message(
                                author = "You",
                                text = userText.removeSurrounding("'").removeSurrounding("\""),
                                timestamp = System.currentTimeMillis(),
                                isAnimated = true
                            )
                        } else {
                            null
                        }
                    }
                    "model" -> {
                        val parts = rawText.split(": ", limit = 2)
                        val author = parts.getOrElse(0) { "Narrator" }
                        val text = parts.getOrElse(1) { rawText }.trim().removeSurrounding("'").removeSurrounding("\"")
                        Message(
                            author = author,
                            text = text,
                            timestamp = System.currentTimeMillis(),
                            isAnimated = true
                        )
                    }
                    else -> null
                }
            }
    }
    
    private fun extractAuthorFromDialogueLine(dialogueLine: String) = dialogueLine.split(": ", limit = 2).getOrElse(0) { "Narrator" }
    
    private fun extractTextFromDialogueLine(dialogueLine: String): String {
        val text = dialogueLine.split(": ", limit = 2).getOrElse(1) { dialogueLine }
        return text.trim().removeSurrounding("'").removeSurrounding("\"")
    }

    fun onAnimationFinished(messageId: String) {
        _messages.update { currentMessages ->
            currentMessages.map { if (it.id == messageId) it.copy(isAnimated = true) else it }
        }
        if (!_isPaused.value && isGenerating && messageCount < maxMessages) {
            viewModelScope.launch {
                delay(INTER_MESSAGE_BUFFER_MS)
                fetchNextDialogueLine(userMessage = "", showLoading = false)
            }
        } else if (!_isPaused.value && isGenerating && messageCount >= maxMessages) {
            generateChoices()
        }
    }

    fun onUserChoiceSelected(choiceText: String) {
        if (_isPaused.value) return

        _isLoading.value = true
        _messages.update { it + Message(
            author = "You",
            text = choiceText,
            timestamp = System.currentTimeMillis(),
            isAnimated = true
        )
        }
        
        // --- FIX: Do NOT add to local conversationHistory. Let the server handle it. ---
        // conversationHistory.add(Content("user", listOf(Part("USER: $choiceText"))))
        
        _choices.value = emptyList()

        messageCount = 0
        isGenerating = true
        fetchNextDialogueLine(userMessage = choiceText, showLoading = true)
    }

    private fun generateChoices() {
        if (_isPaused.value) return

        _isLoading.value = true
        isGenerating = false
        viewModelScope.launch {
            try {
                val request = GetChoicesRequest(userId, storyId, conversationHistory)
                val response = storyRepository.getChoices(request)
                if (response.error != null) throw Exception(response.error)
                _choices.value = response.choices ?: emptyList()
            } catch (e: Exception) {
                handleError("Failed to generate choices", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun handleError(message: String, e: Exception) {
        Log.e("ChatViewModel", "$message: ", e)
        val errorText = if (e is HttpException) "Error ${e.code()}: ${e.message()}. The story couldn't continue. Please try again."
        else "An unexpected error occurred: ${e.localizedMessage}. Please check your connection."
        _messages.update { it + Message(
            author = "System",
            text = errorText,
            timestamp = System.currentTimeMillis(),
            isAnimated = true
        )
        }
    }
}
