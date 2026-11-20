package com.example.data

import com.example.domain.TradeEntry
import com.example.domain.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TradeRepository(private val tradeDao: TradeDao) {
    val allTrades: Flow<List<TradeEntry>> = tradeDao.observeAllTrades().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun saveTrade(entry: TradeEntry): Long {
        return if (entry.id == 0L) {
            tradeDao.insertTrade(entry.toEntity())
        } else {
            tradeDao.updateTrade(entry.toEntity())
            entry.id
        }
    }

    suspend fun deleteTrade(id: Long) {
        tradeDao.deleteTradeById(id)
    }
}
