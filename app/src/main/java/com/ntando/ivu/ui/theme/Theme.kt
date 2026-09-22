package com.ntando.ivu.ui.theme

import android.app.Activity
import android.os.Build
import android.util.Log
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private const val TAG = "IVUTheme"

/**
 * Material Design 3 dark color scheme definition for the IVU application.
 */
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryOrange,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = BackgroundDark,
    surface = BackgroundDark
)

/**
 * Material Design 3 light color scheme definition for the IVU application.
 */
private val LightColorScheme = lightColorScheme(
    primary = PrimaryOrange,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = BackgroundLight,
    surface = Color(0xFFFFFDF9),
    onBackground = DarkBrown,
    onSurface = DarkBrown
)

/**
 * Central custom Material 3 Theme wrapper composable for the IVU Android application.
 * Configures light/dark color palettes, Android 12+ dynamic color capabilities, and sets window status bar colors.
 *
 * @param darkTheme Whether dark color scheme should be used. Defaults to system setting ([isSystemInDarkTheme]).
 * @param dynamicColor Whether dynamic wallpaper-based colors (Android 12+) are enabled.
 * @param content Slot layout hierarchy wrapped inside [MaterialTheme].
 */
@Composable
fun IVUTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            Log.d(TAG, "Using Android 12+ dynamic color scheme (darkTheme=$darkTheme)")
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> {
            Log.d(TAG, "Applying custom DarkColorScheme")
            DarkColorScheme
        }
        else -> {
            Log.d(TAG, "Applying custom LightColorScheme")
            LightColorScheme
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            Log.d(TAG, "Updated window statusBarColor to ${colorScheme.primary.toArgb()} (lightStatusBars=${!darkTheme})")
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
