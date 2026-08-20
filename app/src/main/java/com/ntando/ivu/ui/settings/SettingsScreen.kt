package com.ntando.ivu.ui.settings

import androidx.compose.foundation.layout.*
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
                title = { Text(stringResource(R.string.settings), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
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
            // Account Section
            Text(
                text = stringResource(R.string.account),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE88A68),
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = stringResource(R.string.label_email), fontSize = 12.sp, color = Color.Gray)
                    Text(text = userEmail, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Preferences Section
            Text(
                text = stringResource(R.string.preferences),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE88A68),
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = MaterialTheme.shapes.medium
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.dark_theme)) },
                        trailingContent = {
                            Switch(
                                checked = isDarkTheme,
                                onCheckedChange = { viewModel.setTheme(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFE88A68))
                            )
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                    
                    var expanded by remember { mutableStateOf(false) }
                    val languages = listOf("en" to "English", "zu" to "isiZulu", "af" to "Afrikaans")
                    
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.app_language)) },
                        trailingContent = {
                            Box {
                                TextButton(onClick = { expanded = true }) {
                                    Text(
                                        text = languages.find { it.first == appLanguage }?.second ?: "English",
                                        color = Color(0xFFE88A68)
                                    )
                                }
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    languages.forEach { (code, name) ->
                                        DropdownMenuItem(
                                            text = { Text(name) },
                                            onClick = {
                                                viewModel.setLanguage(code)
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Sign Out
            Button(
                onClick = {
                    viewModel.signOut()
                    onSignOut()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Red),
                shape = MaterialTheme.shapes.medium,
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
            ) {
                Text(stringResource(R.string.sign_out), fontWeight = FontWeight.Bold)
            }
        }
    }
}
