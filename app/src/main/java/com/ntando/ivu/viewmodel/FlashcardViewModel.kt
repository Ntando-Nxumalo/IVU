package com.ntando.ivu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntando.ivu.data.repository.FlashcardRepository
import com.ntando.ivu.network.Flashcard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class FlashcardUiState {
    object Idle : FlashcardUiState()
    object Loading : FlashcardUiState()
    data class Success(val card: Flashcard) : FlashcardUiState()
    data class Error(val message: String) : FlashcardUiState()
}

class FlashcardViewModel(private val repository: FlashcardRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<FlashcardUiState>(FlashcardUiState.Idle)
    val uiState: StateFlow<FlashcardUiState> = _uiState.asStateFlow()

    fun createFlashcard(deckId: String, front: String, back: String, onResult: (Boolean) -> Unit) {
        _uiState.value = FlashcardUiState.Loading
        viewModelScope.launch {
            val result = repository.createCard(deckId, front, back)
            if (result.isSuccess) {
                _uiState.value = FlashcardUiState.Success(result.getOrThrow())
                onResult(true)
            } else {
                _uiState.value = FlashcardUiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
                onResult(false)
            }
        }
    }

    fun deleteFlashcard(deckId: String, cardId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.deleteCard(deckId, cardId)
            onResult(result.isSuccess)
        }
    }
}
