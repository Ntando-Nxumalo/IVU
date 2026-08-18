package com.ntando.ivu.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "flashcards",
    foreignKeys = [
        ForeignKey(
            entity = Deck::class,
            parentColumns = ["deckId"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["deckId"])]
)
data class Flashcard(
    @PrimaryKey(autoGenerate = true) val cardId: Long = 0,
    val remoteId: String? = null,
    val deckId: Long,
    val frontText: String,
    val backText: String,
    val imageUrl: String? = null,
    val easeFactor: Float = 2.5f,
    val intervalDays: Int = 0,
    val repetitions: Int = 0,
    val dueDate: Long = System.currentTimeMillis()
)
