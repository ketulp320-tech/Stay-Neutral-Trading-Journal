package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.data.SignalBias
import com.example.ui.theme.LocalTradingColors
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

private const val INR_SYMBOL = "₹"

/**
 * Formats an absolute amount using Indian numbering grouping (e.g., 1,25,000 or 12,500.50).
 */
fun formatIndianAmount(absAmount: Double): String {
    if (absAmount == 0.0) return "0"
    val whole = absAmount.toLong()
    val hasFraction = abs(absAmount - whole) >= 0.005

    val wholeStr = whole.toString()
    val groupedWhole = if (wholeStr.length <= 3) {
        wholeStr
    } else {
        val lastThree = wholeStr.takeLast(3)
        val rest = wholeStr.dropLast(3)
        val pairs = rest.reversed().chunked(2).joinToString(",").reversed()
        "$pairs,$lastThree"
    }

    return if (hasFraction) {
        val symbols = DecimalFormatSymbols(Locale.US)
        val frac = DecimalFormat(".00", symbols).format(absAmount - whole)
        "$groupedWhole$frac"
    } else {
        groupedWhole
    }
}

fun formatSignedPnl(amount: Double): String {
    val absVal = abs(amount)
    if (absVal == 0.0) return "${INR_SYMBOL}0"
    val body = formatIndianAmount(absVal)
    return when {
        amount > 0.0 -> "+$INR_SYMBOL$body"
        amount < 0.0 -> "−$INR_SYMBOL$body"
        else -> "${INR_SYMBOL}0"
    }
}

fun formatCompactPnl(amount: Double): String {
    val absVal = abs(amount)
    if (absVal == 0.0) return "${INR_SYMBOL}0"
    val sign = when {
        amount > 0.0 -> "+"
        amount < 0.0 -> "−"
        else -> ""
    }
    val scaled = when {
        absVal >= 10_000_000.0 -> String.format(Locale.US, "%.1fCr", absVal / 10_000_000.0)
        absVal >= 100_000.0 -> String.format(Locale.US, "%.1fL", absVal / 100_000.0)
        absVal >= 10_000.0 -> String.format(Locale.US, "%.0fK", absVal / 1_000.0)
        absVal >= 1_000.0 -> String.format(Locale.US, "%.1fK", absVal / 1_000.0)
        absVal % 1.0 == 0.0 -> String.format(Locale.US, "%.0f", absVal)
        else -> String.format(Locale.US, "%.1f", absVal)
    }
    return "$sign$INR_SYMBOL$scaled"
}

fun formatPercent(pct: Double): String = String.format(Locale.US, "%.0f%%", pct)

@Composable
fun pnlColor(amount: Double): Color {
    val colors = LocalTradingColors.current
    return when {
        amount > 0.0 -> colors.profit
        amount < 0.0 -> colors.loss
        else -> colors.neutral
    }
}

@Composable
fun pnlContainerColor(amount: Double): Color {
    val colors = LocalTradingColors.current
    return when {
        amount > 0.0 -> colors.profitContainer
        amount < 0.0 -> colors.lossContainer
        else -> colors.neutralContainer
    }
}

@Composable
fun biasTextColor(bias: SignalBias): Color {
    val colors = LocalTradingColors.current
    return when (bias) {
        SignalBias.BULLISH -> colors.onProfitContainer
        SignalBias.NEUTRAL -> colors.onNeutralContainer
        SignalBias.BEARISH -> colors.onLossContainer
    }
}

@Composable
fun biasContainerColor(bias: SignalBias): Color {
    val colors = LocalTradingColors.current
    return when (bias) {
        SignalBias.BULLISH -> colors.profitContainer
        SignalBias.NEUTRAL -> colors.neutralContainer
        SignalBias.BEARISH -> colors.lossContainer
    }
}

@Composable
fun biasAccentColor(bias: SignalBias): Color {
    val colors = LocalTradingColors.current
    return when (bias) {
        SignalBias.BULLISH -> colors.profit
        SignalBias.NEUTRAL -> colors.neutral
        SignalBias.BEARISH -> colors.loss
    }
}
