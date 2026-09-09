package com.ntando.ivu.data.repository

import com.ntando.ivu.data.dao.JournalDao
import com.ntando.ivu.data.dao.UserStatsDao
import com.ntando.ivu.data.entity.Badge
import com.ntando.ivu.data.entity.UserStats
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AchievementRepositoryTest {

    private lateinit var repository: AchievementRepository
    private val userStatsDao: UserStatsDao = mockk()
    private val journalDao: JournalDao = mockk()
    private val userId = "test-user-id"

    @Before
    fun setup() {
        repository = AchievementRepository(userStatsDao, journalDao)
    }

    @Test
    fun `recordReview should add 10 XP and increment total reviews`() = runBlocking {
        // Given
        val initialStats = UserStats(userId = userId, xp = 0, totalReviews = 0)
        coEvery { userStatsDao.getUserStats(userId) } returns flowOf(initialStats)
        coEvery { userStatsDao.insertOrUpdate(any()) } returns Unit

        // When
        val unlocked = repository.recordReview(userId)

        // Then
        coVerify {
            userStatsDao.insertOrUpdate(match {
                it.xp == 10 && it.totalReviews == 1
            })
        }
        // Should unlock FIRST_REVIEW badge
        assertTrue(unlocked.any { it.id == Badge.FIRST_REVIEW.id })
    }

    @Test
    fun `calculateStreak should increment streak if last active was yesterday`() = runBlocking {
        // Given
        val yesterday = System.currentTimeMillis() - 24 * 60 * 60 * 1000
        val statsWithYesterday = UserStats(userId = userId, currentStreak = 5, lastActiveDate = yesterday)
        coEvery { userStatsDao.getUserStats(userId) } returns flowOf(statsWithYesterday)
        coEvery { userStatsDao.insertOrUpdate(any()) } returns Unit

        // When
        repository.recordReview(userId)

        // Then
        coVerify {
            userStatsDao.insertOrUpdate(match {
                it.currentStreak == 6
            })
        }
    }

    @Test
    fun `calculateStreak should reset streak if last active was before yesterday`() = runBlocking {
        // Given
        val twoDaysAgo = System.currentTimeMillis() - 48 * 60 * 60 * 1000
        val statsWithOldDate = UserStats(userId = userId, currentStreak = 5, lastActiveDate = twoDaysAgo)
        coEvery { userStatsDao.getUserStats(userId) } returns flowOf(statsWithOldDate)
        coEvery { userStatsDao.insertOrUpdate(any()) } returns Unit

        // When
        repository.recordReview(userId)

        // Then
        coVerify {
            userStatsDao.insertOrUpdate(match {
                it.currentStreak == 1
            })
        }
    }
}
