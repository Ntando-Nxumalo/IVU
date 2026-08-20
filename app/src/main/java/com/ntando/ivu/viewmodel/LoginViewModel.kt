package com.ntando.ivu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntando.ivu.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    object Success : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun loginWithEmail(email: String, password: String) {
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            val result = authRepository.loginUser(email, password)
            if (result.isSuccess) {
                _uiState.value = LoginUiState.Success
            } else {
                _uiState.value = LoginUiState.Error(result.exceptionOrNull()?.message ?: "Login failed")
            }
        }
    }

    // Google Sign-In Success callback
    fun onGoogleSignInSuccess() {
        _uiState.value = LoginUiState.Success
    }

    fun onSignInError(message: String) {
        _uiState.value = LoginUiState.Error(message)
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }
}
