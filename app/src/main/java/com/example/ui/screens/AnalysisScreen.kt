package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.ConfluenceTierStat
import com.example.domain.DataInsight
import com.example.domain.FactorBreakdown
import com.example.domain.InsightSentiment
import com.example.domain.OverallStats
import com.example.domain.SessionStat
import com.example.domain.WeekdayStat
import com.example.ui.StayNeutralUiState
import com.example.ui.components.biasAccentColor
import com.example.ui.components.biasContainerColor
import com.example.ui.components.biasTextColor
import com.example.ui.components.formatCompactPnl
import com.example.ui.components.formatPercent
import com.example.ui.components.formatSignedPnl
import com.example.ui.components.pnlColor
import com.example.ui.theme.LocalTradingColors
import com.example.ui.theme.MonoTextStyles
import java.util.Locale
import kotlin.math.abs

@Composable
fun AnalysisScreen(
    uiState: StayNeutralUiState,
    onAddTrade: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val report = uiState.analytics
    val tradingColors = LocalTradingColors.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("analysis_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "analysis_header") {
            Column {
                Text(
                    text = "Checklist Analysis",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Computed strictly from your Date, Time, 6 checklist signals & Result",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (report.overall.totalTrades == 0) {
            item(key = "analysis_empty") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = tradingColors.cardElevated,
                    border = BorderStroke(1.dp, tradingColors.tableGridBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Analytics,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .padding(14.dp)
                                    .size(28.dp),
                            )
                        }
                        Text(
                            text = stringResource(R.string.empty_analysis_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.empty_analysis_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = onAddTrade,
                            modifier = Modifier.testTag("analysis_empty_add_button"),
                        ) {
                            Text(stringResource(R.string.action_log_trade))
                        }
                    }
                }
            }
        } else {
            // 1. Overall Performance Scorecard
            item(key = "scorecard") {
                ScorecardSection(stats = report.overall)
            }

            // 2. Data-Driven Takeaways
            if (report.insights.isNotEmpty()) {
                item(key = "takeaways") {
                    InsightsSection(insights = report.insights)
                }
            }

            // 3. Confluence Alignment Edge
            item(key = "confluence_edge") {
                ConfluenceEdgeCard(tiers = report.confluenceTiers)
            }

            // 4. Factor-by-Factor Breakdown (FII, Global, PCR, OI High, EMA Signal, Zone)
            item(key = "factors_title") {
                Text(
                    text = stringResource(R.string.analysis_factors),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            report.factorBreakdowns.forEach { breakdown ->
                item(key = "factor_${breakdown.factor.name}") {
                    FactorBreakdownCard(breakdown = breakdown)
                }
            }

            // 5. Session Time Performance (from Time column)
            item(key = "session_performance") {
                SessionPerformanceCard(sessions = report.sessionStats)
            }

            // 6. Day-of-Week Performance (from Date column)
            item(key = "weekday_performance") {
                WeekdayPerformanceCard(weekdays = report.weekdayStats)
            }
        }
    }
}

