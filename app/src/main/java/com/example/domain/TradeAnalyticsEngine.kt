package com.example.domain

import com.example.data.AlignmentTier
import com.example.data.ChecklistFactor
import com.example.data.SignalBias
import kotlin.math.abs

data class OverallStats(
    val totalTrades: Int,
    val wins: Int,
    val losses: Int,
    val breakevens: Int,
    val winRatePct: Double,
    val netPnl: Double,
    val grossProfit: Double,
    val grossLoss: Double,
    val profitFactor: Double?, // null if no losses and profit > 0 (infinite) or 0 trades
    val avgWin: Double,
    val avgLoss: Double,
    val expectancy: Double,
    val bestTrade: Double,
    val worstTrade: Double,
    val maxDrawdown: Double,
    val currentStreakLabel: String,
    val tradingDays: Int,
)

data class DailyPnlPoint(
    val dateIso: String,
    val shortLabel: String,
    val netPnl: Double,
    val tradeCount: Int,
    val wins: Int,
    val losses: Int,
) {
    val winRatePct: Double
        get() = if (tradeCount == 0) 0.0 else (wins * 100.0) / tradeCount
}

data class MonthlyPnlPoint(
    val yearMonth: String,
    val monthNumber: Int,
    val shortLabel: String,
    val netPnl: Double,
    val tradeCount: Int,
    val wins: Int,
    val losses: Int,
    val greenDays: Int,
    val redDays: Int,
) {
    val winRatePct: Double
        get() = if (tradeCount == 0) 0.0 else (wins * 100.0) / tradeCount
}

data class EquityCurvePoint(
    val label: String,
    val fullDateOrMonth: String,
    val periodPnl: Double,
    val cumulativePnl: Double,
)

data class SignalOutcomeStat(
    val bias: SignalBias,
    val label: String,
    val count: Int,
    val wins: Int,
    val losses: Int,
    val winRatePct: Double,
    val netPnl: Double,
    val avgPnl: Double,
)

data class FactorBreakdown(
    val factor: ChecklistFactor,
    val outcomes: List<SignalOutcomeStat>,
    val bestOutcome: SignalOutcomeStat?,
)

data class ConfluenceTierStat(
    val tier: AlignmentTier,
    val count: Int,
    val wins: Int,
    val winRatePct: Double,
    val netPnl: Double,
    val avgPnl: Double,
)

data class SessionStat(
    val title: String,
    val rangeLabel: String,
    val count: Int,
    val wins: Int,
    val winRatePct: Double,
    val netPnl: Double,
)

data class WeekdayStat(
    val dayIndex: Int,
    val shortName: String,
    val count: Int,
    val wins: Int,
    val winRatePct: Double,
    val netPnl: Double,
)

enum class InsightSentiment { POSITIVE, CAUTION, NEUTRAL }

data class DataInsight(
    val title: String,
    val detail: String,
    val sentiment: InsightSentiment,
)

data class FullAnalyticsReport(
    val overall: OverallStats,
    val confluenceTiers: List<ConfluenceTierStat>,
    val factorBreakdowns: List<FactorBreakdown>,
    val sessionStats: List<SessionStat>,
    val weekdayStats: List<WeekdayStat>,
    val insights: List<DataInsight>,
)

object TradeAnalyticsEngine {

    fun computeFullReport(trades: List<TradeEntry>): FullAnalyticsReport {
        val chronological = trades.sortedWith(
            compareBy<TradeEntry> { it.dateIso }
                .thenBy { it.timeHm }
                .thenBy { it.id },
        )
        val overall = computeOverallStats(chronological)
        val confluence = computeConfluenceStats(chronological)
        val factors = computeFactorBreakdowns(chronological)
        val sessions = computeSessionStats(chronological)
        val weekdays = computeWeekdayStats(chronological)
        val insights = generateInsights(overall, confluence, factors, sessions, weekdays)

        return FullAnalyticsReport(
            overall = overall,
            confluenceTiers = confluence,
            factorBreakdowns = factors,
            sessionStats = sessions,
            weekdayStats = weekdays,
            insights = insights,
        )
    }

    fun computeDailySeriesForMonth(
        trades: List<TradeEntry>,
        yearMonth: String,
    ): List<DailyPnlPoint> {
        val monthTrades = trades.filter { it.yearMonth == yearMonth }
        val byDate = monthTrades.groupBy { it.dateIso }
        return byDate.entries
            .sortedBy { it.key }
            .map { (dateIso, list) ->
                DailyPnlPoint(
                    dateIso = dateIso,
                    shortLabel = DateTimeUtils.dayOfMonthNumber(dateIso),
                    netPnl = list.sumOf { it.resultAmount },
                    tradeCount = list.size,
                    wins = list.count { it.isWin },
                    losses = list.count { it.isLoss },
                )
            }
    }

