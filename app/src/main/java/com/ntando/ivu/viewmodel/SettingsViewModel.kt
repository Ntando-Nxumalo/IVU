package com.ntando.ivu.viewmodel

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntando.ivu.data.prefs.PreferenceManager
import com.ntando.ivu.data.repository.AuthRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val authRepository: AuthRepository,
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    private val firebaseUser = authRepository.getCurrentUser()
    val userEmail = firebaseUser?.email ?: "Not signed in"
    val userName = firebaseUser?.displayName ?: "IVU Learner"
    
    val isDarkTheme: StateFlow<Boolean> = preferenceManager.isDarkTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val appLanguage: StateFlow<String> = preferenceManager.appLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en")

    val isRemindersEnabled: StateFlow<Boolean> = preferenceManager.isRemindersEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setTheme(isDark: Boolean) {
        viewModelScope.launch {
            preferenceManager.setTheme(isDark)
            applyTheme(isDark)
        }
    }

    private fun applyTheme(isDark: Boolean) {
        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    fun setLanguage(language: String) {
        viewModelScope.launch {
            preferenceManager.setLanguage(language)
            val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(language)
            AppCompatDelegate.setApplicationLocales(appLocale)
        }
    }

    fun setRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferenceManager.setRemindersEnabled(enabled)
        }
    }

    fun signOut() {
        authRepository.signOut()
    }
}
