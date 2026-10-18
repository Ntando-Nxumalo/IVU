package com.ntando.ivu.ui.decks

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ntando.ivu.R
import com.ntando.ivu.data.entity.Flashcard
import com.ntando.ivu.ui.components.PronunciationButton
import com.ntando.ivu.util.SpeechManager
import com.ntando.ivu.viewmodel.FlashcardListUiState
import com.ntando.ivu.viewmodel.FlashcardListViewModel
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "FlashcardListScreen"

/**
 * Screen displaying the scrollable list of flashcards associated with a specific deck.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardListScreen(
    viewModel: FlashcardListViewModel,
    deckId: String,
    deckTitle: String,
    onBack: () -> Unit,
    onDeleteDeck: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(deckId) {
        Log.d(TAG, "LaunchedEffect loading cards for deckId: $deckId")
        viewModel.loadCards(deckId)
    }

    LaunchedEffect(uiState) {
        Log.d(TAG, "Observed FlashcardListUiState: $uiState")
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val speechManager = remember { SpeechManager(context) }
    DisposableEffect(Unit) {
        onDispose {
            speechManager.shutdown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(deckTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        Log.d(TAG, "Navigating back from FlashcardListScreen")
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    TextButton(
                        onClick = {
                            val parsedDeckId = deckId.toLongOrNull() ?: -1L
                            Log.i(TAG, "Quiz button clicked in FlashcardListScreen for deckId: $parsedDeckId")
                            val intent = android.content.Intent(context, com.ntando.ivu.QuizActivity::class.java).apply {
                                putExtra("deck_id", parsedDeckId)
                                putExtra("deck_title", deckTitle)
                            }
                            context.startActivity(intent)
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.title_quiz_mode),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete_deck_title), color = Color.Red) },
                                onClick = {
                                    Log.w(TAG, "Delete deck requested from top bar options menu for deckId: $deckId")
                                    showMenu = false
                                    onDeleteDeck()
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val sharedPref = context.getSharedPreferences("IVUPrefs", android.content.Context.MODE_PRIVATE)
            val firebaseUid = sharedPref.getString("firebase_uid", "") ?: ""

            val networkMonitor = remember(context) { com.ntando.ivu.network.NetworkMonitor(context) }
            val isOnline by networkMonitor.isOnline.collectAsState(initial = networkMonitor.isCurrentlyConnected())

            com.ntando.ivu.ui.components.OfflineBanner(isOffline = !isOnline)
            com.ntando.ivu.ui.components.PendingSyncBanner(ownerUserId = firebaseUid)

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (val state = uiState) {
                    is FlashcardListUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color(0xFFE88A68))
                    is FlashcardListUiState.Error -> Text(text = state.message, color = Color.Red, modifier = Modifier.align(Alignment.Center))
                    is FlashcardListUiState.Success -> {
                        if (state.cards.isEmpty()) {
                            Text(
                                text = stringResource(R.string.no_cards_msg),
                                modifier = Modifier.align(Alignment.Center),
                                color = Color.Gray
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(state.cards) { card ->
                                    FlashcardListItem(
                                        card = card,
                                        speechManager = speechManager,
                                        onDelete = { 
                                            Log.w(TAG, "Deleting card with id: ${card.cardId} from deckId: $deckId")
                                            viewModel.deleteCard(deckId, card.cardId.toString())
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Card component rendering front and back text of a single flashcard alongside its due status tag and a delete action button.
 */
@Composable
fun FlashcardListItem(
    card: Flashcard,
    speechManager: SpeechManager? = null,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = card.frontText, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    if (speechManager != null) {
                        PronunciationButton(
                            speechManager = speechManager,
                            textToSpeak = card.frontText,
                            languageCode = "en",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = card.backText, fontSize = 14.sp, color = Color(0xFFE88A68), modifier = Modifier.padding(top = 4.dp))
                    if (speechManager != null) {
                        PronunciationButton(
                            speechManager = speechManager,
                            textToSpeak = card.backText,
                            languageCode = "en",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                val dueDateText = formatDueDate(card.dueDate)
                val dueDateColor = when (dueDateText) {
                    "Overdue" -> Color.Red
                    "Due today" -> Color(0xFFE88A68)
                    else -> Color.Gray
                }
                
                Text(
                    text = dueDateText,
                    fontSize = 12.sp,
                    color = dueDateColor,
                    fontWeight = FontWeight.Medium
                )
            }
            
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.LightGray)
            }
        }
    }
}

/**
 * Utility function calculating human-readable due date status strings based on millisecond epoch timestamps.
 */
fun formatDueDate(dueDate: Long): String {
    if (dueDate == 0L) return "New"
    
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    
    val due = Calendar.getInstance().apply {
        timeInMillis = dueDate
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    return when {
        due.before(today) -> "Overdue"
        due == today -> "Due today"
        else -> {
            val sdf = SimpleDateFormat("d MMM", Locale.getDefault())
            "Due: ${sdf.format(Date(dueDate))}"
        }
    }
}
