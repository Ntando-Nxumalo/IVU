package com.ntando.ivu.data.dao

import androidx.room.*
import com.ntando.ivu.data.entity.Deck
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: Deck): Long

    @Update
    suspend fun updateDeck(deck: Deck)

    @Delete
    suspend fun deleteDeck(deck: Deck)

    @Query("SELECT * FROM decks WHERE ownerId = :userId ORDER BY createdAt DESC")
    fun getDecksByUser(userId: Long): Flow<List<Deck>>

    @Query("SELECT * FROM decks WHERE deckId = :deckId")
    suspend fun getDeckById(deckId: Long): Deck?

    @Query("UPDATE decks SET cardCount = (SELECT COUNT(*) FROM flashcards WHERE deckId = :deckId) WHERE deckId = :deckId")
    suspend fun updateCardCount(deckId: Long)
}
