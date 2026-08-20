package com.ntando.ivu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ntando.ivu.data.prefs.PreferenceManager
import com.ntando.ivu.data.repository.AchievementRepository
import com.ntando.ivu.data.repository.AuthRepository
import com.ntando.ivu.data.repository.DeckRepository
import com.ntando.ivu.data.repository.FlashcardRepository
import com.ntando.ivu.data.repository.JournalRepository

import com.ntando.ivu.data.repository.ChatRepository

class ViewModelFactory(private val repository: Any) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(repository as ChatRepository) as T
        }
        if (modelClass.isAssignableFrom(JournalViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return JournalViewModel(repository as JournalRepository) as T
        }
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(repository as AuthRepository) as T
        }
        if (modelClass.isAssignableFrom(RegisterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RegisterViewModel(repository as AuthRepository) as T
        }
        if (modelClass.isAssignableFrom(FlashcardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FlashcardViewModel(repository as FlashcardRepository) as T
        }
        if (modelClass.isAssignableFrom(DecksViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DecksViewModel(repository as DeckRepository) as T
        }
        if (modelClass.isAssignableFrom(FlashcardReviewViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FlashcardReviewViewModel(repository as FlashcardRepository) as T
        }
        if (modelClass.isAssignableFrom(AchievementViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            val params = repository as Pair<*, *>
            return AchievementViewModel(params.first as AchievementRepository, params.second as Long) as T
        }
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            val params = repository as Pair<*, *>
            return SettingsViewModel(params.first as AuthRepository, params.second as PreferenceManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
