package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Crisp White Surface Palette — Stay Neutral (White + Blue + Green)
val PureWhite = Color(0xFFFFFFFF)
val SnowSurface = Color(0xFFF8FAFC)
val IceBlueSurface = Color(0xFFF1F6FD)
val MistBorder = Color(0xFFDCE6F2)
val Slate900 = Color(0xFF0F172A)
val Slate700 = Color(0xFF334155)
val Slate600 = Color(0xFF475569)
val Slate500 = Color(0xFF64748B)
val Slate300 = Color(0xFFCBD5E1)
val Slate200 = Color(0xFFE2E8F0)

// Primary Blue Palette (Disciplined Institutional Blue)
val Blue700 = Color(0xFF1D4ED8)
val Blue600 = Color(0xFF2563EB)
val Blue500 = Color(0xFF3B82F6)
val Blue100 = Color(0xFFDBEAFE)
val Blue50 = Color(0xFFEFF6FF)
val Blue900 = Color(0xFF1E3A8A)

// Secondary & Profit Green Palette (Crisp Emerald Green)
val Green700 = Color(0xFF15803D)
val Green600 = Color(0xFF16A34A)
val Green500 = Color(0xFF22C55E)
val Green100 = Color(0xFFDCFCE7)
val Green900 = Color(0xFF14532D)

// Loss Crimson Palette (Clear Loss Contrast on White)
val Red600 = Color(0xFFDC2626)
val Red100 = Color(0xFFFEE2E2)
val Red900 = Color(0xFF991B1B)

// Neutral Balanced Palette (Cool Blue-Slate Neutral)
val NeutralBlue700 = Color(0xFF0369A1)
val NeutralBlue100 = Color(0xFFE0F2FE)
val NeutralBlue900 = Color(0xFF0C4A6E)

@Immutable
data class TradingColors(
    val profit: Color,
    val profitContainer: Color,
    val onProfitContainer: Color,
    val loss: Color,
    val lossContainer: Color,
    val onLossContainer: Color,
    val neutral: Color,
    val neutralContainer: Color,
    val onNeutralContainer: Color,
    val tableHeaderBg: Color,
    val tableRowAltBg: Color,
    val tableGridBorder: Color,
    val cardElevated: Color,
)

val LightTradingColors = TradingColors(
    profit = Green700,
    profitContainer = Green100,
    onProfitContainer = Green900,
    loss = Red600,
    lossContainer = Red100,
    onLossContainer = Red900,
    neutral = NeutralBlue700,
    neutralContainer = NeutralBlue100,
    onNeutralContainer = NeutralBlue900,
    tableHeaderBg = Blue50,
    tableRowAltBg = SnowSurface,
    tableGridBorder = MistBorder,
    cardElevated = PureWhite,
)

val LocalTradingColors = staticCompositionLocalOf { LightTradingColors }
