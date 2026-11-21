package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ChecklistFactor
import com.example.data.SignalBias
import com.example.data.TradeInstrument
import com.example.data.evaluateConfluence
import com.example.domain.DateTimeUtils
import com.example.domain.TradeEntry
import com.example.ui.TradeEditorState
import com.example.ui.components.biasAccentColor
import com.example.ui.components.biasContainerColor
import com.example.ui.components.biasTextColor
import com.example.ui.theme.LocalTradingColors
import com.example.ui.theme.MonoTextStyles
import kotlin.math.abs

private enum class ResultSign { PROFIT, FLAT, LOSS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeEntrySheet(
    editorState: TradeEditorState,
    onDismiss: () -> Unit,
    onSave: (TradeEntry) -> Unit,
    onDelete: (Long) -> Unit,
) {
    val existing = editorState.editingTrade
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val tradingColors = LocalTradingColors.current

    var dateIso by remember(existing, editorState.initialDateIso) {
        mutableStateOf(existing?.dateIso ?: editorState.initialDateIso)
    }

    val initialHm = remember(existing) {
        DateTimeUtils.parseHourMinute(existing?.timeHm ?: DateTimeUtils.currentTimeHm())
    }
    var hourText by remember(existing) {
        mutableStateOf(initialHm.first.toString().padStart(2, '0'))
    }
    var minuteText by remember(existing) {
        mutableStateOf(initialHm.second.toString().padStart(2, '0'))
    }
    var timeError by remember { mutableStateOf<String?>(null) }

    var tradeName by remember(existing) { mutableStateOf(existing?.tradeName ?: TradeInstrument.NIFTY) }
    var fii by remember(existing) { mutableStateOf(existing?.fii ?: SignalBias.NEUTRAL) }
    var global by remember(existing) { mutableStateOf(existing?.global ?: SignalBias.NEUTRAL) }
    var pcr by remember(existing) { mutableStateOf(existing?.pcr ?: SignalBias.NEUTRAL) }
    var oiHigh by remember(existing) { mutableStateOf(existing?.oiHigh ?: SignalBias.NEUTRAL) }
    var emaSignal by remember(existing) { mutableStateOf(existing?.emaSignal ?: SignalBias.NEUTRAL) }
    var zone by remember(existing) { mutableStateOf(existing?.zone ?: SignalBias.NEUTRAL) }

    var resultSign by remember(existing) {
        mutableStateOf(
            when {
                existing == null -> ResultSign.PROFIT
                existing.resultAmount > 0.0 -> ResultSign.PROFIT
                existing.resultAmount < 0.0 -> ResultSign.LOSS
                else -> ResultSign.FLAT
            },
        )
    }
    var amountText by remember(existing) {
        mutableStateOf(
            if (existing == null || existing.resultAmount == 0.0) {
                ""
            } else {
                val absVal = abs(existing.resultAmount)
                if (absVal % 1.0 == 0.0) absVal.toLong().toString() else absVal.toString()
            },
        )
    }
    var remarkText by remember(existing) {
        mutableStateOf(existing?.remark ?: "")
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val confluence = remember(fii, global, pcr, oiHigh, emaSignal, zone) {
        evaluateConfluence(fii, global, pcr, oiHigh, emaSignal, zone)
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateTimeUtils.isoToUtcMillis(dateIso),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            dateIso = DateTimeUtils.utcMillisToIso(millis)
                        }
                        showDatePicker = false
                    },
                ) {
                    Text("Set Date")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showTimePicker) {
        val initH = hourText.toIntOrNull()?.coerceIn(0, 23) ?: 9
        val initM = minuteText.toIntOrNull()?.coerceIn(0, 59) ?: 30
        val timeState = rememberTimePickerState(
            initialHour = initH,
            initialMinute = initM,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(text = stringResource(R.string.action_pick_time)) },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        hourText = timeState.hour.toString().padStart(2, '0')
                        minuteText = timeState.minute.toString().padStart(2, '0')
                        timeError = null
                        showTimePicker = false
                    },
                ) {
                    Text("Set Time")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (showDeleteConfirm && existing != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(existing.id)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = tradingColors.loss),
                    modifier = Modifier.testTag("confirm_delete_trade_button"),
                ) {
                    Text(stringResource(R.string.action_delete_entry))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .testTag("trade_entry_sheet"),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Sheet Title Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = if (existing == null) {
                            stringResource(R.string.sheet_title_new)
                        } else {
                            stringResource(R.string.sheet_title_edit)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Date • Time • Trade Name • 6 Factors • Result (₹) • Remark",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (existing != null) {
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.testTag("delete_trade_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = stringResource(R.string.action_delete_entry),
                            tint = tradingColors.loss,
                        )
                    }
                }
            }

            // 1. Date & Manual Time Entry
            Text(
                text = stringResource(R.string.section_schedule).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ScheduleFieldCard(
                label = stringResource(R.string.col_date),
                value = DateTimeUtils.formatMediumDate(dateIso),
                icon = Icons.Outlined.CalendarMonth,
                onClick = { showDatePicker = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("entry_date_picker_field"),
            )

            // Manual Time Input (HH : MM) + Now + optional Clock Picker
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (timeError != null) tradingColors.loss else MaterialTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AccessTime,
                                contentDescription = stringResource(R.string.col_time),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "Time (Manual 24h HH:MM)",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            TextButton(
                                onClick = {
                                    val (h, m) = DateTimeUtils.parseHourMinute(DateTimeUtils.currentTimeHm())
                                    hourText = h.toString().padStart(2, '0')
                                    minuteText = m.toString().padStart(2, '0')
                                    timeError = null
                                },
                                modifier = Modifier.testTag("time_set_now_button"),
                            ) {
                                Text(stringResource(R.string.action_now))
                            }
                            IconButton(
                                onClick = { showTimePicker = true },
                                modifier = Modifier.testTag("entry_time_picker_field"),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Schedule,
                                    contentDescription = stringResource(R.string.action_pick_time),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = hourText,
                            onValueChange = { raw ->
                                val digits = raw.filter { it.isDigit() }.take(2)
                                hourText = digits
                                timeError = null
                            },
                            label = { Text("Hour (00–23)") },
                            placeholder = { Text("09") },
                            textStyle = MonoTextStyles.kpiValue.copy(textAlign = TextAlign.Center),
                            singleLine = true,
                            isError = timeError != null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("manual_hour_input"),
                        )

                        Text(
                            text = ":",
                            style = MonoTextStyles.heroNumber,
                            color = MaterialTheme.colorScheme.primary,
                        )

                        OutlinedTextField(
                            value = minuteText,
                            onValueChange = { raw ->
                                val digits = raw.filter { it.isDigit() }.take(2)
                                minuteText = digits
                                timeError = null
                            },
                            label = { Text("Min (00–59)") },
                            placeholder = { Text("30") },
                            textStyle = MonoTextStyles.kpiValue.copy(textAlign = TextAlign.Center),
                            singleLine = true,
                            isError = timeError != null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("manual_minute_input"),
                        )
                    }

                    timeError?.let { err ->
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = tradingColors.loss,
                        )
                    }
                }
            }

            // Trade Name Selector (Nifty / Sensex / Stock)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(R.string.col_trade_name).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TradeInstrument.entries.forEach { instrument ->
                        val isSelected = tradeName == instrument
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { tradeName = instrument }
                                .testTag("trade_name_${instrument.name.lowercase()}"),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = instrument.displayName,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    ),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }

            // 2. Live Stay Neutral Confluence Meter
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = biasContainerColor(confluence.dominantBias),
                border = BorderStroke(1.dp, biasAccentColor(confluence.dominantBias).copy(alpha = 0.45f)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = when (confluence.dominantBias) {
                            SignalBias.BULLISH -> Icons.Outlined.TrendingUp
                            SignalBias.BEARISH -> Icons.Outlined.TrendingDown
                            SignalBias.NEUTRAL -> Icons.Outlined.Balance
                        },
                        contentDescription = null,
                        tint = biasAccentColor(confluence.dominantBias),
                        modifier = Modifier.size(24.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = confluence.verdictTitle,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = biasTextColor(confluence.dominantBias),
                            modifier = Modifier.testTag("confluence_verdict_text"),
                        )
                        Text(
                            text = confluence.verdictDetail,
                            style = MaterialTheme.typography.bodySmall,
                            color = biasTextColor(confluence.dominantBias).copy(alpha = 0.85f),
                        )
                    }
                }
            }

            // 3. The Exact 6 Checklist Factors
            Text(
                text = stringResource(R.string.section_checklist).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            FactorSelectorRow(
                factor = ChecklistFactor.FII,
                selected = fii,
                onSelect = { fii = it },
            )
            FactorSelectorRow(
                factor = ChecklistFactor.GLOBAL,
                selected = global,
                onSelect = { global = it },
            )
            FactorSelectorRow(
                factor = ChecklistFactor.PCR,
                selected = pcr,
                onSelect = { pcr = it },
            )
            FactorSelectorRow(
                factor = ChecklistFactor.OI_HIGH,
                selected = oiHigh,
                onSelect = { oiHigh = it },
            )
            FactorSelectorRow(
                factor = ChecklistFactor.EMA_SIGNAL,
                selected = emaSignal,
                onSelect = { emaSignal = it },
            )
            FactorSelectorRow(
                factor = ChecklistFactor.ZONE,
                selected = zone,
                onSelect = { zone = it },
            )

            // 4. Trade Result Section (INR ₹)
            Text(
                text = "${stringResource(R.string.section_outcome).uppercase()} (INR ₹)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ResultSignButton(
                    label = stringResource(R.string.result_profit),
                    selected = resultSign == ResultSign.PROFIT,
                    activeBg = tradingColors.profitContainer,
                    activeFg = tradingColors.onProfitContainer,
                    activeBorder = tradingColors.profit,
                    onClick = { resultSign = ResultSign.PROFIT },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("result_sign_profit"),
                )
                ResultSignButton(
                    label = stringResource(R.string.result_breakeven),
                    selected = resultSign == ResultSign.FLAT,
                    activeBg = tradingColors.neutralContainer,
                    activeFg = tradingColors.onNeutralContainer,
                    activeBorder = tradingColors.neutral,
                    onClick = {
                        resultSign = ResultSign.FLAT
                        amountText = "0"
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("result_sign_flat"),
                )
                ResultSignButton(
                    label = stringResource(R.string.result_loss),
                    selected = resultSign == ResultSign.LOSS,
                    activeBg = tradingColors.lossContainer,
                    activeFg = tradingColors.onLossContainer,
                    activeBorder = tradingColors.loss,
                    onClick = { resultSign = ResultSign.LOSS },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("result_sign_loss"),
                )
            }

            if (resultSign != ResultSign.FLAT) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { raw ->
                        val cleaned = raw.filter { it.isDigit() || it == '.' }
                        if (cleaned.count { it == '.' } <= 1) {
                            amountText = cleaned
                        }
                    },
                    label = { Text(stringResource(R.string.result_amount_label)) },
                    placeholder = { Text(stringResource(R.string.result_amount_hint)) },
                    prefix = {
                        Text(
                            text = if (resultSign == ResultSign.PROFIT) "+ ₹" else "− ₹",
                            style = MonoTextStyles.kpiValue,
                            color = if (resultSign == ResultSign.PROFIT) tradingColors.profit else tradingColors.loss,
                        )
                    },
                    suffix = {
                        Text(
                            text = "INR",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    textStyle = MonoTextStyles.kpiValue,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("result_amount_input"),
                )
            }

            // 5. Remark Input
            OutlinedTextField(
                value = remarkText,
                onValueChange = { remarkText = it },
                label = { Text(stringResource(R.string.remark_label)) },
                placeholder = { Text(stringResource(R.string.remark_hint)) },
                minLines = 1,
                maxLines = 3,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("remark_input"),
            )

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("cancel_trade_button"),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(stringResource(R.string.action_cancel))
                }

                Button(
                    onClick = {
                        val h = hourText.toIntOrNull()
                        val m = minuteText.toIntOrNull()
                        if (h == null || h !in 0..23 || m == null || m !in 0..59) {
                            timeError = "Enter a valid 24-hour time (Hour 00–23, Min 00–59)"
                            return@Button
                        }
                        val formattedTime = DateTimeUtils.formatHourMinute(h, m)
                        val rawAbs = amountText.toDoubleOrNull() ?: 0.0
                        val signedResult = when (resultSign) {
                            ResultSign.PROFIT -> abs(rawAbs)
                            ResultSign.LOSS -> -abs(rawAbs)
                            ResultSign.FLAT -> 0.0
                        }
                        onSave(
                            TradeEntry(
                                id = existing?.id ?: 0L,
                                dateIso = dateIso,
                                timeHm = formattedTime,
                                tradeName = tradeName,
                                fii = fii,
                                global = global,
                                pcr = pcr,
                                oiHigh = oiHigh,
                                emaSignal = emaSignal,
                                zone = zone,
                                resultAmount = signedResult,
                                remark = remarkText.trim(),
                                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                            ),
                        )
                    },
                    modifier = Modifier
                        .weight(1.6f)
                        .height(50.dp)
                        .testTag("save_trade_button"),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (existing == null) {
                            stringResource(R.string.action_save_entry)
                        } else {
                            stringResource(R.string.action_update_entry)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleFieldCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = value,
                    style = MonoTextStyles.tableValue,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun FactorSelectorRow(
    factor: ChecklistFactor,
    selected: SignalBias,
    onSelect: (SignalBias) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = factor.columnTitle,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SignalBias.entries.forEach { bias ->
                val isSelected = selected == bias
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) biasContainerColor(bias) else MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) biasAccentColor(bias) else MaterialTheme.colorScheme.outlineVariant,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelect(bias) }
                        .testTag("factor_${factor.name.lowercase()}_${bias.name.lowercase()}"),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = factor.labelFor(bias),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            ),
                            color = if (isSelected) biasTextColor(bias) else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultSignButton(
    label: String,
    selected: Boolean,
    activeBg: Color,
    activeFg: Color,
    activeBorder: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) activeBg else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) activeBorder else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                ),
                color = if (selected) activeFg else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
