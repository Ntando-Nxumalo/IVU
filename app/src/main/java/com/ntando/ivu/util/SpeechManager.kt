package com.ntando.ivu.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

private const val TAG = "IVU_TTS"

/**
 * Result outcomes for a [SpeechManager.speak] attempt.
 */
sealed class SpeechResult {
    /** Speech utterance accepted and queued/started successfully. */
    object Spoken : SpeechResult()

    /** Target language voice is not installed on the system TTS engine. */
    object LanguageNotInstalled : SpeechResult()

    /** TTS engine is not initialized or not yet ready. */
    object EngineNotReady : SpeechResult()

    /** General error occurred during speech request. */
    data class Error(val message: String) : SpeechResult()
}

/**
 * Maps deck language codes ("en", "zu", "af", etc.) to system [Locale]s.
 */
object SpeechLocaleMapper {
    /**
     * Resolves a [Locale] matching the deck [languageCode].
     * Defaults to South African English (`en-ZA`) with fallbacks.
     */
    fun getLocaleForLanguageCode(languageCode: String): Locale {
        return when (languageCode.lowercase().trim()) {
            "zu", "isizulu" -> Locale("zu", "ZA")
            "af", "afrikaans" -> Locale("af", "ZA")
            "en", "english" -> Locale("en", "ZA")
            else -> Locale("en", "ZA")
        }
    }
}

/**
 * Manages Android [TextToSpeech] engine lifecycle, locale availability, utterance control, and cleanup.
 *
 * @param context Application context.
 */
class SpeechManager(context: Context) : TextToSpeech.OnInitListener {

    private val appContext: Context = context.applicationContext
    private var tts: TextToSpeech? = TextToSpeech(appContext, this)

    private val _isReady = MutableStateFlow(false)
    /** Reactive flow emitting whether the TextToSpeech engine is initialized and ready. */
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    /** Reactive flow emitting whether the engine is currently speaking an utterance. */
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var speechRate: Float = 1.0f

    init {
        Log.d(TAG, "Initializing SpeechManager TextToSpeech engine...")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            Log.i(TAG, "TextToSpeech engine initialized successfully")
            _isReady.value = true
            setupUtteranceListener()
        } else {
            Log.e(TAG, "TextToSpeech engine initialization failed with status: $status")
            _isReady.value = false
        }
    }

    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d(TAG, "Utterance started: utteranceId=$utteranceId")
                _isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                Log.d(TAG, "Utterance completed: utteranceId=$utteranceId")
                _isSpeaking.value = false
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                Log.e(TAG, "Utterance error: utteranceId=$utteranceId")
                _isSpeaking.value = false
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.e(TAG, "Utterance error code $errorCode for utteranceId=$utteranceId")
                _isSpeaking.value = false
            }
        })
    }

    /**
     * Sets the speech playback rate multiplier (e.g., 0.75f for Slow, 1.0f for Normal).
     *
     * @param rate Multiplier float value.
     */
    fun setSpeechRate(rate: Float) {
        this.speechRate = rate
        tts?.setSpeechRate(rate)
        Log.d(TAG, "Speech rate set to: $rate")
    }

    /**
     * Speaks [text] in the target deck [languageCode].
     *
     * @param text Text content to pronounce.
     * @param languageCode Deck language code string ("en", "zu", "af").
     * @return [SpeechResult] indicating outcome.
     */
    fun speak(text: String, languageCode: String): SpeechResult {
        if (text.isBlank()) {
            Log.d(TAG, "speak ignored: text is blank")
            return SpeechResult.Error("Text is blank")
        }

        val engine = tts
        if (engine == null || !_isReady.value) {
            Log.w(TAG, "speak failed: EngineNotReady")
            return SpeechResult.EngineNotReady
        }

        try {
            // Stop current speech so rapid taps do not overlap
            engine.stop()

            val targetLocale = SpeechLocaleMapper.getLocaleForLanguageCode(languageCode)
            Log.d(TAG, "Requested speech for languageCode='$languageCode' -> targetLocale='$targetLocale'")

            var availability = engine.isLanguageAvailable(targetLocale)
            var activeLocale = targetLocale

            if (availability < TextToSpeech.LANG_AVAILABLE) {
                if (targetLocale.language == "en") {
                    val fallbackLocale = Locale.US
                    val fallbackAvailability = engine.isLanguageAvailable(fallbackLocale)
                    Log.w(TAG, "Target locale '$targetLocale' availability=$availability. Trying fallback '$fallbackLocale' (availability=$fallbackAvailability)...")
                    if (fallbackAvailability >= TextToSpeech.LANG_AVAILABLE) {
                        availability = fallbackAvailability
                        activeLocale = fallbackLocale
                    }
                }
            }

            Log.d(TAG, "Locale '$activeLocale' availability code: $availability")

            if (availability < TextToSpeech.LANG_AVAILABLE) {
                Log.w(TAG, "speak failed: LanguageNotInstalled for locale '$activeLocale'")
                return SpeechResult.LanguageNotInstalled
            }

            val setLangResult = engine.setLanguage(activeLocale)
            if (setLangResult < TextToSpeech.LANG_AVAILABLE) {
                Log.w(TAG, "setLanguage failed for locale '$activeLocale' with code: $setLangResult")
                return SpeechResult.LanguageNotInstalled
            }

            engine.setSpeechRate(speechRate)

            val utteranceId = "IVU_TTS_${System.currentTimeMillis()}"
            val speakStatus = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)

            return if (speakStatus == TextToSpeech.SUCCESS) {
                Log.i(TAG, "speak succeeded for text: \"$text\" with locale '$activeLocale'")
                SpeechResult.Spoken
            } else {
                Log.e(TAG, "speak failed with status code: $speakStatus")
                SpeechResult.Error("TTS speak error code $speakStatus")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception caught during speak", e)
            return SpeechResult.Error(e.message ?: "Unknown TTS exception")
        }
    }

    /**
     * Stops any currently active speech playback.
     */
    fun stop() {
        try {
            tts?.stop()
            _isSpeaking.value = false
            Log.d(TAG, "Speech stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS engine", e)
        }
    }

    /**
     * Releases TextToSpeech resources when destroyed.
     */
    fun shutdown() {
        Log.d(TAG, "Shutting down SpeechManager TextToSpeech engine...")
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            _isReady.value = false
            _isSpeaking.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TTS engine", e)
        }
    }
}
