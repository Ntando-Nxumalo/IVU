package com.ntando.ivu.ui.chat

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.ntando.ivu.R
import com.ntando.ivu.data.model.ChatMessage

/**
 * [RecyclerView.Adapter] responsible for rendering chat conversation streams containing both user and AI bot messages.
 * Uses distinct item view types ([TYPE_USER] and [TYPE_BOT]) to inflate appropriate item layouts.
 *
 * @property messages The list of [ChatMessage] items currently held and displayed by the adapter.
 */
class ChatAdapter(private var messages: List<ChatMessage>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TAG = "ChatAdapter"
        private const val TYPE_USER = 1
        private const val TYPE_BOT = 2
    }

    /**
     * Determines whether the chat item at [position] belongs to the user or the bot.
     *
     * @param position Index of the item in [messages].
     * @return [TYPE_USER] if the message is from the user; otherwise [TYPE_BOT].
     */
    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUser) TYPE_USER else TYPE_BOT
    }

    /**
     * Inflates the appropriate layout XML file according to [viewType] and constructs the view holder.
     *
     * @param parent The parent ViewGroup into which the new View will be added.
     * @param viewType The view type integer returned by [getItemViewType].
     * @return A [UserViewHolder] or [BotViewHolder] containing the inflated view.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        Log.d(TAG, "onCreateViewHolder invoked for viewType: $viewType")
        return if (viewType == TYPE_USER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_user, parent, false)
            UserViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_bot, parent, false)
            BotViewHolder(view)
        }
    }

    /**
     * Binds message data to the corresponding view holder based on item position.
     *
     * @param holder The RecyclerView ViewHolder to be updated.
     * @param position The adapter position of the item being bound.
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        Log.d(TAG, "onBindViewHolder binding position: $position, isUser: ${message.isUser}")
        if (holder is UserViewHolder) {
            holder.tvMessage.text = message.text
        } else if (holder is BotViewHolder) {
            holder.tvMessage.text = message.text
        }
    }

    /**
     * Returns the total number of chat messages currently managed by the adapter.
     */
    override fun getItemCount(): Int = messages.size

    /**
     * Updates the adapter's underlying dataset using [DiffUtil] for optimal animation and re-rendering performance.
     *
     * @param newMessages The updated list of [ChatMessage] objects to display.
     */
    fun updateMessages(newMessages: List<ChatMessage>) {
        Log.d(TAG, "updateMessages updating dataset from size ${messages.size} to ${newMessages.size}")
        val diffResult = DiffUtil.calculateDiff(MessageDiffCallback(messages, newMessages))
        messages = newMessages
        diffResult.dispatchUpdatesTo(this)
    }

    /**
     * [DiffUtil.Callback] implementation to calculate structural changes between two chat message lists.
     */
    class MessageDiffCallback(
        private val oldList: List<ChatMessage>,
        private val newList: List<ChatMessage>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] === newList[newItemPosition] ||
                   (oldList[oldItemPosition].text == newList[newItemPosition].text &&
                    oldList[oldItemPosition].isUser == newList[newItemPosition].isUser)
        }
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }

    /**
     * ViewHolder for user messages containing [R.id.tvUserMessage].
     */
    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView = view.findViewById(R.id.tvUserMessage)
    }

    /**
     * ViewHolder for AI bot messages containing [R.id.tvBotMessage].
     */
    class BotViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView = view.findViewById(R.id.tvBotMessage)
    }
}
