package com.ntando.ivu.data.dao

import androidx.room.*
import com.ntando.ivu.data.entity.Achievement
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements WHERE userId = :userId")
    fun getAllAchievements(userId: String): Flow<List<Achievement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: Achievement)

    @Query("UPDATE achievements SET isUnlocked = 1, dateUnlocked = :date WHERE title = :title AND userId = :userId")
    suspend fun unlockAchievement(title: String, userId: String, date: Long)

    @Query("UPDATE achievements SET isNotified = 1 WHERE id = :id")
    suspend fun markAsNotified(id: Long)
}
