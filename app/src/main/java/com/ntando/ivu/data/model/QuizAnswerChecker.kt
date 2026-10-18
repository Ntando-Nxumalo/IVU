package com.ntando.ivu.data.model

import java.text.Normalizer

/**
 * Utility for normalizing user inputs and verifying typed answers in Quiz Mode.
 */
object QuizAnswerChecker {

    /**
     * Normalizes an answer string for strict case-insensitive and accent-insensitive comparison:
     * - Removes diacritics / accents (e.g. "café" -> "cafe")
     * - Strips leading/trailing punctuation and whitespace
     * - Converts to lowercase
     * - Collapses multiple spaces into a single space
     *
     * @param text Input answer string to normalize.
     * @return Normalized string.
     */
    fun normalize(text: String): String {
        if (text.isBlank()) return ""

        // 1. Normalize accents (NFD decomposition and removal of mark characters)
        val temp = Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")

        // 2. Trim surrounding punctuation and whitespace
        val trimmed = temp.trim().replace(Regex("^[\\p{Punct}\\s]+|[\\p{Punct}\\s]+$"), "")

        // 3. Lowercase & collapse repeated spaces
        return trimmed.lowercase().replace(Regex("\\s+"), " ")
    }

    /**
     * Evaluates if [userAnswer] matches [correctAnswer] after normalization.
     *
     * @param userAnswer Answer typed by user.
     * @param correctAnswer Expected correct answer.
     * @return `true` if correct; `false` otherwise.
     */
    fun isCorrect(userAnswer: String, correctAnswer: String): Boolean {
        val normUser = normalize(userAnswer)
        val normCorrect = normalize(correctAnswer)
        return normUser.isNotEmpty() && normUser == normCorrect
    }
}
