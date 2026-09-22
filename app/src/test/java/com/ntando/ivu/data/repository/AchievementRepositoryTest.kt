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

/**
 * Unit tests for [AchievementRepository].
 *
 * Scenarios Tested:
 * - Adding XP and incrementing total review count upon recording a review, including unlocking badges like [Badge.FIRST_REVIEW].
 * - Incrementing the daily streak when the last active timestamp was yesterday (24 hours prior).
 * - Resetting the daily streak to 1 when the last active timestamp was before yesterday (e.g. 48 hours prior).
 *
 * Mocking & Test Execution Rules:
 * - Uses [mockk] to mock data access objects ([UserStatsDao] and [JournalDao]).
 * - Uses [runBlocking] coroutine builder to execute suspend repository functions synchronously within test scope.
 * - Uses [coEvery] to configure mock behavior for DAO queries/inserts and [coVerify] to verify invocation arguments.
 */
class AchievementRepositoryTest {

    private lateinit var repository: AchievementRepository
    private val userStatsDao: UserStatsDao = mockk()
    private val journalDao: JournalDao = mockk()
    private val userId = "test-user-id"

    /**
     * Set up test dependencies and initialize [AchievementRepository] before each test execution.
     */
    @Before
    fun setup() {
        println("Setting up AchievementRepositoryTest dependencies...")
        repository = AchievementRepository(userStatsDao, journalDao)
    }

    /**
     * Test Scenario: Record a user review for a user with initial zero stats.
     *
     * Setup / Mock Behavior:
     * - `userStatsDao.getUserStats` returns a flow emitting initial stats with 0 XP and 0 total reviews.
     * - `userStatsDao.insertOrUpdate` is stubbed to return [Unit].
     *
     * Expected Outcomes:
     * - `userStatsDao.insertOrUpdate` is verified to be called with updated stats reflecting 10 XP and 1 total review.
     * - The returned list of unlocked badges contains [Badge.FIRST_REVIEW].
     *
     * Coroutine Execution Rule:
     * - Executed within [runBlocking] to handle suspend repository calls.
     */
    @Test
    fun `recordReview should add 10 XP and increment total reviews`() = runBlocking {
        println("--- Starting Test: recordReview should add 10 XP and increment total reviews ---")
        // Given
        val initialStats = UserStats(userId = userId, xp = 0, totalReviews = 0)
        println("[Given] Initial stats: $initialStats")
        coEvery { userStatsDao.getUserStats(userId) } returns flowOf(initialStats)
        coEvery { userStatsDao.insertOrUpdate(any()) } returns Unit

        // When
        println("[When] Calling repository.recordReview($userId)...")
        val unlocked = repository.recordReview(userId)
        println("[When] Unlocked badges received: $unlocked")

        // Then
        println("[Then] Verifying userStatsDao.insertOrUpdate was called with updated XP (10) and totalReviews (1)...")
        coVerify {
            userStatsDao.insertOrUpdate(match {
                it.xp == 10 && it.totalReviews == 1
            })
        }
        println("[Then] Validating FIRST_REVIEW badge was unlocked...")
        val hasFirstReviewBadge = unlocked.any { it.id == Badge.FIRST_REVIEW.id }
        println("[Assertion] FIRST_REVIEW badge unlocked status: $hasFirstReviewBadge")
        // Should unlock FIRST_REVIEW badge
        assertTrue(hasFirstReviewBadge)
        println("--- Completed Test: recordReview should add 10 XP and increment total reviews ---")
    }

    /**
     * Test Scenario: Record a review when the user's last active date was yesterday (24 hours prior).
     *
     * Setup / Mock Behavior:
     * - `userStatsDao.getUserStats` returns a flow emitting stats with currentStreak = 5 and lastActiveDate = yesterday.
     * - `userStatsDao.insertOrUpdate` is stubbed to return [Unit].
     *
     * Expected Outcomes:
     * - `userStatsDao.insertOrUpdate` is verified to receive updated stats with currentStreak incremented to 6.
     *
     * Coroutine Execution Rule:
     * - Executed within [runBlocking] to handle suspend repository calls.
     */
    @Test
    fun `calculateStreak should increment streak if last active was yesterday`() = runBlocking {
        println("--- Starting Test: calculateStreak should increment streak if last active was yesterday ---")
        // Given
        val yesterday = System.currentTimeMillis() - 24 * 60 * 60 * 1000
        val statsWithYesterday = UserStats(userId = userId, currentStreak = 5, lastActiveDate = yesterday)
        println("[Given] Stats with yesterday's active timestamp: $statsWithYesterday")
        coEvery { userStatsDao.getUserStats(userId) } returns flowOf(statsWithYesterday)
        coEvery { userStatsDao.insertOrUpdate(any()) } returns Unit

        // When
        println("[When] Calling repository.recordReview($userId)...")
        repository.recordReview(userId)

        // Then
        println("[Then] Verifying userStatsDao.insertOrUpdate was called with incremented currentStreak (6)...")
        coVerify {
            userStatsDao.insertOrUpdate(match {
                it.currentStreak == 6
            })
        }
        println("--- Completed Test: calculateStreak should increment streak if last active was yesterday ---")
    }

    /**
     * Test Scenario: Record a review when the user's last active date was prior to yesterday (48 hours ago).
     *
     * Setup / Mock Behavior:
     * - `userStatsDao.getUserStats` returns a flow emitting stats with currentStreak = 5 and lastActiveDate = 2 days ago.
     * - `userStatsDao.insertOrUpdate` is stubbed to return [Unit].
     *
     * Expected Outcomes:
     * - `userStatsDao.insertOrUpdate` is verified to receive updated stats with currentStreak reset to 1.
     *
     * Coroutine Execution Rule:
     * - Executed within [runBlocking] to handle suspend repository calls.
     */
    @Test
    fun `calculateStreak should reset streak if last active was before yesterday`() = runBlocking {
        println("--- Starting Test: calculateStreak should reset streak if last active was before yesterday ---")
        // Given
        val twoDaysAgo = System.currentTimeMillis() - 48 * 60 * 60 * 1000
        val statsWithOldDate = UserStats(userId = userId, currentStreak = 5, lastActiveDate = twoDaysAgo)
        println("[Given] Stats with old active timestamp (2 days ago): $statsWithOldDate")
        coEvery { userStatsDao.getUserStats(userId) } returns flowOf(statsWithOldDate)
        coEvery { userStatsDao.insertOrUpdate(any()) } returns Unit

        // When
        println("[When] Calling repository.recordReview($userId)...")
        repository.recordReview(userId)

        // Then
        println("[Then] Verifying userStatsDao.insertOrUpdate was called with reset currentStreak (1)...")
        coVerify {
            userStatsDao.insertOrUpdate(match {
                it.currentStreak == 1
            })
        }
        println("--- Completed Test: calculateStreak should reset streak if last active was before yesterday ---")
    }
}
