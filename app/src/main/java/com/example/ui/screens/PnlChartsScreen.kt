package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.DailyPnlPoint
import com.example.domain.DateTimeUtils
import com.example.domain.EquityCurvePoint
import com.example.domain.MonthlyPnlPoint
import com.example.ui.ChartPeriodMode
import com.example.ui.StayNeutralUiState
import com.example.ui.components.formatCompactPnl
import com.example.ui.components.formatPercent
import com.example.ui.components.formatSignedPnl
import com.example.ui.components.pnlColor
import com.example.ui.components.pnlContainerColor
import com.example.ui.theme.LocalTradingColors
import com.example.ui.theme.MonoTextStyles
import kotlin.math.abs
import kotlin.math.max

private data class ChartBarDatum(
    val shortLabel: String,
    val fullTitle: String,
    val value: Double,
    val tradeCount: Int,
    val winRatePct: Double,
    val dateIsoOrNull: String? = null,
)

@Composable
fun PnlChartsScreen(
    uiState: StayNeutralUiState,
    onSetPeriodMode: (ChartPeriodMode) -> Unit,
    onStepMonth: (Int) -> Unit,
    onStepYear: (Int) -> Unit,
    onInspectDay: (String) -> Unit,
    onAddTrade: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tradingColors = LocalTradingColors.current

    val isDaily = uiState.chartPeriodMode == ChartPeriodMode.DAILY
    val barItems = remember(isDaily, uiState.dailyChartPoints, uiState.monthlyChartPoints) {
        if (isDaily) {
            uiState.dailyChartPoints.map { pt ->
                ChartBarDatum(
                    shortLabel = pt.shortLabel,
                    fullTitle = DateTimeUtils.formatMediumDate(pt.dateIso),
                    value = pt.netPnl,
                    tradeCount = pt.tradeCount,
                    winRatePct = pt.winRatePct,
                    dateIsoOrNull = pt.dateIso,
                )
            }
        } else {
            uiState.monthlyChartPoints.map { pt ->
                ChartBarDatum(
                    shortLabel = pt.shortLabel,
                    fullTitle = DateTimeUtils.formatMonthYear(pt.yearMonth),
                    value = pt.netPnl,
                    tradeCount = pt.tradeCount,
                    winRatePct = pt.winRatePct,
                )
            }
        }
    }

    val equityPoints = if (isDaily) uiState.dailyEquityPoints else uiState.monthlyEquityPoints
    var selectedBarIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(isDaily, uiState.selectedChartMonth, uiState.selectedChartYear, barItems.size) {
        selectedBarIndex = barItems.indexOfLast { it.tradeCount > 0 }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("pnl_charts_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Header + Daily/Monthly Segmented Switcher
        item(key = "charts_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "P&L Visualizations",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = if (isDaily) "Day-wise profit & loss breakdown" else "Month-wise annual P&L overview",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(modifier = Modifier.padding(3.dp)) {
                        PeriodModeChip(
                            label = stringResource(R.string.tab_daily),
                            selected = isDaily,
                            onClick = { onSetPeriodMode(ChartPeriodMode.DAILY) },
                            testTag = "chart_mode_daily",
                        )
                        PeriodModeChip(
                            label = stringResource(R.string.tab_monthly),
                            selected = !isDaily,
                            onClick = { onSetPeriodMode(ChartPeriodMode.MONTHLY) },
                            testTag = "chart_mode_monthly",
                        )
                    }
                }
            }
        }

        // Period Navigation Bar (< Nov 2026 > or < 2026 >)
        item(key = "period_stepper") {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = tradingColors.cardElevated,
                border = BorderStroke(1.dp, tradingColors.tableGridBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(
                        onClick = { if (isDaily) onStepMonth(-1) else onStepYear(-1) },
                        modifier = Modifier.testTag("chart_prev_period_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChevronLeft,
                            contentDescription = stringResource(R.string.action_prev_period),
                        )
                    }

                    Text(
                        text = if (isDaily) {
                            DateTimeUtils.formatMonthYear(uiState.selectedChartMonth)
                        } else {
                            uiState.selectedChartYear.toString()
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    IconButton(
                        onClick = { if (isDaily) onStepMonth(1) else onStepYear(1) },
                        modifier = Modifier.testTag("chart_next_period_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChevronRight,
                            contentDescription = stringResource(R.string.action_next_period),
                        )
                    }
                }
            }
        }

        if (uiState.allTrades.isEmpty()) {
            item(key = "charts_empty") {
                EmptyVisualizationCard(onAddTrade = onAddTrade)
            }
        } else {
            val activePeriods = barItems.filter { it.tradeCount > 0 }
            val periodNet = activePeriods.sumOf { it.value }
            val bestPeriod = activePeriods.maxByOrNull { it.value }
            val worstPeriod = activePeriods.minByOrNull { it.value }
            val greenCount = activePeriods.count { it.value > 0.0 }
            val redCount = activePeriods.count { it.value < 0.0 }

            // Period KPI Row
            item(key = "period_kpis") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ChartKpiTile(
                        label = if (isDaily) "Month Net P&L (INR)" else "Year Net P&L (INR)",
                        value = if (activePeriods.isEmpty()) "₹0" else formatSignedPnl(periodNet),
                        valueColor = if (activePeriods.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else pnlColor(periodNet),
                        subtitle = "${activePeriods.sumOf { it.tradeCount }} trades",
                        modifier = Modifier.weight(1.2f),
                    )
                    ChartKpiTile(
                        label = if (isDaily) "Green / Red" else "Profit Months",
                        value = "$greenCount / $redCount",
                        valueColor = MaterialTheme.colorScheme.onSurface,
                        subtitle = if (isDaily) "Trading days" else "Active months",
                        modifier = Modifier.weight(0.9f),
                    )
                }
            }

            item(key = "period_best_worst") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ChartKpiTile(
                        label = if (isDaily) "Best Day" else "Best Month",
                        value = bestPeriod?.let { formatSignedPnl(it.value) } ?: "—",
                        valueColor = bestPeriod?.let { pnlColor(it.value) } ?: MaterialTheme.colorScheme.onSurfaceVariant,
                        subtitle = bestPeriod?.shortLabel ?: "No entries",
                        modifier = Modifier.weight(1f),
                    )
                    ChartKpiTile(
                        label = if (isDaily) "Worst Day" else "Worst Month",
                        value = worstPeriod?.let { formatSignedPnl(it.value) } ?: "—",
                        valueColor = worstPeriod?.let { pnlColor(it.value) } ?: MaterialTheme.colorScheme.onSurfaceVariant,
                        subtitle = worstPeriod?.shortLabel ?: "No entries",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Diverging Bar Chart Card
            item(key = "diverging_bar_chart") {
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
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = if (isDaily) {
                                    stringResource(R.string.chart_daily_pnl_title)
                                } else {
                                    stringResource(R.string.chart_monthly_pnl_title)
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Tap bar to inspect",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        if (activePeriods.isEmpty()) {
                            Text(
                                text = if (isDaily) {
                                    "No trades recorded in ${DateTimeUtils.formatMonthYear(uiState.selectedChartMonth)}."
                                } else {
                                    "No trades recorded in ${uiState.selectedChartYear}."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 24.dp),
                            )
                        } else {
                            // Inspected bar detail pill
                            val inspected = barItems.getOrNull(selectedBarIndex)
                            if (inspected != null && inspected.tradeCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = pnlContainerColor(inspected.value),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Column {
                                            Text(
                                                text = inspected.fullTitle,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                            Text(
                                                text = "${inspected.tradeCount} trade(s) • ${formatPercent(inspected.winRatePct)} win rate",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            Text(
                                                text = formatSignedPnl(inspected.value),
                                                style = MonoTextStyles.kpiValue,
                                                color = pnlColor(inspected.value),
                                            )
                                            if (inspected.dateIsoOrNull != null) {
                                                IconButton(
                                                    onClick = { onInspectDay(inspected.dateIsoOrNull) },
                                                    modifier = Modifier.size(32.dp),
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.OpenInNew,
                                                        contentDescription = "Open day in checklist",
                                                        tint = MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.size(16.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            DivergingPnlBarChart(
                                items = barItems,
                                selectedIndex = selectedBarIndex,
                                onSelectIndex = { selectedBarIndex = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(210.dp),
                            )
                        }
                    }
                }
            }

            // Cumulative Equity Curve Card
            if (equityPoints.isNotEmpty()) {
                item(key = "equity_curve_card") {
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = stringResource(R.string.chart_equity_curve_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                val endEquity = equityPoints.last().cumulativePnl
                                Text(
                                    text = formatSignedPnl(endEquity),
                                    style = MonoTextStyles.kpiValue.copy(fontSize = 15.sp),
                                    color = pnlColor(endEquity),
                                )
                            }

                            EquityCurveChart(
                                points = equityPoints,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(175.dp),
                            )
                        }
                    }
                }
            }

            // Daily P&L Calendar Heatmap (Daily Mode) OR Monthly Ledger Table (Monthly Mode)
            if (isDaily) {
                item(key = "daily_calendar_heatmap") {
                    DailyCalendarHeatmapCard(
                        yearMonth = uiState.selectedChartMonth,
                        dailyPoints = uiState.dailyChartPoints,
                        onDayClick = onInspectDay,
                    )
                }
            } else {
                item(key = "monthly_breakdown_table") {
                    MonthlyBreakdownCard(monthlyPoints = uiState.monthlyChartPoints)
                }
            }
        }
    }
}

@Composable
private fun PeriodModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
        )
    }
}

