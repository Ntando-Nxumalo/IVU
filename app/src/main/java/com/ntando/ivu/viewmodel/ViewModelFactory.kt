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
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(ChatViewModel::class.java) -> 
                ChatViewModel(repository as ChatRepository) as T
            
            modelClass.isAssignableFrom(JournalViewModel::class.java) -> {
                val params = repository as Pair<JournalRepository, DeckRepository>
                JournalViewModel(params.first, params.second) as T
            }
            
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> 
                LoginViewModel(repository as AuthRepository) as T
                
            modelClass.isAssignableFrom(RegisterViewModel::class.java) -> 
                RegisterViewModel(repository as AuthRepository) as T
                
            modelClass.isAssignableFrom(FlashcardViewModel::class.java) -> 
                FlashcardViewModel(repository as FlashcardRepository) as T
                
            modelClass.isAssignableFrom(DecksViewModel::class.java) -> 
                DecksViewModel(repository as DeckRepository) as T
                
            modelClass.isAssignableFrom(FlashcardReviewViewModel::class.java) -> 
                FlashcardReviewViewModel(repository as FlashcardRepository) as T
                
            modelClass.isAssignableFrom(FlashcardListViewModel::class.java) -> 
                FlashcardListViewModel(repository as FlashcardRepository) as T
                
            modelClass.isAssignableFrom(AchievementViewModel::class.java) -> {
                val params = repository as Pair<AchievementRepository, String>
                AchievementViewModel(params.first, params.second) as T
            }
            
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                val params = repository as Pair<AuthRepository, PreferenceManager>
                SettingsViewModel(params.first, params.second) as T
            }
            
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
