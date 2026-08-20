package com.ntando.ivu.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_stats")
data class UserStats(
    @PrimaryKey val userId: Long,
    val xp: Int = 0,
    val level: Int = 1,
    val currentStreak: Int = 0,
    val totalReviews: Int = 0,
    val lastReviewDate: Long = 0
)
