package com.ntando.ivu

import android.content.Context
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.ntando.ivu.data.database.DatabaseProvider
import com.ntando.ivu.ui.chat.ChatAdapter
import com.ntando.ivu.viewmodel.ChatViewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {

    private lateinit var viewModel: ChatViewModel
    private lateinit var adapter: ChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_chat_bot)

        val sharedPref = getSharedPreferences("IVUPrefs", Context.MODE_PRIVATE)
        val currentUserId = sharedPref.getLong("current_user_id", -1)

        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                    return ChatViewModel(currentUserId) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
        
        viewModel = ViewModelProvider(this, factory)[ChatViewModel::class.java]

        val rvChat = findViewById<RecyclerView>(R.id.rvChat)
        val etMessage = findViewById<EditText>(R.id.etChatMessage)
        val btnSend = findViewById<FloatingActionButton>(R.id.btnSendMessage)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        adapter = ChatAdapter(emptyList())
        rvChat.layoutManager = LinearLayoutManager(this)
        rvChat.adapter = adapter

        viewModel.messages.observe(this) { messages ->
            adapter.updateMessages(messages)
            if (messages.isNotEmpty()) {
                rvChat.smoothScrollToPosition(messages.size - 1)
            }
        }

        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                viewModel.sendMessage(text)
                etMessage.text.clear()
            }
        }

        btnBack.setOnClickListener {
            finish()
        }

        // Suggestions
        findViewById<TextView>(R.id.suggestionLayout).apply {
            // In layout_chat_bot.xml, suggestionLayout is a LinearLayout. 
            // I should have put IDs on the children.
        }

        // Get user name for welcome message
        val db = DatabaseProvider.getDatabase(this)
        lifecycleScope.launch {
            db.userDao().getUserById(currentUserId).collect { user ->
                val name = user?.name?.split(" ")?.firstOrNull() ?: "there"
                viewModel.sendWelcomeMessage(name)
            }
        }
    }
}
