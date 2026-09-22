package com.ntando.ivu.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks gamification progress, activity metrics, and streak data for a specific user.
 *
 * Maintains experience points (XP), user level, study streaks, and earned badge IDs.
 *
 * @property userId Unique string primary key matching the user identifier.
 * @property currentStreak Current consecutive active study days streak.
 * @property longestStreak Longest historical streak achieved by user.
 * @property xp Total accumulated experience points.
 * @property level Current user level calculated from XP.
 * @property totalReviews All-time flashcard review count.
 * @property dailyReviews Cards reviewed during current calendar day.
 * @property lastActiveDate Epoch timestamp in milliseconds of user's last study session.
 * @property badges List of unlocked badge ID strings (converted via TypeConverter).
 */
@Entity(tableName = "user_stats")
data class UserStats(
    /** Primary key string identifying user. */
    @PrimaryKey val userId: String,
    /** Current active daily study streak count. */
    val currentStreak: Int = 0,
    /** Maximum consecutive study streak days reached. */
    val longestStreak: Int = 0,
    /** Accumulated experience points. */
    val xp: Int = 0,
    /** Current progression level. */
    val level: Int = 1,
    /** Total lifetime flashcard reviews completed. */
    val totalReviews: Int = 0,
    /** Number of flashcard reviews completed today. */
    val dailyReviews: Int = 0,
    /** Timestamp in epoch milliseconds when user was last active. */
    val lastActiveDate: Long = 0,
    /** List of badge string identifiers earned by user. */
    val badges: List<String> = emptyList()
)
