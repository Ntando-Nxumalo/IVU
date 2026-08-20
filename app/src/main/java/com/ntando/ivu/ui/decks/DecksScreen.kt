package com.ntando.ivu.ui.decks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
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
import com.ntando.ivu.network.Deck
import com.ntando.ivu.viewmodel.DecksUiState
import com.ntando.ivu.viewmodel.DecksViewModel
import com.ntando.ivu.viewmodel.FlashcardViewModel
import com.ntando.ivu.viewmodel.FlashcardUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecksScreen(
    viewModel: DecksViewModel,
    flashcardViewModel: FlashcardViewModel,
    onDeckClick: (String) -> Unit,
    onViewCards: (String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val flashcardUiState by flashcardViewModel.uiState.collectAsState()
    
    var showAddDeckDialog by remember { mutableStateOf(false) }
    var selectedDeckForAddCard by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.my_decks), fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFE88A68)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDeckDialog = true },
                containerColor = Color(0xFFE88A68),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_deck))
            }
        },
        containerColor = Color(0xFFFFF8F0)
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                is DecksUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color(0xFFE88A68))
                }
                is DecksUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = state.message, color = Color.Red)
                        Button(onClick = { viewModel.loadDecks() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE88A68))) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
                is DecksUiState.Success -> {
                    if (state.decks.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_decks_msg),
                            modifier = Modifier.align(Alignment.Center),
                            color = Color.Gray
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(state.decks) { deck ->
                                DeckItem(
                                    deck = deck, 
                                    onClick = { deck.deckId?.let { onDeckClick(it) } },
                                    onAddCard = { selectedDeckForAddCard = deck.deckId },
                                    onViewCards = { deck.deckId?.let { onViewCards(it, deck.title) } }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDeckDialog) {
        CreateDeckDialog(
            onDismiss = { showAddDeckDialog = false },
            onConfirm = { title, language ->
                viewModel.createNewDeck(title, language)
                showAddDeckDialog = false
            }
        )
    }

    if (selectedDeckForAddCard != null) {
        AddFlashcardDialog(
            onDismiss = { selectedDeckForAddCard = null },
            onConfirm = { front, back ->
                flashcardViewModel.createFlashcard(selectedDeckForAddCard!!, front, back) { success ->
                    if (success) {
                        selectedDeckForAddCard = null
                        viewModel.loadDecks() // Refresh to update counts
                    }
                }
            },
            isLoading = flashcardUiState is FlashcardUiState.Loading,
            errorMessage = (flashcardUiState as? FlashcardUiState.Error)?.message
        )
    }
}

@Composable
fun DeckItem(deck: Deck, onClick: () -> Unit, onAddCard: () -> Unit, onViewCards: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val (tagName, tagColor) = when (deck.language.lowercase()) {
                    "zu" -> "isiZulu" to Color(0xFFE88A68)
                    "af" -> "Afrikaans" to Color(0xFF008080)
                    else -> "English" to Color(0xFFFFD700)
                }

                Surface(
                    color = tagColor,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = tagName,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onViewCards) {
                        Icon(Icons.Default.Info, contentDescription = "View Cards", tint = Color.Gray)
                    }
                    IconButton(onClick = onAddCard) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_card), tint = Color.Gray)
                    }
                }
            }

            Text(
                text = deck.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3D2B1F),
                modifier = Modifier.padding(top = 8.dp)
            )

            val progress = if (deck.cardCount > 0) 0.25f else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .height(10.dp),
                color = Color(0xFFE88A68),
                trackColor = Color(0xFFEEEEEE),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )

            Text(
                text = stringResource(R.string.due_cards_format, deck.cardCount),
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun CreateDeckDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("en") }
    val languages = listOf("en" to "English", "zu" to "isiZulu", "af" to "Afrikaans")
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_new_deck)) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.deck_title)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(languages.find { it.first == language }?.second ?: stringResource(R.string.select_language))
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        languages.forEach { (code, name) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    language = code
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, language) },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE88A68))
            ) {
                Text(stringResource(R.string.create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
