package com.ntando.ivu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntando.ivu.data.entity.Deck
import com.ntando.ivu.data.repository.DeckRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class DeckViewModel(private val repository: DeckRepository) : ViewModel() {

    fun getDecks(userId: Long): Flow<List<Deck>> = repository.getLocalDecks(userId)

    fun refreshDecks(userId: Long) {
        viewModelScope.launch {
            repository.refreshDecks(userId)
        }
    }

    fun createDeck(userId: Long, title: String, language: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.createDeck(userId, title, language)
            onResult(success)
        }
    }
}
