package com.ntando.ivu.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntando.ivu.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state representing account registration workflow progression.
 */
sealed class RegisterUiState {
    /** Initial idle state before registration. */
    object Idle : RegisterUiState()

    /** Loading state while creating account credentials. */
    object Loading : RegisterUiState()

    /** Successful account creation state. */
    object Success : RegisterUiState()

    /**
     * Failure state during registration.
     *
     * @property message Descriptive error message detailing failure cause.
     */
    data class Error(val message: String) : RegisterUiState()
}

/**
 * ViewModel managing user account creation operations.
 *
 * Exposes state via [RegisterUiState] and delegates registration requests to [AuthRepository].
 *
 * @property authRepository Repository handling user registration and authentication APIs.
 */
class RegisterViewModel(private val authRepository: AuthRepository) : ViewModel() {

    companion object {
        private const val TAG = "RegisterViewModel"
    }

    private val _uiState = MutableStateFlow<RegisterUiState>(RegisterUiState.Idle)

    /**
     * Immutable StateFlow producing current [RegisterUiState].
     */
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "RegisterViewModel initialized")
    }

    /**
     * Registers a new user account with display name, email, and password credentials.
     *
     * @param displayName User's preferred display name.
     * @param email User's email address.
     * @param password Desired account password.
     */
    fun registerUser(displayName: String, email: String, password: String) {
        Log.i(TAG, "registerUser initiated for displayName: $displayName, email: $email")
        _uiState.value = RegisterUiState.Loading
        viewModelScope.launch {
            val result = authRepository.registerUser(displayName, email, password)
            if (result.isSuccess) {
                Log.i(TAG, "registerUser succeeded for email: $email")
                _uiState.value = RegisterUiState.Success
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Registration failed"
                Log.w(TAG, "registerUser failed for email $email: $errorMsg", result.exceptionOrNull())
                _uiState.value = RegisterUiState.Error(errorMsg)
            }
        }
    }
}
