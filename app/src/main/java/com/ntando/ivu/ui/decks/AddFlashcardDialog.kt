package com.ntando.ivu.ui.decks

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ntando.ivu.R

@Composable
fun AddFlashcardDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    var frontText by remember { mutableStateOf("") }
    var backText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_card_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = frontText,
                    onValueChange = { frontText = it },
                    label = { Text(stringResource(R.string.front_text)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = backText,
                    onValueChange = { backText = it },
                    label = { Text(stringResource(R.string.back_text)) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                
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
                onClick = { onConfirm(frontText, backText) },
                enabled = frontText.isNotBlank() && backText.isNotBlank() && !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE88A68))
            ) {
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
