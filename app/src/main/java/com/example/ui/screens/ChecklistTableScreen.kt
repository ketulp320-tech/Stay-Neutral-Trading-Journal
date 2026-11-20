package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.SwipeRight
import androidx.compose.material.icons.outlined.TableRows
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ChecklistFactor
import com.example.data.SignalBias
import com.example.domain.DateTimeUtils
import com.example.domain.TradeEntry
import com.example.ui.DayTradeGroup
import com.example.ui.SheetFilterMode
import com.example.ui.StayNeutralUiState
import com.example.ui.components.biasContainerColor
import com.example.ui.components.biasTextColor
import com.example.ui.components.formatCompactPnl
import com.example.ui.components.formatPercent
import com.example.ui.components.formatSignedPnl
import com.example.ui.components.pnlColor
import com.example.ui.components.pnlContainerColor
import com.example.ui.theme.LocalTradingColors
import com.example.ui.theme.MonoTextStyles

// Fixed column widths so header, data rows, and subtotal footer stay 100% grid-aligned
private val ColDateWidth = 88.dp
private val ColTimeWidth = 66.dp
private val ColSignalWidth = 84.dp
private val ColOiWidth = 92.dp
private val ColEmaWidth = 92.dp
private val ColZoneWidth = 88.dp
private val ColResultWidth = 116.dp
private val TotalTableWidth: Dp =
    ColDateWidth + ColTimeWidth + ColSignalWidth * 3 + ColOiWidth + ColEmaWidth + ColZoneWidth + ColResultWidth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistTableScreen(
    uiState: StayNeutralUiState,
    onSelectDate: (String) -> Unit,
    onStepDay: (Int) -> Unit,
    onJumpToToday: () -> Unit,
    onSetFilterMode: (SheetFilterMode) -> Unit,
    onAddTradeForDate: (String) -> Unit,
    onEditTrade: (TradeEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val horizontalScrollState = rememberScrollState()
    val tradingColors = LocalTradingColors.current

    val displayedTrades = remember(uiState.visibleDayGroups) {
        uiState.visibleDayGroups.flatMap { it.trades }
    }
    val summaryNetPnl = remember(displayedTrades) { displayedTrades.sumOf { it.resultAmount } }
    val summaryWins = remember(displayedTrades) { displayedTrades.count { it.isWin } }
    val summaryLosses = remember(displayedTrades) { displayedTrades.count { it.isLoss } }
    val summaryDecided = summaryWins + summaryLosses
    val summaryWinRate = if (summaryDecided > 0) (summaryWins * 100.0) / summaryDecided else 0.0

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateTimeUtils.isoToUtcMillis(uiState.selectedDateIso),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            onSelectDate(DateTimeUtils.utcMillisToIso(millis))
                        }
                        showDatePicker = false
                    },
                    modifier = Modifier.testTag("confirm_date_filter_button"),
                ) {
                    Text(text = "Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("checklist_screen"),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        // Brand Header + Mode Switcher
        item(key = "header") {
            ChecklistHeaderBar(
                filterMode = uiState.sheetFilterMode,
                allTimeNetPnl = uiState.analytics.overall.netPnl,
                totalTrades = uiState.allTrades.size,
                onSetFilterMode = onSetFilterMode,
            )
        }

        // Day-wise date navigation strip (active in BY_DAY mode, quick-jump in ALL_DAYS)
        item(key = "day_navigator") {
            DayNavigatorSection(
                selectedDateIso = uiState.selectedDateIso,
                filterMode = uiState.sheetFilterMode,
                dayPnlMap = uiState.dayPnlMap,
                dayCountMap = uiState.dayCountMap,
                onSelectDate = onSelectDate,
                onStepDay = onStepDay,
                onJumpToToday = onJumpToToday,
                onOpenDatePicker = { showDatePicker = true },
            )
        }

        // Compact Period KPI Summary Card
        item(key = "summary_banner") {
            DaySummaryBanner(
                title = if (uiState.sheetFilterMode == SheetFilterMode.BY_DAY) {
                    DateTimeUtils.formatMediumDate(uiState.selectedDateIso)
                } else {
                    "All Logged Days (${uiState.visibleDayGroups.size} days)"
                },
                netPnl = summaryNetPnl,
                tradeCount = displayedTrades.size,
                wins = summaryWins,
                losses = summaryLosses,
                winRatePct = summaryWinRate,
            )
        }

        // Section Label & Horizontal Scroll Hint
        item(key = "table_caption") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "TRADING CHECKLIST SHEET",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SwipeRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = "Scroll columns • Tap row to edit",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Spreadsheet Checklist Table (Exact 9 columns from reference layout)
        item(key = "checklist_sheet_card") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .testTag("checklist_table_container"),
                shape = RoundedCornerShape(14.dp),
                color = tradingColors.cardElevated,
                border = BorderStroke(1.dp, tradingColors.tableGridBorder),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Horizontally scrollable grid with aligned Header + Rows + Footer
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(horizontalScrollState),
                    ) {
                        ChecklistTableHeaderRow()

                        if (uiState.visibleDayGroups.isEmpty()) {
                            // Empty table body inside the sheet so column headers stay immediately visible!
                            EmptyTablePromptRow(
                                isDayMode = uiState.sheetFilterMode == SheetFilterMode.BY_DAY,
                                selectedDateIso = uiState.selectedDateIso,
                                onAddClick = { onAddTradeForDate(uiState.selectedDateIso) },
                            )
                        } else {
                            uiState.visibleDayGroups.forEach { group ->
                                if (uiState.sheetFilterMode == SheetFilterMode.ALL_DAYS) {
                                    DayGroupDividerRow(
                                        group = group,
                                        onAddForDay = { onAddTradeForDate(group.dateIso) },
                                    )
                                }
                                group.trades.forEachIndexed { index, trade ->
                                    ChecklistDataRow(
                                        trade = trade,
                                        isAlt = index % 2 == 1,
                                        onClick = { onEditTrade(trade) },
                                    )
                                }
                            }

                            // Totals Footer Row
                            ChecklistTotalsRow(
                                label = if (uiState.sheetFilterMode == SheetFilterMode.BY_DAY) {
                                    "Day Total (${displayedTrades.size})"
                                } else {
                                    "Net Total (${displayedTrades.size})"
                                },
                                netPnl = summaryNetPnl,
                            )
                        }
                    }

                    // Bottom inline action bar to add a new row for the active date
                    HorizontalDivider(color = tradingColors.tableGridBorder)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAddTradeForDate(uiState.selectedDateIso) }
                            .minimumInteractiveComponentSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .testTag("inline_add_trade_row"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uiState.sheetFilterMode == SheetFilterMode.BY_DAY) {
                                "Add Entry for ${DateTimeUtils.formatShortDate(uiState.selectedDateIso)}"
                            } else {
                                "Add New Trade Entry"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChecklistHeaderBar(
    filterMode: SheetFilterMode,
    allTimeNetPnl: Double,
    totalTrades: Int,
    onSetFilterMode: (SheetFilterMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (totalTrades > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = pnlContainerColor(allTimeNetPnl),
                    ) {
                        Text(
                            text = formatCompactPnl(allTimeNetPnl),
                            style = MonoTextStyles.tableCaption.copy(fontWeight = FontWeight.Bold),
                            color = pnlColor(allTimeNetPnl),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        )
                    }
                }
            }
            Text(
                text = stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Compact By Day / All Days toggle
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(modifier = Modifier.padding(3.dp)) {
                FilterChipButton(
                    label = stringResource(R.string.action_day_view),
                    selected = filterMode == SheetFilterMode.BY_DAY,
                    onClick = { onSetFilterMode(SheetFilterMode.BY_DAY) },
                    testTag = "filter_by_day_button",
                )
                FilterChipButton(
                    label = stringResource(R.string.action_all_days),
                    selected = filterMode == SheetFilterMode.ALL_DAYS,
                    onClick = { onSetFilterMode(SheetFilterMode.ALL_DAYS) },
                    testTag = "filter_all_days_button",
                )
            }
        }
    }
}

