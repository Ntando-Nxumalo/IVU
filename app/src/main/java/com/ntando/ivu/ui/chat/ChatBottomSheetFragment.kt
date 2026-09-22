package com.ntando.ivu.ui.chat

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.ntando.ivu.R
import com.ntando.ivu.data.database.DatabaseProvider
import com.ntando.ivu.data.repository.ChatRepository
import com.ntando.ivu.viewmodel.ChatViewModel
import com.ntando.ivu.viewmodel.ViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = "ChatBottomSheetFragment"

/**
 * [BottomSheetDialogFragment] providing a slide-up modal bottom sheet interface for chatting with the AI study assistant.
 * Inflates `layout_chat_bot` and connects a [RecyclerView] with [ChatAdapter] to display realtime conversation updates.
 */
class ChatBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var viewModel: ChatViewModel
    private lateinit var adapter: ChatAdapter

    /**
     * Inflates the bottom sheet dialog layout XML ([R.layout.layout_chat_bot]).
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView inflating layout_chat_bot view hierarchy")
        return inflater.inflate(R.layout.layout_chat_bot, container, false)
    }

    /**
     * Binds UI elements, sets up ViewModel observation, initializes [ChatAdapter], and listens for send actions.
     *
     * @param view The Root view returned by [onCreateView].
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated initializing ChatBottomSheetFragment views and viewmodel")

        val sharedPref = requireContext().getSharedPreferences("IVUPrefs", Context.MODE_PRIVATE)
        val currentUserId = sharedPref.getLong("current_user_id", -1)
        Log.d(TAG, "Retrieved current_user_id from shared preferences: $currentUserId")

        val factory = ViewModelFactory(ChatRepository())
        viewModel = ViewModelProvider(this, factory)[ChatViewModel::class.java]

        val rvChat = view.findViewById<RecyclerView>(R.id.rvChat)
        val etMessage = view.findViewById<EditText>(R.id.etChatMessage)
        val btnSend = view.findViewById<FloatingActionButton>(R.id.btnSendMessage)

        adapter = ChatAdapter(emptyList())
        rvChat.layoutManager = LinearLayoutManager(context)
        rvChat.adapter = adapter

        lifecycleScope.launch {
            viewModel.messages.collectLatest { messages ->
                Log.d(TAG, "Observed ${messages.size} chat messages from ChatViewModel StateFlow")
                adapter.updateMessages(messages)
                if (messages.isNotEmpty()) {
                    rvChat.smoothScrollToPosition(messages.size - 1)
                }
            }
        }

        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                Log.i(TAG, "User clicked send button in bottom sheet with text: $text")
                viewModel.sendMessage(text)
                etMessage.text.clear()
            } else {
                Log.w(TAG, "User clicked send button with empty input string")
            }
        }

        // Get user name for welcome message
        val db = DatabaseProvider.getDatabase(requireContext())
        lifecycleScope.launch {
            db.userDao().getUserById(currentUserId).collect { user ->
                val name = user?.name?.split(" ")?.firstOrNull() ?: "there"
                Log.d(TAG, "Fetched user profile from DB, sending initial welcome message for name: $name")
                viewModel.sendWelcomeMessage(name)
            }
        }
    }

    /**
     * Returns the custom theme style ID applied to this bottom sheet ([R.style.CustomBottomSheetDialog]).
     */
    override fun getTheme(): Int = R.style.CustomBottomSheetDialog
}
