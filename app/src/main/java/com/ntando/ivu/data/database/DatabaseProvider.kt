package com.ntando.ivu.data.database

import android.content.Context
import android.util.Log
import androidx.room.Room

private const val TAG = "DatabaseProvider"

/**
 * Thread-safe Singleton provider for obtaining and initializing the application's single [AppDatabase] instance.
 *
 * Uses double-checked locking with `@Volatile` caching to ensure efficient singleton instantiation.
 */
object DatabaseProvider {
    @Volatile
    private var instance: AppDatabase? = null

    /**
     * Returns the singleton instance of [AppDatabase], constructing it if necessary.
     *
     * @param context Application context used to build Room database instance.
     * @return The initialized [AppDatabase] instance.
     */
    fun getDatabase(context: Context): AppDatabase {
        return instance ?: synchronized(this) {
            val current = instance
            if (current != null) {
                current
            } else {
                Log.i(TAG, "Initializing Room database instance: 'ivu_database'")
                val newInstance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ivu_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                instance = newInstance
                Log.d(TAG, "Room database 'ivu_database' successfully built and cached")
                newInstance
            }
        }
    }
}
