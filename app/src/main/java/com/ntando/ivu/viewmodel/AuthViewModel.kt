package com.ntando.ivu.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntando.ivu.data.entity.User
import com.ntando.ivu.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for user profile querying and user account registration operations.
 *
 * Manages the active user ID state flow, reactive database lookup pipeline via [UserRepository],
 * and background user insertion coroutines.
 *
 * @property repository The repository providing user data persistence operations.
 */
class AuthViewModel(private val repository: UserRepository) : ViewModel() {

    companion object {
        private const val TAG = "AuthViewModel"
    }

    private val _userId = MutableStateFlow<Long>(-1L)

    init {
        Log.d(TAG, "AuthViewModel initialized")
    }

    /**
     * StateFlow pipeline producing the currently selected [User] details or null if no valid user ID is set.
     *
     * Reactively shifts queries using [flatMapLatest] whenever [_userId] changes and keeps state active
     * for 5000ms after subscribers detach.
     */
    val user: StateFlow<User?> = _userId.flatMapLatest { id ->
        Log.d(TAG, "Fetching user by id: $id")
        repository.getUserById(id)
    }.onEach { user ->
        Log.d(TAG, "User state updated: $user")
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    /**
     * Updates the target user ID, triggering a reactive query for the matching user details.
     *
     * @param id The ID of the user to load.
     */
    fun setUserId(id: Long) {
        Log.i(TAG, "setUserId called with id: $id")
        _userId.value = id
    }

    /**
     * Registers a new user with the specified credentials in the local repository asynchronously.
     *
     * @param name Full name or display name of the user.
     * @param email User email address.
     * @param password Account password.
     */
    fun registerUser(name: String, email: String, password: String) {
        Log.i(TAG, "registerUser started for name: $name, email: $email")
        viewModelScope.launch {
            try {
                val newUser = User(name = name, email = email, password = password)
                repository.insertUser(newUser)
                Log.i(TAG, "registerUser succeeded for email: $email")
            } catch (e: Exception) {
                Log.e(TAG, "registerUser failed for email: $email", e)
            }
        }
    }
}
