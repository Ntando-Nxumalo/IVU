package com.ntando.ivu.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Mood { GREAT, OKAY, TOUGH }

@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey(autoGenerate = true) val entryId: Long = 0,
    val userId: Long,
    val date: Long = System.currentTimeMillis(),
    val mood: Mood,
    val text: String,
    val linkedDeckId: Long? = null
)
