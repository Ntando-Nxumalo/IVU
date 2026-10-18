package com.ntando.ivu.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntando.ivu.data.entity.Flashcard
import com.ntando.ivu.data.model.*
import com.ntando.ivu.data.repository.AchievementRepository
import com.ntando.ivu.data.repository.FlashcardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "IVU_QUIZ"

/**
 * Represents a missed question detail in Quiz Mode.
 */
data class MissedQuestion(
    val question: QuizQuestion,
    val userAnswer: String
)

/**
 * UI State representing Quiz Mode session lifecycle.
 */
sealed class QuizUiState {
    object Loading : QuizUiState()

    /**
     * Displayed when the target deck has no flashcards.
     */
    data class Empty(
        val deckTitle: String
    ) : QuizUiState()

    /**
     * Setup screen configuration options before starting a quiz.
     */
    data class Setup(
        val deckTitle: String,
        val cardCount: Int,
        val selectedMode: QuizMode = QuizMode.MIXED,
        val selectedDirection: QuizDirection = QuizDirection.FRONT_TO_BACK,
        val selectedLength: Int = 10,
        val isMultipleChoiceAvailable: Boolean = cardCount >= 4
    ) : QuizUiState()

    /**
     * Active quiz question screen state.
     */
    data class InProgress(
        val deckTitle: String,
        val questionIndex: Int,
        val totalQuestions: Int,
        val currentQuestion: QuizQuestion,
        val userAnswer: String = "",
        val isChecked: Boolean = false,
        val isCorrect: Boolean = false
    ) : QuizUiState()

    /**
     * Final quiz session results summary.
     */
    data class Finished(
        val deckTitle: String,
        val score: Int,
        val totalQuestions: Int,
        val missedQuestions: List<MissedQuestion> = emptyList(),
        val xpEarned: Int = 0
    ) : QuizUiState()
}

/**
 * ViewModel managing Quiz Mode configuration, question progression, answer checking, and score reporting.
 */