@Composable
private fun FilterChipButton(
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
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
        )
    }
}

@Composable
private fun DayNavigatorSection(
    selectedDateIso: String,
    filterMode: SheetFilterMode,
    dayPnlMap: Map<String, Double>,
    dayCountMap: Map<String, Int>,
    onSelectDate: (String) -> Unit,
    onStepDay: (Int) -> Unit,
    onJumpToToday: () -> Unit,
    onOpenDatePicker: () -> Unit,
) {
    val todayIso = remember { DateTimeUtils.todayIso() }
    val stripDates = remember(selectedDateIso) {
        DateTimeUtils.recentDateStrip(selectedDateIso, daysBefore = 5, daysAfter = 3)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Date stepper bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onStepDay(-1) },
                    modifier = Modifier.testTag("prev_day_button"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronLeft,
                        contentDescription = stringResource(R.string.action_prev_day),
                    )
                }

                Surface(
                    onClick = onOpenDatePicker,
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.testTag("open_date_picker_button"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = stringResource(R.string.action_pick_date),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = DateTimeUtils.formatMediumDate(selectedDateIso),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                IconButton(
                    onClick = { onStepDay(1) },
                    modifier = Modifier.testTag("next_day_button"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = stringResource(R.string.action_next_day),
                    )
                }
            }

            if (selectedDateIso != todayIso || filterMode == SheetFilterMode.ALL_DAYS) {
                TextButton(
                    onClick = onJumpToToday,
                    modifier = Modifier.testTag("jump_today_button"),
                ) {
                    Text(text = stringResource(R.string.action_today))
                }
            }
        }

        // Quick horizontal day pills with profit/loss dot indicators
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(stripDates, key = { it }) { dateIso ->
                val isSelected = filterMode == SheetFilterMode.BY_DAY && dateIso == selectedDateIso
                val hasTrades = (dayCountMap[dateIso] ?: 0) > 0
                val dayPnl = dayPnlMap[dateIso] ?: 0.0
                val isToday = dateIso == todayIso

                Surface(
                    onClick = { onSelectDate(dateIso) },
                    shape = RoundedCornerShape(12.dp),
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                            else -> MaterialTheme.colorScheme.outlineVariant
                        },
                    ),
                    modifier = Modifier
                        .width(54.dp)
                        .testTag("day_pill_$dateIso"),
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 7.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = DateTimeUtils.formatDayOfWeekShort(dateIso).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = DateTimeUtils.dayOfMonthNumber(dateIso),
                            style = MonoTextStyles.kpiValue.copy(fontSize = 15.sp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (hasTrades) pnlColor(dayPnl) else Color.Transparent,
                                ),
                        )
                    }
                }
            }
        }
    }
}

