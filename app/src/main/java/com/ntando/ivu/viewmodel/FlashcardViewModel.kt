package com.ntando.ivu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntando.ivu.data.entity.Flashcard
import com.ntando.ivu.data.repository.FlashcardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class FlashcardViewModel(private val repository: FlashcardRepository) : ViewModel() {

    fun getFlashcards(deckId: Long): Flow<List<Flashcard>> = repository.getLocalFlashcards(deckId)

    suspend fun getFlashcardById(cardId: Long): Flashcard? = repository.getFlashcardById(cardId)

    fun refreshFlashcards(remoteDeckId: String, localDeckId: Long) {
        viewModelScope.launch {
            repository.refreshFlashcards(remoteDeckId, localDeckId)
        }
    }

    fun reviewCard(remoteDeckId: String, remoteCardId: String, localCard: Flashcard, rating: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.reviewCard(remoteDeckId, remoteCardId, localCard, rating)
            onResult(success)
        }
    }

    fun createFlashcard(remoteDeckId: String, localDeckId: Long, front: String, back: String, image: String?, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.createFlashcard(remoteDeckId, localDeckId, front, back, image)
            onResult(success)
        }
    }

    fun updateFlashcardLocal(flashcard: Flashcard) {
        viewModelScope.launch {
            repository.updateFlashcardLocal(flashcard)
        }
    }

    fun deleteCard(remoteDeckId: String?, remoteCardId: String?, localCard: Flashcard, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.deleteCard(remoteDeckId, remoteCardId, localCard)
            onResult(success)
        }
    }
}
