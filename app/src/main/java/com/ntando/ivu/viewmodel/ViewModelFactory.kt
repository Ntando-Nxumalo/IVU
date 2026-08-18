package com.ntando.ivu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ntando.ivu.data.repository.DeckRepository
import com.ntando.ivu.data.repository.FlashcardRepository
import com.ntando.ivu.data.repository.JournalRepository

class ViewModelFactory(private val repository: Any) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DeckViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DeckViewModel(repository as DeckRepository) as T
        }
        if (modelClass.isAssignableFrom(JournalViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return JournalViewModel(repository as JournalRepository) as T
        }
        if (modelClass.isAssignableFrom(FlashcardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FlashcardViewModel(repository as FlashcardRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