@Composable
private fun ChartKpiTile(
    label: String,
    value: String,
    valueColor: Color,
    subtitle: String,
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
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
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
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun DivergingPnlBarChart(
    items: List<ChartBarDatum>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tradingColors = LocalTradingColors.current
    val gridColor = tradingColors.tableGridBorder
    val zeroLineColor = MaterialTheme.colorScheme.outline
    val highlightColor = MaterialTheme.colorScheme.primary

    val maxAbs = remember(items) {
        max(1.0, items.maxOfOrNull { abs(it.value) } ?: 1.0)
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            // Left Axis Labels
            Column(
                modifier = Modifier
                    .width(54.dp)
                    .fillMaxSize()
                    .padding(end = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = formatCompactPnl(maxAbs),
                    style = MonoTextStyles.tableCaption.copy(fontSize = 10.sp),
                    color = tradingColors.profit,
                )
                Text(
                    text = "0",
                    style = MonoTextStyles.tableCaption.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatCompactPnl(-maxAbs),
                    style = MonoTextStyles.tableCaption.copy(fontSize = 10.sp),
                    color = tradingColors.loss,
                )
            }

            // Canvas Plot Area
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .pointerInput(items) {
                        detectTapGestures { offset ->
                            if (items.isNotEmpty() && size.width > 0) {
                                val slot = size.width.toFloat() / items.size
                                val tapped = (offset.x / slot).toInt().coerceIn(0, items.lastIndex)
                                onSelectIndex(tapped)
                            }
                        }
                    },
            ) {
                val w = size.width
                val h = size.height
                val zeroY = h / 2f
                val maxBarHalfHeight = (h / 2f) - 10f

                // Dashed guide lines at +50% and -50%
                val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, zeroY - maxBarHalfHeight * 0.5f),
                    end = Offset(w, zeroY - maxBarHalfHeight * 0.5f),
                    strokeWidth = 1f,
                    pathEffect = dash,
                )
                drawLine(
                    color = gridColor,
                    start = Offset(0f, zeroY + maxBarHalfHeight * 0.5f),
                    end = Offset(w, zeroY + maxBarHalfHeight * 0.5f),
                    strokeWidth = 1f,
                    pathEffect = dash,
                )
                // Solid zero baseline
                drawLine(
                    color = zeroLineColor.copy(alpha = 0.7f),
                    start = Offset(0f, zeroY),
                    end = Offset(w, zeroY),
                    strokeWidth = 1.5f,
                )

                val count = items.size.coerceAtLeast(1)
                val slotWidth = w / count
                val barWidth = (slotWidth * 0.62f).coerceIn(6f, 36f)

                items.forEachIndexed { idx, item ->
                    val centerX = slotWidth * idx + slotWidth / 2f
                    val isSelected = idx == selectedIndex

                    if (isSelected) {
                        drawRoundRect(
                            color = highlightColor.copy(alpha = 0.12f),
                            topLeft = Offset(slotWidth * idx + 2f, 2f),
                            size = Size((slotWidth - 4f).coerceAtLeast(2f), h - 4f),
                            cornerRadius = CornerRadius(6f, 6f),
                        )
                    }

                    if (item.value != 0.0) {
                        val barH = ((abs(item.value) / maxAbs).toFloat() * maxBarHalfHeight).coerceAtLeast(4f)
                        val isPos = item.value > 0.0
                        val barColor = if (isPos) tradingColors.profit else tradingColors.loss
                        val topY = if (isPos) zeroY - barH else zeroY

                        drawRoundRect(
                            color = if (isSelected) barColor else barColor.copy(alpha = 0.85f),
                            topLeft = Offset(centerX - barWidth / 2f, topY),
                            size = Size(barWidth, barH),
                            cornerRadius = CornerRadius(5f, 5f),
                        )
                    } else if (item.tradeCount > 0) {
                        drawCircle(
                            color = tradingColors.neutral,
                            radius = 3.5f,
                            center = Offset(centerX, zeroY),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // X-Axis Labels aligned to bars
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 54.dp),
        ) {
            val step = when {
                items.size > 16 -> 3
                items.size > 12 -> 2
                else -> 1
            }
            items.forEachIndexed { idx, item ->
                val showLabel = idx % step == 0 || idx == selectedIndex || idx == items.lastIndex
                Text(
                    text = if (showLabel) item.shortLabel else "",
                    style = MonoTextStyles.tableCaption.copy(
                        fontSize = 10.sp,
                        fontWeight = if (idx == selectedIndex) FontWeight.Bold else FontWeight.Normal,
                    ),
                    color = if (idx == selectedIndex) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun EquityCurveChart(
    points: List<EquityCurvePoint>,
    modifier: Modifier = Modifier,
) {
    val tradingColors = LocalTradingColors.current
    val gridColor = tradingColors.tableGridBorder
    val zeroLineColor = MaterialTheme.colorScheme.outline
    val endVal = points.lastOrNull()?.cumulativePnl ?: 0.0
    val lineColor = if (endVal >= 0.0) tradingColors.profit else tradingColors.loss

    val minVal = remember(points) { minOf(0.0, points.minOf { it.cumulativePnl }) }
    val maxVal = remember(points) { maxOf(0.0, points.maxOf { it.cumulativePnl }) }
    val span = (maxVal - minVal).takeIf { it > 0.0 } ?: 1.0

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            Column(
                modifier = Modifier
                    .width(54.dp)
                    .fillMaxSize()
                    .padding(end = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = formatCompactPnl(maxVal),
                    style = MonoTextStyles.tableCaption.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatCompactPnl(minVal),
                    style = MonoTextStyles.tableCaption.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
            ) {
                val w = size.width
                val h = size.height
                val padV = 10f
                val plotH = (h - padV * 2f).coerceAtLeast(1f)

                fun yFor(value: Double): Float {
                    val norm = ((value - minVal) / span).toFloat()
                    return (h - padV) - norm * plotH
                }

                val zeroY = yFor(0.0)
                val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, padV),
                    end = Offset(w, padV),
                    strokeWidth = 1f,
                    pathEffect = dash,
                )
                drawLine(
                    color = zeroLineColor.copy(alpha = 0.65f),
                    start = Offset(0f, zeroY),
                    end = Offset(w, zeroY),
                    strokeWidth = 1.5f,
                )

                val coords = if (points.size == 1) {
                    listOf(
                        Offset(0f, zeroY),
                        Offset(w, yFor(points[0].cumulativePnl)),
                    )
                } else {
                    points.mapIndexed { idx, pt ->
                        val x = (idx.toFloat() / (points.size - 1).coerceAtLeast(1)) * w
                        Offset(x, yFor(pt.cumulativePnl))
                    }
                }

                val linePath = Path().apply {
                    coords.forEachIndexed { idx, pt ->
                        if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                    }
                }
                val areaPath = Path().apply {
                    addPath(linePath)
                    lineTo(coords.last().x, zeroY)
                    lineTo(coords.first().x, zeroY)
                    close()
                }

                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(lineColor.copy(alpha = 0.28f), lineColor.copy(alpha = 0.02f)),
                    ),
                )
                drawPath(
                    path = linePath,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )

                coords.forEachIndexed { idx, pt ->
                    if (points.size <= 14 || idx == coords.lastIndex) {
                        drawCircle(
                            color = lineColor,
                            radius = if (idx == coords.lastIndex) 5.dp.toPx() else 3.dp.toPx(),
                            center = pt,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 54.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = points.first().fullDateOrMonth,
                style = MonoTextStyles.tableCaption.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (points.size > 1) {
                Text(
                    text = points.last().fullDateOrMonth,
                    style = MonoTextStyles.tableCaption.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DailyCalendarHeatmapCard(
    yearMonth: String,
    dailyPoints: List<DailyPnlPoint>,
    onDayClick: (String) -> Unit,
) {
    val tradingColors = LocalTradingColors.current
    val pointByDay = remember(dailyPoints) { dailyPoints.associateBy { it.dateIso } }
    val daysInMonth = remember(yearMonth) { DateTimeUtils.daysInMonth(yearMonth) }
    val leadingBlanks = remember(yearMonth) { DateTimeUtils.firstWeekdayOffsetMondayZero(yearMonth) }
    val dayHeaders = listOf("M", "T", "W", "T", "F", "S", "S")

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.chart_calendar_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Tap day to view trades",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                dayHeaders.forEach { h ->
                    Text(
                        text = h,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            val totalCells = leadingBlanks + daysInMonth
            val rows = (totalCells + 6) / 7
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                for (r in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        for (c in 0 until 7) {
                            val cellIndex = r * 7 + c
                            val dayNum = cellIndex - leadingBlanks + 1
                            if (dayNum in 1..daysInMonth) {
                                val iso = "$yearMonth-${dayNum.toString().padStart(2, '0')}"
                                val stat = pointByDay[iso]
                                val bg = when {
                                    stat == null -> MaterialTheme.colorScheme.surfaceContainerLow
                                    stat.netPnl > 0.0 -> tradingColors.profitContainer
                                    stat.netPnl < 0.0 -> tradingColors.lossContainer
                                    else -> tradingColors.neutralContainer
                                }
                                val border = when {
                                    stat == null -> tradingColors.tableGridBorder.copy(alpha = 0.5f)
                                    stat.netPnl > 0.0 -> tradingColors.profit.copy(alpha = 0.5f)
                                    stat.netPnl < 0.0 -> tradingColors.loss.copy(alpha = 0.5f)
                                    else -> tradingColors.neutral.copy(alpha = 0.5f)
                                }
                                Surface(
                                    onClick = { onDayClick(iso) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = bg,
                                    border = BorderStroke(1.dp, border),
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(0.95f),
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(2.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                    ) {
                                        Text(
                                            text = dayNum.toString(),
                                            style = MonoTextStyles.tableCaption.copy(
                                                fontSize = 10.sp,
                                                fontWeight = if (stat != null) FontWeight.Bold else FontWeight.Normal,
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        if (stat != null) {
                                            Text(
                                                text = formatCompactPnl(stat.netPnl),
                                                style = MonoTextStyles.tableCaption.copy(
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                ),
                                                color = pnlColor(stat.netPnl),
                                                maxLines = 1,
                                            )
                                        }
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlyBreakdownCard(
    monthlyPoints: List<MonthlyPnlPoint>,
) {
    val tradingColors = LocalTradingColors.current
    val active = monthlyPoints.filter { it.tradeCount > 0 }
    if (active.isEmpty()) return

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = tradingColors.cardElevated,
        border = BorderStroke(1.dp, tradingColors.tableGridBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Monthly Summary Ledger",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(16.dp),
            )
            HorizontalDivider(color = tradingColors.tableGridBorder)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(tradingColors.tableHeaderBg)
                    .padding(horizontal = 16.dp, vertical = 9.dp),
            ) {
                Text("Month", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1.1f))
                Text("Trades", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, modifier = Modifier.weight(0.7f))
                Text("Win %", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, modifier = Modifier.weight(0.8f))
                Text("Net P&L", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End, modifier = Modifier.weight(1.2f))
            }
            active.forEachIndexed { idx, pt ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (idx % 2 == 1) tradingColors.tableRowAltBg else Color.Transparent)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = pt.shortLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1.1f),
                    )
                    Text(
                        text = pt.tradeCount.toString(),
                        style = MonoTextStyles.tableCaption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(0.7f),
                    )
                    Text(
                        text = formatPercent(pt.winRatePct),
                        style = MonoTextStyles.tableCaption,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(0.8f),
                    )
                    Text(
                        text = formatSignedPnl(pt.netPnl),
                        style = MonoTextStyles.tableValue.copy(fontWeight = FontWeight.Bold),
                        color = pnlColor(pt.netPnl),
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1.2f),
                    )
                }
                if (idx != active.lastIndex) {
                    HorizontalDivider(color = tradingColors.tableGridBorder.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
private fun EmptyVisualizationCard(onAddTrade: () -> Unit) {
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
                .padding(horizontal = 24.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Icon(
                    imageVector = Icons.Outlined.BarChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .padding(14.dp)
                        .size(28.dp),
                )
            }
            Text(
                text = stringResource(R.string.empty_charts_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.empty_charts_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = onAddTrade,
                modifier = Modifier.testTag("charts_empty_add_button"),
            ) {
                Text(stringResource(R.string.action_log_trade))
            }
        }
    }
}
