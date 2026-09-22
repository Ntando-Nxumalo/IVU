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
 * UI state representing authentication/login workflow progression.
 */
sealed class LoginUiState {
    /** Initial idle state before login operation. */
    object Idle : LoginUiState()

    /** Loading state while authenticating user credentials. */
    object Loading : LoginUiState()

    /** Successful authentication state. */
    object Success : LoginUiState()

    /**
     * Failure state during login.
     *
     * @property message Descriptive error message detailing failure cause.
     */
    data class Error(val message: String) : LoginUiState()
}

/**
 * ViewModel managing user authentication workflows via email/password or third-party providers (Google Sign-In).
 *
 * Exposes state via [LoginUiState] and delegates login operations to [AuthRepository].
 *
 * @property authRepository Repository handling user authentication APIs.
 */
class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    companion object {
        private const val TAG = "LoginViewModel"
    }

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)

    /**
     * Immutable StateFlow producing the current [LoginUiState].
     */
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "LoginViewModel initialized")
    }

    /**
     * Authenticates a user using email and password credentials.
     *
     * @param email Email address of the user.
     * @param password Password corresponding to the user account.
     */
    fun loginWithEmail(email: String, password: String) {
        Log.i(TAG, "loginWithEmail initiated for email: $email")
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            val result = authRepository.loginUser(email, password)
            if (result.isSuccess) {
                Log.i(TAG, "loginWithEmail succeeded for email: $email")
                _uiState.value = LoginUiState.Success
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Login failed"
                Log.w(TAG, "loginWithEmail failed for email $email: $errorMsg", result.exceptionOrNull())
                _uiState.value = LoginUiState.Error(errorMsg)
            }
        }
    }

    /**
     * Callback handler triggered when Google Sign-In completes successfully.
     */
    fun onGoogleSignInSuccess() {
        Log.i(TAG, "onGoogleSignInSuccess triggered")
        _uiState.value = LoginUiState.Success
    }

    /**
     * Callback handler for third-party or credential sign-in errors.
     *
     * @param message Descriptive error message.
     */
    fun onSignInError(message: String) {
        Log.w(TAG, "onSignInError triggered with message: $message")
        _uiState.value = LoginUiState.Error(message)
    }

    /**
     * Resets the login state flow back to [LoginUiState.Idle].
     */
    fun resetState() {
        Log.d(TAG, "resetState called: resetting UI state to Idle")
        _uiState.value = LoginUiState.Idle
    }
}
