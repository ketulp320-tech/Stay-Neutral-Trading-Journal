package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.TradeRepository
import com.example.domain.DailyPnlPoint
import com.example.domain.DateTimeUtils
import com.example.domain.EquityCurvePoint
import com.example.domain.FullAnalyticsReport
import com.example.domain.MonthlyPnlPoint
import com.example.domain.TradeAnalyticsEngine
import com.example.domain.TradeEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppTab { CHECKLIST, CHARTS, ANALYSIS }

enum class SheetFilterMode { BY_DAY, ALL_DAYS }

enum class ChartPeriodMode { DAILY, MONTHLY }

data class DayTradeGroup(
    val dateIso: String,
    val trades: List<TradeEntry>,
    val dayNetPnl: Double,
    val wins: Int,
    val losses: Int,
)

data class TradeEditorState(
    val isOpen: Boolean = false,
    val editingTrade: TradeEntry? = null,
    val initialDateIso: String = DateTimeUtils.todayIso(),
)

data class StayNeutralUiState(
    val selectedTab: AppTab = AppTab.CHECKLIST,
    val selectedDateIso: String = DateTimeUtils.todayIso(),
    val sheetFilterMode: SheetFilterMode = SheetFilterMode.BY_DAY,
    val chartPeriodMode: ChartPeriodMode = ChartPeriodMode.DAILY,
    val selectedChartMonth: String = DateTimeUtils.currentYearMonth(),
    val selectedChartYear: Int = DateTimeUtils.currentYear(),
    val editorState: TradeEditorState = TradeEditorState(),
    val allTrades: List<TradeEntry> = emptyList(),
    val visibleDayGroups: List<DayTradeGroup> = emptyList(),
    val dayPnlMap: Map<String, Double> = emptyMap(),
    val dayCountMap: Map<String, Int> = emptyMap(),
    val dailyChartPoints: List<DailyPnlPoint> = emptyList(),
    val dailyEquityPoints: List<EquityCurvePoint> = emptyList(),
    val monthlyChartPoints: List<MonthlyPnlPoint> = emptyList(),
    val monthlyEquityPoints: List<EquityCurvePoint> = emptyList(),
    val analytics: FullAnalyticsReport = TradeAnalyticsEngine.computeFullReport(emptyList()),
    val snackbarMessage: String? = null,
)

private data class UiControlState(
    val selectedTab: AppTab = AppTab.CHECKLIST,
    val selectedDateIso: String = DateTimeUtils.todayIso(),
    val sheetFilterMode: SheetFilterMode = SheetFilterMode.BY_DAY,
    val chartPeriodMode: ChartPeriodMode = ChartPeriodMode.DAILY,
    val selectedChartMonth: String = DateTimeUtils.currentYearMonth(),
    val selectedChartYear: Int = DateTimeUtils.currentYear(),
    val editorState: TradeEditorState = TradeEditorState(),
    val snackbarMessage: String? = null,
)

