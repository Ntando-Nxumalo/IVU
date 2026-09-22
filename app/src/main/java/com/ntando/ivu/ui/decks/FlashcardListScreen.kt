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
import com.ntando.ivu.network.Flashcard
import com.ntando.ivu.viewmodel.FlashcardListUiState
import com.ntando.ivu.viewmodel.FlashcardListViewModel
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "FlashcardListScreen"

/**
 * Screen displaying a scrollable list of all individual flashcards belonging to a given deck.
 *
 * Layout Structure:
 * - [Scaffold] with a [TopAppBar] showing the deck title, back navigation button, and an overflow options menu (e.g. Delete Deck).
 * - Body handling [FlashcardListUiState]: Loading indicator, error message text, empty list indicator, or a [LazyColumn] of [FlashcardListItem] cards.
 *
 * @param viewModel ViewModel fetching and managing cards for the deck.
 * @param deckId Unique identifier of the selected deck.
 * @param deckTitle Display title of the deck shown in the app bar.
 * @param onBack Navigation callback invoked to return to the previous screen.
 * @param onDeleteDeck Callback invoked when the user confirms deck deletion from the options menu.
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
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                is FlashcardListUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color(0xFFE88A68))
                is FlashcardListUiState.Error -> Text(state.message, color = Color.Red, modifier = Modifier.align(Alignment.Center))
                is FlashcardListUiState.Success -> {
                    if (state.cards.isEmpty()) {
                        Text(stringResource(R.string.no_cards_msg), modifier = Modifier.align(Alignment.Center), color = Color.Gray)
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.cards) { card ->
                                FlashcardListItem(
                                    card = card,
                                    onDelete = { 
                                        card.cardId?.let { cardId ->
                                            Log.w(TAG, "Deleting card with id: $cardId from deckId: $deckId")
                                            viewModel.deleteCard(deckId, cardId)
                                        }
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

/**
 * Card component rendering front and back text of a single flashcard alongside its due status tag and a delete action button.
 *
 * @param card The [Flashcard] entity containing prompt, answer, and scheduling timestamps.
 * @param onDelete Event listener executed when tapping the delete icon button.
 */
@Composable
fun FlashcardListItem(card: Flashcard, onDelete: () -> Unit) {
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
                Text(text = card.frontText, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(text = card.backText, fontSize = 14.sp, color = Color(0xFFE88A68), modifier = Modifier.padding(top = 4.dp))
                
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
 *
 * @param dueDate Epoc time in milliseconds indicating when the card is next scheduled for review.
 * @return Formatted string: "New", "Overdue", "Due today", or "Due: d MMM".
 */
fun formatDueDate(dueDate: Long): String {
    if (dueDate == 0L) return "New"
    
    val now = Calendar.getInstance()
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
