package com.ntando.ivu.viewmodel

import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntando.ivu.data.prefs.PreferenceManager
import com.ntando.ivu.data.repository.AuthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel managing user preferences, app language, theme modes, reminder settings, and account sign-out.
 *
 * Coordinates reading and writing persistent settings via [PreferenceManager] and user session state via [AuthRepository].
 *
 * @property authRepository Repository handling user authentication and session status.
 * @property preferenceManager Manager handling persistent preferences.
 */
class SettingsViewModel(
    private val authRepository: AuthRepository,
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    companion object {
        private const val TAG = "SettingsViewModel"
    }

    private val firebaseUser = authRepository.getCurrentUser()

    /** User email retrieved from current Firebase authentication state. */
    val userEmail = firebaseUser?.email ?: "Not signed in"

    /** User display name retrieved from current Firebase authentication state. */
    val userName = firebaseUser?.displayName ?: "IVU Learner"

    init {
        Log.d(TAG, "SettingsViewModel initialized for user: $userName ($userEmail)")
    }

    /**
     * StateFlow pipeline reflecting dark theme preference state.
     */
    val isDarkTheme: StateFlow<Boolean> = preferenceManager.isDarkTheme
        .onEach { isDark ->
            Log.d(TAG, "isDarkTheme preference emitted: $isDark")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /**
     * StateFlow pipeline reflecting current app language ISO code.
     */
    val appLanguage: StateFlow<String> = preferenceManager.appLanguage
        .onEach { lang ->
            Log.d(TAG, "appLanguage preference emitted: $lang")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en")

    /**
     * StateFlow pipeline reflecting whether study reminders are enabled.
     */
    val isRemindersEnabled: StateFlow<Boolean> = preferenceManager.isRemindersEnabled
        .onEach { enabled ->
            Log.d(TAG, "isRemindersEnabled preference emitted: $enabled")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    /**
     * Updates theme preference and applies day/night mode to AppCompatDelegate.
     *
     * @param isDark True to enable dark theme, false for light theme.
     */
    fun setTheme(isDark: Boolean) {
        Log.i(TAG, "setTheme called with isDark: $isDark")
        viewModelScope.launch {
            preferenceManager.setTheme(isDark)
            applyTheme(isDark)
        }
    }

    /**
     * Internal helper applying night mode setting to [AppCompatDelegate].
     *
     * @param isDark True for night mode, false for day mode.
     */
    private fun applyTheme(isDark: Boolean) {
        Log.d(TAG, "applyTheme executing for isDark: $isDark")
        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    /**
     * Updates app language preference and sets app locales dynamically via [AppCompatDelegate].
     *
     * @param language ISO language tag string (e.g. "en", "es").
     */
    fun setLanguage(language: String) {
        Log.i(TAG, "setLanguage called with language: $language")
        viewModelScope.launch {
            preferenceManager.setLanguage(language)
            val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(language)
            AppCompatDelegate.setApplicationLocales(appLocale)
        }
    }

    /**
     * Updates notification study reminders toggle preference.
     *
     * @param enabled True to enable reminders, false to disable.
     */
    fun setRemindersEnabled(enabled: Boolean) {
        Log.i(TAG, "setRemindersEnabled called with enabled: $enabled")
        viewModelScope.launch {
            preferenceManager.setRemindersEnabled(enabled)
        }
    }

    /**
     * Signs out the current user session via [AuthRepository].
     */
    fun signOut() {
        Log.i(TAG, "signOut initiated for user: $userEmail")
        authRepository.signOut()
    }
}
