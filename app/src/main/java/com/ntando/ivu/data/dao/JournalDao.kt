package com.ntando.ivu.data.dao

import androidx.room.*
import com.ntando.ivu.data.entity.JournalEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: JournalEntry): Long

    @Update
    suspend fun updateEntry(entry: JournalEntry)

    @Delete
    suspend fun deleteEntry(entry: JournalEntry)

    @Query("SELECT * FROM journal_entries WHERE userId = :userId ORDER BY date DESC")
    fun getEntriesByUser(userId: Long): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries WHERE entryId = :entryId")
    suspend fun getEntryById(entryId: Long): JournalEntry?
}
