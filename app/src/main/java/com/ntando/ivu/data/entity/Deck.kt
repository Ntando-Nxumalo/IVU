package com.ntando.ivu.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Language { EN, ZU, AF }

@Entity(tableName = "decks")
data class Deck(
    @PrimaryKey(autoGenerate = true) val deckId: Long = 0,
    val remoteId: String? = null,
    val ownerId: Long,
    val title: String,
    val language: Language,
    val cardCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
