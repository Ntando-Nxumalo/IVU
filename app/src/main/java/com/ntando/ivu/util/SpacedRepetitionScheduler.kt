package com.ntando.ivu.util

import com.ntando.ivu.data.entity.Flashcard
import java.util.concurrent.TimeUnit

/**
 * Kotlin port of SuperMemo SM-2 spaced-repetition algorithm logic.
 */
object SpacedRepetitionScheduler {

    /**
     * Calculates updated [Flashcard] scheduling parameters (easeFactor, intervalDays, repetitions, dueDate)
     * based on the user's review rating ("again", "hard", "good", "easy").
     *
     * @param card Target [Flashcard] instance being reviewed.
     * @param rating Rating value string ("again", "hard", "good", "easy").
     * @return Updated copy of [Flashcard] with new scheduling fields.
     */
    fun calculateNextReview(card: Flashcard, rating: String): Flashcard {
        val quality = when (rating.lowercase()) {
            "again" -> 1
            "hard" -> 3
            "good" -> 4
            "easy" -> 5
            else -> 3
        }

        var newEaseFactor = card.easeFactor + (0.1f - (5 - quality) * (0.08f + (5 - quality) * 0.02f))
        if (newEaseFactor < 1.3f) {
            newEaseFactor = 1.3f
        }

        val newRepetitions: Int
        val newIntervalDays: Int

        if (quality < 3) {
            newRepetitions = 0
            newIntervalDays = 1
        } else {
            newRepetitions = card.repetitions + 1
            newIntervalDays = when (newRepetitions) {
                1 -> 1
                2 -> 6
                else -> (card.intervalDays * newEaseFactor).toInt().coerceAtLeast(1)
            }
        }

        val currentTime = System.currentTimeMillis()
        val newDueDate = currentTime + TimeUnit.DAYS.toMillis(newIntervalDays.toLong())

        return card.copy(
            easeFactor = newEaseFactor,
            intervalDays = newIntervalDays,
            repetitions = newRepetitions,
            dueDate = newDueDate,
            updatedAt = currentTime
        )
    }
}
