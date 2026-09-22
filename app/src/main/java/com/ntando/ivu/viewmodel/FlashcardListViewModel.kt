package com.ntando.ivu.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntando.ivu.data.repository.FlashcardRepository
import com.ntando.ivu.network.Flashcard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state representing flashcard list operations for a target deck.
 */
sealed class FlashcardListUiState {
    /** Indicates flashcards are currently loading. */
    object Loading : FlashcardListUiState()

    /**
     * Indicates flashcards were retrieved successfully.
     *
     * @property cards The list of retrieved [Flashcard] items.
     */
    data class Success(val cards: List<Flashcard>) : FlashcardListUiState()

    /**
     * Indicates an error occurred during flashcard loading.
     *
     * @property message Descriptive error message.
     */
    data class Error(val message: String) : FlashcardListUiState()
}

/**
 * ViewModel managing flashcard list retrieving and deleting operations for a specific deck.
 *
 * Uses [FlashcardRepository] and exposes [FlashcardListUiState].
 *
 * @property repository The repository handling flashcard network/data operations.
 */
class FlashcardListViewModel(
    private val repository: FlashcardRepository
) : ViewModel() {

    companion object {
        private const val TAG = "FlashcardListViewModel"
    }

    private val _uiState = MutableStateFlow<FlashcardListUiState>(FlashcardListUiState.Loading)

    /**
     * Immutable StateFlow producing the current [FlashcardListUiState].
     */
    val uiState: StateFlow<FlashcardListUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "FlashcardListViewModel initialized")
    }

    /**
     * Loads all flashcards associated with the given [deckId].
     *
     * @param deckId The unique identifier of the deck to load cards for.
     */
    fun loadCards(deckId: String) {
        Log.d(TAG, "loadCards requested for deckId: $deckId")
        _uiState.value = FlashcardListUiState.Loading
        viewModelScope.launch {
            val result = repository.fetchAllCards(deckId)
            result.onSuccess { cards ->
                Log.i(TAG, "loadCards succeeded for deckId $deckId: loaded ${cards.size} cards")
                _uiState.value = FlashcardListUiState.Success(cards)
            }.onFailure { throwable ->
                val errorMsg = throwable.message ?: "Failed to load cards"
                Log.e(TAG, "loadCards failed for deckId $deckId: $errorMsg", throwable)
                _uiState.value = FlashcardListUiState.Error(errorMsg)
            }
        }
    }

    /**
     * Deletes a specific flashcard in a deck and reloads cards upon success.
     *
     * @param deckId Identifier of the deck containing the card.
     * @param cardId Identifier of the card to be deleted.
     */
    fun deleteCard(deckId: String, cardId: String) {
        Log.i(TAG, "deleteCard requested for deckId: $deckId, cardId: $cardId")
        viewModelScope.launch {
            val result = repository.deleteCard(deckId, cardId)
            if (result.isSuccess) {
                Log.i(TAG, "deleteCard succeeded for cardId $cardId in deck $deckId")
                loadCards(deckId)
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                Log.e(TAG, "deleteCard failed for cardId $cardId in deck $deckId: $errorMsg", result.exceptionOrNull())
            }
        }
    }
}
