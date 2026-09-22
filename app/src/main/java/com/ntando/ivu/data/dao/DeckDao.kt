package com.ntando.ivu.data.dao

import androidx.room.*
import com.ntando.ivu.data.entity.Deck
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for executing database operations on [Deck] entities.
 *
 * Supports CRUD actions on study decks as well as query execution for updating cached flashcard counts.
 */
@Dao
interface DeckDao {
    /**
     * Inserts a new deck or updates an existing deck in the database.
     *
     * @param deck The [Deck] entity to save.
     * @return Auto-generated primary key row ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: Deck): Long

    /**
     * Updates an existing deck record.
     *
     * @param deck The [Deck] entity with updated properties.
     */
    @Update
    suspend fun updateDeck(deck: Deck)

    /**
     * Removes a deck from the database.
     *
     * Note: Associated flashcards will be cascade-deleted by Room foreign key constraint.
     *
     * @param deck The [Deck] entity to delete.
     */
    @Delete
    suspend fun deleteDeck(deck: Deck)

    /**
     * Streams decks belonging to a user ID, ordered newest first by creation timestamp.
     *
     * @param userId Primary key ID of deck owner.
     * @return A [Flow] emitting lists of [Deck] entities.
     */
    @Query("SELECT * FROM decks WHERE ownerId = :userId ORDER BY createdAt DESC")
    fun getDecksByUser(userId: Long): Flow<List<Deck>>

    /**
     * Looks up a single deck by its primary key ID.
     *
     * @param deckId Unique ID of the deck.
     * @return The [Deck] instance, or `null` if not found.
     */
    @Query("SELECT * FROM decks WHERE deckId = :deckId")
    suspend fun getDeckById(deckId: Long): Deck?

    /**
     * Recalculates and updates [Deck.cardCount] based on actual count of flashcards matching `deckId`.
     *
     * @param deckId Primary key ID of target deck.
     */
    @Query("UPDATE decks SET cardCount = (SELECT COUNT(*) FROM flashcards WHERE deckId = :deckId) WHERE deckId = :deckId")
    suspend fun updateCardCount(deckId: Long)
}
