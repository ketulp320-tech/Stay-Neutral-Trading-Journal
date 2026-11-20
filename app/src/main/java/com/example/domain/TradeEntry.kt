package com.example.domain

import com.example.data.ChecklistFactor
import com.example.data.ConfluenceSummary
import com.example.data.SignalBias
import com.example.data.TradeEntryEntity
import com.example.data.evaluateConfluence

data class TradeEntry(
    val id: Long = 0,
    val dateIso: String,
    val timeHm: String,
    val fii: SignalBias,
    val global: SignalBias,
    val pcr: SignalBias,
    val oiHigh: SignalBias,
    val emaSignal: SignalBias,
    val zone: SignalBias,
    val resultAmount: Double,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val confluence: ConfluenceSummary
        get() = evaluateConfluence(fii, global, pcr, oiHigh, emaSignal, zone)

    val isWin: Boolean get() = resultAmount > 0.0
    val isLoss: Boolean get() = resultAmount < 0.0
    val isBreakeven: Boolean get() = resultAmount == 0.0

    val yearMonth: String get() = if (dateIso.length >= 7) dateIso.substring(0, 7) else dateIso
    val year: Int get() = dateIso.take(4).toIntOrNull() ?: 2026

    fun biasFor(factor: ChecklistFactor): SignalBias = when (factor) {
        ChecklistFactor.FII -> fii
        ChecklistFactor.GLOBAL -> global
        ChecklistFactor.PCR -> pcr
        ChecklistFactor.OI_HIGH -> oiHigh
        ChecklistFactor.EMA_SIGNAL -> emaSignal
        ChecklistFactor.ZONE -> zone
    }

    fun toEntity(): TradeEntryEntity = TradeEntryEntity(
        id = id,
        dateIso = dateIso,
        timeHm = timeHm,
        fii = fii.name,
        global = global.name,
        pcr = pcr.name,
        oiHigh = oiHigh.name,
        emaSignal = emaSignal.name,
        zone = zone.name,
        resultAmount = resultAmount,
        createdAt = createdAt,
    )
}

fun TradeEntryEntity.toDomain(): TradeEntry = TradeEntry(
    id = id,
    dateIso = dateIso,
    timeHm = timeHm,
    fii = SignalBias.fromStored(fii),
    global = SignalBias.fromStored(global),
    pcr = SignalBias.fromStored(pcr),
    oiHigh = SignalBias.fromStored(oiHigh),
    emaSignal = SignalBias.fromStored(emaSignal),
    zone = SignalBias.fromStored(zone),
    resultAmount = resultAmount,
    createdAt = createdAt,
)
