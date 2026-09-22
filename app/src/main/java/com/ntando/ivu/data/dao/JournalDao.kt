package com.ntando.ivu.data.dao

import androidx.room.*
import com.ntando.ivu.data.entity.JournalEntry
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for managing [JournalEntry] persistence in the Room database.
 *
 * Supports creating, updating, removing, and querying user reflection entries.
 */
@Dao
interface JournalDao {
    /**
     * Inserts or replaces a journal reflection entry.
     *
     * @param entry The [JournalEntry] object to save.
     * @return Auto-generated primary key entry ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: JournalEntry): Long

    /**
     * Updates an existing journal reflection entry.
     *
     * @param entry The updated [JournalEntry] entity.
     */
    @Update
    suspend fun updateEntry(entry: JournalEntry)

    /**
     * Deletes a journal entry from the database.
     *
     * @param entry The [JournalEntry] entity to remove.
     */
    @Delete
    suspend fun deleteEntry(entry: JournalEntry)

    /**
     * Streams journal entries belonging to a specific user ID, ordered newest first.
     *
     * @param userId Identifier string of owner user.
     * @return A [Flow] emitting lists of [JournalEntry] entities.
     */
    @Query("SELECT * FROM journal_entries WHERE userId = :userId ORDER BY date DESC")
    fun getEntriesByUser(userId: String): Flow<List<JournalEntry>>

    /**
     * Fetches a single journal reflection entry by its primary key ID.
     *
     * @param entryId Primary key entry ID.
     * @return The [JournalEntry] entity, or `null` if not found.
     */
    @Query("SELECT * FROM journal_entries WHERE entryId = :entryId")
    suspend fun getEntryById(entryId: Long): JournalEntry?
}
