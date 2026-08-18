package com.ntando.ivu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntando.ivu.data.entity.JournalEntry
import com.ntando.ivu.data.repository.JournalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class JournalViewModel(private val repository: JournalRepository) : ViewModel() {

    fun getJournalEntries(userId: Long): Flow<List<JournalEntry>> = repository.getLocalEntries(userId)

    fun refreshJournalEntries(userId: Long) {
        viewModelScope.launch {
            repository.refreshJournalEntries(userId)
        }
    }

    fun createJournalEntry(
        userId: Long,
        date: String,
        mood: String,
        text: String,
        deckId: String?,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val success = repository.createEntry(userId, date, mood, text, deckId)
            onResult(success)
        }
    }
}