@Composable
private fun ScorecardSection(stats: OverallStats) {
    val tradingColors = LocalTradingColors.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.analysis_scorecard),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ScoreTile(
                label = "Net P&L (INR)",
                value = formatSignedPnl(stats.netPnl),
                sub = "${stats.totalTrades} trades • ${stats.tradingDays}d",
                valueColor = pnlColor(stats.netPnl),
                modifier = Modifier.weight(1f),
            )
            ScoreTile(
                label = "Win Rate",
                value = formatPercent(stats.winRatePct),
                sub = "${stats.wins}W / ${stats.losses}L / ${stats.breakevens}F",
                valueColor = if (stats.winRatePct >= 50.0) tradingColors.profit else tradingColors.loss,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val pfLabel = when {
                stats.profitFactor != null -> String.format(Locale.US, "%.2f", stats.profitFactor)
                stats.grossProfit > 0.0 -> "∞"
                else -> "0.00"
            }
            ScoreTile(
                label = "Profit Factor",
                value = pfLabel,
                sub = "Expectancy: ${formatCompactPnl(stats.expectancy)}",
                valueColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            ScoreTile(
                label = "Max Drawdown",
                value = if (stats.maxDrawdown == 0.0) "₹0" else formatCompactPnl(-stats.maxDrawdown),
                sub = "Streak: ${stats.currentStreakLabel}",
                valueColor = if (stats.maxDrawdown > 0.0) tradingColors.loss else tradingColors.profit,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ScoreTile(
                label = "Avg Win",
                value = formatSignedPnl(stats.avgWin),
                sub = "Best: ${formatCompactPnl(stats.bestTrade)}",
                valueColor = tradingColors.profit,
                modifier = Modifier.weight(1f),
            )
            ScoreTile(
                label = "Avg Loss",
                value = if (stats.avgLoss == 0.0) "₹0" else formatSignedPnl(-stats.avgLoss),
                sub = "Worst: ${formatCompactPnl(stats.worstTrade)}",
                valueColor = tradingColors.loss,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ScoreTile(
    label: String,
    value: String,
    sub: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    val tradingColors = LocalTradingColors.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = tradingColors.cardElevated,
        border = BorderStroke(1.dp, tradingColors.tableGridBorder),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MonoTextStyles.kpiValue,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = sub,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun InsightsSection(insights: List<DataInsight>) {
    val tradingColors = LocalTradingColors.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = tradingColors.cardElevated,
        border = BorderStroke(1.dp, tradingColors.tableGridBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(R.string.analysis_takeaways),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            insights.forEachIndexed { idx, insight ->
                val accent = when (insight.sentiment) {
                    InsightSentiment.POSITIVE -> tradingColors.profit
                    InsightSentiment.CAUTION -> tradingColors.loss
                    InsightSentiment.NEUTRAL -> tradingColors.neutral
                }
                val icon = when (insight.sentiment) {
                    InsightSentiment.POSITIVE -> Icons.Outlined.AutoGraph
                    InsightSentiment.CAUTION -> Icons.Outlined.WarningAmber
                    InsightSentiment.NEUTRAL -> Icons.Outlined.Balance
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(18.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = insight.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = insight.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (idx != insights.lastIndex) {
                    HorizontalDivider(color = tradingColors.tableGridBorder.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
private fun ConfluenceEdgeCard(tiers: List<ConfluenceTierStat>) {
    val tradingColors = LocalTradingColors.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = tradingColors.cardElevated,
        border = BorderStroke(1.dp, tradingColors.tableGridBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column {
                Text(
                    text = stringResource(R.string.analysis_confluence),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "How checklist agreement impacts your results",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            tiers.forEach { stat ->
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stat.tier.title,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "${stat.count} trades • ${if (stat.count == 0) "—" else formatPercent(stat.winRatePct)} win",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = if (stat.count == 0) "—" else formatSignedPnl(stat.netPnl),
                            style = MonoTextStyles.tableValue.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                            color = if (stat.count == 0) MaterialTheme.colorScheme.onSurfaceVariant else pnlColor(stat.netPnl),
                        )
                    }

                    val fillFraction = (stat.winRatePct / 100.0).toFloat().coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        if (stat.count > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fillFraction)
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (stat.netPnl >= 0.0) tradingColors.profit else tradingColors.loss,
                                    ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FactorBreakdownCard(breakdown: FactorBreakdown) {
    val tradingColors = LocalTradingColors.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = tradingColors.cardElevated,
        border = BorderStroke(1.dp, tradingColors.tableGridBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${breakdown.factor.columnTitle} — ${breakdown.factor.fullTitle}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                breakdown.bestOutcome?.let { best ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = biasContainerColor(best.bias),
                    ) {
                        Text(
                            text = "Best: ${best.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = biasTextColor(best.bias),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        )
                    }
                }
            }

            breakdown.outcomes.forEach { outcome ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = biasContainerColor(outcome.bias),
                        modifier = Modifier.width(78.dp),
                    ) {
                        Text(
                            text = outcome.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = biasTextColor(outcome.bias),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        )
                    }

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "${outcome.count} tr • ${if (outcome.count == 0) "—" else formatPercent(outcome.winRatePct)}",
                                style = MonoTextStyles.tableCaption,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = if (outcome.count == 0) "—" else formatSignedPnl(outcome.netPnl),
                                style = MonoTextStyles.tableCaption.copy(fontWeight = FontWeight.Bold),
                                color = if (outcome.count == 0) MaterialTheme.colorScheme.onSurfaceVariant else pnlColor(outcome.netPnl),
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            if (outcome.count > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth((outcome.winRatePct / 100.0).toFloat().coerceIn(0f, 1f))
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(biasAccentColor(outcome.bias)),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionPerformanceCard(sessions: List<SessionStat>) {
    val tradingColors = LocalTradingColors.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = tradingColors.cardElevated,
        border = BorderStroke(1.dp, tradingColors.tableGridBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.analysis_time_sessions),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            sessions.forEachIndexed { idx, s ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1.3f)) {
                        Text(
                            text = s.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = s.rangeLabel,
                            style = MonoTextStyles.tableCaption,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = if (s.count == 0) "0 tr" else "${s.count} tr • ${formatPercent(s.winRatePct)}",
                        style = MonoTextStyles.tableCaption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = if (s.count == 0) "—" else formatSignedPnl(s.netPnl),
                        style = MonoTextStyles.tableValue.copy(fontWeight = FontWeight.Bold),
                        color = if (s.count == 0) MaterialTheme.colorScheme.onSurfaceVariant else pnlColor(s.netPnl),
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(0.9f),
                    )
                }
                if (idx != sessions.lastIndex) {
                    HorizontalDivider(color = tradingColors.tableGridBorder.copy(alpha = 0.55f))
                }
            }
        }
    }
}

@Composable
private fun WeekdayPerformanceCard(weekdays: List<WeekdayStat>) {
    val tradingColors = LocalTradingColors.current
    val maxAbs = weekdays.maxOfOrNull { abs(it.netPnl) }?.takeIf { it > 0.0 } ?: 1.0

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = tradingColors.cardElevated,
        border = BorderStroke(1.dp, tradingColors.tableGridBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.analysis_weekday),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            weekdays.forEach { w ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = w.shortName,
                        style = MonoTextStyles.tableValue,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(34.dp),
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        if (w.count > 0 && w.netPnl != 0.0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth((abs(w.netPnl) / maxAbs).toFloat().coerceIn(0.06f, 1f))
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(pnlColor(w.netPnl)),
                            )
                        }
                    }

                    Text(
                        text = if (w.count == 0) "—" else formatCompactPnl(w.netPnl),
                        style = MonoTextStyles.tableCaption.copy(fontWeight = FontWeight.Bold),
                        color = if (w.count == 0) MaterialTheme.colorScheme.onSurfaceVariant else pnlColor(w.netPnl),
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(66.dp),
                    )
                }
            }
        }
    }
}
