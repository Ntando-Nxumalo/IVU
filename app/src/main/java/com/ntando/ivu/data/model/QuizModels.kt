package com.ntando.ivu.data.model

import com.ntando.ivu.data.entity.Flashcard

/**
 * Question types supported in a Quiz Mode session.
 */
enum class QuizQuestionType {
    MULTIPLE_CHOICE,
    TYPE_ANSWER
}

/**
 * Quiz modes selectable during quiz setup.
 */
enum class QuizMode {
    MULTIPLE_CHOICE,
    TYPE_ANSWER,
    MIXED
}

/**
 * Prompt direction for quiz questions.
 */
enum class QuizDirection {
    FRONT_TO_BACK,
    BACK_TO_FRONT
}

/**
 * Represents a single question in a quiz session.
 *
 * @property card Source [Flashcard] associated with this question.
 * @property promptText Prompt or question displayed to the user.
 * @property correctAnswer The expected correct answer text.
 * @property questionType Whether this question is multiple choice or type answer.
 * @property options Shuffled list of 4 options if multiple choice, or empty list if type answer.
 */
data class QuizQuestion(
    val card: Flashcard,
    val promptText: String,
    val correctAnswer: String,
    val questionType: QuizQuestionType,
    val options: List<String> = emptyList()
)
