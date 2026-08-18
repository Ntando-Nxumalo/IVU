package com.ntando.ivu.data.database

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    @Volatile
    private var instance: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return instance ?: synchronized(this) {
            val newInstance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "ivu_database"
            )
            .fallbackToDestructiveMigration()
            .build()
            instance = newInstance
            newInstance
        }
    }
}
