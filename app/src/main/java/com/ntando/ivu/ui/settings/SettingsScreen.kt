package com.ntando.ivu.ui.settings

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ntando.ivu.R
import com.ntando.ivu.viewmodel.SettingsViewModel

private const val TAG = "SettingsScreen"

/**
 * Screen providing user profile overview, application preferences (theme, language, notification reminders, sync), and sign-out controls.
 *
 * Layout Structure:
 * - [Scaffold] with a [TopAppBar] and [com.ntando.ivu.ui.components.BottomNavigationBar].
 * - Profile header section displaying user name and email in a white rounded card.
 * - Preferences section ([SettingsCard]) holding configurable [SettingsItem] rows:
 *   1. App language selection dialog trigger.
 *   2. Dark/Light theme toggle invoking [SettingsViewModel.setTheme].
 *   3. Study reminders toggle invoking [SettingsViewModel.setRemindersEnabled].
 *   4. Sync settings preview row.
 * - Logout text button invoking [SettingsViewModel.signOut] and executing [onSignOut].
 *
 * @param viewModel ViewModel providing settings preference flows and auth logout actions.
 * @param onBack Navigation callback invoked when popping the settings screen.
 * @param onSignOut Callback invoked after user session termination to redirect to the authentication screen.
 * @param onNavigate Navigation callback triggered when switching bottom bar screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val isRemindersEnabled by viewModel.isRemindersEnabled.collectAsState()
    val userEmail = viewModel.userEmail
    val userName = viewModel.userName
    var showLanguageDialog by remember { mutableStateOf(value = false) }

    LaunchedEffect(isDarkTheme, appLanguage, isRemindersEnabled) {
        Log.d(TAG, "Observed settings update: darkTheme=$isDarkTheme, language=$appLanguage, reminders=$isRemindersEnabled")
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
                        viewModel.setRemindersEnabled(targetReminders)
                    }
                )
                SettingsItem(
                    label = stringResource(R.string.label_sync),
                    value = "Wi-Fi only",
                    showDivider = false,
                    onClick = {
                        Log.d(TAG, "Sync setting item clicked")
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

/**
 * Text header formatting category section titles in the settings layout.
 *
 * @param text Section header title string.
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
 *
 * @param content Slot layout containing [SettingsItem] rows.
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
 *
 * @param label Descriptive string title for the setting preference.
 * @param value Current value or state string displayed on the right.
 * @param showDivider Whether to render a horizontal divider line beneath this row.
 * @param onClick Event listener executed when tapping this row.
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
