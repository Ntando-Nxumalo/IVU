package com.ntando.ivu.data.model

import com.ntando.ivu.data.entity.Flashcard

/**
 * Generates quiz sessions with multiple choice and type-answer questions from Room flashcards.
 */
object QuizGenerator {

    /**
     * Constructs a list of [QuizQuestion]s for a quiz session.
     *
     * @param cards The list of flashcards belonging to the deck.
     * @param mode Quiz mode: MULTIPLE_CHOICE, TYPE_ANSWER, or MIXED.
     * @param direction Direction of prompt: FRONT_TO_BACK or BACK_TO_FRONT.
     * @param sessionSize Desired question count (e.g. 5, 10, or total cards). Capped at cards size.
     * @return List of generated [QuizQuestion]s.
     */
    fun generateSession(
        cards: List<Flashcard>,
        mode: QuizMode = QuizMode.MIXED,
        direction: QuizDirection = QuizDirection.FRONT_TO_BACK,
        sessionSize: Int = 10
    ): List<QuizQuestion> {
        if (cards.isEmpty()) return emptyList()

        val actualSize = sessionSize.coerceAtMost(cards.size).coerceAtLeast(1)
        val shuffledCards = cards.shuffled().take(actualSize)
        val canDoMultipleChoice = cards.size >= 4

        val questions = mutableListOf<QuizQuestion>()

        shuffledCards.forEachIndexed { index, card ->
            val promptText = if (direction == QuizDirection.FRONT_TO_BACK) card.frontText else card.backText
            val correctAnswer = if (direction == QuizDirection.FRONT_TO_BACK) card.backText else card.frontText

            val questionType = when {
                !canDoMultipleChoice -> QuizQuestionType.TYPE_ANSWER
                mode == QuizMode.MULTIPLE_CHOICE -> QuizQuestionType.MULTIPLE_CHOICE
                mode == QuizMode.TYPE_ANSWER -> QuizQuestionType.TYPE_ANSWER
                mode == QuizMode.MIXED -> if (index % 2 == 0) QuizQuestionType.MULTIPLE_CHOICE else QuizQuestionType.TYPE_ANSWER
                else -> QuizQuestionType.TYPE_ANSWER
            }

            if (questionType == QuizQuestionType.MULTIPLE_CHOICE) {
                val distractorPool = cards.filter { it.cardId != card.cardId }
                    .map { if (direction == QuizDirection.FRONT_TO_BACK) it.backText else it.frontText }

                val normCorrect = correctAnswer.trim().lowercase()
                val uniqueDistractors = mutableListOf<String>()
                val seenNorm = mutableSetOf(normCorrect)

                for (candidate in distractorPool.shuffled()) {
                    val normCandidate = candidate.trim().lowercase()
                    if (seenNorm.add(normCandidate)) {
                        uniqueDistractors.add(candidate.trim())
                        if (uniqueDistractors.size == 3) break
                    }
                }

                if (uniqueDistractors.size < 3) {
                    questions.add(
                        QuizQuestion(
                            card = card,
                            promptText = promptText,
                            correctAnswer = correctAnswer,
                            questionType = QuizQuestionType.TYPE_ANSWER,
                            options = emptyList()
                        )
                    )
                } else {
                    val options = (uniqueDistractors + correctAnswer.trim()).shuffled()
                    questions.add(
                        QuizQuestion(
                            card = card,
                            promptText = promptText,
                            correctAnswer = correctAnswer,
                            questionType = QuizQuestionType.MULTIPLE_CHOICE,
                            options = options
                        )
                    )
                }
            } else {
                questions.add(
                    QuizQuestion(
                        card = card,
                        promptText = promptText,
                        correctAnswer = correctAnswer,
                        questionType = QuizQuestionType.TYPE_ANSWER,
                        options = emptyList()
                    )
                )
            }
        }

        return questions
    }
}