class StayNeutralViewModel(
    private val repository: TradeRepository,
) : ViewModel() {

    private val controls = MutableStateFlow(UiControlState())

    val uiState: StateFlow<StayNeutralUiState> = combine(
        repository.allTrades,
        controls,
    ) { trades, ctrl ->
        val byDate = trades.groupBy { it.dateIso }
        val dayPnlMap = byDate.mapValues { (_, list) -> list.sumOf { it.resultAmount } }
        val dayCountMap = byDate.mapValues { (_, list) -> list.size }

        val visibleGroups = when (ctrl.sheetFilterMode) {
            SheetFilterMode.BY_DAY -> {
                val list = byDate[ctrl.selectedDateIso].orEmpty()
                    .sortedWith(compareBy<TradeEntry> { it.timeHm }.thenBy { it.id })
                if (list.isEmpty()) {
                    emptyList()
                } else {
                    listOf(
                        DayTradeGroup(
                            dateIso = ctrl.selectedDateIso,
                            trades = list,
                            dayNetPnl = list.sumOf { it.resultAmount },
                            wins = list.count { it.isWin },
                            losses = list.count { it.isLoss },
                        ),
                    )
                }
            }

            SheetFilterMode.ALL_DAYS -> {
                byDate.entries
                    .sortedByDescending { it.key }
                    .map { (dateIso, list) ->
                        val sorted = list.sortedWith(compareBy<TradeEntry> { it.timeHm }.thenBy { it.id })
                        DayTradeGroup(
                            dateIso = dateIso,
                            trades = sorted,
                            dayNetPnl = sorted.sumOf { it.resultAmount },
                            wins = sorted.count { it.isWin },
                            losses = sorted.count { it.isLoss },
                        )
                    }
            }
        }

        val dailyPoints = TradeAnalyticsEngine.computeDailySeriesForMonth(trades, ctrl.selectedChartMonth)
        val dailyEquity = TradeAnalyticsEngine.computeDailyEquityCurve(dailyPoints)
        val monthlyPoints = TradeAnalyticsEngine.computeMonthlySeriesForYear(trades, ctrl.selectedChartYear)
        val monthlyEquity = TradeAnalyticsEngine.computeMonthlyEquityCurve(monthlyPoints)
        val report = TradeAnalyticsEngine.computeFullReport(trades)

        StayNeutralUiState(
            selectedTab = ctrl.selectedTab,
            selectedDateIso = ctrl.selectedDateIso,
            sheetFilterMode = ctrl.sheetFilterMode,
            chartPeriodMode = ctrl.chartPeriodMode,
            selectedChartMonth = ctrl.selectedChartMonth,
            selectedChartYear = ctrl.selectedChartYear,
            editorState = ctrl.editorState,
            allTrades = trades,
            visibleDayGroups = visibleGroups,
            dayPnlMap = dayPnlMap,
            dayCountMap = dayCountMap,
            dailyChartPoints = dailyPoints,
            dailyEquityPoints = dailyEquity,
            monthlyChartPoints = monthlyPoints,
            monthlyEquityPoints = monthlyEquity,
            analytics = report,
            snackbarMessage = ctrl.snackbarMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StayNeutralUiState(),
    )

    fun selectTab(tab: AppTab) {
        controls.update { it.copy(selectedTab = tab) }
    }

    fun selectDate(dateIso: String) {
        controls.update {
            it.copy(
                selectedDateIso = dateIso,
                sheetFilterMode = SheetFilterMode.BY_DAY,
            )
        }
    }

    fun stepDay(delta: Int) {
        controls.update {
            it.copy(
                selectedDateIso = DateTimeUtils.addDays(it.selectedDateIso, delta),
                sheetFilterMode = SheetFilterMode.BY_DAY,
            )
        }
    }

    fun jumpToToday() {
        val today = DateTimeUtils.todayIso()
        controls.update {
            it.copy(
                selectedDateIso = today,
                sheetFilterMode = SheetFilterMode.BY_DAY,
            )
        }
    }

    fun setSheetFilterMode(mode: SheetFilterMode) {
        controls.update { it.copy(sheetFilterMode = mode) }
    }

    fun openNewTradeSheet(dateIso: String? = null) {
        controls.update {
            it.copy(
                editorState = TradeEditorState(
                    isOpen = true,
                    editingTrade = null,
                    initialDateIso = dateIso ?: it.selectedDateIso,
                ),
            )
        }
    }

    fun openEditTradeSheet(trade: TradeEntry) {
        controls.update {
            it.copy(
                editorState = TradeEditorState(
                    isOpen = true,
                    editingTrade = trade,
                    initialDateIso = trade.dateIso,
                ),
            )
        }
    }

    fun dismissTradeSheet() {
        controls.update { it.copy(editorState = it.editorState.copy(isOpen = false)) }
    }

    fun saveTrade(entry: TradeEntry) {
        viewModelScope.launch {
            val isUpdate = entry.id != 0L
            repository.saveTrade(entry)
            controls.update {
                it.copy(
                    selectedDateIso = entry.dateIso,
                    selectedChartMonth = entry.yearMonth,
                    selectedChartYear = entry.year,
                    editorState = it.editorState.copy(isOpen = false, editingTrade = null),
                    snackbarMessage = if (isUpdate) "Trade entry updated" else "Trade logged for ${DateTimeUtils.formatShortDate(entry.dateIso)}",
                )
            }
        }
    }

    fun deleteTrade(id: Long) {
        viewModelScope.launch {
            repository.deleteTrade(id)
            controls.update {
                it.copy(
                    editorState = it.editorState.copy(isOpen = false, editingTrade = null),
                    snackbarMessage = "Trade entry deleted",
                )
            }
        }
    }

    fun setChartPeriodMode(mode: ChartPeriodMode) {
        controls.update { it.copy(chartPeriodMode = mode) }
    }

    fun stepChartMonth(delta: Int) {
        controls.update {
            it.copy(selectedChartMonth = DateTimeUtils.addMonths(it.selectedChartMonth, delta))
        }
    }

    fun stepChartYear(delta: Int) {
        controls.update {
            it.copy(selectedChartYear = (it.selectedChartYear + delta).coerceIn(2000, 2100))
        }
    }

    fun inspectDayFromChart(dateIso: String) {
        controls.update {
            it.copy(
                selectedDateIso = dateIso,
                sheetFilterMode = SheetFilterMode.BY_DAY,
                selectedTab = AppTab.CHECKLIST,
            )
        }
    }

    fun clearSnackbar() {
        controls.update { it.copy(snackbarMessage = null) }
    }

    class Factory(private val repository: TradeRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StayNeutralViewModel(repository) as T
        }
    }
}
