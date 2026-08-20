package com.ntando.ivu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntando.ivu.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class RegisterUiState {
    object Idle : RegisterUiState()
    object Loading : RegisterUiState()
    object Success : RegisterUiState()
    data class Error(val message: String) : RegisterUiState()
}

class RegisterViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<RegisterUiState>(RegisterUiState.Idle)
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun registerUser(displayName: String, email: String, password: String) {
        _uiState.value = RegisterUiState.Loading
        viewModelScope.launch {
            val result = authRepository.registerUser(displayName, email, password)
            if (result.isSuccess) {
                _uiState.value = RegisterUiState.Success
            } else {
                _uiState.value = RegisterUiState.Error(result.exceptionOrNull()?.message ?: "Registration failed")
            }
        }
    }
}
