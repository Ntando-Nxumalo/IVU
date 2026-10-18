package com.ntando.ivu.viewmodel

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntando.ivu.R
import com.ntando.ivu.data.prefs.PreferenceManager
import com.ntando.ivu.data.repository.AuthRepository
import com.ntando.ivu.network.ApiClient
import com.ntando.ivu.network.FcmTokenManager
import com.ntando.ivu.network.NetworkMonitor
import com.ntando.ivu.network.TestNotificationRequest
import com.ntando.ivu.util.NotificationHelper
import com.ntando.ivu.util.StudyReminderScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel managing user preferences, app language, theme modes, reminder settings, and account sign-out.
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
     * StateFlow pipeline reflecting daily study reminder hour (0-23). Defaults to 18 (6:00 PM).
     */
    val reminderHour: StateFlow<Int> = preferenceManager.reminderHour
        .onEach { hour ->
            Log.d(TAG, "reminderHour preference emitted: $hour")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 18)

    /**
     * StateFlow pipeline reflecting auto-play pronunciation setting.
     */
    val isAutoplayTts: StateFlow<Boolean> = preferenceManager.isAutoplayTts
        .onEach { autoplay ->
            Log.d(TAG, "isAutoplayTts preference emitted: $autoplay")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /**
     * StateFlow pipeline reflecting TTS speech playback rate.
     */
    val speechRate: StateFlow<Float> = preferenceManager.speechRate
        .onEach { rate ->
            Log.d(TAG, "speechRate preference emitted: $rate")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)

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
     * Updates notification study reminders toggle preference and re-registers token with backend server.
     *
     * @param enabled True to enable reminders, false to disable.
     * @param context Application context for token registration.
     */
    fun setRemindersEnabled(enabled: Boolean, context: Context) {
        Log.i(TAG, "setRemindersEnabled called with enabled: $enabled")
        viewModelScope.launch {
            preferenceManager.setRemindersEnabled(enabled)
            StudyReminderScheduler.scheduleDailyReminder(context, enabled = enabled)
            FcmTokenManager(context).registerCurrentToken(reminderEnabled = enabled)
        }
    }

    /**
     * Updates daily study reminder hour setting and re-registers token with backend server.
     *
     * @param hour Target hour integer between 0 and 23.
     * @param context Application context for token registration.
     */
    fun setReminderHour(hour: Int, context: Context) {
        Log.i(TAG, "setReminderHour called with hour: $hour")
        viewModelScope.launch {
            preferenceManager.setReminderHour(hour)
            StudyReminderScheduler.scheduleDailyReminder(context, hour = hour)
            FcmTokenManager(context).registerCurrentToken(reminderHour = hour)
        }
    }

    /**
     * Updates auto-play pronunciation setting preference.
     *
     * @param enabled True to auto-play card front text on card change.
     */
    fun setAutoplayTts(enabled: Boolean) {
        Log.i(TAG, "setAutoplayTts called with enabled: $enabled")
        viewModelScope.launch {
            preferenceManager.setAutoplayTts(enabled)
        }
    }

    /**
     * Updates TTS speech playback rate multiplier preference.
     *
     * @param rate Multiplier float value (0.75f for Slow, 1.0f for Normal).
     */
    fun setSpeechRate(rate: Float) {
        Log.i(TAG, "setSpeechRate called with rate: $rate")
        viewModelScope.launch {
            preferenceManager.setSpeechRate(rate)
        }
    }

    /**
     * Triggers a test push notification scheduled on the backend after 10 seconds.
     *
     * @param context Application context.
     * @param onResult Callback yielding `(success: Boolean, message: String)`.
     */
    fun sendTestNotification(context: Context, onResult: (Boolean, String) -> Unit) {
        Log.i(TAG, "sendTestNotification requested")
        val networkMonitor = NetworkMonitor(context)
        if (!networkMonitor.isCurrentlyConnected()) {
            Log.w(TAG, "sendTestNotification rejected: Device is offline")
            onResult(false, context.getString(R.string.toast_test_notification_offline))
            return
        }

        viewModelScope.launch {
            try {
                // Ensure FCM token is registered with backend first
                Log.d(TAG, "Ensuring FCM token is registered on backend before test notification request...")
                FcmTokenManager(context).registerCurrentToken()

                val response = ApiClient.apiService.sendTestNotification(TestNotificationRequest(delaySeconds = 10))
                if (response.isSuccessful && response.body()?.success == true) {
                    Log.i(TAG, "sendTestNotification request accepted by backend API")
                    onResult(true, context.getString(R.string.toast_test_notification_sent))

                    // Local fallback after 10.5s if FCM message is delayed
                    delay(10500)
                    NotificationHelper.showBadgeUnlockedNotification(
                        context = context,
                        badgeName = "First Steps"
                    )
                } else {
                    val code = response.code()
                    val err = response.body()?.error ?: "HTTP error $code"
                    Log.e(TAG, "sendTestNotification failed: $err")
                    onResult(false, "Failed to schedule test notification: $err")
                }
            } catch (e: Exception) {
                Log.e(TAG, "sendTestNotification exception", e)
                onResult(false, "Error: ${e.message}")
            }
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
