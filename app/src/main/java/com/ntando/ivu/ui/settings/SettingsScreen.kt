package com.ntando.ivu.ui.settings

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onSignOut: () -> Unit
) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val userEmail = viewModel.userEmail

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_settings), fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFE88A68)
                )
            )
        },
        containerColor = Color(0xFFFFF8F0)
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
                        Text(text = "Ayanda Maseko", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3D2B1F))
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
                    value = if (appLanguage == "zu") "isiZulu" else "English",
                    onClick = { /* ... */ }
                )
                SettingsItem(
                    label = stringResource(R.string.label_theme),
                    value = if (isDarkTheme) "Dark" else "Warm (light)",
                    onClick = { viewModel.setTheme(!isDarkTheme) }
                )
                SettingsItem(
                    label = stringResource(R.string.label_reminders),
                    value = "On – 6:00 PM",
                    onClick = { /* ... */ }
                )
                SettingsItem(
                    label = stringResource(R.string.label_sync),
                    value = "Wi-Fi only",
                    showDivider = false,
                    onClick = { /* ... */ }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECURITY SECTION
            SectionHeader(stringResource(R.string.section_security))
            SettingsCard {
                SettingsItem(
                    label = stringResource(R.string.btn_change_password),
                    showDivider = false,
                    onClick = { /* ... */ }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = {
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

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        content = content
    )
}

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
            Text(text = label, fontSize = 14.sp, color = Color(0xFF3D2B1F))
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
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color(0xFFEEEEEE))
    }
}
