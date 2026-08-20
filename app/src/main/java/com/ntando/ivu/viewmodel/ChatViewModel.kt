package com.ntando.ivu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntando.ivu.data.model.ChatMessage
import com.ntando.ivu.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(private val repository: ChatRepository) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val userMessage = ChatMessage(text, true)
        _messages.value = _messages.value + userMessage
        _isLoading.value = true

        viewModelScope.launch {
            val result = repository.sendMessage(text)
            _isLoading.value = false
            
            val aiResponse = result.getOrElse { it.message ?: "Sorry, I couldn't process that." }
            _messages.value = _messages.value + ChatMessage(aiResponse, false)
        }
    }

    fun sendWelcomeMessage(userName: String) {
        if (_messages.value.isEmpty()) {
            _messages.value = listOf(
                ChatMessage("Hi $userName! I'm your IVU study assistant. How can I help you today?", false)
            )
        }
    }
}
