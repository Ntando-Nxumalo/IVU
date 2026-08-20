package com.ntando.ivu

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.ntando.ivu.data.database.DatabaseProvider
import com.ntando.ivu.data.repository.ChatRepository
import com.ntando.ivu.ui.chat.AiAssistScreen
import com.ntando.ivu.viewmodel.ChatViewModel
import com.ntando.ivu.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

class ChatActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels {
        ViewModelFactory(ChatRepository())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPref = getSharedPreferences("IVUPrefs", Context.MODE_PRIVATE)
        val currentUserId = sharedPref.getLong("current_user_id", -1)

        setContent {
            AiAssistScreen(
                viewModel = viewModel,
                onBack = { finish() }
            )
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
