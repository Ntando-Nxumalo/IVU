package com.ntando.ivu.ui.decks

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
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
import com.ntando.ivu.network.Deck
import com.ntando.ivu.viewmodel.DecksUiState
import com.ntando.ivu.viewmodel.DecksViewModel
import com.ntando.ivu.viewmodel.FlashcardViewModel
import com.ntando.ivu.viewmodel.FlashcardUiState

private const val TAG = "DecksScreen"

/**
 * Main management screen for displaying, creating, reviewing, and deleting flashcard decks.
 *
 * Layout Structure:
 * - [Scaffold] containing top bar, bottom navigation, and a floating action button for adding decks.
 * - State handling via [DecksUiState]: Loading spinner, error display with retry button, or empty state message / [LazyColumn].
 * - Card list displaying [DeckItem] components with metadata, language tag, mastery progress bar, and action buttons.
 * - [CreateDeckDialog] overlay for creating new decks with title and target language selection.
 * - [AddFlashcardDialog] overlay for creating flashcards within a selected deck.
 * - [AlertDialog] confirmation for deleting a deck.
 *
 * @param viewModel ViewModel handling deck listing, deck creation, and deletion operations.
 * @param flashcardViewModel ViewModel managing individual flashcard additions.
 * @param onDeckClick Callback triggered when tapping a deck card to start a review session: `(deckId) -> Unit`.
 * @param onViewCards Callback triggered to navigate to the detailed flashcard list screen: `(deckId, deckTitle) -> Unit`.
 * @param onNavigate Navigation callback triggered when selecting a bottom navigation tab.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecksScreen(
    viewModel: DecksViewModel,
    flashcardViewModel: FlashcardViewModel,
    onDeckClick: (String) -> Unit,
    onViewCards: (String, String) -> Unit,
    onNavigate: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val flashcardUiState by flashcardViewModel.uiState.collectAsState()
    
    var showAddDeckDialog by remember { mutableStateOf(false) }
    var selectedDeckForAddCard by remember { mutableStateOf<String?>(null) }
    var deckToDelete by remember { mutableStateOf<Deck?>(null) }

    LaunchedEffect(uiState) {
        Log.d(TAG, "Observed DecksUiState change: $uiState")
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.my_decks), fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFE88A68)
                )
            )
        },
        bottomBar = {
            com.ntando.ivu.ui.components.BottomNavigationBar(
                currentScreen = "decks",
                onNavigate = { route ->
                    Log.d(TAG, "Navigating from DecksScreen to route: $route")
                    onNavigate(route)
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    Log.d(TAG, "FAB clicked to show CreateDeckDialog")
                    showAddDeckDialog = true
                },
                containerColor = Color(0xFFE88A68),
                contentColor = Color.White,
                shape = androidx.compose.foundation.shape.CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_deck))
            }
        }
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
                        Button(
                            onClick = {
                                Log.i(TAG, "Retry button clicked to reload decks")
                                viewModel.loadDecks()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE88A68))
                        ) {
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
                                    onClick = {
                                        deck.deckId?.let { id ->
                                            Log.d(TAG, "Deck clicked for review session: id=$id, title=${deck.title}")
                                            onDeckClick(id)
                                        }
                                    },
                                    onAddCard = {
                                        Log.d(TAG, "Add card button clicked for deckId: ${deck.deckId}")
                                        selectedDeckForAddCard = deck.deckId
                                    },
                                    onViewCards = {
                                        deck.deckId?.let { id ->
                                            Log.d(TAG, "View cards button clicked for deckId: $id, title: ${deck.title}")
                                            onViewCards(id, deck.title)
                                        }
                                    },
                                    onDelete = {
                                        Log.d(TAG, "Delete deck action triggered for deck: ${deck.title}")
                                        deckToDelete = deck
                                    }
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
            onDismiss = {
                Log.d(TAG, "Dismissing CreateDeckDialog")
                showAddDeckDialog = false
            },
            onConfirm = { title, language ->
                Log.i(TAG, "Creating deck with title='$title', language='$language'")
                viewModel.createNewDeck(title, language)
                showAddDeckDialog = false
            }
        )
    }

    if (selectedDeckForAddCard != null) {
        AddFlashcardDialog(
            onDismiss = {
                Log.d(TAG, "Dismissing AddFlashcardDialog")
                selectedDeckForAddCard = null
            },
            onConfirm = { front, back ->
                val deckId = selectedDeckForAddCard!!
                Log.i(TAG, "Creating card in deckId=$deckId with front='$front'")
                flashcardViewModel.createFlashcard(deckId, front, back) { success ->
                    if (success) {
                        Log.d(TAG, "Flashcard created successfully, reloading decks")
                        selectedDeckForAddCard = null
                        viewModel.loadDecks() // Refresh to update counts
                    } else {
                        Log.w(TAG, "Flashcard creation failed")
                    }
                }
            },
            isLoading = flashcardUiState is FlashcardUiState.Loading,
            errorMessage = (flashcardUiState as? FlashcardUiState.Error)?.message
        )
    }

    if (deckToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                Log.d(TAG, "Dismissing delete deck confirmation dialog")
                deckToDelete = null
            },
            title = { Text(stringResource(R.string.delete_deck_title)) },
            text = { Text(stringResource(R.string.delete_deck_confirm, deckToDelete?.title ?: "")) },
            confirmButton = {
                Button(
                    onClick = {
                        deckToDelete?.deckId?.let { id ->
                            Log.w(TAG, "Confirmed deletion of deckId: $id")
                            viewModel.deleteDeck(id)
                        }
                        deckToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text(stringResource(R.string.delete), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    Log.d(TAG, "Canceled deck deletion dialog")
                    deckToDelete = null
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

/**
 * Card UI representing a single deck item in the list.
 * Shows language tags, action icons (view cards, add card, delete deck), title, progress indicator, and card counts.
 *
 * @param deck The [Deck] model object containing deck metadata.
 * @param onClick Event listener for clicking on the card body to enter review mode.
 * @param onAddCard Event listener for tapping the add card icon.
 * @param onViewCards Event listener for tapping the view cards icon.
 * @param onDelete Event listener for tapping the delete deck icon.
 */
@Composable
fun DeckItem(
    deck: Deck,
    onClick: () -> Unit,
    onAddCard: () -> Unit,
    onViewCards: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val (tagName, tagColor) = when (deck.language.lowercase()) {
                    "zu" -> stringResource(R.string.label_isizulu) to Color(0xFFE88A68)
                    "af" -> stringResource(R.string.label_afrikaans) to Color(0xFF7FB6A7)
                    else -> stringResource(R.string.label_english) to Color(0xFFE8C07C)
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
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Deck", tint = Color.LightGray)
                    }
                }
            }

            Text(
                text = deck.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )

            val progress = 0f 
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF7FB6A7),
                trackColor = Color(0xFFEEEEEE)
            )

            Text(
                text = stringResource(R.string.cards_mastered_format, 0, deck.cardCount),
                fontSize = 12.sp,
                color = Color.LightGray,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/**
 * Modal dialog for inputting new deck details (Title and Target Language).
 *
 * @param onDismiss Callback to dismiss the dialog.
 * @param onConfirm Callback submitting the title and language string code: `(title, languageCode) -> Unit`.
 */
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
                                    Log.d(TAG, "Selected language option: $name ($code)")
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
