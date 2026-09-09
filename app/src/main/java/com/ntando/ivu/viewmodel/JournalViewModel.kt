package com.ntando.ivu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntando.ivu.data.repository.JournalRepository
import com.ntando.ivu.network.JournalEntry
import com.ntando.ivu.data.entity.Badge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class JournalUiState {
    object Loading : JournalUiState()
    data class Success(
        val entries: List<JournalEntry>,
        val newlyUnlockedBadges: List<Badge> = emptyList()
    ) : JournalUiState()
    data class Error(val message: String) : JournalUiState()
}

class JournalViewModel(private val repository: JournalRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<JournalUiState>(JournalUiState.Loading)
    val uiState: StateFlow<JournalUiState> = _uiState.asStateFlow()

    init {
        loadEntries()
    }

    fun loadEntries() {
        _uiState.value = JournalUiState.Loading
        viewModelScope.launch {
            val result = repository.fetchEntries()
            result.onSuccess { entries ->
                _uiState.value = JournalUiState.Success(entries)
            }.onFailure {
                _uiState.value = JournalUiState.Error(it.message ?: "Failed to load journal")
            }
        }
    }

    fun createEntry(date: String, mood: String, text: String, linkedDeckId: String?, onResult: (Boolean, List<Badge>) -> Unit) {
        viewModelScope.launch {
            val result = repository.createEntry(date, mood, text, linkedDeckId)
            result.onSuccess { (entry, unlockedBadges) ->
                loadEntries()
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
