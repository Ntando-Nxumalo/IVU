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

    val userEmail = authRepository.getCurrentUser()?.email ?: "Not signed in"
    
    val isDarkTheme: StateFlow<Boolean> = preferenceManager.isDarkTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val appLanguage: StateFlow<String> = preferenceManager.appLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en")

    fun setTheme(isDark: Boolean) {
        viewModelScope.launch {
            preferenceManager.setTheme(isDark)
        }
    }

    fun setLanguage(language: String) {
        viewModelScope.launch {
            preferenceManager.setLanguage(language)
            val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(language)
            AppCompatDelegate.setApplicationLocales(appLocale)
        }
    }

    fun signOut() {
        authRepository.signOut()
    }
}
