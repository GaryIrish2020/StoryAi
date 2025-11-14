package com.example.storyai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.storyai.data.Message
import com.example.storyai.ui.theme.StoryAiTheme
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val choices by viewModel.choices.collectAsState()
    val listState = rememberLazyListState()

    // ViewModel's init block now handles loading, so this is just for auto-scrolling
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Story Chat") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        content = { paddingValues ->
            Column(modifier = modifier.fillMaxSize().padding(paddingValues)) {
                LazyColumn(
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    state = listState,
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    itemsIndexed(messages, key = { _, message -> message.id }) { index, message ->
                        MessageItem(
                            message = message,
                            index = index,
                            onAnimationFinished = { viewModel.onAnimationFinished(message.id) }
                        )
                    }
                }

                if (choices.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        choices.forEach { choice ->
                            Button(onClick = { viewModel.onUserChoiceSelected(choice) }) {
                                Text(choice)
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun MessageItem(
    message: Message,
    index: Int,
    onAnimationFinished: () -> Unit
) {
    // Keep only fixed colors for Narrator and You
    val characterColors = remember {
        mapOf(
            "Narrator" to Color(0xFFB0BEC5), // Blue Grey
            "You" to Color(0xFFD3E0EA), // Light Blue for user
        )
    }

    // These colors will be used for alternating AI messages
    val defaultAIChatColors = listOf(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
    )

    val bubbleColor = characterColors.getOrElse(message.author) {
        // For AI characters (not Narrator or You), use alternating colors based on index
        defaultAIChatColors[index % defaultAIChatColors.size]
    }

    val alignment = when (message.author) {
        "You" -> Alignment.End
        "Narrator" -> Alignment.CenterHorizontally
        else -> Alignment.Start
    }

    val horizontalArrangement = when (message.author) {
        "You" -> Arrangement.End
        "Narrator" -> Arrangement.Center
        else -> Arrangement.Start
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = horizontalArrangement
    ) {
        Column(horizontalAlignment = alignment) {
            // Timestamp at the top left of the message group
            Text(
                text = formatTimestamp(message.timestamp),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(bottom = 2.dp, start = 8.dp)
            )

            if (message.author != "Narrator") {
                val name = message.author
                if (name.startsWith("**") && name.endsWith("**")) {
                    val actualName = name.substring(2, name.length - 2)
                    Text(
                        text = actualName,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary, // Stand-out color
                        modifier = Modifier.padding(bottom = 2.dp, start = 8.dp, end = 8.dp)
                    )
                } else {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(bottom = 2.dp, start = 8.dp, end = 8.dp)
                    )
                }
            }
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = bubbleColor),
                modifier = Modifier.widthIn(max = 300.dp) // Limit bubble width
            ) {
                SelectionContainer {
                    var displayedText by remember { mutableStateOf("") }

                    if (!message.isAnimated) {
                        LaunchedEffect(message.id) {
                            message.text.forEach { char ->
                                displayedText += char
                                delay(50)
                            }
                            // Animation is finished, notify the ViewModel
                            onAnimationFinished()
                        }
                    } else {
                        // If already animated, show full text immediately
                        displayedText = message.text
                    }

                    Text(
                        text = displayedText,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun formatTimestamp(timestamp: Long): String {
    val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
    return DateTimeFormatter.ofPattern("HH:mm").format(dateTime)
}

@Preview(showBackground = true)
@Composable
fun ChatScreenPreview() {
    StoryAiTheme {
        // Can't really preview this screen anymore as it depends on ViewModel logic
        // ChatScreen(navController = rememberNavController())
    }
}
