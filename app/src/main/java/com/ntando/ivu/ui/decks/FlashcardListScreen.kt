package com.ntando.ivu.ui.decks

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ntando.ivu.network.Flashcard
import com.ntando.ivu.viewmodel.FlashcardListUiState
import com.ntando.ivu.viewmodel.FlashcardListViewModel
import java.text.SimpleDateFormat
import java.util.*

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
        viewModel.loadCards(deckId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(deckTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                                text = { Text("Delete Deck", color = Color.Red) },
                                onClick = {
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
                        Text("No cards in this deck", modifier = Modifier.align(Alignment.Center), color = Color.Gray)
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.cards) { card ->
                                FlashcardListItem(card = card, onDelete = { 
                                    card.cardId?.let { viewModel.deleteCard(deckId, it) }
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}

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
