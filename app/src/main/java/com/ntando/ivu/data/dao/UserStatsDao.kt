package com.ntando.ivu.data.dao

import androidx.room.*
import com.ntando.ivu.data.entity.UserStats
import kotlinx.coroutines.flow.Flow

@Dao
interface UserStatsDao {
    @Query("SELECT * FROM user_stats WHERE userId = :userId")
    fun getUserStats(userId: Long): Flow<UserStats?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stats: UserStats)

    @Query("UPDATE user_stats SET xp = xp + :amount WHERE userId = :userId")
    suspend fun addXp(userId: Long, amount: Int)
}
