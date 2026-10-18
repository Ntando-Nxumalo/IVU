package com.ntando.ivu.ui.components

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.ntando.ivu.R
import com.ntando.ivu.util.SpeechManager
import com.ntando.ivu.util.SpeechResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAG = "IVU_TTS"

/**
 * Safely launches the system TTS settings screen with fallbacks.
 *
 * @param context Context used to start settings activity.
 */
fun launchTtsSettings(context: Context) {
    try {
        val intent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        Log.i(TAG, "Launched ACTION_INSTALL_TTS_DATA successfully")
    } catch (e: Exception) {
        Log.w(TAG, "Failed to launch ACTION_INSTALL_TTS_DATA, attempting Settings.ACTION_SETTINGS fallback", e)
        try {
            val fallbackIntent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallbackIntent)
            Log.i(TAG, "Launched Settings.ACTION_SETTINGS fallback")
        } catch (e2: Exception) {
            Log.e(TAG, "Failed to launch system settings fallback", e2)
        }
    }
}

/**
 * Speaker icon button that speaks the target text in the deck's language.
 * Displays brief active state while speaking and shows fallback snackbar if language voice is not installed.
 */
@Composable
fun PronunciationButton(
    speechManager: SpeechManager,
    textToSpeak: String,
    languageCode: String,
    snackbarHostState: SnackbarHostState? = null,
    coroutineScope: CoroutineScope? = null,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    val context = LocalContext.current
    val isSpeaking by speechManager.isSpeaking.collectAsState()
    var isThisButtonSpeaking by remember { mutableStateOf(false) }

    IconButton(
        onClick = {
            isThisButtonSpeaking = true
            val result = speechManager.speak(textToSpeak, languageCode)
            if (result is SpeechResult.LanguageNotInstalled) {
                isThisButtonSpeaking = false
                val msg = context.getString(R.string.tts_lang_not_installed)
                val actionLabel = context.getString(R.string.btn_tts_settings)

                if (snackbarHostState != null && coroutineScope != null) {
                    coroutineScope.launch {
                        val snackbarResult = snackbarHostState.showSnackbar(
                            message = msg,
                            actionLabel = actionLabel,
                            duration = SnackbarDuration.Long
                        )
                        if (snackbarResult == SnackbarResult.ActionPerformed) {
                            launchTtsSettings(context)
                        }
                    }
                } else {
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    launchTtsSettings(context)
                }
            }
        },
        modifier = modifier
    ) {
        val activeColor = if (isSpeaking && isThisButtonSpeaking) MaterialTheme.colorScheme.tertiary else tint
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = stringResource(R.string.content_desc_speak),
            tint = activeColor
        )
    }

    LaunchedEffect(isSpeaking) {
        if (!isSpeaking) {
            isThisButtonSpeaking = false
        }
    }
}
