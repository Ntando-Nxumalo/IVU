package com.ntando.ivu.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntando.ivu.data.model.ChatMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ChatViewModel for IVU AI Assist.
 * Acts as a study companion that explains flashcards, generates examples, and quizzes users.
 */
class ChatViewModel(
    private val userId: Long
) : ViewModel() {

    private val TAG = "ChatViewModel"

    private val _messages = MutableLiveData<List<ChatMessage>>(emptyList())
    val messages: LiveData<List<ChatMessage>> = _messages

    fun sendWelcomeMessage(userName: String) {
        if (_messages.value?.isEmpty() == true) {
            addMessage(ChatMessage("Hi $userName! Want me to quiz you on isiZulu greetings?", false))
        }
    }

    fun sendMessage(text: String) {
        addMessage(ChatMessage(text, true))

        viewModelScope.launch {
            delay(1000) // Simulate AI thinking
            val response = getBotResponse(text.lowercase())
            addMessage(ChatMessage(response, false))
        }
    }

    private fun addMessage(message: ChatMessage) {
        val current = _messages.value.orEmpty().toMutableList()
        current.add(message)
        _messages.value = current
    }

    private fun getBotResponse(input: String): String {
        return when {
            input.contains("quiz") && (input.contains("yes") || input.contains("please")) -> {
                "Great! How do you say 'thank you' in isiZulu?\n\n(Type your answer below)"
            }
            input.contains("ngiyabonga") -> {
                "Halleluja! That's correct. 'Ngiyabonga' means thank you. Want to try another one?"
            }
            input.contains("quiz") -> {
                "Quiz mode activated! I'll pick a card from your recent deck. Ready for your first question?"
            }
            input.contains("explain") || input.contains("meaning") -> {
                "I can definitely help break that down. Which word or concept would you like me to explain?"
            }
            else -> {
                "I'm here to help you learn! Would you like an example sentence, or should we try a quick quiz?"
            }
        }
    }
}