private val Int.sp
    get() = androidx.compose.ui.unit.TextUnit(this.toFloat(), androidx.compose.ui.unit.TextUnitType.Sp)

@Composable
private fun DaySummaryBanner(
    title: String,
    netPnl: Double,
    tradeCount: Int,
    wins: Int,
    losses: Int,
    winRatePct: Double,
) {
    val tradingColors = LocalTradingColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        color = tradingColors.cardElevated,
        border = BorderStroke(1.dp, tradingColors.tableGridBorder),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (tradeCount == 0) "₹0" else formatSignedPnl(netPnl),
                    style = MonoTextStyles.heroNumber,
                    color = if (tradeCount == 0) MaterialTheme.colorScheme.onSurfaceVariant else pnlColor(netPnl),
                    modifier = Modifier.testTag("day_summary_net_pnl"),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                MiniMetric(
                    label = "TRADES",
                    value = tradeCount.toString(),
                    valueColor = MaterialTheme.colorScheme.onSurface,
                )
                MiniMetric(
                    label = "W / L",
                    value = "$wins/$losses",
                    valueColor = MaterialTheme.colorScheme.onSurface,
                )
                MiniMetric(
                    label = "WIN %",
                    value = if (wins + losses == 0) "—" else formatPercent(winRatePct),
                    valueColor = if (wins + losses == 0) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else if (winRatePct >= 50.0) {
                        tradingColors.profit
                    } else {
                        tradingColors.loss
                    },
                )
            }
        }
    }
}

@Composable
private fun MiniMetric(
    label: String,
    value: String,
    valueColor: Color,
) {
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MonoTextStyles.kpiValue.copy(fontSize = 14.sp),
            color = valueColor,
        )
    }
}

@Composable
private fun ChecklistTableHeaderRow() {
    val tradingColors = LocalTradingColors.current
    Row(
        modifier = Modifier
            .width(TotalTableWidth)
            .background(tradingColors.tableHeaderBg)
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderCell(text = stringResource(R.string.col_date), width = ColDateWidth, align = TextAlign.Start)
        HeaderCell(text = stringResource(R.string.col_time), width = ColTimeWidth, align = TextAlign.Center)
        HeaderCell(text = stringResource(R.string.col_fii), width = ColSignalWidth, align = TextAlign.Center)
        HeaderCell(text = stringResource(R.string.col_global), width = ColSignalWidth, align = TextAlign.Center)
        HeaderCell(text = stringResource(R.string.col_pcr), width = ColSignalWidth, align = TextAlign.Center)
        HeaderCell(text = stringResource(R.string.col_oi_high), width = ColOiWidth, align = TextAlign.Center)
        HeaderCell(text = stringResource(R.string.col_ema_signal), width = ColEmaWidth, align = TextAlign.Center)
        HeaderCell(text = stringResource(R.string.col_zone), width = ColZoneWidth, align = TextAlign.Center)
        HeaderCell(text = stringResource(R.string.col_result), width = ColResultWidth, align = TextAlign.End)
    }
    HorizontalDivider(color = tradingColors.tableGridBorder)
}

@Composable
private fun HeaderCell(
    text: String,
    width: Dp,
    align: TextAlign,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = align,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .width(width)
            .padding(horizontal = 8.dp),
    )
}

