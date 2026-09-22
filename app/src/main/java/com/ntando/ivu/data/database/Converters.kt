package com.ntando.ivu.data.database

import android.util.Log
import androidx.room.TypeConverter
import com.ntando.ivu.data.entity.Language
import com.ntando.ivu.data.entity.Mood

private const val TAG = "RoomConverters"

/**
 * Type converters used by Room database to convert custom complex types
 * (such as Enums and String Lists) into primitive types suitable for SQLite storage.
 */
class Converters {

    /**
     * Converts a [Language] enum value into string representation for database persistence.
     *
     * @param value The [Language] enum instance.
     * @return The string name of the enum constant.
     */
    @TypeConverter
    fun fromLanguage(value: Language): String {
        Log.v(TAG, "fromLanguage: $value -> ${value.name}")
        return value.name
    }
    
    /**
     * Restores a [Language] enum instance from stored database string name.
     *
     * @param value The stored string name.
     * @return Reconstructed [Language] enum constant.
     */
    @TypeConverter
    fun toLanguage(value: String): Language {
        Log.v(TAG, "toLanguage: $value")
        return enumValueOf<Language>(value)
    }

    /**
     * Converts a [Mood] enum value into string representation for database persistence.
     *
     * @param value The [Mood] enum instance.
     * @return The string name of the enum constant.
     */
    @TypeConverter
    fun fromMood(value: Mood): String {
        Log.v(TAG, "fromMood: $value -> ${value.name}")
        return value.name
    }
    
    /**
     * Restores a [Mood] enum instance from stored database string name.
     *
     * @param value The stored string name.
     * @return Reconstructed [Mood] enum constant.
     */
    @TypeConverter
    fun toMood(value: String): Mood {
        Log.v(TAG, "toMood: $value")
        return enumValueOf<Mood>(value)
    }

    /**
     * Converts a list of strings into a single comma-separated string for SQLite column storage.
     *
     * @param value List of string values (e.g., unlocked badge IDs).
     * @return Comma-separated concatenated string representation.
     */
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        val converted = value.joinToString(",")
        Log.v(TAG, "fromStringList: list size=${value.size} -> '$converted'")
        return converted
    }

    /**
     * Restores a list of strings from a comma-separated database string.
     *
     * @param value Comma-separated string stored in database column.
     * @return List of reconstructed string elements.
     */
    @TypeConverter
    fun toStringList(value: String): List<String> {
        val list = if (value.isEmpty()) emptyList() else value.split(",")
        Log.v(TAG, "toStringList: '$value' -> list size=${list.size}")
        return list
    }
}
