package com.ntando.ivu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntando.ivu.data.repository.DeckRepository
import com.ntando.ivu.network.Deck
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DecksUiState {
    object Loading : DecksUiState()
    data class Success(val decks: List<Deck>) : DecksUiState()
    data class Error(val message: String) : DecksUiState()
}

class DecksViewModel(private val repository: DeckRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<DecksUiState>(DecksUiState.Loading)
    val uiState: StateFlow<DecksUiState> = _uiState.asStateFlow()

    init {
        loadDecks()
    }

    fun loadDecks() {
        _uiState.value = DecksUiState.Loading
        viewModelScope.launch {
            val result = repository.fetchDecks()
            if (result.isSuccess) {
                _uiState.value = DecksUiState.Success(result.getOrDefault(emptyList()))
            } else {
                _uiState.value = DecksUiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }

    fun createNewDeck(title: String, language: String) {
        viewModelScope.launch {
            val result = repository.createDeck(title, language)
            if (result.isSuccess) {
                loadDecks() // Refresh list on success
            } else {
                // Optionally handle error here, e.g. expose a side-effect flow for Toast
            }
        }
    }

    fun deleteDeck(deckId: String) {
        viewModelScope.launch {
            val result = repository.deleteDeck(deckId)
            if (result.isSuccess) {
                loadDecks()
            }
        }
    }
}
