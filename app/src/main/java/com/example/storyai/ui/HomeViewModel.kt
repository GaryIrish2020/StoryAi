package com.example.storyai.ui

import androidx.lifecycle.ViewModel
import com.example.storyai.data.Story
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {

    private val _stories = MutableStateFlow<List<Story>>(emptyList())
    val stories: StateFlow<List<Story>> = _stories

    init {
        loadStories()
    }

    private fun loadStories() {
        _stories.value = listOf(
            Story("fantasy_quest", "A Grand Fantasy Quest", "Fantasy", "https://imgur.com/vKGCVlx.png"),
            Story("sci_fi_mystery", "Sci-Fi Mystery on Mars", "Sci-Fi", "https://imgur.com/TqmRXPi.png"),
            Story("modern_comedy", "Modern Office Comedy", "Comedy", "https://imgur.com/SzQ33eH.png")
        )
    }
}
