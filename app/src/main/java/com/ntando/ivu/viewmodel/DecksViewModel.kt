package com.ntando.ivu.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntando.ivu.data.repository.DeckRepository
import com.ntando.ivu.network.Deck
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Represents the UI state for flashcard deck collection operations.
 */
sealed class DecksUiState {
    /** State indicating deck data is actively loading. */
    object Loading : DecksUiState()

    /**
     * State representing successful deck retrieval.
     *
     * @property decks The list of user flashcard decks.
     */
    data class Success(val decks: List<Deck>) : DecksUiState()

    /**
     * State representing a failure during deck operation.
     *
     * @property message Detailed error message describing the failure cause.
     */
    data class Error(val message: String) : DecksUiState()
}

/**
 * ViewModel managing deck list fetching, creation, and deletion operations.
 *
 * Interacts with [DeckRepository] and exposes state via [DecksUiState].
 *
 * @property repository The repository providing deck management operations.
 */
class DecksViewModel(private val repository: DeckRepository) : ViewModel() {

    companion object {
        private const val TAG = "DecksViewModel"
    }

    private val _uiState = MutableStateFlow<DecksUiState>(DecksUiState.Loading)

    /**
     * Immutable StateFlow exposing current [DecksUiState].
     */
    val uiState: StateFlow<DecksUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "DecksViewModel initialized")
        loadDecks()
    }

    /**
     * Fetches all flashcard decks from the repository and updates UI state accordingly.
     */
    fun loadDecks() {
        Log.d(TAG, "loadDecks initiated")
        _uiState.value = DecksUiState.Loading
        viewModelScope.launch {
            val result = repository.fetchDecks()
            if (result.isSuccess) {
                val decks = result.getOrDefault(emptyList())
                Log.i(TAG, "loadDecks succeeded: retrieved ${decks.size} decks")
                _uiState.value = DecksUiState.Success(decks)
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                Log.e(TAG, "loadDecks failed: $errorMsg", result.exceptionOrNull())
                _uiState.value = DecksUiState.Error(errorMsg)
            }
        }
    }

    /**
     * Creates a new flashcard deck and reloads the deck list upon success.
     *
     * @param title Title of the deck.
     * @param language Target language for the deck.
     */
    fun createNewDeck(title: String, language: String) {
        Log.i(TAG, "createNewDeck requested with title: \"$title\", language: \"$language\"")
        viewModelScope.launch {
            val result = repository.createDeck(title, language)
            if (result.isSuccess) {
                Log.i(TAG, "createNewDeck succeeded for title: \"$title\"")
                loadDecks() // Refresh list on success
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                Log.e(TAG, "createNewDeck failed for title: \"$title\": $errorMsg", result.exceptionOrNull())
            }
        }
    }

    /**
     * Deletes a flashcard deck by ID and reloads the deck list upon success.
     *
     * @param deckId Identifier of the deck to delete.
     */
    fun deleteDeck(deckId: String) {
        Log.i(TAG, "deleteDeck requested for deckId: $deckId")
        viewModelScope.launch {
            val result = repository.deleteDeck(deckId)
            if (result.isSuccess) {
                Log.i(TAG, "deleteDeck succeeded for deckId: $deckId")
                loadDecks()
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                Log.e(TAG, "deleteDeck failed for deckId: $deckId: $errorMsg", result.exceptionOrNull())
            }
        }
    }
}
