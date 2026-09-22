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

/**
 * Unit tests for [RegisterViewModel].
 *
 * Scenarios Tested:
 * - Successful user registration updating [RegisterUiState] to [RegisterUiState.Success].
 * - Failed user registration updating [RegisterUiState] to [RegisterUiState.Error] with the expected exception message.
 *
 * Coroutine Rules & Test Dispatchers:
 * - Uses [StandardTestDispatcher] for deterministic coroutine execution control.
 * - Sets the Main dispatcher via [Dispatchers.setMain] in `@Before` setup and resets it via [Dispatchers.resetMain] in `@After` tearDown.
 * - Uses [runTest] test builder to run asynchronous coroutines in test scope and `testDispatcher.scheduler.advanceUntilIdle()` to drain pending tasks prior to asserting UI state.
 *
 * Mocking Strategy:
 * - Uses [mockk] for [AuthRepository] dependency, mocking `AuthRepository.registerUser` suspend calls using [coEvery].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    private val authRepository: AuthRepository = mockk()
    private lateinit var viewModel: RegisterViewModel
    private val testDispatcher = StandardTestDispatcher()

    /**
     * Set up test dispatcher and initialize [RegisterViewModel] with mocked dependencies before each test.
     */
    @Before
    fun setup() {
        println("Setting up RegisterViewModelTest with StandardTestDispatcher...")
        Dispatchers.setMain(testDispatcher)
        viewModel = RegisterViewModel(authRepository)
    }

    /**
     * Clean up test dispatchers after each test execution to avoid state leakage across test cases.
     */
    @After
    fun tearDown() {
        println("Tearing down RegisterViewModelTest and resetting Main dispatcher...")
        Dispatchers.resetMain()
    }

    /**
     * Test Scenario: Registering a new user succeeds via [AuthRepository].
     *
     * Setup / Mock Behavior:
     * - `authRepository.registerUser` is mocked to return [Result.success] containing user ID ("uid").
     * - ViewModel receives registration request with name, email, and password.
     *
     * Expected Outcomes:
     * - `viewModel.uiState` transitions to [RegisterUiState.Success].
     *
     * Coroutine Control:
     * - Calls `viewModel.registerUser`, advances test dispatcher scheduler using `advanceUntilIdle`, and asserts expected UI state.
     */
    @Test
    fun `registerUser success updates state to Success`() = runTest {
        println("--- Starting Test: registerUser success updates state to Success ---")
        // Given
        val mockUid = "uid"
        println("[Given] Mocking authRepository.registerUser to return Result.success('$mockUid')...")
        coEvery { authRepository.registerUser(any(), any(), any()) } returns Result.success(mockUid)

        // When
        println("[When] Calling viewModel.registerUser('Name', 'test@test.com', 'password')...")
        viewModel.registerUser("Name", "test@test.com", "password")
        println("[When] Advancing test dispatcher scheduler until idle...")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val currentState = viewModel.uiState.value
        println("[Then] Current UI State after execution: $currentState")
        println("[Assertion] Validating currentState == RegisterUiState.Success...")
        assertEquals(RegisterUiState.Success, currentState)
        println("--- Completed Test: registerUser success updates state to Success ---")
    }

    /**
     * Test Scenario: Registering a new user fails via [AuthRepository] due to an exception (e.g. email already in use).
     *
     * Setup / Mock Behavior:
     * - `authRepository.registerUser` is mocked to return [Result.failure] containing an Exception with "Email already in use".
     *
     * Expected Outcomes:
     * - `viewModel.uiState` transitions to [RegisterUiState.Error].
     * - The error state's message matches "Email already in use".
     *
     * Coroutine Control:
     * - Calls `viewModel.registerUser`, advances test dispatcher scheduler using `advanceUntilIdle`, and asserts state type and error message.
     */
    @Test
    fun `registerUser failure updates state to Error`() = runTest {
        println("--- Starting Test: registerUser failure updates state to Error ---")
        // Given
        val errorMessage = "Email already in use"
        println("[Given] Mocking authRepository.registerUser to return Result.failure with message: '$errorMessage'...")
        coEvery { authRepository.registerUser(any(), any(), any()) } returns Result.failure(Exception(errorMessage))

        // When
        println("[When] Calling viewModel.registerUser('Name', 'test@test.com', 'password')...")
        viewModel.registerUser("Name", "test@test.com", "password")
        println("[When] Advancing test dispatcher scheduler until idle...")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        println("[Then] Current UI State after execution: $state")
        println("[Assertion] Validating state is RegisterUiState.Error...")
        assertTrue(state is RegisterUiState.Error)
        val errorState = state as RegisterUiState.Error
        println("[Assertion] Validating error message matches expected ('$errorMessage'): actual='${errorState.message}'")
        assertEquals(errorMessage, errorState.message)
        println("--- Completed Test: registerUser failure updates state to Error ---")
    }
}
