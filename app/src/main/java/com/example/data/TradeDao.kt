package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TradeDao {
    @Query("SELECT * FROM trade_entries ORDER BY dateIso DESC, timeHm DESC, id DESC")
    fun observeAllTrades(): Flow<List<TradeEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrade(entry: TradeEntryEntity): Long

    @Update
    suspend fun updateTrade(entry: TradeEntryEntity)

    @Query("DELETE FROM trade_entries WHERE id = :id")
    suspend fun deleteTradeById(id: Long)
}
