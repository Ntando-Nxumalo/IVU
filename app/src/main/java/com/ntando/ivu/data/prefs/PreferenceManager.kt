package com.ntando.ivu.data.prefs

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

private const val TAG = "PreferenceManager"

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Manages user preference settings using Jetpack DataStore Preferences.
 *
 * Exposes reactive [Flow] streams to observe setting changes and suspend functions
 * to update preference values asynchronously.
 *
 * @param context Application context used to access Preferences DataStore.
 */
class PreferenceManager(private val context: Context) {

    companion object {
        /** Preference key for dark theme setting boolean. */
        val THEME_KEY = booleanPreferencesKey("dark_theme")
        /** Preference key for language setting string code. */
        val LANGUAGE_KEY = stringPreferencesKey("app_language")
        /** Preference key for study reminders setting boolean. */
        val REMINDERS_KEY = booleanPreferencesKey("study_reminders")
    }

    /**
     * Flow emitting the current dark theme preference state. Defaults to `false`.
     */
    val isDarkTheme: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[THEME_KEY] ?: false
        }
        .onEach { isDark ->
            Log.d(TAG, "isDarkTheme preference emitted: $isDark")
        }

    /**
     * Flow emitting the current application language code preference (e.g., "en", "zu", "af"). Defaults to `"en"`.
     */
    val appLanguage: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[LANGUAGE_KEY] ?: "en"
        }
        .onEach { lang ->
            Log.d(TAG, "appLanguage preference emitted: $lang")
        }

    /**
     * Flow emitting whether study reminder notifications are enabled. Defaults to `true`.
     */
    val isRemindersEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[REMINDERS_KEY] ?: true
        }
        .onEach { enabled ->
            Log.d(TAG, "isRemindersEnabled preference emitted: $enabled")
        }

    /**
     * Asynchronously updates dark theme setting in DataStore.
     *
     * @param isDark `true` to enable dark theme, `false` for light theme.
     */
    suspend fun setTheme(isDark: Boolean) {
        Log.d(TAG, "Updating dark theme preference: isDark = $isDark")
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = isDark
        }
    }

    /**
     * Asynchronously updates application language preference setting in DataStore.
     *
     * @param language ISO language code string (e.g. "en", "zu", "af").
     */
    suspend fun setLanguage(language: String) {
        Log.d(TAG, "Updating language preference: language = $language")
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language
        }
    }

    /**
     * Asynchronously updates study reminders enabled state setting in DataStore.
     *
     * @param enabled `true` to enable study reminders, `false` to disable.
     */
    suspend fun setRemindersEnabled(enabled: Boolean) {
        Log.d(TAG, "Updating reminders preference: enabled = $enabled")
        context.dataStore.edit { preferences ->
            preferences[REMINDERS_KEY] = enabled
        }
    }
}