    fun computeMonthlySeriesForYear(
        trades: List<TradeEntry>,
        year: Int,
    ): List<MonthlyPnlPoint> {
        val yearTrades = trades.filter { it.year == year }
        val byMonth = yearTrades.groupBy { it.yearMonth }
        return (1..12).map { month ->
            val ym = String.format(java.util.Locale.US, "%04d-%02d", year, month)
            val list = byMonth[ym].orEmpty()
            val dayTotals = list.groupBy { it.dateIso }.values.map { d -> d.sumOf { it.resultAmount } }
            MonthlyPnlPoint(
                yearMonth = ym,
                monthNumber = month,
                shortLabel = DateTimeUtils.formatShortMonth(month),
                netPnl = list.sumOf { it.resultAmount },
                tradeCount = list.size,
                wins = list.count { it.isWin },
                losses = list.count { it.isLoss },
                greenDays = dayTotals.count { it > 0.0 },
                redDays = dayTotals.count { it < 0.0 },
            )
        }
    }

    fun computeDailyEquityCurve(dailyPoints: List<DailyPnlPoint>): List<EquityCurvePoint> {
        var running = 0.0
        return dailyPoints.map { pt ->
            running += pt.netPnl
            EquityCurvePoint(
                label = pt.shortLabel,
                fullDateOrMonth = DateTimeUtils.formatShortDate(pt.dateIso),
                periodPnl = pt.netPnl,
                cumulativePnl = running,
            )
        }
    }

    fun computeMonthlyEquityCurve(monthlyPoints: List<MonthlyPnlPoint>): List<EquityCurvePoint> {
        val activeMonths = monthlyPoints.filter { it.tradeCount > 0 }
        var running = 0.0
        return activeMonths.map { pt ->
            running += pt.netPnl
            EquityCurvePoint(
                label = pt.shortLabel,
                fullDateOrMonth = DateTimeUtils.formatMonthYear(pt.yearMonth),
                periodPnl = pt.netPnl,
                cumulativePnl = running,
            )
        }
    }

    private fun computeOverallStats(chronological: List<TradeEntry>): OverallStats {
        if (chronological.isEmpty()) {
            return OverallStats(
                totalTrades = 0,
                wins = 0,
                losses = 0,
                breakevens = 0,
                winRatePct = 0.0,
                netPnl = 0.0,
                grossProfit = 0.0,
                grossLoss = 0.0,
                profitFactor = null,
                avgWin = 0.0,
                avgLoss = 0.0,
                expectancy = 0.0,
                bestTrade = 0.0,
                worstTrade = 0.0,
                maxDrawdown = 0.0,
                currentStreakLabel = "—",
                tradingDays = 0,
            )
        }

        val winsList = chronological.filter { it.isWin }
        val lossesList = chronological.filter { it.isLoss }
        val breakevens = chronological.count { it.isBreakeven }
        val grossProfit = winsList.sumOf { it.resultAmount }
        val grossLoss = abs(lossesList.sumOf { it.resultAmount })
        val netPnl = chronological.sumOf { it.resultAmount }
        val total = chronological.size
        val decided = winsList.size + lossesList.size
        val winRate = if (decided > 0) (winsList.size * 100.0) / decided else 0.0
        val profitFactor = if (grossLoss > 0.0) grossProfit / grossLoss else null
        val avgWin = if (winsList.isNotEmpty()) grossProfit / winsList.size else 0.0
        val avgLoss = if (lossesList.isNotEmpty()) grossLoss / lossesList.size else 0.0
        val expectancy = netPnl / total
        val bestTrade = chronological.maxOf { it.resultAmount }
        val worstTrade = chronological.minOf { it.resultAmount }

        var equity = 0.0
        var peak = 0.0
        var maxDd = 0.0
        for (trade in chronological) {
            equity += trade.resultAmount
            if (equity > peak) peak = equity
            val dd = peak - equity
            if (dd > maxDd) maxDd = dd
        }

        val lastNonFlat = chronological.lastOrNull { !it.isBreakeven }
        val streakLabel = if (lastNonFlat == null) {
            "Flat"
        } else {
            val isWinStreak = lastNonFlat.isWin
            var count = 0
            for (i in chronological.lastIndex downTo 0) {
                val t = chronological[i]
                if (t.isBreakeven) continue
                if (t.isWin == isWinStreak) count++ else break
            }
            if (isWinStreak) "${count}W Streak" else "${count}L Streak"
        }

        val tradingDays = chronological.map { it.dateIso }.distinct().size

        return OverallStats(
            totalTrades = total,
            wins = winsList.size,
            losses = lossesList.size,
            breakevens = breakevens,
            winRatePct = winRate,
            netPnl = netPnl,
            grossProfit = grossProfit,
            grossLoss = grossLoss,
            profitFactor = profitFactor,
            avgWin = avgWin,
            avgLoss = avgLoss,
            expectancy = expectancy,
            bestTrade = bestTrade,
            worstTrade = worstTrade,
            maxDrawdown = maxDd,
            currentStreakLabel = streakLabel,
            tradingDays = tradingDays,
        )
    }

