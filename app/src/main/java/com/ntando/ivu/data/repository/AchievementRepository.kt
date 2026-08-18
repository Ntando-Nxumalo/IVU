package com.ntando.ivu.data.repository

import com.ntando.ivu.data.dao.AchievementDao
import com.ntando.ivu.data.dao.DeckDao
import com.ntando.ivu.data.dao.FlashcardDao
import com.ntando.ivu.data.dao.JournalDao
import com.ntando.ivu.data.entity.Achievement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repository to manage study-related achievements in IVU.
 */
class AchievementRepository(
    private val achievementDao: AchievementDao,
    private val deckDao: DeckDao,
    private val flashcardDao: FlashcardDao,
    private val journalDao: JournalDao
) {
    fun getAllAchievements(userId: Long): Flow<List<Achievement>> = 
        achievementDao.getAllAchievements(userId)

    suspend fun checkAndUnlockAchievements(userId: Long): List<String> {
        val newlyUnlocked = mutableListOf<String>()
        val currentTime = System.currentTimeMillis()

        // 1. "7-day streak" logic using Journal dates
        val entries = journalDao.getEntriesByUser(userId).first()
        if (hasStreak(entries.map { it.date }, 7)) {
            if (achievementDao.unlockAchievement("7-day streak", userId, currentTime) > 0) {
                newlyUnlocked.add("7-day streak")
            }
        }

        // 2. "100 cards mastered" (intervalDays > 21)
        val allDecks = deckDao.getDecksByUser(userId).first()
        var totalMastered = 0
        allDecks.forEach { deck ->
            val cards = flashcardDao.getFlashcardsByDeck(deck.deckId).first()
            totalMastered += cards.count { it.intervalDays > 21 }
        }
        
        if (totalMastered >= 100) {
            if (achievementDao.unlockAchievement("100 cards mastered", userId, currentTime) > 0) {
                newlyUnlocked.add("100 cards mastered")
            }
        }

        return newlyUnlocked
    }

    private fun hasStreak(dates: List<Long>, requiredDays: Int): Boolean {
        if (dates.size < requiredDays) return false
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val sortedDates = dates.map { sdf.format(Date(it)).toLong() }.distinct().sortedDescending()
        
        if (sortedDates.size < requiredDays) return false
        
        var currentStreak = 1
        for (i in 0 until sortedDates.size - 1) {
            // Check if dates are consecutive days
            if (isYesterday(sortedDates[i], sortedDates[i+1])) {
                currentStreak++
                if (currentStreak >= requiredDays) return true
            } else {
                currentStreak = 1
            }
        }
        return false
    }

    private fun isYesterday(today: Long, yesterday: Long): Boolean {
        // Simplified consecutive day check for yyyyMMdd format
        return today - yesterday == 1L || (today % 100 == 1L && yesterday % 100 >= 28L)
    }

    suspend fun updateAchievement(achievement: Achievement) {
        achievementDao.updateAchievement(achievement)
    }

    suspend fun markAsNotified(id: Long) {
        achievementDao.markAsNotified(id)
    }
}
