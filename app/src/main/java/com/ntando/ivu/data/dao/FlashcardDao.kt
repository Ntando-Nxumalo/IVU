package com.ntando.ivu.data.dao

import androidx.room.*
import com.ntando.ivu.data.entity.Flashcard
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for managing [Flashcard] database persistence.
 */
@Dao
interface FlashcardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(flashcard: Flashcard): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcards(flashcards: List<Flashcard>): List<Long>

    @Update
    suspend fun updateFlashcard(flashcard: Flashcard)

    @Delete
    suspend fun deleteFlashcard(flashcard: Flashcard)

    @Query("DELETE FROM flashcards WHERE cardId = :cardId")
    suspend fun deleteFlashcardById(cardId: Long)

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId AND ownerUserId = :ownerUserId ORDER BY dueDate ASC")
    fun getFlashcardsByDeckAndOwner(deckId: Long, ownerUserId: String): Flow<List<Flashcard>>

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId AND ownerUserId = :ownerUserId ORDER BY dueDate ASC")
    suspend fun getFlashcardListByDeckAndOwner(deckId: Long, ownerUserId: String): List<Flashcard>

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId ORDER BY dueDate ASC")
    fun getFlashcardsByDeck(deckId: Long): Flow<List<Flashcard>>

    @Query("SELECT * FROM flashcards WHERE cardId = :cardId")
    suspend fun getFlashcardById(cardId: Long): Flashcard?

    @Query("SELECT * FROM flashcards WHERE remoteId = :remoteId AND ownerUserId = :ownerUserId LIMIT 1")
    suspend fun getFlashcardByRemoteId(remoteId: String, ownerUserId: String): Flashcard?

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId AND dueDate <= :currentTime AND ownerUserId = :ownerUserId ORDER BY dueDate ASC")
    fun getDueFlashcardsByOwner(deckId: Long, currentTime: Long, ownerUserId: String): Flow<List<Flashcard>>

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId AND dueDate <= :currentTime")
    fun getDueFlashcards(deckId: Long, currentTime: Long): Flow<List<Flashcard>>

    @Query("SELECT COUNT(*) FROM flashcards WHERE ownerUserId = :ownerUserId AND (syncState = 'PENDING_CREATE' OR syncState = 'PENDING_UPDATE')")
    fun getPendingFlashcardCountFlow(ownerUserId: String): Flow<Int>

    @Query("SELECT * FROM flashcards WHERE ownerUserId = :ownerUserId AND syncState = 'PENDING_CREATE'")
    suspend fun getPendingFlashcards(ownerUserId: String): List<Flashcard>

    @Query("DELETE FROM flashcards WHERE deckId = :deckId AND ownerUserId = :ownerUserId AND syncState = 'SYNCED' AND remoteId NOT IN (:serverRemoteIds)")
    suspend fun deleteSyncedFlashcardsNotIn(deckId: Long, serverRemoteIds: List<String>, ownerUserId: String)
}