class QuizViewModel(
    private val flashcardRepository: FlashcardRepository,
    private val achievementRepository: AchievementRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<QuizUiState>(QuizUiState.Loading)
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private var deckId: Long = 0L
    private var deckTitle: String = "Quiz"
    private var ownerUserId: String = ""

    private var allDeckCards: List<Flashcard> = emptyList()
    private var activeQuestions: List<QuizQuestion> = emptyList()
    private var currentQuestionIndex: Int = 0
    private var correctCount: Int = 0
    private val missedQuestionsList = mutableListOf<MissedQuestion>()

    private var selectedMode: QuizMode = QuizMode.MIXED
    private var selectedDirection: QuizDirection = QuizDirection.FRONT_TO_BACK
    private var selectedLength: Int = 10

    private var isSubmittingAnswer = false

    /**
     * Initializes the quiz session for the specified deck and loads flashcards from Room.
     */
    fun initDeck(deckId: Long, deckTitle: String, ownerUserId: String) {
        if (this.deckId == deckId && allDeckCards.isNotEmpty()) {
            Log.d(TAG, "Deck already initialized for deckId=$deckId")
            return
        }

        this.deckId = deckId
        this.deckTitle = deckTitle
        this.ownerUserId = ownerUserId

        Log.d(TAG, "Initializing QuizViewModel for deckId=$deckId, title='$deckTitle', ownerUserId='$ownerUserId'")

        viewModelScope.launch {
            _uiState.value = QuizUiState.Loading
            val cards = try {
                flashcardRepository.getCards(deckId, ownerUserId).first()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load flashcards for quiz deckId=$deckId", e)
                emptyList()
            }

            allDeckCards = cards
            if (cards.isEmpty()) {
                Log.d(TAG, "Deck has 0 cards -> Setting state to Empty")
                _uiState.value = QuizUiState.Empty(deckTitle)
            } else {
                val availableLength = if (cards.size < 10) cards.size else 10
                selectedLength = availableLength
                val isMcAvailable = cards.size >= 4
                if (!isMcAvailable && selectedMode == QuizMode.MULTIPLE_CHOICE) {
                    selectedMode = QuizMode.TYPE_ANSWER
                }

                _uiState.value = QuizUiState.Setup(
                    deckTitle = deckTitle,
                    cardCount = cards.size,
                    selectedMode = selectedMode,
                    selectedDirection = selectedDirection,
                    selectedLength = selectedLength,
                    isMultipleChoiceAvailable = isMcAvailable
                )
            }
        }
    }

    /**
     * Updates setup preferences.
     */
    fun updateSetup(mode: QuizMode, direction: QuizDirection, length: Int) {
        val currentState = _uiState.value
        if (currentState is QuizUiState.Setup) {
            val isMcAvailable = currentState.isMultipleChoiceAvailable
            val finalMode = if (!isMcAvailable && mode == QuizMode.MULTIPLE_CHOICE) QuizMode.TYPE_ANSWER else mode
            val cappedLength = length.coerceAtMost(allDeckCards.size).coerceAtLeast(1)

            selectedMode = finalMode
            selectedDirection = direction
            selectedLength = cappedLength

            _uiState.value = currentState.copy(
                selectedMode = finalMode,
                selectedDirection = direction,
                selectedLength = cappedLength
            )
        }
    }

    /**
     * Begins the quiz session using configured setup settings.
     */
    fun startQuiz() {
        if (allDeckCards.isEmpty()) {
            _uiState.value = QuizUiState.Empty(deckTitle)
            return
        }

        activeQuestions = QuizGenerator.generateSession(
            cards = allDeckCards,
            mode = selectedMode,
            direction = selectedDirection,
            sessionSize = selectedLength
        )

        Log.d(TAG, "Quiz session started with ${activeQuestions.size} questions (requested size: $selectedLength, mode: $selectedMode)")

        if (activeQuestions.isEmpty()) {
            _uiState.value = QuizUiState.Empty(deckTitle)
            return
        }

        currentQuestionIndex = 0
        correctCount = 0
        missedQuestionsList.clear()
        isSubmittingAnswer = false

        _uiState.value = QuizUiState.InProgress(
            deckTitle = deckTitle,
            questionIndex = 0,
            totalQuestions = activeQuestions.size,
            currentQuestion = activeQuestions[0],
            userAnswer = "",
            isChecked = false,
            isCorrect = false
        )
    }

    /**
     * Updates typed or selected answer in current state.
     */
    fun onAnswerChanged(answer: String) {
        val state = _uiState.value
        if (state is QuizUiState.InProgress && !state.isChecked) {
            _uiState.value = state.copy(userAnswer = answer)
        }
    }

    /**
     * Evaluates current user answer and updates state.
     */
    fun submitAnswer() {
        val state = _uiState.value
        if (state !is QuizUiState.InProgress || state.isChecked || isSubmittingAnswer) {
            Log.w(TAG, "submitAnswer ignored: state is not InProgress or already checked")
            return
        }

        isSubmittingAnswer = true
        val question = state.currentQuestion
        val userAnswer = state.userAnswer

        val isCorrect = if (question.questionType == QuizQuestionType.MULTIPLE_CHOICE) {
            userAnswer.trim().equals(question.correctAnswer.trim(), ignoreCase = true)
        } else {
            QuizAnswerChecker.isCorrect(userAnswer, question.correctAnswer)
        }

        Log.d(TAG, "Question ${state.questionIndex + 1}/${state.totalQuestions} answered. Result: isCorrect=$isCorrect")

        if (isCorrect) {
            correctCount++
        } else {
            missedQuestionsList.add(MissedQuestion(question, userAnswer))
        }

        _uiState.value = state.copy(
            isChecked = true,
            isCorrect = isCorrect
        )
        isSubmittingAnswer = false
    }

    /**
     * Advances to the next question or completes the quiz session.
     */
    fun nextQuestion() {
        val state = _uiState.value
        if (state !is QuizUiState.InProgress) return

        val nextIndex = state.questionIndex + 1
        if (nextIndex < state.totalQuestions) {
            isSubmittingAnswer = false
            _uiState.value = QuizUiState.InProgress(
                deckTitle = deckTitle,
                questionIndex = nextIndex,
                totalQuestions = state.totalQuestions,
                currentQuestion = activeQuestions[nextIndex],
                userAnswer = "",
                isChecked = false,
                isCorrect = false
            )
        } else {
            finishQuiz()
        }
    }

    /**
     * Restarts a new quiz session with only the missed questions.
     */
    fun retryMissed() {
        if (missedQuestionsList.isEmpty()) return

        val missedCards = missedQuestionsList.map { it.question.card }
        activeQuestions = QuizGenerator.generateSession(
            cards = missedCards,
            mode = selectedMode,
            direction = selectedDirection,
            sessionSize = missedCards.size
        )

        Log.d(TAG, "Retrying missed questions: ${activeQuestions.size} cards")

        currentQuestionIndex = 0
        correctCount = 0
        missedQuestionsList.clear()
        isSubmittingAnswer = false

        _uiState.value = QuizUiState.InProgress(
            deckTitle = deckTitle,
            questionIndex = 0,
            totalQuestions = activeQuestions.size,
            currentQuestion = activeQuestions[0],
            userAnswer = "",
            isChecked = false,
            isCorrect = false
        )
    }

    /**
     * Resets state back to setup screen.
     */
    fun resetToSetup() {
        if (allDeckCards.isEmpty()) {
            _uiState.value = QuizUiState.Empty(deckTitle)
        } else {
            val isMcAvailable = allDeckCards.size >= 4
            _uiState.value = QuizUiState.Setup(
                deckTitle = deckTitle,
                cardCount = allDeckCards.size,
                selectedMode = selectedMode,
                selectedDirection = selectedDirection,
                selectedLength = selectedLength,
                isMultipleChoiceAvailable = isMcAvailable
            )
        }
    }

    private fun finishQuiz() {
        val total = activeQuestions.size
        val score = correctCount
        Log.d(TAG, "Quiz finished. Score: $score / $total")

        val xpEarned = (score * 5) + if (score == total && total > 0) 20 else 0

        if (ownerUserId.isNotBlank()) {
            viewModelScope.launch {
                try {
                    achievementRepository?.recordQuizCompleted(ownerUserId, score, total)
                } catch (e: Exception) {
                    Log.e(TAG, "Error recording quiz completion gamification", e)
                }
            }
        }

        _uiState.value = QuizUiState.Finished(
            deckTitle = deckTitle,
            score = score,
            totalQuestions = total,
            missedQuestions = missedQuestionsList.toList(),
            xpEarned = xpEarned
        )
    }
}
