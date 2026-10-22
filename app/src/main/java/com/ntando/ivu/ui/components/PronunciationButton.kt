package com.ntando.ivu.ui.components

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ntando.ivu.R
import com.ntando.ivu.util.SpeechManager
import com.ntando.ivu.util.SpeechResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAG = "IVU_TTS"

/** Custom Material Volume Up / Speaker icon vector. */
val VolumeUpIcon: ImageVector
    get() {
        if (_volumeUp != null) return _volumeUp!!
        _volumeUp = ImageVector.Builder(
            name = "VolumeUp",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(3f, 9f)
                verticalLineToRelative(6f)
                horizontalLineToRelative(4f)
                lineTo(12f, 20f)
                verticalLineTo(4f)
                lineTo(7f, 9f)
                horizontalLineTo(3f)
                close()
                moveTo(16.5f, 12f)
                curveTo(16.5f, 10.23f, 15.48f, 8.71f, 14f, 7.97f)
                verticalLineToRelative(8.05f)
                curveTo(15.48f, 15.29f, 16.5f, 13.77f, 16.5f, 12f)
                close()
                moveTo(14f, 3.23f)
                verticalLineToRelative(2.06f)
                curveTo(16.89f, 6.15f, 19f, 8.83f, 19f, 12f)
                curveTo(19f, 15.17f, 16.89f, 17.85f, 14f, 18.71f)
                verticalLineToRelative(2.06f)
                curveTo(18.01f, 19.86f, 21f, 16.28f, 21f, 12f)
                curveTo(21f, 7.72f, 18.01f, 4.14f, 14f, 3.23f)
                close()
            }
        }.build()
        return _volumeUp!!
    }

private var _volumeUp: ImageVector? = null

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
            imageVector = VolumeUpIcon,
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
