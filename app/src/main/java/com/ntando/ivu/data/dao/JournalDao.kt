package com.ntando.ivu.data.dao

import androidx.room.*
import com.ntando.ivu.data.entity.JournalEntry
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for managing [JournalEntry] persistence in the Room database.
 */
@Dao
interface JournalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: JournalEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<JournalEntry>): List<Long>

    @Update
    suspend fun updateEntry(entry: JournalEntry)

    @Delete
    suspend fun deleteEntry(entry: JournalEntry)

    @Query("DELETE FROM journal_entries WHERE entryId = :entryId")
    suspend fun deleteEntryById(entryId: Long)

    @Query("SELECT * FROM journal_entries WHERE ownerUserId = :ownerUserId ORDER BY date DESC")
    fun getEntriesByOwnerUserId(ownerUserId: String): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries WHERE ownerUserId = :ownerUserId ORDER BY date DESC")
    suspend fun getEntriesListByOwnerUserId(ownerUserId: String): List<JournalEntry>

    @Query("SELECT * FROM journal_entries WHERE ownerUserId = :ownerUserId AND (syncState = 'PENDING_CREATE' OR syncState = 'PENDING_UPDATE') ORDER BY date ASC")
    suspend fun getPendingJournalEntries(ownerUserId: String): List<JournalEntry>

    @Query("SELECT COUNT(*) FROM journal_entries WHERE ownerUserId = :ownerUserId AND (syncState = 'PENDING_CREATE' OR syncState = 'PENDING_UPDATE')")
    fun getPendingJournalEntriesCountFlow(ownerUserId: String): Flow<Int>

    @Query("SELECT * FROM journal_entries WHERE userId = :userId ORDER BY date DESC")
    fun getEntriesByUser(userId: String): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries WHERE entryId = :entryId")
    suspend fun getEntryById(entryId: Long): JournalEntry?

    @Query("SELECT * FROM journal_entries WHERE remoteId = :remoteId AND ownerUserId = :ownerUserId LIMIT 1")
    suspend fun getEntryByRemoteId(remoteId: String, ownerUserId: String): JournalEntry?

    @Query("DELETE FROM journal_entries WHERE ownerUserId = :ownerUserId AND syncState = 'SYNCED' AND remoteId NOT IN (:serverRemoteIds)")
    suspend fun deleteSyncedJournalEntriesNotIn(serverRemoteIds: List<String>, ownerUserId: String)
}
