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
        /** Preference key for FCM device token string. */
        val FCM_TOKEN_KEY = stringPreferencesKey("fcm_token")
        /** Preference key for daily study reminder hour (0-23). */
        val REMINDER_HOUR_KEY = intPreferencesKey("reminder_hour")
        /** Preference key for auto-play pronunciation toggle. */
        val AUTOPLAY_TTS_KEY = booleanPreferencesKey("autoplay_tts")
        /** Preference key for TTS speech playback rate. */
        val SPEECH_RATE_KEY = floatPreferencesKey("speech_rate")
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
     * Flow emitting the cached FCM push notification token.
     */
    val fcmToken: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[FCM_TOKEN_KEY]
        }

    /**
     * Flow emitting the daily study reminder hour (0-23). Defaults to `18` (6:00 PM).
     */
    val reminderHour: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[REMINDER_HOUR_KEY] ?: 18
        }
        .onEach { hour ->
            Log.d(TAG, "reminderHour preference emitted: $hour")
        }

    /**
     * Flow emitting auto-play pronunciation preference boolean. Defaults to `false`.
     */
    val isAutoplayTts: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[AUTOPLAY_TTS_KEY] ?: false
        }
        .onEach { autoplay ->
            Log.d(TAG, "isAutoplayTts preference emitted: $autoplay")
        }

    /**
     * Flow emitting TTS speech playback rate multiplier. Defaults to `1.0f` (Normal).
     */
    val speechRate: Flow<Float> = context.dataStore.data
        .map { preferences ->
            preferences[SPEECH_RATE_KEY] ?: 1.0f
        }
        .onEach { rate ->
            Log.d(TAG, "speechRate preference emitted: $rate")
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

    /**
     * Asynchronously updates the FCM device push notification token in DataStore.
     *
     * @param token FCM token string.
     */
    suspend fun setFcmToken(token: String) {
        Log.d(TAG, "Updating FCM token preference in DataStore")
        context.dataStore.edit { preferences ->
            preferences[FCM_TOKEN_KEY] = token
        }
    }

    /**
     * Asynchronously updates the daily study reminder hour setting in DataStore.
     *
     * @param hour Target hour integer between 0 and 23.
     */
    suspend fun setReminderHour(hour: Int) {
        Log.d(TAG, "Updating reminder hour preference: hour = $hour")
        context.dataStore.edit { preferences ->
            preferences[REMINDER_HOUR_KEY] = hour
        }
    }

    /**
     * Asynchronously updates auto-play pronunciation preference in DataStore.
     *
     * @param enabled `true` to auto-play card front text on card change.
     */
    suspend fun setAutoplayTts(enabled: Boolean) {
        Log.d(TAG, "Updating autoplay TTS preference: enabled = $enabled")
        context.dataStore.edit { preferences ->
            preferences[AUTOPLAY_TTS_KEY] = enabled
        }
    }

    /**
     * Asynchronously updates TTS speech rate in DataStore.
     *
     * @param rate Speech rate multiplier float (0.75f or 1.0f).
     */
    suspend fun setSpeechRate(rate: Float) {
        Log.d(TAG, "Updating speech rate preference: rate = $rate")
        context.dataStore.edit { preferences ->
            preferences[SPEECH_RATE_KEY] = rate
        }
    }
}
