package com.ntando.ivu.ui.decks

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ntando.ivu.R

private const val TAG = "AddFlashcardDialog"

/**
 * Modal alert dialog composable for creating and adding new flashcards to a specific deck.
 *
 * Layout Structure:
 * - [AlertDialog] container wrapping a vertically scrollable column.
 * - Outlined text input for the card front (prompt / term).
 * - Outlined text input for the card back (definition / translation).
 * - Optional red error text if request fails.
 * - [LinearProgressIndicator] displayed while [isLoading] is true.
 * - Action buttons: Confirm (disabled if input blank or loading) and Cancel.
 *
 * @param onDismiss Callback invoked when the user requests dialog dismissal or taps Cancel.
 * @param onConfirm Callback invoked when valid front/back text is submitted: `(frontText, backText) -> Unit`.
 * @param isLoading State flag displaying a loading progress bar and disabling confirm button during network requests.
 * @param errorMessage Optional error string displayed if flashcard creation fails.
 */
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
        onDismissRequest = {
            Log.d(TAG, "AddFlashcardDialog onDismissRequest triggered")
            onDismiss()
        },
        title = { Text(stringResource(R.string.add_card_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = frontText,
                    onValueChange = { frontText = it },
                    label = { Text(stringResource(R.string.front_text)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 5
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = backText,
                    onValueChange = { backText = it },
                    label = { Text(stringResource(R.string.back_text)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 5
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
                onClick = {
                    Log.i(TAG, "Submitting new flashcard with front length=${frontText.length}, back length=${backText.length}")
                    onConfirm(frontText, backText)
                },
                enabled = frontText.isNotBlank() && backText.isNotBlank() && !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE88A68))
            ) {
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    Log.d(TAG, "Cancel button clicked in AddFlashcardDialog")
                    onDismiss()
                },
                enabled = !isLoading
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
