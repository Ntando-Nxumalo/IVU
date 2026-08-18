package com.ntando.ivu.data.dao

import androidx.room.*
import com.ntando.ivu.data.entity.Flashcard
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(flashcard: Flashcard): Long

    @Update
    suspend fun updateFlashcard(flashcard: Flashcard)

    @Delete
    suspend fun deleteFlashcard(flashcard: Flashcard)

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId ORDER BY dueDate ASC")
    fun getFlashcardsByDeck(deckId: Long): Flow<List<Flashcard>>

    @Query("SELECT * FROM flashcards WHERE cardId = :cardId")
    suspend fun getFlashcardById(cardId: Long): Flashcard?

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId AND dueDate <= :currentTime")
    fun getDueFlashcards(deckId: Long, currentTime: Long): Flow<List<Flashcard>>
}
