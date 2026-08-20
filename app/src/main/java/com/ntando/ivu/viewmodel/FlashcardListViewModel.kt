package com.ntando.ivu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntando.ivu.data.repository.FlashcardRepository
import com.ntando.ivu.network.Flashcard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class FlashcardListUiState {
    object Loading : FlashcardListUiState()
    data class Success(val cards: List<Flashcard>) : FlashcardListUiState()
    data class Error(val message: String) : FlashcardListUiState()
}

class FlashcardListViewModel(
    private val repository: FlashcardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<FlashcardListUiState>(FlashcardListUiState.Loading)
    val uiState: StateFlow<FlashcardListUiState> = _uiState.asStateFlow()

    fun loadCards(deckId: String) {
        _uiState.value = FlashcardListUiState.Loading
        viewModelScope.launch {
            val result = repository.fetchAllCards(deckId)
            result.onSuccess { cards ->
                _uiState.value = FlashcardListUiState.Success(cards)
            }.onFailure {
                _uiState.value = FlashcardListUiState.Error(it.message ?: "Failed to load cards")
            }
        }
    }
    
    fun deleteCard(deckId: String, cardId: String) {
        viewModelScope.launch {
            val result = repository.deleteCard(deckId, cardId)
            if (result.isSuccess) {
                loadCards(deckId)
            }
        }
    }
}
