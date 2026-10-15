package com.ntando.ivu.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntando.ivu.data.entity.Deck
import com.ntando.ivu.data.repository.DeckRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state representing flashcard deck collection operations.
 */
sealed class DecksUiState {
    /** State indicating deck data is actively loading. */
    object Loading : DecksUiState()

    /**
     * State representing successful deck retrieval from local Room storage.
     *
     * @property decks The list of user flashcard decks.
     */
    data class Success(val decks: List<Deck>) : DecksUiState()

    /**
     * State representing a failure during deck operation.
     *
     * @property message Detailed error message describing failure cause.
     */
    data class Error(val message: String) : DecksUiState()
}

/**
 * ViewModel managing deck list fetching, creation, and deletion operations.
 *
 * Observes local Room database flows from [DeckRepository] for offline-first display.
 *
 * @property repository The repository providing deck management operations.
 * @property ownerUserId Firebase UID string of user.
 */
class DecksViewModel(
    private val repository: DeckRepository,
    private var ownerUserId: String = ""
) : ViewModel() {

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
        observeDecks()
    }

    /**
     * Updates current user ID and rebinds reactive Room database flows.
     *
     * @param userId Firebase UID string.
     */
    fun setOwnerUserId(userId: String) {
        if (ownerUserId != userId) {
            Log.d(TAG, "setOwnerUserId updated from '$ownerUserId' to '$userId'")
            ownerUserId = userId
            observeDecks()
        }
    }

    /**
     * Observes local Room database Flow for user decks.
     */
    private fun observeDecks() {
        viewModelScope.launch {
            repository.getDecks(ownerUserId).collect { localDecks ->
                Log.d(TAG, "Observed Room decks count: ${localDecks.size} for ownerUserId='$ownerUserId'")
                _uiState.value = DecksUiState.Success(localDecks)
            }
        }
        refreshDecks()
    }

    /**
     * Triggers background network refresh for decks when online.
     */
    fun refreshDecks() {
        Log.d(TAG, "refreshDecks requested for ownerUserId='$ownerUserId'")
        viewModelScope.launch {
            repository.refreshDecks(ownerUserId)
        }
    }

    /**
     * Legacy loadDecks trigger.
     */
    fun loadDecks() {
        refreshDecks()
    }

    /**
     * Creates a new flashcard deck online-only.
     *
     * @param title Title of the deck.
     * @param language Target language for the deck.
     */
    fun createNewDeck(title: String, language: String) {
        Log.i(TAG, "createNewDeck requested with title: \"$title\", language: \"$language\"")
        viewModelScope.launch {
            val result = repository.createDeck(title, language, ownerUserId)
            if (result.isSuccess) {
                Log.i(TAG, "createNewDeck succeeded for title: \"$title\"")
                refreshDecks()
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                Log.e(TAG, "createNewDeck failed for title: \"$title\": $errorMsg", result.exceptionOrNull())
                _uiState.value = DecksUiState.Error(errorMsg)
            }
        }
    }

    /**
     * Deletes a flashcard deck online-only.
     *
     * @param deckId Identifier of the deck to delete.
     */
    fun deleteDeck(deckId: String) {
        Log.i(TAG, "deleteDeck requested for deckId: $deckId")
        viewModelScope.launch {
            val result = repository.deleteDeck(deckId)
            if (result.isSuccess) {
                Log.i(TAG, "deleteDeck succeeded for deckId: $deckId")
                refreshDecks()
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                Log.e(TAG, "deleteDeck failed for deckId: $deckId: $errorMsg", result.exceptionOrNull())
            }
        }
    }
}
