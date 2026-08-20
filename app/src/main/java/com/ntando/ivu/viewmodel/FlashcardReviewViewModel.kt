package com.ntando.ivu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntando.ivu.data.repository.FlashcardRepository
import com.ntando.ivu.network.Flashcard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class FlashcardReviewUiState {
    object Loading : FlashcardReviewUiState()
    data class Success(
        val cards: List<Flashcard>,
        val currentIndex: Int = 0,
        val isFlipped: Boolean = false,
        val remainingCount: Int
    ) : FlashcardReviewUiState()
    data class Error(val message: String) : FlashcardReviewUiState()
    data class SessionComplete(val reviewedCount: Int, val dueTomorrowCount: Int) : FlashcardReviewUiState()
}

class FlashcardReviewViewModel(
    private val repository: FlashcardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<FlashcardReviewUiState>(FlashcardReviewUiState.Loading)
    val uiState: StateFlow<FlashcardReviewUiState> = _uiState.asStateFlow()
    
    private var currentDeckId: String? = null
    private val reviewedCards = mutableListOf<Flashcard>()

    fun loadDueCards(deckId: String) {
        currentDeckId = deckId
        reviewedCards.clear()
        _uiState.value = FlashcardReviewUiState.Loading
        viewModelScope.launch {
            val result = repository.fetchDueCards(deckId)
            result.onSuccess { cards ->
                if (cards.isEmpty()) {
                    _uiState.value = FlashcardReviewUiState.SessionComplete(0, 0)
                } else {
                    _uiState.value = FlashcardReviewUiState.Success(
                        cards = cards,
                        remainingCount = cards.size
                    )
                }
            }.onFailure {
                _uiState.value = FlashcardReviewUiState.Error(it.message ?: "Failed to load cards")
            }
        }
    }

    fun flipCard() {
        val state = _uiState.value
        if (state is FlashcardReviewUiState.Success) {
            _uiState.value = state.copy(isFlipped = !state.isFlipped)
        }
    }

    fun submitReview(rating: String) {
        val state = _uiState.value
        val deckId = currentDeckId ?: return
        
        if (state is FlashcardReviewUiState.Success) {
            val currentCard = state.cards[state.currentIndex]
            val cardId = currentCard.cardId ?: return
            
            viewModelScope.launch {
                val result = repository.submitReview(deckId, cardId, rating)
                result.onSuccess { updatedCard ->
                    reviewedCards.add(updatedCard)
                }
                
                val nextIndex = state.currentIndex + 1
                if (nextIndex < state.cards.size) {
                    _uiState.value = state.copy(
                        currentIndex = nextIndex,
                        isFlipped = false,
                        remainingCount = state.cards.size - nextIndex
                    )
                } else {
                    val dueTomorrow = calculateDueTomorrow(reviewedCards)
                    _uiState.value = FlashcardReviewUiState.SessionComplete(
                        reviewedCount = state.cards.size,
                        dueTomorrowCount = dueTomorrow
                    )
                }
            }
        }
    }

    private fun calculateDueTomorrow(cards: List<Flashcard>): Int {
        val tomorrow = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val dayAfterTomorrow = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, 2)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        return cards.count { it.dueDate in tomorrow until dayAfterTomorrow }
    }
}
