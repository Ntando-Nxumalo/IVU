package com.ntando.ivu

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.ntando.ivu.data.database.DatabaseProvider
import com.ntando.ivu.data.prefs.PreferenceManager
import com.ntando.ivu.data.repository.ChatRepository
import com.ntando.ivu.ui.chat.AiAssistScreen
import com.ntando.ivu.ui.theme.IVUTheme
import com.ntando.ivu.viewmodel.ChatViewModel
import com.ntando.ivu.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

/**
 * [ChatActivity] provides an AI assistant chat interface for learners to ask study questions,
 * seek language clarification, and generate learning aids.
 *
 * Architecture & Features:
 * - Integrates [AiAssistScreen] using Jetpack Compose.
 * - Retrieves user details asynchronously from Room database to issue personalized greetings.
 * - Responds to theme preference changes dynamically.
 */
class ChatActivity : ComponentActivity() {

    companion object {
        private const val TAG = "ChatActivity"
    }

    private val viewModel: ChatViewModel by viewModels {
        ViewModelFactory(ChatRepository())
    }

    /**
     * Initializes the activity lifecycle, retrieves current user preferences, sets up Compose content,
     * and triggers an automated welcome message with the user's first name.
     *
     * @param savedInstanceState Saved instance bundle.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Starting ChatActivity")

        // Retrieve current local user ID from SharedPreferences
        val sharedPref = getSharedPreferences("IVUPrefs", MODE_PRIVATE)
        val currentUserId = sharedPref.getLong("current_user_id", -1)
        Log.d(TAG, "Retrieved current_user_id: $currentUserId")

        // Bind Jetpack Compose UI
        setContent {
            val preferenceManager = PreferenceManager(this)
            val isDarkTheme by preferenceManager.isDarkTheme.collectAsState(initial = false)

            IVUTheme(darkTheme = isDarkTheme) {
                AiAssistScreen(
                    viewModel = viewModel,
                    onBack = {
                        Log.d(TAG, "Back action triggered from AiAssistScreen")
                        finish()
                    }
                )
            }
        }

        // Retrieve user's name from database to send a personalized welcome message
        val db = DatabaseProvider.getDatabase(this)
        lifecycleScope.launch {
            Log.d(TAG, "Fetching user info for ID $currentUserId to trigger welcome message")
            db.userDao().getUserById(currentUserId).collect { user ->
                val name = user?.name?.split(" ")?.firstOrNull() ?: "there"
                Log.i(TAG, "Sending welcome message for user name: '$name'")
                viewModel.sendWelcomeMessage(name)
            }
        }
    }

    /**
     * Called when the activity becomes visible.
     */
    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart: ChatActivity visible")
    }

    /**
     * Called before the activity is destroyed.
     */
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: ChatActivity destroyed")
    }
}
