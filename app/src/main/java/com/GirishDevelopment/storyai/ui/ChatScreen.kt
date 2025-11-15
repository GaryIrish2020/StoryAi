package com.GirishDevelopment.storyai.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.GirishDevelopment.storyai.data.Message
import com.GirishDevelopment.storyai.ui.theme.StoryAiTheme
import com.GirishDevelopment.storyai.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
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
    val isLoading by viewModel.isLoading.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val isNewStory by viewModel.isNewStory.collectAsState()
    val characterRoles by viewModel.characterRoles.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        snapshotFlow { listState.layoutInfo.totalItemsCount }
            .distinctUntilChanged()
            .filter { it > 0 }
            .collect {
                listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
            }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background), // Corrected filename
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Story Chat") },
                    navigationIcon = {
                        IconButton(onClick = {
                            navController.navigate("home") { popUpTo("home") { inclusive = true } }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        if (!isNewStory || messages.isNotEmpty()) {
                            IconButton(onClick = { viewModel.togglePause() }) {
                                if (isPaused) Icon(Icons.Filled.PlayArrow, "Resume Story")
                                else Icon(Icons.Filled.Pause, "Pause Story")
                            }
                        }
                    }
                )
            },
            content = { paddingValues ->
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    val showContinueButton = messages.isEmpty() && !isLoading && !isNewStory

                    Column(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            state = listState,
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            itemsIndexed(messages, key = { _, message -> message.id }) { index, message ->
                                MessageItem(
                                    message = message,
                                    characterRoles = characterRoles.keys.toList(),
                                    onAnimationFinished = { viewModel.onAnimationFinished(message.id) },
                                    onCharacterTyped = {
                                        coroutineScope.launch {
                                            listState.animateScrollToItem(listState.layoutInfo.totalItemsCount)
                                        }
                                    }
                                )
                            }
                            if (isLoading && !isPaused && messages.isNotEmpty()) {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                        }

                        if (choices.isNotEmpty()) {
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                choices.forEach { choice ->
                                    Button(
                                        onClick = { viewModel.onUserChoiceSelected(choice) },
                                        enabled = !isPaused && !isLoading
                                    ) {
                                        Text(choice)
                                    }
                                }
                            }
                        }
                    }

                    if (showContinueButton) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(onClick = { viewModel.beginStory() }) {
                                Text("Continue Story")
                            }
                        }
                    }

                    AnimatedVisibility(visible = isPaused, enter = fadeIn(), exit = fadeOut()) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Story Paused", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun createFormattedText(text: String, author: String): AnnotatedString {
    val blueColor = MaterialTheme.colorScheme.primary

    if (author == "Narrator") {
        return buildAnnotatedString {
            withStyle(style = SpanStyle(color = blueColor)) {
                append("($text)")
            }
        }
    }

    return buildAnnotatedString {
        val regex = Regex("""(\*\*[^*]+\*\*)""")
        var lastIndex = 0

        regex.findAll(text).forEach { matchResult ->
            val match = matchResult.value
            val startIndex = matchResult.range.first

            if (startIndex > lastIndex) {
                append(text.substring(lastIndex, startIndex))
            }

            withStyle(style = SpanStyle(color = blueColor, fontWeight = FontWeight.Bold)) {
                append(match.removeSurrounding("**"))
            }

            lastIndex = matchResult.range.last + 1
        }

        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
}

@Composable
fun MessageItem(
    message: Message,
    characterRoles: List<String>,
    onAnimationFinished: () -> Unit,
    onCharacterTyped: () -> Unit
) {
    val storyCharacterColors = remember(characterRoles) {
        val colors = listOf(
            Color(0xFF003366),
            Color(0xFF4A0033),
            Color(0xFF004D40),
            Color(0xFF5E3A00)
        )
        characterRoles.mapIndexed { index, name -> name to colors[index % colors.size] }.toMap()
    }

    val bubbleColor = when (message.author) {
        "You" -> Color(0xFFD3E0EA)
        "System" -> Color.Gray
        "Narrator" -> Color(0xFFB0BEC5)
        else -> storyCharacterColors[message.author] ?: Color(0xFF37474F)
    }
    
    val alignment = when (message.author) {
        "You" -> Alignment.End
        "Narrator", "System" -> Alignment.CenterHorizontally
        else -> Alignment.Start
    }
    
    val horizontalArrangement = when (message.author) {
        "You" -> Arrangement.End
        "Narrator", "System" -> Arrangement.Center
        else -> Arrangement.Start 
    }

    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = horizontalArrangement) {
        Column(horizontalAlignment = alignment) {
            Text(formatTimestamp(message.timestamp), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(bottom = 2.dp, start = 8.dp))
            if (message.author != "Narrator") {
                val name = message.author
                if (name.startsWith("**") && name.endsWith("**")) {
                    val actualName = name.substring(2, name.length - 2)
                    Text(actualName, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 2.dp, start = 8.dp, end = 8.dp))
                } else {
                    Text(name, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 2.dp, start = 8.dp, end = 8.dp))
                }
            }
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = bubbleColor), modifier = Modifier.widthIn(max = 300.dp)) {
                Box(modifier = Modifier.background(Color.Black.copy(alpha = 0.2f))) {
                    SelectionContainer {
                        var displayedText by remember { mutableStateOf<AnnotatedString>(AnnotatedString("")) }
                        
                        val formattedText = createFormattedText(text = message.text, author = message.author)

                        if (!message.isAnimated) {
                            LaunchedEffect(message.id) {
                                val fullText = formattedText.text
                                for (i in 1..fullText.length) {
                                    displayedText = formattedText.subSequence(0, i)
                                    onCharacterTyped()
                                    delay(50)
                                }
                                onAnimationFinished()
                            }
                        } else {
                            displayedText = formattedText
                        }
                        Text(displayedText, modifier = Modifier.padding(12.dp), color = Color.White)
                    }
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
    }
}
