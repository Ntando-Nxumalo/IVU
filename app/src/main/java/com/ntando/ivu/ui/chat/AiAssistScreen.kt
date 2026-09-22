package com.ntando.ivu.ui.chat

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ntando.ivu.R
import com.ntando.ivu.data.model.ChatMessage
import com.ntando.ivu.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

private const val TAG = "AiAssistScreen"

/**
 * Composable screen providing an interactive AI study assistant chat interface.
 *
 * Layout Structure:
 * - [Scaffold] with a custom top app bar displaying IVU AI branding and study buddy subtitle.
 * - [LazyColumn] rendering user and bot message bubbles ([ChatBubble]) and an animated typing indicator.
 * - Horizontal scrollable/wrapped row of preset [SuggestionChip] options ("Quiz me", "Explain").
 * - Sticky bottom input bar with an [OutlinedTextField] and a circular [FloatingActionButton] for sending messages.
 *
 * @param viewModel ViewModel handling AI chat communication flow and message history state.
 * @param onBack Navigation callback invoked when the back button is pressed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        Log.d(TAG, "Chat message list updated, current message count: ${messages.size}")
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.title_ivu_ai), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                        Text(stringResource(R.string.subtitle_study_buddy), fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        Log.d(TAG, "Navigating back from AiAssistScreen")
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFE88A68)
                )
            )
        },
        containerColor = Color(0xFFFFF8F0)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(messages) { message ->
                    ChatBubble(message)
                }
                
                if (isLoading) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(modifier = Modifier.size(32.dp), shape = CircleShape, color = Color(0xFF7FB6A7)) {}
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.label_ivu_typing),
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val quizText = stringResource(R.string.label_suggestion_quiz)
                SuggestionChip(
                    text = quizText,
                    onClick = {
                        Log.i(TAG, "Suggestion chip tapped: $quizText")
                        viewModel.sendMessage(quizText)
                    }
                )
                val explainText = stringResource(R.string.label_suggestion_explain)
                SuggestionChip(
                    text = explainText,
                    onClick = {
                        Log.i(TAG, "Suggestion chip tapped: $explainText")
                        viewModel.sendMessage(explainText)
                    }
                )
            }

            // Input Area
            Surface(
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .navigationBarsPadding()
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text(stringResource(R.string.hint_message)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color.LightGray,
                            unfocusedBorderColor = Color.LightGray
                        ),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    FloatingActionButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                Log.i(TAG, "Sending chat message: $textInput")
                                viewModel.sendMessage(textInput)
                                textInput = ""
                            } else {
                                Log.w(TAG, "Send button clicked with empty message")
                            }
                        },
                        containerColor = Color(0xFFE88A68),
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Send", modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

/**
 * Clickable pill-shaped suggestion chip used to prefill or directly send prompt commands to the AI model.
 *
 * @param text The text prompt displayed inside the chip.
 * @param onClick Callback triggered when the chip is tapped.
 */
@Composable
fun SuggestionChip(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF2E6D3),
        modifier = Modifier.height(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF3D2B1F))
        }
    }
}

/**
 * Message bubble UI component rendered in the chat stream.
 * Formats alignment (Right for user, Left for AI bot) and color schemes accordingly.
 *
 * @param message The [ChatMessage] model object containing message text and ownership flag (`isUser`).
 */
@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.isUser
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            Surface(modifier = Modifier.size(36.dp), shape = CircleShape, color = Color(0xFF7FB6A7)) {}
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        val bubbleColor = if (isUser) Color(0xFFF2E6D3) else Color.White
        val textColor = Color(0xFF3D2B1F)
        val shape = RoundedCornerShape(20.dp)

        Box(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .clip(shape)
                .background(bubbleColor)
                .border(if (!isUser) 0.5.dp else 0.dp, Color.LightGray, shape)
                .padding(16.dp)
        ) {
            Text(
                text = message.text,
                color = textColor,
                fontSize = 14.sp
            )
        }
    }
}
