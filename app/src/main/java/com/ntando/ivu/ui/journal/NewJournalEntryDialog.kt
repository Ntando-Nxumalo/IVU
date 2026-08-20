package com.ntando.ivu.ui.journal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun NewJournalEntryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String?) -> Unit,
    initialDate: Calendar = Calendar.getInstance(),
    isLoading: Boolean = false
) {
    var mood by remember { mutableStateOf("okay") }
    var text by remember { mutableStateOf("") }
    val moods = listOf("great" to "😊", "okay" to "😐", "tough" to "😔")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            val sdf = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
            Text("${stringResource(R.string.how_was_session)} (${sdf.format(initialDate.time)})") 
        },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    moods.forEach { (m, emoji) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { mood = m }
                                .padding(8.dp)
                        ) {
                            Text(emoji, fontSize = 32.sp)
                            Text(
                                text = m.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = if (mood == m) FontWeight.Bold else FontWeight.Normal,
                                color = if (mood == m) Color(0xFFE88A68) else Color.Gray
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Write something about your progress...") },
                    modifier = Modifier.fillMaxWidth().height(150.dp)
                )
                
                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        color = Color(0xFFE88A68)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(initialDate.time)
                    onConfirm(dateStr, mood, text, null) 
                },
                enabled = text.isNotBlank() && !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE88A68))
            ) {
                Text(stringResource(R.string.save_entry))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
