package com.ntando.ivu.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ntando.ivu.data.dao.*
import com.ntando.ivu.data.entity.*

@Database(
    entities = [
        User::class,
        Achievement::class,
        Deck::class,
        Flashcard::class,
        JournalEntry::class
    ],
    version = 8,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun achievementDao(): AchievementDao
    abstract fun deckDao(): DeckDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun journalDao(): JournalDao
}