    private fun computeConfluenceStats(trades: List<TradeEntry>): List<ConfluenceTierStat> {
        return AlignmentTier.entries.map { tier ->
            val group = trades.filter { it.confluence.tier == tier }
            val wins = group.count { it.isWin }
            val decided = group.count { !it.isBreakeven }
            val net = group.sumOf { it.resultAmount }
            ConfluenceTierStat(
                tier = tier,
                count = group.size,
                wins = wins,
                winRatePct = if (decided > 0) (wins * 100.0) / decided else 0.0,
                netPnl = net,
                avgPnl = if (group.isNotEmpty()) net / group.size else 0.0,
            )
        }
    }

    private fun computeFactorBreakdowns(trades: List<TradeEntry>): List<FactorBreakdown> {
        return ChecklistFactor.entries.map { factor ->
            val outcomes = SignalBias.entries.map { bias ->
                val group = trades.filter { it.biasFor(factor) == bias }
                val wins = group.count { it.isWin }
                val losses = group.count { it.isLoss }
                val decided = wins + losses
                val net = group.sumOf { it.resultAmount }
                SignalOutcomeStat(
                    bias = bias,
                    label = factor.labelFor(bias),
                    count = group.size,
                    wins = wins,
                    losses = losses,
                    winRatePct = if (decided > 0) (wins * 100.0) / decided else 0.0,
                    netPnl = net,
                    avgPnl = if (group.isNotEmpty()) net / group.size else 0.0,
                )
            }
            val best = outcomes.filter { it.count > 0 }.maxWithOrNull(
                compareBy<SignalOutcomeStat> { it.netPnl }.thenBy { it.winRatePct },
            )
            FactorBreakdown(
                factor = factor,
                outcomes = outcomes,
                bestOutcome = best,
            )
        }
    }

    private fun computeSessionStats(trades: List<TradeEntry>): List<SessionStat> {
        data class Bucket(val title: String, val range: String, val predicate: (Int) -> Boolean)

        val buckets = listOf(
            Bucket("Opening Drive", "09:00–10:30") { mins -> mins in (9 * 60) until (10 * 60 + 30) },
            Bucket("Mid-Morning", "10:30–12:30") { mins -> mins in (10 * 60 + 30) until (12 * 60 + 30) },
            Bucket("Afternoon", "12:30–14:30") { mins -> mins in (12 * 60 + 30) until (14 * 60 + 30) },
            Bucket("Closing Hour", "14:30–16:00") { mins -> mins in (14 * 60 + 30)..(16 * 60) },
            Bucket("Other Hours", "Before 09:00 / After 16:00") { mins ->
                mins < 9 * 60 || mins > 16 * 60
            },
        )

        return buckets.mapNotNull { bucket ->
            val group = trades.filter { trade ->
                val (h, m) = DateTimeUtils.parseHourMinute(trade.timeHm)
                bucket.predicate(h * 60 + m)
            }
            if (group.isEmpty() && bucket.title == "Other Hours") {
                null
            } else {
                val wins = group.count { it.isWin }
                val decided = group.count { !it.isBreakeven }
                SessionStat(
                    title = bucket.title,
                    rangeLabel = bucket.range,
                    count = group.size,
                    wins = wins,
                    winRatePct = if (decided > 0) (wins * 100.0) / decided else 0.0,
                    netPnl = group.sumOf { it.resultAmount },
                )
            }
        }
    }

    private fun computeWeekdayStats(trades: List<TradeEntry>): List<WeekdayStat> {
        val names = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        return (1..7).mapNotNull { idx ->
            val group = trades.filter { DateTimeUtils.weekdayIndexMondayOne(it.dateIso) == idx }
            if (idx >= 6 && group.isEmpty()) {
                null
            } else {
                val wins = group.count { it.isWin }
                val decided = group.count { !it.isBreakeven }
                WeekdayStat(
                    dayIndex = idx,
                    shortName = names[idx - 1],
                    count = group.size,
                    wins = wins,
                    winRatePct = if (decided > 0) (wins * 100.0) / decided else 0.0,
                    netPnl = group.sumOf { it.resultAmount },
                )
            }
        }
    }

