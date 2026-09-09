package com.ntando.ivu.data.entity

sealed class Badge(
    val id: String,
    val displayName: String,
    val description: String
) {
    object FIRST_REVIEW : Badge(
        id = "FIRST_REVIEW",
        displayName = "First Steps",
        description = "Reviewed your first flashcard"
    )
    object STREAK_7 : Badge(
        id = "STREAK_7",
        displayName = "Week Warrior",
        description = "7-day streak"
    )
    object STREAK_30 : Badge(
        id = "STREAK_30",
        displayName = "Monthly Master",
        description = "30-day streak"
    )
    object CARDS_50 : Badge(
        id = "CARDS_50",
        displayName = "Half Century",
        description = "Reviewed 50 cards total"
    )
    object FIRST_JOURNAL : Badge(
        id = "FIRST_JOURNAL",
        displayName = "Reflective",
        description = "Wrote your first journal entry"
    )

    companion object {
        val ALL by lazy { listOf(FIRST_REVIEW, STREAK_7, STREAK_30, CARDS_50, FIRST_JOURNAL) }
        fun fromId(id: String) = ALL.find { it.id == id }
    }
}
