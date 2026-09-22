package com.ntando.ivu.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ntando.ivu.data.prefs.PreferenceManager
import com.ntando.ivu.data.repository.AchievementRepository
import com.ntando.ivu.data.repository.AuthRepository
import com.ntando.ivu.data.repository.ChatRepository
import com.ntando.ivu.data.repository.DeckRepository
import com.ntando.ivu.data.repository.FlashcardRepository
import com.ntando.ivu.data.repository.JournalRepository

/**
 * Custom Factory class responsible for instantiating application ViewModels with required repository and parameter dependencies.
 *
 * Supports single-repository ViewModels as well as multi-dependency ViewModels injected via [Pair] parameters.
 *
 * @property repository Depended repository or [Pair] containing multiple repository/parameter instances.
 */
class ViewModelFactory(private val repository: Any) : ViewModelProvider.Factory {

    companion object {
        private const val TAG = "ViewModelFactory"
    }

    /**
     * Creates a new instance of the specified [ViewModel] class [T].
     *
     * @param modelClass Requested ViewModel type.
     * @return Newly instantiated [ViewModel] instance.
     * @throws IllegalArgumentException If [modelClass] is not registered or supported by this factory.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        Log.d(TAG, "Creating ViewModel for class: ${modelClass.name}")
        return when {
            modelClass.isAssignableFrom(ChatViewModel::class.java) -> {
                Log.d(TAG, "Instantiating ChatViewModel")
                ChatViewModel(repository as ChatRepository) as T
            }

            modelClass.isAssignableFrom(JournalViewModel::class.java) -> {
                Log.d(TAG, "Instantiating JournalViewModel")
                val params = repository as Pair<JournalRepository, DeckRepository>
                JournalViewModel(params.first, params.second) as T
            }

            modelClass.isAssignableFrom(LoginViewModel::class.java) -> {
                Log.d(TAG, "Instantiating LoginViewModel")
                LoginViewModel(repository as AuthRepository) as T
            }

            modelClass.isAssignableFrom(RegisterViewModel::class.java) -> {
                Log.d(TAG, "Instantiating RegisterViewModel")
                RegisterViewModel(repository as AuthRepository) as T
            }

            modelClass.isAssignableFrom(FlashcardViewModel::class.java) -> {
                Log.d(TAG, "Instantiating FlashcardViewModel")
                FlashcardViewModel(repository as FlashcardRepository) as T
            }

            modelClass.isAssignableFrom(DecksViewModel::class.java) -> {
                Log.d(TAG, "Instantiating DecksViewModel")
                DecksViewModel(repository as DeckRepository) as T
            }

            modelClass.isAssignableFrom(FlashcardReviewViewModel::class.java) -> {
                Log.d(TAG, "Instantiating FlashcardReviewViewModel")
                FlashcardReviewViewModel(repository as FlashcardRepository) as T
            }

            modelClass.isAssignableFrom(FlashcardListViewModel::class.java) -> {
                Log.d(TAG, "Instantiating FlashcardListViewModel")
                FlashcardListViewModel(repository as FlashcardRepository) as T
            }

            modelClass.isAssignableFrom(AchievementViewModel::class.java) -> {
                Log.d(TAG, "Instantiating AchievementViewModel")
                val params = repository as Pair<AchievementRepository, String>
                AchievementViewModel(params.first, params.second) as T
            }

            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                Log.d(TAG, "Instantiating SettingsViewModel")
                val params = repository as Pair<AuthRepository, PreferenceManager>
                SettingsViewModel(params.first, params.second) as T
            }

            else -> {
                Log.e(TAG, "Failed to instantiate ViewModel: Unknown class ${modelClass.name}")
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}
