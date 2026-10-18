package com.ntando.ivu.data.model

import com.ntando.ivu.data.entity.Flashcard
import org.junit.Assert.*
import org.junit.Test

class QuizGeneratorTest {

    private fun createDummyCards(count: Int): List<Flashcard> {
        return (1..count).map { i ->
            Flashcard(
                cardId = i.toLong(),
                deckId = 100L,
                frontText = "Front $i",
                backText = "Back $i"
            )
        }
    }

    @Test
    fun `empty deck produces empty session`() {
        val result = QuizGenerator.generateSession(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `deck with less than 4 cards forces TYPE_ANSWER questions`() {
        val cards = createDummyCards(3)
        val questions = QuizGenerator.generateSession(
            cards = cards,
            mode = QuizMode.MULTIPLE_CHOICE,
            sessionSize = 10
        )

        assertEquals(3, questions.size)
        assertTrue(questions.all { it.questionType == QuizQuestionType.TYPE_ANSWER })
    }

    @Test
    fun `session length is capped at deck size`() {
        val cards = createDummyCards(5)
        val questions = QuizGenerator.generateSession(cards, sessionSize = 20)
        assertEquals(5, questions.size)
    }

    @Test
    fun `multiple choice question has 4 options with correct answer appearing exactly once`() {
        val cards = createDummyCards(10)
        val questions = QuizGenerator.generateSession(
            cards = cards,
            mode = QuizMode.MULTIPLE_CHOICE,
            direction = QuizDirection.FRONT_TO_BACK,
            sessionSize = 5
        )

        assertEquals(5, questions.size)
        questions.forEach { q ->
            assertEquals(QuizQuestionType.MULTIPLE_CHOICE, q.questionType)
            assertEquals(4, q.options.size)

            val correctCount = q.options.count { it.trim().equals(q.correctAnswer.trim(), ignoreCase = true) }
            assertEquals(1, correctCount)

            val uniqueNormalized = q.options.map { it.trim().lowercase() }.toSet()
            assertEquals(4, uniqueNormalized.size)
        }
    }

    @Test
    fun `back to front direction sets prompt to back and answer to front`() {
        val cards = createDummyCards(5)
        val questions = QuizGenerator.generateSession(
            cards = cards,
            mode = QuizMode.TYPE_ANSWER,
            direction = QuizDirection.BACK_TO_FRONT,
            sessionSize = 5
        )

        assertEquals(5, questions.size)
        questions.forEach { q ->
            assertEquals(q.card.backText, q.promptText)
            assertEquals(q.card.frontText, q.correctAnswer)
        }
    }

    @Test
    fun `QuizAnswerChecker handles case, spaces, accents and punctuation`() {
        assertTrue(QuizAnswerChecker.isCorrect("Café", "cafe"))
        assertTrue(QuizAnswerChecker.isCorrect("  Hello   World! ", "hello world"))
        assertTrue(QuizAnswerChecker.isCorrect("(Hola?)", "hola"))
        assertFalse(QuizAnswerChecker.isCorrect("Wrong Answer", "Right Answer"))
    }
}
