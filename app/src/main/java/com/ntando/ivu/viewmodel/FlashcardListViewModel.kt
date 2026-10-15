package com.ntando.ivu.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntando.ivu.data.entity.Flashcard
import com.ntando.ivu.data.repository.FlashcardRepository
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
     * Indicates flashcards were retrieved successfully from Room storage.
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
 * Observes local Room database flows from [FlashcardRepository].
 *
 * @property repository The repository handling flashcard network/data operations.
 * @property ownerUserId Firebase UID string of user.
 */
class FlashcardListViewModel(
    private val repository: FlashcardRepository,
    private var ownerUserId: String = ""
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
     * Updates current user ID.
     *
     * @param userId Firebase UID string.
     */
    fun setOwnerUserId(userId: String) {
        if (ownerUserId != userId) {
            Log.d(TAG, "setOwnerUserId updated to '$userId'")
            ownerUserId = userId
        }
    }

    /**
     * Loads all flashcards associated with the given [deckId] from Room and triggers background refresh.
     *
     * @param deckId The unique identifier of the deck to load cards for.
     */
    fun loadCards(deckId: String) {
        Log.d(TAG, "loadCards requested for deckId: $deckId, ownerUserId: '$ownerUserId'")
        val deckLocalId = deckId.toLongOrNull() ?: 0L
        viewModelScope.launch {
            repository.getCards(deckLocalId, ownerUserId).collect { cards ->
                Log.i(TAG, "Observed Room cards for deckId $deckId: loaded ${cards.size} cards")
                _uiState.value = FlashcardListUiState.Success(cards)
            }
        }
        viewModelScope.launch {
            repository.refreshCards(deckLocalId, deckId, ownerUserId)
        }
    }

    /**
     * Deletes a specific flashcard in a deck locally and online if connected.
     *
     * @param deckId Identifier of the deck containing the card.
     * @param cardId Identifier of the card to be deleted.
     */
    fun deleteCard(deckId: String, cardId: String) {
        Log.i(TAG, "deleteCard requested for deckId: $deckId, cardId: $cardId")
        val deckLocalId = deckId.toLongOrNull() ?: 0L
        val cardLocalId = cardId.toLongOrNull() ?: 0L
        viewModelScope.launch {
            repository.deleteCard(cardLocalId, cardId, deckLocalId, deckId, ownerUserId)
        }
    }
}
