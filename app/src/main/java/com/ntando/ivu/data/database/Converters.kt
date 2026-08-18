package com.ntando.ivu.data.database

import androidx.room.TypeConverter
import com.ntando.ivu.data.entity.Language
import com.ntando.ivu.data.entity.Mood

class Converters {
    @TypeConverter
    fun fromLanguage(value: Language): String = value.name
    
    @TypeConverter
    fun toLanguage(value: String): Language = enumValueOf<Language>(value)

    @TypeConverter
    fun fromMood(value: Mood): String = value.name
    
    @TypeConverter
    fun toMood(value: String): Mood = enumValueOf<Mood>(value)
}
