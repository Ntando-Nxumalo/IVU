package com.ntando.ivu.data.dao

import androidx.room.*
import com.ntando.ivu.data.entity.Achievement
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for managing [Achievement] records in the Room database.
 *
 * Provides operations to stream user achievements, insert records, and update unlock status or notification states.
 */
@Dao
interface AchievementDao {
    /**
     * Streams all achievements recorded for a specified user ID.
     *
     * @param userId Unique identifier string of the user.
     * @return A [Flow] emitting lists of matching [Achievement] entities.
     */
    @Query("SELECT * FROM achievements WHERE userId = :userId")
    fun getAllAchievements(userId: String): Flow<List<Achievement>>

    /**
     * Inserts or updates an achievement entity in the database.
     *
     * @param achievement The [Achievement] instance to save.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: Achievement)

    /**
     * Marks an achievement as unlocked for a given user and sets the unlock date timestamp.
     *
     * @param title Title of achievement to unlock.
     * @param userId Unique identifier string of the user.
     * @param date Epoch timestamp in milliseconds when achievement was unlocked.
     */
    @Query("UPDATE achievements SET isUnlocked = 1, dateUnlocked = :date WHERE title = :title AND userId = :userId")
    suspend fun unlockAchievement(title: String, userId: String, date: Long)

    /**
     * Updates notification state to mark that unlock alert was displayed to user.
     *
     * @param id Primary key ID of the achievement.
     */
    @Query("UPDATE achievements SET isNotified = 1 WHERE id = :id")
    suspend fun markAsNotified(id: Long)
}
