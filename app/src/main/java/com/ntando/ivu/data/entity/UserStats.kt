package com.ntando.ivu.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_stats")
data class UserStats(
    @PrimaryKey val userId: String,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val xp: Int = 0,
    val level: Int = 1,
    val totalReviews: Int = 0,
    val dailyReviews: Int = 0,
    val lastActiveDate: Long = 0,
    val badges: List<String> = emptyList()
)