    private fun generateInsights(
        overall: OverallStats,
        confluence: List<ConfluenceTierStat>,
        factors: List<FactorBreakdown>,
        sessions: List<SessionStat>,
        weekdays: List<WeekdayStat>,
    ): List<DataInsight> {
        if (overall.totalTrades == 0) return emptyList()
        val list = mutableListOf<DataInsight>()

        val highTier = confluence.firstOrNull { it.tier == AlignmentTier.HIGH }
        val conflictedTier = confluence.firstOrNull { it.tier == AlignmentTier.CONFLICTED }
        if (highTier != null && highTier.count > 0 && conflictedTier != null && conflictedTier.count > 0) {
            if (highTier.avgPnl > conflictedTier.avgPnl) {
                list += DataInsight(
                    title = "Confluence Pays Off",
                    detail = "5–6/6 aligned setups average ${formatPlainSigned(highTier.avgPnl)} (${highTier.winRatePct.toInt()}% win rate) vs ${formatPlainSigned(conflictedTier.avgPnl)} on conflicted signals.",
                    sentiment = InsightSentiment.POSITIVE,
                )
            } else {
                list += DataInsight(
                    title = "Review High-Alignment Entries",
                    detail = "Your 5–6/6 setups currently average ${formatPlainSigned(highTier.avgPnl)}. Double-check timing and zone execution.",
                    sentiment = InsightSentiment.CAUTION,
                )
            }
        } else if (conflictedTier != null && conflictedTier.count > 0 && conflictedTier.netPnl < 0.0) {
            list += DataInsight(
                title = "Stay Neutral on Mixed Signals",
                detail = "Conflicted setups (≤3/6 aligned) account for ${formatPlainSigned(conflictedTier.netPnl)} across ${conflictedTier.count} trade(s). Skipping mixed checklists protects capital.",
                sentiment = InsightSentiment.CAUTION,
            )
        }

        val allFactorSignals = factors.flatMap { fb ->
            fb.outcomes.filter { it.count > 0 }.map { fb.factor to it }
        }
        val strongestSignal = allFactorSignals.maxWithOrNull(
            compareBy<Pair<ChecklistFactor, SignalOutcomeStat>> { it.second.netPnl }
                .thenBy { it.second.winRatePct },
        )
        if (strongestSignal != null && strongestSignal.second.netPnl > 0.0) {
            val (factor, stat) = strongestSignal
            list += DataInsight(
                title = "Strongest Factor: ${factor.columnTitle} (${stat.label})",
                detail = "${stat.wins}/${stat.count} wins (${stat.winRatePct.toInt()}%) generating ${formatPlainSigned(stat.netPnl)} net P&L.",
                sentiment = InsightSentiment.POSITIVE,
            )
        }

        val weakestSignal = allFactorSignals.minByOrNull { it.second.netPnl }
        if (weakestSignal != null && weakestSignal.second.netPnl < 0.0) {
            val (factor, stat) = weakestSignal
            list += DataInsight(
                title = "Drag Factor: ${factor.columnTitle} (${stat.label})",
                detail = "${stat.count} trades netted ${formatPlainSigned(stat.netPnl)} (${stat.winRatePct.toInt()}% win rate). Consider stricter confirmation.",
                sentiment = InsightSentiment.CAUTION,
            )
        }

        val bestSession = sessions.filter { it.count > 0 }.maxByOrNull { it.netPnl }
        if (bestSession != null && bestSession.netPnl > 0.0) {
            list += DataInsight(
                title = "Best Window: ${bestSession.title} (${bestSession.rangeLabel})",
                detail = "Logged ${bestSession.count} trade(s) with ${bestSession.winRatePct.toInt()}% win rate and ${formatPlainSigned(bestSession.netPnl)} net.",
                sentiment = InsightSentiment.POSITIVE,
            )
        }

        val bestDay = weekdays.filter { it.count > 0 }.maxByOrNull { it.netPnl }
        if (bestDay != null && weekdays.count { it.count > 0 } >= 2) {
            list += DataInsight(
                title = "Top Trading Day: ${bestDay.shortName}",
                detail = "${bestDay.count} trade(s) with ${bestDay.winRatePct.toInt()}% win rate (${formatPlainSigned(bestDay.netPnl)}).",
                sentiment = if (bestDay.netPnl >= 0) InsightSentiment.NEUTRAL else InsightSentiment.CAUTION,
            )
        }

        return list.take(4)
    }

    private fun formatPlainSigned(value: Double): String {
        val absVal = abs(value)
        val formatted = if (absVal % 1.0 == 0.0) {
            String.format(java.util.Locale.US, "%,.0f", absVal)
        } else {
            String.format(java.util.Locale.US, "%,.2f", absVal)
        }
        return when {
            value > 0.0 -> "+₹$formatted"
            value < 0.0 -> "−₹$formatted"
            else -> "₹0"
        }
    }
}
