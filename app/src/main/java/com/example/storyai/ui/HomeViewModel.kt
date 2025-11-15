package com.example.storyai.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storyai.data.StoryPreset
import com.example.storyai.data.repository.StoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class NavigationEvent {
    data class NavigateToVideo(val storyId: String) : NavigationEvent()
    data class NavigateToChat(val storyId: String) : NavigationEvent()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val storyRepository: StoryRepository
) : ViewModel() {

    private val _groupedStories = MutableStateFlow<Map<String, List<StoryPreset>>>(emptyMap())
    val groupedStories: StateFlow<Map<String, List<StoryPreset>>> = _groupedStories.asStateFlow()

    private val _navigationEvent = Channel<NavigationEvent>()
    val navigationEvent = _navigationEvent.receiveAsFlow()

    init {
        loadStories()
    }

    fun onStorySelected(storyId: String) {
        viewModelScope.launch {
            val storyExists = storyRepository.checkIfStoryExists(storyId)
            if (storyExists) {
                _navigationEvent.send(NavigationEvent.NavigateToChat(storyId))
            } else {
                _navigationEvent.send(NavigationEvent.NavigateToVideo(storyId))
            }
        }
    }

    private fun loadStories() {
        viewModelScope.launch {
            try {
                Log.d("HomeViewModel", "Attempting to load story presets...")
                val presets = storyRepository.getStoryPresets()
                if (presets.isEmpty()) {
                    Log.w("HomeViewModel", "No story presets found after fetching.")
                } else {
                    Log.d("HomeViewModel", "Successfully fetched ${presets.size} story presets.")
                }

                val grouped = presets.flatMap { story ->
                    story.genres.map { genre -> genre to story }
                }.groupBy(
                    keySelector = { it.first },
                    valueTransform = { it.second }
                )
                _groupedStories.value = grouped
                Log.d("HomeViewModel", "Grouped stories updated. Number of genres: ${grouped.size}")

            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading stories: ", e)
            }
        }
    }
}
