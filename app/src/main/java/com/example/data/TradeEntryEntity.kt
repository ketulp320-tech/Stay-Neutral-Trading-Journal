package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trade_entries")
data class TradeEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateIso: String, // yyyy-MM-dd
    val timeHm: String,  // HH:mm
    val tradeName: String = TradeInstrument.NIFTY.name,
    val fii: String,
    val global: String,
    val pcr: String,
    val oiHigh: String,
    val emaSignal: String,
    val zone: String,
    val resultAmount: Double,
    val remark: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)
