package com.ntando.ivu.data.repository

import com.ntando.ivu.data.dao.*
import com.ntando.ivu.data.entity.Badge
import com.ntando.ivu.data.entity.UserStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

class AchievementRepository(
    private val userStatsDao: UserStatsDao,
    private val journalDao: JournalDao
) {
    fun getUserStats(userId: String): Flow<UserStats?> = 
        userStatsDao.getUserStats(userId)

    suspend fun recordReview(userId: String): List<Badge> {
        return recordActivity(userId, ActivityType.FLASHCARD_REVIEW)
    }

    suspend fun recordJournalEntry(userId: String): List<Badge> {
        return recordActivity(userId, ActivityType.JOURNAL_ENTRY)
    }

    private suspend fun recordActivity(userId: String, activityType: ActivityType): List<Badge> {
        val currentTime = System.currentTimeMillis()
        val stats = userStatsDao.getUserStats(userId).first() ?: UserStats(userId)
        
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
        
        // Streak logic
        val newStreak = calculateStreak(stats, currentTime)
        val newLongestStreak = if (newStreak > stats.longestStreak) newStreak else stats.longestStreak
        
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
                Badge.ALL.find { it.id == badgeId }?.let { badgesToReturn.add(it) }
            }
        }

        userStatsDao.insertOrUpdate(updatedStats.copy(badges = finalBadges))
        return badgesToReturn
    }

    private fun calculateStreak(stats: UserStats, currentTime: Long): Int {
        if (stats.lastActiveDate == 0L) return 1
        
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val todayStr = sdf.format(Date(currentTime))
        val lastActiveStr = sdf.format(Date(stats.lastActiveDate))
        
        val today = todayStr.toLong()
        val lastActive = lastActiveStr.toLong()
        
        return when {
            today == lastActive -> stats.currentStreak
            isYesterday(stats.lastActiveDate, currentTime) -> stats.currentStreak + 1
            else -> 1
        }
    }

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
            val journalCount = journalDao.getEntriesByUser(stats.userId).first().size
            if (journalCount >= 1) {
                unlockedIds.add(Badge.FIRST_JOURNAL.id)
            }
        }
        
        return unlockedIds
    }
}

enum class ActivityType {
    FLASHCARD_REVIEW, JOURNAL_ENTRY
}
