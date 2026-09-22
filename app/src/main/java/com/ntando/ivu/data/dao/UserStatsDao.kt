package com.ntando.ivu.data.dao

import androidx.room.*
import com.ntando.ivu.data.entity.UserStats
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for managing [UserStats] progress and gamification records.
 */
@Dao
interface UserStatsDao {
    /**
     * Streams statistics record for specified user ID.
     *
     * @param userId Unique identifier string of user.
     * @return A [Flow] emitting the [UserStats] entity, or `null` if not initialized.
     */
    @Query("SELECT * FROM user_stats WHERE userId = :userId")
    fun getUserStats(userId: String): Flow<UserStats?>

    /**
     * Inserts or replaces user statistics record.
     *
     * @param stats The [UserStats] entity to save.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stats: UserStats)

    /**
     * Increments user's current experience points (XP) by specified amount.
     *
     * @param userId Unique identifier string of user.
     * @param amount Numeric XP points to add.
     */
    @Query("UPDATE user_stats SET xp = xp + :amount WHERE userId = :userId")
    suspend fun addXp(userId: String, amount: Int)
}
