package com.example

import com.example.data.AlignmentTier
import com.example.data.ChecklistFactor
import com.example.data.SignalBias
import com.example.domain.TradeAnalyticsEngine
import com.example.domain.TradeEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TradeAnalyticsEngineTest {

    @Test
    fun `analytics computes scorecard confluence daily and monthly pnl accurately`() {
        val sampleTrades = listOf(
            TradeEntry(
                id = 1,
                dateIso = "2026-11-18",
                timeHm = "09:45",
                fii = SignalBias.BULLISH,
                global = SignalBias.BULLISH,
                pcr = SignalBias.BULLISH,
                oiHigh = SignalBias.BULLISH,
                emaSignal = SignalBias.BULLISH,
                zone = SignalBias.BULLISH,
                resultAmount = 1500.0,
            ),
            TradeEntry(
                id = 2,
                dateIso = "2026-11-18",
                timeHm = "11:15",
                fii = SignalBias.BULLISH,
                global = SignalBias.BEARISH,
                pcr = SignalBias.NEUTRAL,
                oiHigh = SignalBias.BEARISH,
                emaSignal = SignalBias.BULLISH,
                zone = SignalBias.NEUTRAL,
                resultAmount = -500.0,
            ),
            TradeEntry(
                id = 3,
                dateIso = "2026-11-19",
                timeHm = "10:05",
                fii = SignalBias.BEARISH,
                global = SignalBias.BEARISH,
                pcr = SignalBias.BEARISH,
                oiHigh = SignalBias.BEARISH,
                emaSignal = SignalBias.BEARISH,
                zone = SignalBias.NEUTRAL,
                resultAmount = 2000.0,
            ),
        )

        val report = TradeAnalyticsEngine.computeFullReport(sampleTrades)

        assertEquals(3, report.overall.totalTrades)
        assertEquals(2, report.overall.wins)
        assertEquals(1, report.overall.losses)
        assertEquals(3000.0, report.overall.netPnl, 0.01)
        assertEquals(500.0, report.overall.maxDrawdown, 0.01)
        assertEquals(7.0, report.overall.profitFactor!!, 0.01)

        val highTier = report.confluenceTiers.first { it.tier == AlignmentTier.HIGH }
        assertEquals(2, highTier.count)
        assertEquals(100.0, highTier.winRatePct, 0.01)
        assertEquals(3500.0, highTier.netPnl, 0.01)

        val conflicted = report.confluenceTiers.first { it.tier == AlignmentTier.CONFLICTED }
        assertEquals(1, conflicted.count)
        assertEquals(-500.0, conflicted.netPnl, 0.01)

        val daily = TradeAnalyticsEngine.computeDailySeriesForMonth(sampleTrades, "2026-11")
        assertEquals(2, daily.size)
        assertEquals(1000.0, daily[0].netPnl, 0.01)
        assertEquals(2000.0, daily[1].netPnl, 0.01)

        val monthly = TradeAnalyticsEngine.computeMonthlySeriesForYear(sampleTrades, 2026)
        assertEquals(12, monthly.size)
        assertEquals(3000.0, monthly[10].netPnl, 0.01)

        val fiiBreakdown = report.factorBreakdowns.first { it.factor == ChecklistFactor.FII }
        assertTrue(fiiBreakdown.outcomes.any { it.bias == SignalBias.BEARISH && it.netPnl == 2000.0 })
        assertTrue(report.insights.all { it.detail.contains("₹") || !it.detail.contains("+") })
        assertEquals("+₹1,25,000", com.example.ui.components.formatSignedPnl(125000.0))
        assertEquals("−₹500", com.example.ui.components.formatSignedPnl(-500.0))
        assertEquals("₹0", com.example.ui.components.formatSignedPnl(0.0))
    }
}
