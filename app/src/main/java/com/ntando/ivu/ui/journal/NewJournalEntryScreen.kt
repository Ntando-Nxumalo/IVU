package com.ntando.ivu.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ntando.ivu.R
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewJournalEntryScreen(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String?) -> Unit,
    initialDate: Calendar = Calendar.getInstance(),
    isLoading: Boolean = false
) {
    var mood by remember { mutableStateOf("okay") }
    var text by remember { mutableStateOf("") }
    val moods = listOf("great" to "😊", "okay" to "😐", "tough" to "😔")
    val moodColors = mapOf("great" to Color(0xFF7FB6A7), "okay" to Color(0xFFE8C07C), "tough" to Color(0xFFE88A68))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("New Entry", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                        val sdf = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
                        Text(sdf.format(initialDate.time), fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFE88A68))
            )
        },
        containerColor = Color(0xFFFFF8F0)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "How did studying feel today?",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3D2B1F)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                moods.forEach { (m, emoji) ->
                    val color = moodColors[m] ?: Color.Gray
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .clickable { mood = m },
                            color = if (mood == m) color else color.copy(alpha = 0.3f),
                            shape = CircleShape
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(emoji, fontSize = 32.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = m.replaceFirstChar { it.uppercase() },
                            fontSize = 14.sp,
                            color = Color(0xFF3D2B1F)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Notes",
                fontSize = 14.sp,
                color = Color.LightGray,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Struggled with verb conjugation but got the greetings down well...", color = Color.LightGray.copy(alpha = 0.5f)) },
                modifier = Modifier.fillMaxWidth().height(180.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color.LightGray,
                    unfocusedBorderColor = Color.LightGray
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Link to today",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3D2B1F),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { /* ... */ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7FB6A7)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("✓ Calendar", fontSize = 12.sp)
                }
                Button(
                    onClick = { /* ... */ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF2E6D3), contentColor = Color(0xFF3D2B1F)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Afrikaans Basics", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { 
                    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(initialDate.time)
                    onConfirm(dateStr, mood, text, null) 
                },
                enabled = text.isNotBlank() && !isLoading,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE88A68)),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(stringResource(R.string.save_entry), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
