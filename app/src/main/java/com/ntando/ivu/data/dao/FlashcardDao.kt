package com.ntando.ivu.data.dao

import androidx.room.*
import com.ntando.ivu.data.entity.Flashcard
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for managing [Flashcard] database persistence.
 *
 * Configured with queries for fetching cards by deck, filtering due cards, and updating SM-2 algorithm variables.
 */
@Dao
interface FlashcardDao {
    /**
     * Inserts or replaces a flashcard entry in the database.
     *
     * @param flashcard The [Flashcard] instance to insert.
     * @return Generated primary key card ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(flashcard: Flashcard): Long

    /**
     * Updates an existing flashcard (e.g. updating SM-2 parameters after study review).
     *
     * @param flashcard The updated [Flashcard] entity.
     */
    @Update
    suspend fun updateFlashcard(flashcard: Flashcard)

    /**
     * Deletes a flashcard from the database.
     *
     * @param flashcard The [Flashcard] entity to remove.
     */
    @Delete
    suspend fun deleteFlashcard(flashcard: Flashcard)

    /**
     * Streams flashcards belonging to a specific deck, sorted by due date ascending.
     *
     * @param deckId Primary key ID of parent deck.
     * @return A [Flow] emitting lists of [Flashcard] entities.
     */
    @Query("SELECT * FROM flashcards WHERE deckId = :deckId ORDER BY dueDate ASC")
    fun getFlashcardsByDeck(deckId: Long): Flow<List<Flashcard>>

    /**
     * Fetches a flashcard by its unique card ID.
     *
     * @param cardId Primary key card ID.
     * @return The [Flashcard] entity, or `null` if not found.
     */
    @Query("SELECT * FROM flashcards WHERE cardId = :cardId")
    suspend fun getFlashcardById(cardId: Long): Flashcard?

    /**
     * Streams flashcards within a deck that are due for review on or before specified timestamp.
     *
     * @param deckId Primary key ID of deck.
     * @param currentTime Current epoch timestamp in milliseconds.
     * @return A [Flow] emitting lists of due [Flashcard] entities.
     */
    @Query("SELECT * FROM flashcards WHERE deckId = :deckId AND dueDate <= :currentTime")
    fun getDueFlashcards(deckId: Long, currentTime: Long): Flow<List<Flashcard>>
}
