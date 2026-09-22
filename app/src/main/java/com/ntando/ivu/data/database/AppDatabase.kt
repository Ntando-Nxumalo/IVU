package com.ntando.ivu.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ntando.ivu.data.dao.*
import com.ntando.ivu.data.entity.*

/**
 * Primary Room database class for IVU application persistent storage.
 *
 * Defines database entities, schema versioning, and abstract DAO access methods.
 * Custom object conversions (enums, lists) are handled by [Converters].
 */
@Database(
    entities = [
        User::class,
        Achievement::class,
        Deck::class,
        Flashcard::class,
        JournalEntry::class,
        UserStats::class
    ],
    version = 12,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    /** Abstract accessor for [UserDao] operations. */
    abstract fun userDao(): UserDao

    /** Abstract accessor for [AchievementDao] operations. */
    abstract fun achievementDao(): AchievementDao

    /** Abstract accessor for [DeckDao] operations. */
    abstract fun deckDao(): DeckDao

    /** Abstract accessor for [FlashcardDao] operations. */
    abstract fun flashcardDao(): FlashcardDao

    /** Abstract accessor for [JournalDao] operations. */
    abstract fun journalDao(): JournalDao

    /** Abstract accessor for [UserStatsDao] operations. */
    abstract fun userStatsDao(): UserStatsDao
}
