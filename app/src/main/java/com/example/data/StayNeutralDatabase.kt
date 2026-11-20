package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TradeEntryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class StayNeutralDatabase : RoomDatabase() {
    abstract fun tradeDao(): TradeDao

    companion object {
        @Volatile
        private var INSTANCE: StayNeutralDatabase? = null

        fun getInstance(context: Context): StayNeutralDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    StayNeutralDatabase::class.java,
                    "stay_neutral_trades.db",
                ).build().also { INSTANCE = it }
            }
        }
    }
}
