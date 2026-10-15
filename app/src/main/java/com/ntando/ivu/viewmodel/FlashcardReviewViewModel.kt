package com.ntando.ivu.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntando.ivu.data.entity.Badge
import com.ntando.ivu.data.entity.Flashcard
import com.ntando.ivu.data.repository.FlashcardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state representing the flashcard review interactive session.
 */
sealed class FlashcardReviewUiState {
    /** Indicates due cards are being fetched. */
    object Loading : FlashcardReviewUiState()

    /**
     * Active flashcard review state.
     *
     * @property cards The list of due [Flashcard]s in the current review batch.
     * @property currentIndex Index of the card currently presented to the user.
     * @property isFlipped Whether the card is flipped showing the answer/back side.
     * @property remainingCount Count of remaining cards in this session.
     * @property newlyUnlockedBadges List of [Badge]s unlocked during the latest card review submission.
     */
    data class Success(
        val cards: List<Flashcard>,
        val currentIndex: Int = 0,
        val isFlipped: Boolean = false,
        val remainingCount: Int,
        val newlyUnlockedBadges: List<Badge> = emptyList()
    ) : FlashcardReviewUiState()

    /**
     * Error state during loading or submit operations.
     *
     * @property message Descriptive error message.
     */
    data class Error(val message: String) : FlashcardReviewUiState()

    /**
     * Indicates completion of the review session.
     *
     * @property reviewedCount Total count of cards reviewed during this session.
     * @property dueTomorrowCount Calculated count of cards scheduled for review tomorrow.
     * @property newlyUnlockedBadges All [Badge]s unlocked throughout the session.
     */
    data class SessionComplete(
        val reviewedCount: Int,
        val dueTomorrowCount: Int,
        val newlyUnlockedBadges: List<Badge> = emptyList()
    ) : FlashcardReviewUiState()
}

/**
 * ViewModel managing flashcard review workflows, SRS (Spaced Repetition System) rating submissions,
 * card flipping state, and unlocked achievement badges.
 *
 * Observes local Room database flows from [FlashcardRepository] for offline-first reviews.
 *
 * @property repository The repository handling flashcard review submissions and due card queries.
 * @property ownerUserId Firebase UID string of user.
 */
class FlashcardReviewViewModel(
    private val repository: FlashcardRepository,
    private var ownerUserId: String = ""
) : ViewModel() {

    companion object {
        private const val TAG = "FlashcardReviewViewModel"
    }

    private val _uiState = MutableStateFlow<FlashcardReviewUiState>(FlashcardReviewUiState.Loading)

    /**
     * Immutable StateFlow exposing the current [FlashcardReviewUiState].
     */
    val uiState: StateFlow<FlashcardReviewUiState> = _uiState.asStateFlow()

    private var currentDeckId: String? = null
    private val reviewedCards = mutableListOf<Flashcard>()
    private val sessionUnlockedBadges = mutableListOf<Badge>()
    private var isSubmitting = false

    init {
        Log.d(TAG, "FlashcardReviewViewModel initialized")
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
     * Fetches due flashcards for review for a given [deckId] from Room database.
     *
     * @param deckId Identifier of the deck to review.
     */
    fun loadDueCards(deckId: String) {
        Log.d(TAG, "loadDueCards started for deckId: $deckId, ownerUserId: '$ownerUserId'")
        currentDeckId = deckId
        reviewedCards.clear()
        sessionUnlockedBadges.clear()
        _uiState.value = FlashcardReviewUiState.Loading

        val deckLocalId = deckId.toLongOrNull() ?: 0L

        viewModelScope.launch {
            repository.getDueCards(deckLocalId, ownerUserId).collect { cards ->
                Log.i(TAG, "Observed ${cards.size} due cards in Room for deck $deckId")
                if (cards.isEmpty()) {
                    _uiState.value = FlashcardReviewUiState.SessionComplete(reviewedCards.size, calculateDueTomorrow(reviewedCards), sessionUnlockedBadges)
                } else {
                    _uiState.value = FlashcardReviewUiState.Success(
                        cards = cards,
                        remainingCount = cards.size
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.refreshCards(deckLocalId, deckId, ownerUserId)
        }
    }

    /**
     * Flips the active flashcard between front and back views.
     */
    fun flipCard() {
        val state = _uiState.value
        if (state is FlashcardReviewUiState.Success) {
            val newFlippedState = !state.isFlipped
            Log.d(TAG, "flipCard toggled to isFlipped: $newFlippedState for card index ${state.currentIndex}")
            _uiState.value = state.copy(isFlipped = newFlippedState)
        } else {
            Log.w(TAG, "flipCard ignored: state is not Success")
        }
    }

    /**
     * Submits a spaced repetition review rating for the current flashcard.
     *
     * Advances to the next card or completes the session if all cards are reviewed.
     *
     * @param rating Rating assigned by the user (e.g. "again", "hard", "good", "easy").
     */
    fun submitReview(rating: String) {
        if (isSubmitting) {
            Log.w(TAG, "submitReview ignored: submit is already in progress")
            return
        }

        val state = _uiState.value
        val deckId = currentDeckId
        if (deckId == null) {
            Log.e(TAG, "submitReview failed: currentDeckId is null")
            return
        }

        if (state is FlashcardReviewUiState.Success) {
            val currentCard = state.cards[state.currentIndex]
            val cardLocalId = currentCard.cardId

            Log.i(TAG, "submitReview submitted for cardLocalId $cardLocalId in deck $deckId with rating: $rating")
            isSubmitting = true
            viewModelScope.launch {
                val result = repository.submitReview(cardLocalId, deckId, rating, ownerUserId)
                isSubmitting = false
                result.onSuccess { (updatedCard, newlyUnlocked) ->
                    Log.i(TAG, "submitReview succeeded for cardLocalId $cardLocalId, newly unlocked badges count: ${newlyUnlocked.size}")
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
                        Log.i(TAG, "Review session completed. Total reviewed: ${state.cards.size}, Due tomorrow: $dueTomorrow")
                        _uiState.value = FlashcardReviewUiState.SessionComplete(
                            reviewedCount = state.cards.size,
                            dueTomorrowCount = dueTomorrow,
                            newlyUnlockedBadges = sessionUnlockedBadges.toList()
                        )
                    }
                }.onFailure { throwable ->
                    Log.e(TAG, "submitReview failed for cardLocalId $cardLocalId: ${throwable.message}", throwable)
                }
            }
        } else {
            Log.w(TAG, "submitReview ignored: state is not Success")
        }
    }

    /**
     * Clears newly unlocked badges from the current UI state.
     */
    fun clearUnlockedBadges() {
        val state = _uiState.value
        if (state is FlashcardReviewUiState.Success) {
            Log.d(TAG, "clearUnlockedBadges called")
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

        val count = cards.count { it.dueDate in tomorrow until dayAfterTomorrow }
        Log.d(TAG, "calculateDueTomorrow computed $count cards due tomorrow")
        return count
    }
}
