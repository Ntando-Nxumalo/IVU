package com.ntando.ivu.data.dao

import androidx.room.*
import com.ntando.ivu.data.entity.Deck
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for executing database operations on [Deck] entities.
 */
@Dao
interface DeckDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: Deck): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDecks(decks: List<Deck>): List<Long>

    @Update
    suspend fun updateDeck(deck: Deck)

    @Delete
    suspend fun deleteDeck(deck: Deck)

    @Query("DELETE FROM decks WHERE deckId = :deckId")
    suspend fun deleteDeckById(deckId: Long)

    @Query("SELECT * FROM decks WHERE ownerUserId = :ownerUserId ORDER BY createdAt DESC")
    fun getDecksByOwnerUserId(ownerUserId: String): Flow<List<Deck>>

    @Query("SELECT * FROM decks WHERE ownerUserId = :ownerUserId ORDER BY createdAt DESC")
    suspend fun getDecksListByOwnerUserId(ownerUserId: String): List<Deck>

    @Query("SELECT * FROM decks WHERE ownerId = :userId ORDER BY createdAt DESC")
    fun getDecksByUser(userId: Long): Flow<List<Deck>>

    @Query("SELECT * FROM decks WHERE deckId = :deckId")
    suspend fun getDeckById(deckId: Long): Deck?

    @Query("SELECT * FROM decks WHERE remoteId = :remoteId AND ownerUserId = :ownerUserId LIMIT 1")
    suspend fun getDeckByRemoteId(remoteId: String, ownerUserId: String): Deck?

    @Query("SELECT COUNT(*) FROM decks WHERE ownerUserId = :ownerUserId AND (syncState = 'PENDING_CREATE' OR syncState = 'PENDING_UPDATE')")
    fun getPendingDeckCountFlow(ownerUserId: String): Flow<Int>

    @Query("SELECT * FROM decks WHERE ownerUserId = :ownerUserId AND syncState = 'PENDING_CREATE'")
    suspend fun getPendingDecks(ownerUserId: String): List<Deck>

    @Query("DELETE FROM decks WHERE ownerUserId = :ownerUserId AND syncState = 'SYNCED' AND remoteId NOT IN (:serverRemoteIds)")
    suspend fun deleteSyncedDecksNotIn(serverRemoteIds: List<String>, ownerUserId: String)

    @Query("UPDATE decks SET cardCount = (SELECT COUNT(*) FROM flashcards WHERE deckId = :deckId) WHERE deckId = :deckId")
    suspend fun updateCardCount(deckId: Long)
}
