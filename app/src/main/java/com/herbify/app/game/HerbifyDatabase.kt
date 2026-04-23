package com.herbify.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CapturedPlantEntity::class],
    version = 2,
    exportSchema = false
)
abstract class HerbifyDatabase : RoomDatabase() {
    abstract fun capturedPlantDao(): CapturedPlantDao

    companion object {
        @Volatile
        private var INSTANCE: HerbifyDatabase? = null

        fun getInstance(context: Context): HerbifyDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    HerbifyDatabase::class.java,
                    "herbify.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}