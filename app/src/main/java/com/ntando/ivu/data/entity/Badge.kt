package com.ntando.ivu.data.entity

/**
 * Sealed class defining earnable badges in the IVU application.
 *
 * Represents specific milestone rewards awarded to users for study streaks, card reviews,
 * and journaling activities.
 *
 * @property id Unique string identifier for the badge type.
 * @property displayName User-facing name of the badge.
 * @property description Explanation of how to unlock the badge.
 */
sealed class Badge(
    val id: String,
    val displayName: String,
    val description: String
) {
    /** Badge awarded when the user completes their first flashcard review session. */
    object FIRST_REVIEW : Badge(
        id = "FIRST_REVIEW",
        displayName = "First Steps",
        description = "Reviewed your first flashcard"
    )

    /** Badge awarded when the user maintains a study streak for 7 consecutive days. */
    object STREAK_7 : Badge(
        id = "STREAK_7",
        displayName = "Week Warrior",
        description = "7-day streak"
    )

    /** Badge awarded when the user maintains a study streak for 30 consecutive days. */
    object STREAK_30 : Badge(
        id = "STREAK_30",
        displayName = "Monthly Master",
        description = "30-day streak"
    )

    /** Badge awarded when the user reviews a total of 50 flashcards across all decks. */
    object CARDS_50 : Badge(
        id = "CARDS_50",
        displayName = "Half Century",
        description = "Reviewed 50 cards total"
    )

    /** Badge awarded when the user writes their first reflection or journal entry. */
    object FIRST_JOURNAL : Badge(
        id = "FIRST_JOURNAL",
        displayName = "Reflective",
        description = "Wrote your first journal entry"
    )

    companion object {
        /** Lazy-initialized list containing all defined [Badge] object instances. */
        val ALL by lazy { listOf(FIRST_REVIEW, STREAK_7, STREAK_30, CARDS_50, FIRST_JOURNAL) }

        /**
         * Resolves a [Badge] object matching the provided [id] string.
         *
         * @param id The string identifier to look up.
         * @return The matching [Badge], or `null` if no match exists.
         */
        fun fromId(id: String): Badge? = ALL.find { it.id == id }
    }
}