@Composable
private fun DayGroupDividerRow(
    group: DayTradeGroup,
    onAddForDay: () -> Unit,
) {
    val tradingColors = LocalTradingColors.current
    Row(
        modifier = Modifier
            .width(TotalTableWidth)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = DateTimeUtils.formatMediumDate(group.dateIso),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${group.trades.size} trade${if (group.trades.size == 1) "" else "s"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = pnlContainerColor(group.dayNetPnl),
            ) {
                Text(
                    text = "Day: ${formatSignedPnl(group.dayNetPnl)}",
                    style = MonoTextStyles.tableCaption.copy(fontWeight = FontWeight.Bold),
                    color = pnlColor(group.dayNetPnl),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }

        Text(
            text = "+ Add",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onAddForDay)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
    HorizontalDivider(color = tradingColors.tableGridBorder)
}

@Composable
private fun ChecklistDataRow(
    trade: TradeEntry,
    isAlt: Boolean,
    onClick: () -> Unit,
) {
    val tradingColors = LocalTradingColors.current
    Row(
        modifier = Modifier
            .width(TotalTableWidth)
            .background(if (isAlt) tradingColors.tableRowAltBg else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
            .testTag("trade_row_${trade.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Date
        Text(
            text = DateTimeUtils.formatCompactDate(trade.dateIso),
            style = MonoTextStyles.tableCaption,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier
                .width(ColDateWidth)
                .padding(horizontal = 8.dp),
        )

        // Time
        Text(
            text = trade.timeHm,
            style = MonoTextStyles.tableValue,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .width(ColTimeWidth)
                .padding(horizontal = 6.dp),
        )

        // FII
        SignalTableCell(
            bias = trade.fii,
            label = ChecklistFactor.FII.labelFor(trade.fii),
            width = ColSignalWidth,
        )

        // Global
        SignalTableCell(
            bias = trade.global,
            label = ChecklistFactor.GLOBAL.labelFor(trade.global),
            width = ColSignalWidth,
        )

        // PCR
        SignalTableCell(
            bias = trade.pcr,
            label = ChecklistFactor.PCR.labelFor(trade.pcr),
            width = ColSignalWidth,
        )

        // OI High
        SignalTableCell(
            bias = trade.oiHigh,
            label = ChecklistFactor.OI_HIGH.labelFor(trade.oiHigh),
            width = ColOiWidth,
        )

        // EMA Signal
        SignalTableCell(
            bias = trade.emaSignal,
            label = ChecklistFactor.EMA_SIGNAL.labelFor(trade.emaSignal),
            width = ColEmaWidth,
        )

        // Zone
        SignalTableCell(
            bias = trade.zone,
            label = ChecklistFactor.ZONE.labelFor(trade.zone),
            width = ColZoneWidth,
        )

        // Result
        Box(
            modifier = Modifier
                .width(ColResultWidth)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Text(
                text = formatSignedPnl(trade.resultAmount),
                style = MonoTextStyles.tableValue.copy(fontWeight = FontWeight.Bold),
                color = pnlColor(trade.resultAmount),
                maxLines = 1,
            )
        }
    }
    HorizontalDivider(color = tradingColors.tableGridBorder.copy(alpha = 0.65f))
}

@Composable
private fun SignalTableCell(
    bias: SignalBias,
    label: String,
    width: Dp,
) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = biasContainerColor(bias),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = biasTextColor(bias),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun ChecklistTotalsRow(
    label: String,
    netPnl: Double,
) {
    val tradingColors = LocalTradingColors.current
    Row(
        modifier = Modifier
            .width(TotalTableWidth)
            .background(tradingColors.tableHeaderBg.copy(alpha = 0.7f))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = formatSignedPnl(netPnl),
            style = MonoTextStyles.kpiValue.copy(fontSize = 14.sp),
            color = pnlColor(netPnl),
        )
    }
}

@Composable
private fun EmptyTablePromptRow(
    isDayMode: Boolean,
    selectedDateIso: String,
    onAddClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(TotalTableWidth)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Icon(
                    imageVector = Icons.Outlined.TableRows,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(22.dp),
                )
            }
            Column {
                Text(
                    text = if (isDayMode) {
                        stringResource(R.string.empty_day_title)
                    } else {
                        stringResource(R.string.empty_all_title)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isDayMode) {
                        stringResource(R.string.empty_day_subtitle)
                    } else {
                        stringResource(R.string.empty_all_subtitle)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(300.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onAddClick,
            modifier = Modifier.testTag("empty_state_add_trade_button"),
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isDayMode) {
                    "Log Trade for ${DateTimeUtils.formatShortDate(selectedDateIso)}"
                } else {
                    stringResource(R.string.action_log_trade)
                },
            )
        }
    }
}
