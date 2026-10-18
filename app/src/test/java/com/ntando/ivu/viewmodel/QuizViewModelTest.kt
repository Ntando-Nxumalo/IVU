package com.ntando.ivu.viewmodel

import com.ntando.ivu.data.entity.Flashcard
import com.ntando.ivu.data.model.QuizDirection
import com.ntando.ivu.data.model.QuizMode
import com.ntando.ivu.data.model.QuizQuestionType
import com.ntando.ivu.data.repository.FlashcardRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuizViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository: FlashcardRepository = mockk()
    private lateinit var viewModel: QuizViewModel

    private fun createCards(count: Int): List<Flashcard> {
        return (1..count).map { i ->
            Flashcard(
                cardId = i.toLong(),
                deckId = 1L,
                frontText = "Front $i",
                backText = "Back $i"
            )
        }
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = QuizViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initDeck with empty deck sets state to Empty`() = runTest {
        every { repository.getCards(1L, "user1") } returns flowOf(emptyList())

        viewModel.initDeck(1L, "Empty Deck", "user1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is QuizUiState.Empty)
        assertEquals("Empty Deck", (state as QuizUiState.Empty).deckTitle)
    }

    @Test
    fun `initDeck with cards sets state to Setup`() = runTest {
        val cards = createCards(5)
        every { repository.getCards(1L, "user1") } returns flowOf(cards)

        viewModel.initDeck(1L, "Sample Deck", "user1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is QuizUiState.Setup)
        val setup = state as QuizUiState.Setup
        assertEquals("Sample Deck", setup.deckTitle)
        assertEquals(5, setup.cardCount)
        assertTrue(setup.isMultipleChoiceAvailable)
    }

    @Test
    fun `startQuiz creates InProgress state and double submit is guarded`() = runTest {
        val cards = createCards(5)
        every { repository.getCards(1L, "user1") } returns flowOf(cards)

        viewModel.initDeck(1L, "Sample Deck", "user1")
        advanceUntilIdle()

        viewModel.updateSetup(QuizMode.TYPE_ANSWER, QuizDirection.FRONT_TO_BACK, 5)
        viewModel.startQuiz()

        val state = viewModel.uiState.value
        assertTrue(state is QuizUiState.InProgress)
        val inProgress = state as QuizUiState.InProgress
        assertEquals(0, inProgress.questionIndex)
        assertEquals(5, inProgress.totalQuestions)
        assertFalse(inProgress.isChecked)

        // Type correct answer
        viewModel.onAnswerChanged(inProgress.currentQuestion.correctAnswer)
        viewModel.submitAnswer()

        val checkedState = viewModel.uiState.value as QuizUiState.InProgress
        assertTrue(checkedState.isChecked)
        assertTrue(checkedState.isCorrect)

        // Double submit attempt should be ignored
        viewModel.submitAnswer()
        val afterDoubleSubmit = viewModel.uiState.value as QuizUiState.InProgress
        assertTrue(afterDoubleSubmit.isChecked)
    }
}
