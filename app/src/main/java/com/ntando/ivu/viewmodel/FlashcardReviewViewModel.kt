package com.ntando.ivu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntando.ivu.data.repository.FlashcardRepository
import com.ntando.ivu.network.Flashcard
import com.ntando.ivu.data.entity.Badge
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
        val remainingCount: Int,
        val newlyUnlockedBadges: List<Badge> = emptyList()
    ) : FlashcardReviewUiState()
    data class Error(val message: String) : FlashcardReviewUiState()
    data class SessionComplete(
        val reviewedCount: Int, 
        val dueTomorrowCount: Int,
        val newlyUnlockedBadges: List<Badge> = emptyList()
    ) : FlashcardReviewUiState()
}

class FlashcardReviewViewModel(
    private val repository: FlashcardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<FlashcardReviewUiState>(FlashcardReviewUiState.Loading)
    val uiState: StateFlow<FlashcardReviewUiState> = _uiState.asStateFlow()
    
    private var currentDeckId: String? = null
    private val reviewedCards = mutableListOf<Flashcard>()
    private val sessionUnlockedBadges = mutableListOf<Badge>()
    private var isSubmitting = false

    fun loadDueCards(deckId: String) {
        currentDeckId = deckId
        reviewedCards.clear()
        sessionUnlockedBadges.clear()
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
        if (isSubmitting) return
        
        val state = _uiState.value
        val deckId = currentDeckId ?: return
        
        if (state is FlashcardReviewUiState.Success) {
            val currentCard = state.cards[state.currentIndex]
            val cardId = currentCard.cardId ?: return
            
            isSubmitting = true
            viewModelScope.launch {
                val result = repository.submitReview(deckId, cardId, rating)
                isSubmitting = false
                result.onSuccess { (updatedCard, newlyUnlocked) ->
                    reviewedCards.add(updatedCard)
                    sessionUnlockedBadges.addAll(newlyUnlocked)
                    
                    val nextIndex = state.currentIndex + 1
                    if (nextIndex < state.cards.size) {
                        _uiState.value = state.copy(
                            currentIndex = nextIndex,
                            isFlipped = false,
                            remainingCount = state.cards.size - nextIndex,
                            newlyUnlockedBadges = newlyUnlocked
                        )
                    } else {
                        val dueTomorrow = calculateDueTomorrow(reviewedCards)
                        _uiState.value = FlashcardReviewUiState.SessionComplete(
                            reviewedCount = state.cards.size,
                            dueTomorrowCount = dueTomorrow,
                            newlyUnlockedBadges = sessionUnlockedBadges.toList()
                        )
                    }
                }
            }
        }
    }

    fun clearUnlockedBadges() {
        val state = _uiState.value
        if (state is FlashcardReviewUiState.Success) {
            _uiState.value = state.copy(newlyUnlockedBadges = emptyList())
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
