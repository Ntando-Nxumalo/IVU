package com.ntando.ivu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntando.ivu.data.repository.JournalRepository
import com.ntando.ivu.data.repository.DeckRepository
import com.ntando.ivu.network.JournalEntry
import com.ntando.ivu.network.Deck
import com.ntando.ivu.data.entity.Badge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class JournalUiState {
    object Loading : JournalUiState()
    data class Success(
        val entries: List<JournalEntry>,
        val newlyUnlockedBadges: List<Badge> = emptyList(),
        val decks: List<Deck> = emptyList()
    ) : JournalUiState()
    data class Error(val message: String) : JournalUiState()
}

class JournalViewModel(
    private val repository: JournalRepository,
    private val deckRepository: DeckRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<JournalUiState>(JournalUiState.Loading)
    val uiState: StateFlow<JournalUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        _uiState.value = JournalUiState.Loading
        viewModelScope.launch {
            val journalResult = repository.fetchEntries()
            val decksResult = deckRepository.fetchDecks()
            
            if (journalResult.isSuccess && decksResult.isSuccess) {
                _uiState.value = JournalUiState.Success(
                    entries = journalResult.getOrDefault(emptyList()),
                    decks = decksResult.getOrDefault(emptyList())
                )
            } else {
                val error = journalResult.exceptionOrNull()?.message ?: decksResult.exceptionOrNull()?.message ?: "Failed to load data"
                _uiState.value = JournalUiState.Error(error)
            }
        }
    }

    fun createEntry(date: String, mood: String, text: String, linkedDeckId: String?, onResult: (Boolean, List<Badge>) -> Unit) {
        viewModelScope.launch {
            val result = repository.createEntry(date, mood, text, linkedDeckId)
            result.onSuccess { (entry, unlockedBadges) ->
                loadData()
                val currentState = _uiState.value
                if (currentState is JournalUiState.Success) {
                    _uiState.value = currentState.copy(newlyUnlockedBadges = unlockedBadges)
                }
                onResult(true, unlockedBadges)
            }.onFailure {
                onResult(false, emptyList())
            }
        }
    }
    
    fun clearUnlockedBadges() {
        val state = _uiState.value
        if (state is JournalUiState.Success) {
            _uiState.value = state.copy(newlyUnlockedBadges = emptyList())
        }
    }
}
