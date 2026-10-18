package com.ntando.ivu.ui.settings

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ntando.ivu.R
import com.ntando.ivu.viewmodel.SettingsViewModel

private const val TAG = "SettingsScreen"

/**
 * Screen providing user profile overview, application preferences (theme, language, notification reminders, sync), and sign-out controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    val context = LocalContext.current
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val isRemindersEnabled by viewModel.isRemindersEnabled.collectAsState()
    val reminderHour by viewModel.reminderHour.collectAsState()
    val isAutoplayTts by viewModel.isAutoplayTts.collectAsState()
    val speechRate by viewModel.speechRate.collectAsState()
    val userEmail = viewModel.userEmail
    val userName = viewModel.userName
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showHourPickerDialog by remember { mutableStateOf(false) }
    var showSpeechRateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isDarkTheme, appLanguage, isRemindersEnabled, reminderHour) {
        Log.d(TAG, "Observed settings update: darkTheme=$isDarkTheme, language=$appLanguage, reminders=$isRemindersEnabled, hour=$reminderHour")
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = {
                Log.d(TAG, "Language selection dialog dismissed")
                showLanguageDialog = false
            },
            title = { Text(stringResource(R.string.select_language)) },
            text = {
                Column {
                    val languages = listOf("en" to "English", "zu" to "isiZulu", "af" to "Afrikaans")
                    languages.forEach { (code, name) ->
                        TextButton(
                            onClick = {
                                Log.i(TAG, "Changing application language to: $name ($code)")
                                viewModel.setLanguage(code)
                                showLanguageDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(name, color = if (appLanguage == code) Color(0xFFE88A68) else Color.Gray)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    Log.d(TAG, "Cancel button clicked in language dialog")
                    showLanguageDialog = false
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showHourPickerDialog) {
        AlertDialog(
            onDismissRequest = { showHourPickerDialog = false },
            title = { Text(stringResource(R.string.label_reminder_hour)) },
            text = {
                LazyColumn(modifier = Modifier.height(260.dp)) {
                    items(24) { hour ->
                        val formatted = formatHour(hour)
                        TextButton(
                            onClick = {
                                Log.i(TAG, "Selected reminder hour: $hour ($formatted)")
                                viewModel.setReminderHour(hour, context)
                                showHourPickerDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = formatted,
                                color = if (reminderHour == hour) Color(0xFFE88A68) else Color.Gray,
                                fontWeight = if (reminderHour == hour) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHourPickerDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showSpeechRateDialog) {
        AlertDialog(
            onDismissRequest = { showSpeechRateDialog = false },
            title = { Text(stringResource(R.string.label_speech_rate)) },
            text = {
                Column {
                    val rates = listOf(
                        0.75f to stringResource(R.string.label_speech_slow),
                        1.0f to stringResource(R.string.label_speech_normal)
                    )
                    rates.forEach { (rateVal, rateLabel) ->
                        Text(
                            text = rateLabel,
                            fontSize = 16.sp,
                            fontWeight = if ((rateVal <= 0.8f && speechRate <= 0.8f) || (rateVal > 0.8f && speechRate > 0.8f)) FontWeight.Bold else FontWeight.Normal,
                            color = if ((rateVal <= 0.8f && speechRate <= 0.8f) || (rateVal > 0.8f && speechRate > 0.8f)) Color(0xFFE88A68) else Color.Unspecified,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    Log.i(TAG, "Speech rate selected: rateVal=$rateVal")
                                    viewModel.setSpeechRate(rateVal)
                                    showSpeechRateDialog = false
                                }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeechRateDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_settings), fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = {
                        Log.d(TAG, "Back arrow button clicked in SettingsScreen")
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFE88A68)
                )
            )
        },
        bottomBar = {
            com.ntando.ivu.ui.components.BottomNavigationBar(
                currentScreen = "profile",
                onNavigate = { route ->
                    Log.d(TAG, "Navigating from SettingsScreen to route: $route")
                    onNavigate(route)
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            val sharedPref = context.getSharedPreferences("IVUPrefs", android.content.Context.MODE_PRIVATE)
            val firebaseUid = sharedPref.getString("firebase_uid", "") ?: ""

            val networkMonitor = remember(context) { com.ntando.ivu.network.NetworkMonitor(context) }
            val isOnline by networkMonitor.isOnline.collectAsState(initial = networkMonitor.isCurrentlyConnected())

            com.ntando.ivu.ui.components.OfflineBanner(isOffline = !isOnline)
            com.ntando.ivu.ui.components.PendingSyncBanner(ownerUserId = firebaseUid)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // PROFILE SECTION
                SectionHeader(stringResource(R.string.section_profile))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(56.dp),
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = Color(0xFFE88A68).copy(alpha = 0.6f)
                        ) {}
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = userName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3D2B1F))
                            Text(text = userEmail, fontSize = 12.sp, color = Color.LightGray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // PREFERENCES SECTION
                SectionHeader(stringResource(R.string.section_preferences))
                SettingsCard {
                    SettingsItem(
                        label = stringResource(R.string.label_app_language),
                        value = when(appLanguage) {
                            "zu" -> stringResource(R.string.label_isizulu)
                            "af" -> stringResource(R.string.label_afrikaans)
                            else -> stringResource(R.string.label_english)
                        },
                        onClick = {
                            Log.d(TAG, "Opening language selection dialog")
                            showLanguageDialog = true
                        }
                    )
                    SettingsItem(
                        label = stringResource(R.string.label_theme),
                        value = if (isDarkTheme) stringResource(R.string.label_theme_dark) else stringResource(R.string.label_theme_light),
                        onClick = {
                            val targetTheme = !isDarkTheme
                            Log.i(TAG, "Toggling app theme preference to darkTheme=$targetTheme")
                            viewModel.setTheme(targetTheme)
                        }
                    )
                    SettingsItem(
                        label = stringResource(R.string.label_reminders),
                        value = if (isRemindersEnabled) stringResource(R.string.label_reminders_on) else stringResource(R.string.label_reminders_off),
                        onClick = {
                            val targetReminders = !isRemindersEnabled
                            Log.i(TAG, "Toggling reminders preference to enabled=$targetReminders")
                            viewModel.setRemindersEnabled(targetReminders, context)
                        }
                    )
                    if (isRemindersEnabled) {
                        SettingsItem(
                            label = stringResource(R.string.label_reminder_hour),
                            value = formatHour(reminderHour),
                            onClick = {
                                Log.d(TAG, "Opening reminder hour picker dialog")
                                showHourPickerDialog = true
                            }
                        )
                    }
                    SettingsItem(
                        label = stringResource(R.string.label_autoplay_tts),
                        value = if (isAutoplayTts) stringResource(R.string.label_reminders_on) else stringResource(R.string.label_reminders_off),
                        onClick = {
                            val targetAutoplay = !isAutoplayTts
                            Log.i(TAG, "Toggling autoplay TTS preference to enabled=$targetAutoplay")
                            viewModel.setAutoplayTts(targetAutoplay)
                        }
                    )
                    SettingsItem(
                        label = stringResource(R.string.label_speech_rate),
                        value = if (speechRate <= 0.8f) stringResource(R.string.label_speech_slow) else stringResource(R.string.label_speech_normal),
                        onClick = {
                            Log.d(TAG, "Opening speech rate selection dialog")
                            showSpeechRateDialog = true
                        }
                    )
                    SettingsItem(
                        label = stringResource(R.string.btn_test_notification),
                        value = "10s delay",
                        showDivider = false,
                        onClick = {
                            Log.i(TAG, "Test notification button clicked")
                            viewModel.sendTestNotification(context) { _, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                TextButton(
                    onClick = {
                        Log.i(TAG, "Sign out requested by user")
                        viewModel.signOut()
                        onSignOut()
                    },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(stringResource(R.string.btn_logout), color = Color(0xFFE88A68), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun formatHour(hour: Int): String {
    val amPm = if (hour < 12) "AM" else "PM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    val paddedHour = if (hour < 10) "0$hour" else "$hour"
    return "$displayHour:00 $amPm ($paddedHour:00)"
}

/**
 * Text header formatting category section titles in the settings layout.
 */
@Composable
fun SectionHeader(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Color.LightGray,
        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
    )
}

/**
 * Card container wrapping group settings items in a rounded white surface with elevation.
 */
@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        content = content
    )
}

/**
 * Single interactive settings item row displaying a label, current selected value, and an optional bottom divider line.
 */
@Composable
fun SettingsItem(
    label: String,
    value: String? = null,
    showDivider: Boolean = true,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (value != null) {
                    Text(text = value, fontSize = 14.sp, color = Color(0xFFE88A68))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    modifier = Modifier.size(20.dp, 12.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF7FB6A7).copy(alpha = 0.6f)
                ) {}
            }
        }
    }
    if (showDivider) {
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
    }
}
