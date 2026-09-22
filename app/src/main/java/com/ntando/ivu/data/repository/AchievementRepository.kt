package com.ntando.ivu.data.repository

import android.util.Log
import com.ntando.ivu.data.dao.*
import com.ntando.ivu.data.entity.Badge
import com.ntando.ivu.data.entity.UserStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repository responsible for managing user gamification statistics, achievement tracking,
 * XP/level progression, daily streaks, and badge unlocking.
 *
 * Interacts with [UserStatsDao] to persist stats and [JournalDao] to verify journal-related accomplishments.
 *
 * @property userStatsDao DAO for accessing and updating [UserStats].
 * @property journalDao DAO for querying user journal entries for achievement verification.
 */
class AchievementRepository(
    private val userStatsDao: UserStatsDao,
    private val journalDao: JournalDao
) {
    private val TAG = "AchievementRepository"

    /**
     * Retrieves a reactive stream ([Flow]) of [UserStats] for a specific user.
     *
     * @param userId Unique identifier of the user.
     * @return A [Flow] emitting [UserStats] or `null` if no stats exist yet.
     */
    fun getUserStats(userId: String): Flow<UserStats?> {
        Log.d(TAG, "getUserStats: Fetching user stats flow for userId=$userId")
        return userStatsDao.getUserStats(userId)
    }

    /**
     * Records a flashcard review activity for the specified user, updating stats and unlocking relevant badges.
     *
     * @param userId Unique identifier of the user performing the review.
     * @return List of newly unlocked [Badge] instances resulting from this activity.
     */
    suspend fun recordReview(userId: String): List<Badge> {
        Log.d(TAG, "recordReview: Recording flashcard review for userId=$userId")
        return recordActivity(userId, ActivityType.FLASHCARD_REVIEW)
    }

    /**
     * Records a journal entry creation activity for the specified user, updating stats and unlocking relevant badges.
     *
     * @param userId Unique identifier of the user creating the journal entry.
     * @return List of newly unlocked [Badge] instances resulting from this activity.
     */
    suspend fun recordJournalEntry(userId: String): List<Badge> {
        Log.d(TAG, "recordJournalEntry: Recording journal entry for userId=$userId")
        return recordActivity(userId, ActivityType.JOURNAL_ENTRY)
    }

    /**
     * Core business logic to process user activity, calculate streak, award XP, update level,
     * evaluate badge unlock conditions, and persist the updated statistics.
     *
     * @param userId Unique identifier of the user.
     * @param activityType Type of activity being performed ([ActivityType.FLASHCARD_REVIEW] or [ActivityType.JOURNAL_ENTRY]).
     * @return List of newly unlocked [Badge] items.
     */
    private suspend fun recordActivity(userId: String, activityType: ActivityType): List<Badge> {
        Log.d(TAG, "recordActivity: Processing $activityType for userId=$userId")
        val currentTime = System.currentTimeMillis()
        val stats = try {
            userStatsDao.getUserStats(userId).first() ?: UserStats(userId)
        } catch (e: Exception) {
            Log.e(TAG, "recordActivity: Error reading user stats for userId=$userId", e)
            UserStats(userId)
        }
        
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val isSameDay = stats.lastActiveDate != 0L && 
                sdf.format(Date(currentTime)) == sdf.format(Date(stats.lastActiveDate))
        
        val baseDailyReviews = if (isSameDay) stats.dailyReviews else 0
        
        var xpToAdd = 0
        var newTotalReviews = stats.totalReviews
        var newDailyReviews = baseDailyReviews
        
        when (activityType) {
            ActivityType.FLASHCARD_REVIEW -> {
                xpToAdd = 10
                newTotalReviews++
                newDailyReviews++
            }
            ActivityType.JOURNAL_ENTRY -> {
                xpToAdd = 5
            }
        }
        
        val newXp = stats.xp + xpToAdd
        val newLevel = (newXp / 100).coerceAtLeast(1)
        if (newLevel > stats.level) {
            Log.i(TAG, "recordActivity: User $userId leveled up from ${stats.level} to $newLevel!")
        }
        
        // Streak logic
        val newStreak = calculateStreak(stats, currentTime)
        val newLongestStreak = if (newStreak > stats.longestStreak) newStreak else stats.longestStreak
        
        Log.d(
            TAG,
            "recordActivity: Stats update for $userId - XP: ${stats.xp}->$newXp, " +
                    "Level: ${stats.level}->$newLevel, Streak: ${stats.currentStreak}->$newStreak, " +
                    "TotalReviews: ${stats.totalReviews}->$newTotalReviews, DailyReviews: $baseDailyReviews->$newDailyReviews"
        )

        val updatedStats = stats.copy(
            xp = newXp,
            level = newLevel,
            currentStreak = newStreak,
            longestStreak = newLongestStreak,
            totalReviews = newTotalReviews,
            dailyReviews = newDailyReviews,
            lastActiveDate = currentTime
        )
        
        val newlyUnlockedBadgeIds = checkAndUnlockBadges(updatedStats, activityType)
        val finalBadges = stats.badges.toMutableList()
        val badgesToReturn = mutableListOf<Badge>()

        newlyUnlockedBadgeIds.forEach { badgeId ->
            if (!finalBadges.contains(badgeId)) {
                finalBadges.add(badgeId)
                Badge.ALL.find { it.id == badgeId }?.let {
                    badgesToReturn.add(it)
                    Log.i(TAG, "recordActivity: Unlocked new badge '$badgeId' for userId=$userId")
                }
            }
        }

        try {
            userStatsDao.insertOrUpdate(updatedStats.copy(badges = finalBadges))
            Log.i(TAG, "recordActivity: Successfully persisted updated stats for userId=$userId")
        } catch (e: Exception) {
            Log.e(TAG, "recordActivity: Failed to save updated stats for userId=$userId", e)
        }

        return badgesToReturn
    }

    /**
     * Calculates the updated streak count based on the user's last active timestamp and current timestamp.
     *
     * @param stats Current [UserStats] for the user.
     * @param currentTime Current timestamp in milliseconds.
     * @return The updated current streak count.
     */
    private fun calculateStreak(stats: UserStats, currentTime: Long): Int {
        if (stats.lastActiveDate == 0L) {
            Log.d(TAG, "calculateStreak: First active day, setting streak to 1")
            return 1
        }
        
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val todayStr = sdf.format(Date(currentTime))
        val lastActiveStr = sdf.format(Date(stats.lastActiveDate))
        
        val today = todayStr.toLong()
        val lastActive = lastActiveStr.toLong()
        
        val streak = when {
            today == lastActive -> stats.currentStreak
            isYesterday(stats.lastActiveDate, currentTime) -> stats.currentStreak + 1
            else -> 1
        }
        Log.d(TAG, "calculateStreak: Calculated streak=$streak (today=$todayStr, lastActive=$lastActiveStr)")
        return streak
    }

    /**
     * Helper function determining whether [lastMillis] occurred on the calendar day immediately preceding [currentMillis].
     *
     * @param lastMillis Timestamp of the previous activity in milliseconds.
     * @param currentMillis Current timestamp in milliseconds.
     * @return `true` if last activity occurred yesterday relative to current timestamp; `false` otherwise.
     */
    private fun isYesterday(lastMillis: Long, currentMillis: Long): Boolean {
        val lastCal = Calendar.getInstance().apply { 
            timeInMillis = lastMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val currentCal = Calendar.getInstance().apply { 
            timeInMillis = currentMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        currentCal.add(Calendar.DAY_OF_YEAR, -1)
        return lastCal.timeInMillis == currentCal.timeInMillis
    }

    /**
     * Evaluates all achievement badge criteria against the current user statistics and returns any badge IDs
     * that meet unlock criteria.
     *
     * @param stats Current updated [UserStats].
     * @param lastActivity The activity type that triggered this check.
     * @return List of badge ID strings that should be unlocked.
     */
    private suspend fun checkAndUnlockBadges(stats: UserStats, lastActivity: ActivityType): List<String> {
        val unlockedIds = mutableListOf<String>()
        
        // FIRST_REVIEW
        if (stats.totalReviews >= 1) {
            unlockedIds.add(Badge.FIRST_REVIEW.id)
        }
        
        // STREAK_7
        if (stats.currentStreak >= 7) {
            unlockedIds.add(Badge.STREAK_7.id)
        }
        
        // STREAK_30
        if (stats.currentStreak >= 30) {
            unlockedIds.add(Badge.STREAK_30.id)
        }
        
        // CARDS_50
        if (stats.totalReviews >= 50) {
            unlockedIds.add(Badge.CARDS_50.id)
        }
        
        // FIRST_JOURNAL
        if (lastActivity == ActivityType.JOURNAL_ENTRY) {
            try {
                val journalCount = journalDao.getEntriesByUser(stats.userId).first().size
                if (journalCount >= 1) {
                    unlockedIds.add(Badge.FIRST_JOURNAL.id)
                }
            } catch (e: Exception) {
                Log.e(TAG, "checkAndUnlockBadges: Failed to check journal entries count for ${stats.userId}", e)
            }
        }
        
        Log.d(TAG, "checkAndUnlockBadges: Evaluated unlocked badge IDs: $unlockedIds")
        return unlockedIds
    }
}

/**
 * Enumeration of supported user activity types for gamification tracking.
 */
enum class ActivityType {
    /** Flashcard study or review activity */
    FLASHCARD_REVIEW,

    /** Journal entry submission activity */
    JOURNAL_ENTRY
}
