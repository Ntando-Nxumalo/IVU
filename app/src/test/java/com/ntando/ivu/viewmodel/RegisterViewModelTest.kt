package com.ntando.ivu.viewmodel

import com.ntando.ivu.data.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    private val authRepository: AuthRepository = mockk()
    private lateinit var viewModel: RegisterViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = RegisterViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `registerUser success updates state to Success`() = runTest {
        // Given
        coEvery { authRepository.registerUser(any(), any(), any()) } returns Result.success("uid")

        // When
        viewModel.registerUser("Name", "test@test.com", "password")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(RegisterUiState.Success, viewModel.uiState.value)
    }

    @Test
    fun `registerUser failure updates state to Error`() = runTest {
        // Given
        val errorMessage = "Email already in use"
        coEvery { authRepository.registerUser(any(), any(), any()) } returns Result.failure(Exception(errorMessage))

        // When
        viewModel.registerUser("Name", "test@test.com", "password")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state is RegisterUiState.Error)
        assertEquals(errorMessage, (state as RegisterUiState.Error).message)
    }
}
