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
 * UI state representing individual flashcard creation or modification operations.
 */
sealed class FlashcardUiState {
    /** Idle initial state. */
    object Idle : FlashcardUiState()

    /** Loading state while creating or modifying a flashcard. */
    object Loading : FlashcardUiState()

    /**
     * Success state after flashcard creation.
     *
     * @property card The newly created [Flashcard].
     */
    data class Success(val card: Flashcard) : FlashcardUiState()

    /**
     * Error state upon failure.
     *
     * @property message Descriptive error message.
     */
    data class Error(val message: String) : FlashcardUiState()
}

/**
 * ViewModel managing flashcard creation and deletion actions.
 *
 * Interacts with [FlashcardRepository] and communicates status via [FlashcardUiState] and completion callbacks.
 *
 * @property repository The repository handling flashcard management.
 */
class FlashcardViewModel(private val repository: FlashcardRepository) : ViewModel() {

    companion object {
        private const val TAG = "FlashcardViewModel"
    }

    private val _uiState = MutableStateFlow<FlashcardUiState>(FlashcardUiState.Idle)

    /**
     * Immutable StateFlow exposing the current [FlashcardUiState].
     */
    val uiState: StateFlow<FlashcardUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "FlashcardViewModel initialized")
    }

    /**
     * Creates a new flashcard with front and back text content in the given deck.
     *
     * @param deckId Identifier of the deck to add the card into.
     * @param front Front text prompt of the flashcard.
     * @param back Back text answer/translation of the flashcard.
     * @param onResult Callback returning true if creation succeeded, false otherwise.
     */
    fun createFlashcard(deckId: String, front: String, back: String, onResult: (Boolean) -> Unit) {
        Log.i(TAG, "createFlashcard requested for deckId: $deckId, front: \"$front\", back: \"$back\"")
        _uiState.value = FlashcardUiState.Loading
        viewModelScope.launch {
            val result = repository.createCard(deckId, front, back)
            if (result.isSuccess) {
                val createdCard = result.getOrThrow()
                Log.i(TAG, "createFlashcard succeeded: cardId ${createdCard.cardId}")
                _uiState.value = FlashcardUiState.Success(createdCard)
                onResult(true)
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                Log.e(TAG, "createFlashcard failed: $errorMsg", result.exceptionOrNull())
                _uiState.value = FlashcardUiState.Error(errorMsg)
                onResult(false)
            }
        }
    }

    /**
     * Deletes a flashcard from the target deck.
     *
     * @param deckId Identifier of the deck containing the card.
     * @param cardId Identifier of the flashcard to delete.
     * @param onResult Callback returning true if deletion succeeded, false otherwise.
     */
    fun deleteFlashcard(deckId: String, cardId: String, onResult: (Boolean) -> Unit) {
        Log.i(TAG, "deleteFlashcard requested for deckId: $deckId, cardId: $cardId")
        viewModelScope.launch {
            val result = repository.deleteCard(deckId, cardId)
            val success = result.isSuccess
            if (success) {
                Log.i(TAG, "deleteFlashcard succeeded for cardId: $cardId")
            } else {
                Log.e(TAG, "deleteFlashcard failed for cardId: $cardId", result.exceptionOrNull())
            }
            onResult(success)
        }
    }
}
