package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.screens.AnalysisScreen
import com.example.ui.screens.ChecklistTableScreen
import com.example.ui.screens.PnlChartsScreen
import com.example.ui.screens.TradeEntrySheet

@Composable
fun StayNeutralApp(
    viewModel: StayNeutralViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    // Back navigation from secondary tabs returns cleanly to Checklist
    BackHandler(enabled = uiState.selectedTab != AppTab.CHECKLIST) {
        viewModel.selectTab(AppTab.CHECKLIST)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.openNewTradeSheet() },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.action_log_trade),
                        )
                    },
                    text = { Text(stringResource(R.string.action_log_trade)) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("fab_log_trade"),
                )
            },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        NavItems(
                            selectedTab = uiState.selectedTab,
                            onSelectTab = viewModel::selectTab,
                        ).forEach { item ->
                            NavigationBarItem(
                                selected = uiState.selectedTab == item.tab,
                                onClick = { viewModel.selectTab(item.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (uiState.selectedTab == item.tab) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label,
                                    )
                                },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag("nav_${item.tab.name.lowercase()}"),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight(),
                    ) {
                        NavItems(
                            selectedTab = uiState.selectedTab,
                            onSelectTab = viewModel::selectTab,
                        ).forEach { item ->
                            NavigationRailItem(
                                selected = uiState.selectedTab == item.tab,
                                onClick = { viewModel.selectTab(item.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (uiState.selectedTab == item.tab) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label,
                                    )
                                },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag("nav_rail_${item.tab.name.lowercase()}"),
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    val contentMod = Modifier
                        .fillMaxSize()
                        .widthIn(max = 920.dp)

                    when (uiState.selectedTab) {
                        AppTab.CHECKLIST -> ChecklistTableScreen(
                            uiState = uiState,
                            onSelectDate = viewModel::selectDate,
                            onStepDay = viewModel::stepDay,
                            onJumpToToday = viewModel::jumpToToday,
                            onSetFilterMode = viewModel::setSheetFilterMode,
                            onAddTradeForDate = { dateIso -> viewModel.openNewTradeSheet(dateIso) },
                            onEditTrade = viewModel::openEditTradeSheet,
                            modifier = contentMod,
                        )

                        AppTab.CHARTS -> PnlChartsScreen(
                            uiState = uiState,
                            onSetPeriodMode = viewModel::setChartPeriodMode,
                            onStepMonth = viewModel::stepChartMonth,
                            onStepYear = viewModel::stepChartYear,
                            onInspectDay = viewModel::inspectDayFromChart,
                            onAddTrade = { viewModel.openNewTradeSheet() },
                            modifier = contentMod,
                        )

                        AppTab.ANALYSIS -> AnalysisScreen(
                            uiState = uiState,
                            onAddTrade = { viewModel.openNewTradeSheet() },
                            modifier = contentMod,
                        )
                    }
                }
            }
        }
    }

    if (uiState.editorState.isOpen) {
        TradeEntrySheet(
            editorState = uiState.editorState,
            onDismiss = viewModel::dismissTradeSheet,
            onSave = viewModel::saveTrade,
            onDelete = viewModel::deleteTrade,
        )
    }
}

private data class NavTabItem(
    val tab: AppTab,
    val label: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
)

@Composable
private fun NavItems(
    selectedTab: AppTab,
    onSelectTab: (AppTab) -> Unit,
): List<NavTabItem> = listOf(
    NavTabItem(
        tab = AppTab.CHECKLIST,
        label = stringResource(R.string.nav_sheet),
        selectedIcon = Icons.Filled.TableChart,
        unselectedIcon = Icons.Outlined.TableChart,
    ),
    NavTabItem(
        tab = AppTab.CHARTS,
        label = stringResource(R.string.nav_charts),
        selectedIcon = Icons.Filled.BarChart,
        unselectedIcon = Icons.Outlined.BarChart,
    ),
    NavTabItem(
        tab = AppTab.ANALYSIS,
        label = stringResource(R.string.nav_analysis),
        selectedIcon = Icons.Filled.Analytics,
        unselectedIcon = Icons.Outlined.Analytics,
    ),
)
