package com.ntando.ivu.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntando.ivu.data.entity.Badge
import com.ntando.ivu.data.repository.DeckRepository
import com.ntando.ivu.data.repository.JournalRepository
import com.ntando.ivu.network.Deck
import com.ntando.ivu.network.JournalEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state representing journal entry data operations and available linkable decks.
 */
sealed class JournalUiState {
    /** Loading state while fetching journal entries and decks. */
    object Loading : JournalUiState()

    /**
     * Successful data state.
     *
     * @property entries List of retrieved [JournalEntry] items.
     * @property newlyUnlockedBadges List of [Badge] items unlocked during journal creation.
     * @property decks Available [Deck] items eligible for linking with journal entries.
     */
    data class Success(
        val entries: List<JournalEntry>,
        val newlyUnlockedBadges: List<Badge> = emptyList(),
        val decks: List<Deck> = emptyList()
    ) : JournalUiState()

    /**
     * Error state upon failure loading journal or deck data.
     *
     * @property message Descriptive error message.
     */
    data class Error(val message: String) : JournalUiState()
}

/**
 * ViewModel managing journal entry fetching, creating entries linked to decks, and handling unlocked badges.
 *
 * Coordinates data operations between [JournalRepository] and [DeckRepository].
 *
 * @property repository Repository for journal entry operations.
 * @property deckRepository Repository for deck retrieval operations.
 */
class JournalViewModel(
    private val repository: JournalRepository,
    private val deckRepository: DeckRepository
) : ViewModel() {

    companion object {
        private const val TAG = "JournalViewModel"
    }

    private val _uiState = MutableStateFlow<JournalUiState>(JournalUiState.Loading)

    /**
     * Immutable StateFlow exposing current [JournalUiState].
     */
    val uiState: StateFlow<JournalUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "JournalViewModel initialized")
        loadData()
    }

    /**
     * Concurrently loads journal entries and user decks from repositories.
     */
    fun loadData() {
        Log.d(TAG, "loadData initiated")
        _uiState.value = JournalUiState.Loading
        viewModelScope.launch {
            val journalResult = repository.fetchEntries()
            val decksResult = deckRepository.fetchDecks()

            if (journalResult.isSuccess && decksResult.isSuccess) {
                val entries = journalResult.getOrDefault(emptyList())
                val decks = decksResult.getOrDefault(emptyList())
                Log.i(TAG, "loadData succeeded: fetched ${entries.size} entries and ${decks.size} decks")
                _uiState.value = JournalUiState.Success(
                    entries = entries,
                    decks = decks
                )
            } else {
                val journalError = journalResult.exceptionOrNull()?.message
                val decksError = decksResult.exceptionOrNull()?.message
                val errorMsg = journalError ?: decksError ?: "Failed to load data"
                Log.e(TAG, "loadData failed: journalError=$journalError, decksError=$decksError")
                _uiState.value = JournalUiState.Error(errorMsg)
            }
        }
    }

    /**
     * Creates a new journal entry and updates UI state with unlocked achievement badges.
     *
     * @param date Timestamp or formatted date string of the entry.
     * @param mood Recorded user mood value.
     * @param text Journal text body content.
     * @param linkedDeckId Optional identifier of a deck associated with this journal entry.
     * @param onResult Callback yielding (success: Boolean, unlockedBadges: List<Badge>).
     */
    fun createEntry(
        date: String,
        mood: String,
        text: String,
        linkedDeckId: String?,
        onResult: (Boolean, List<Badge>) -> Unit
    ) {
        Log.i(TAG, "createEntry requested for date: $date, mood: $mood, linkedDeckId: $linkedDeckId")
        viewModelScope.launch {
            val result = repository.createEntry(date, mood, text, linkedDeckId)
            result.onSuccess { (entry, unlockedBadges) ->
                Log.i(TAG, "createEntry succeeded for entryId: ${entry.entryId}, unlocked ${unlockedBadges.size} badges")
                loadData()
                val currentState = _uiState.value
                if (currentState is JournalUiState.Success) {
                    _uiState.value = currentState.copy(newlyUnlockedBadges = unlockedBadges)
                }
                onResult(true, unlockedBadges)
            }.onFailure { throwable ->
                Log.e(TAG, "createEntry failed: ${throwable.message}", throwable)
                onResult(false, emptyList())
            }
        }
    }

    /**
     * Clears newly unlocked badges from the current UI state.
     */
    fun clearUnlockedBadges() {
        val state = _uiState.value
        if (state is JournalUiState.Success) {
            Log.d(TAG, "clearUnlockedBadges called")
            _uiState.value = state.copy(newlyUnlockedBadges = emptyList())
        }
    }
}
