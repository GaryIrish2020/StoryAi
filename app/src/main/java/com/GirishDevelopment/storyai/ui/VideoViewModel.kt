package com.GirishDevelopment.storyai.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.GirishDevelopment.storyai.data.StoryPreset
import com.GirishDevelopment.storyai.data.repository.StoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoViewModel @Inject constructor(
    private val storyRepository: StoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _storyPreset = MutableStateFlow<StoryPreset?>(null)
    val storyPreset: StateFlow<StoryPreset?> = _storyPreset.asStateFlow()

    private val storyId: String = savedStateHandle.get<String>("storyId")!!

    init {
        loadStoryPreset()
    }

    private fun loadStoryPreset() {
        viewModelScope.launch {
            _storyPreset.value = storyRepository.getStoryPreset(storyId)
        }
    }
}
