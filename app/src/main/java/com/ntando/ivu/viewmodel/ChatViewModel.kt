package com.ntando.ivu.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntando.ivu.data.model.ChatMessage
import com.ntando.ivu.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel managing interactive study assistant chat sessions and AI response generation.
 *
 * Maintains message history and loading indicator state flows while handling async communication
 * with [ChatRepository].
 *
 * @property repository The repository managing AI chat API communication and message formatting.
 */
class ChatViewModel(private val repository: ChatRepository) : ViewModel() {

    companion object {
        private const val TAG = "ChatViewModel"
    }

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())

    /**
     * Immutable StateFlow emitting the list of chat messages exchanged in the current session.
     */
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)

    /**
     * Immutable StateFlow indicating whether an AI response is currently pending.
     */
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        Log.d(TAG, "ChatViewModel initialized")
    }

    /**
     * Sends a user prompt to the AI chat service and appends the response to message history.
     *
     * Guards against blank inputs and concurrent requests while loading.
     *
     * @param text Prompt string entered by the user.
     */
    fun sendMessage(text: String) {
        if (text.isBlank()) {
            Log.w(TAG, "sendMessage ignored: prompt is blank")
            return
        }
        if (_isLoading.value) {
            Log.w(TAG, "sendMessage ignored: another message is currently loading")
            return
        }

        Log.i(TAG, "sendMessage dispatched: \"$text\"")
        val userMessage = ChatMessage(text, true)
        _messages.value = _messages.value + userMessage
        _isLoading.value = true

        viewModelScope.launch {
            val result = repository.sendMessage(text)
            _isLoading.value = false

            val aiResponse = result.getOrElse { throwable ->
                Log.e(TAG, "sendMessage error encountered from ChatRepository", throwable)
                throwable.message ?: "Sorry, I couldn't process that."
            }
            Log.d(TAG, "sendMessage response received: \"$aiResponse\"")
            _messages.value = _messages.value + ChatMessage(aiResponse, false)
        }
    }

    /**
     * Initializes the chat conversation with a greeting message addressed to the specified user if no messages exist yet.
     *
     * @param userName Display name of the active user.
     */
    fun sendWelcomeMessage(userName: String) {
        if (_messages.value.isEmpty()) {
            Log.i(TAG, "sendWelcomeMessage initializing welcome message for $userName")
            _messages.value = listOf(
                ChatMessage("Hi $userName! I'm your IVU study assistant. How can I help you today?", false)
            )
        } else {
            Log.d(TAG, "sendWelcomeMessage skipped: chat history is not empty")
        }
    }
}
