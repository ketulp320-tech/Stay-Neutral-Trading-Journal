package com.example.data

enum class SignalBias(val shortCode: String, val defaultLabel: String) {
    BULLISH("BULL", "Bullish"),
    NEUTRAL("NEUT", "Neutral"),
    BEARISH("BEAR", "Bearish");

    companion object {
        fun fromStored(value: String): SignalBias =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: NEUTRAL
    }
}

enum class ChecklistFactor(
    val columnTitle: String,
    val fullTitle: String,
    val bullishLabel: String,
    val neutralLabel: String,
    val bearishLabel: String,
) {
    FII(
        columnTitle = "FII",
        fullTitle = "FII Flow",
        bullishLabel = "Bullish",
        neutralLabel = "Neutral",
        bearishLabel = "Bearish",
    ),
    GLOBAL(
        columnTitle = "Global",
        fullTitle = "Global Cues",
        bullishLabel = "Bullish",
        neutralLabel = "Neutral",
        bearishLabel = "Bearish",
    ),
    PCR(
        columnTitle = "PCR",
        fullTitle = "PCR Sentiment",
        bullishLabel = "Bullish",
        neutralLabel = "Neutral",
        bearishLabel = "Bearish",
    ),
    OI_HIGH(
        columnTitle = "OI High",
        fullTitle = "OI High Side",
        bullishLabel = "Put High",
        neutralLabel = "Balanced",
        bearishLabel = "Call High",
    ),
    EMA_SIGNAL(
        columnTitle = "EMA Signal",
        fullTitle = "EMA Signal",
        bullishLabel = "Bullish",
        neutralLabel = "Neutral",
        bearishLabel = "Bearish",
    ),
    ZONE(
        columnTitle = "Zone",
        fullTitle = "Price Zone",
        bullishLabel = "Demand",
        neutralLabel = "Mid Zone",
        bearishLabel = "Supply",
    );

    fun labelFor(bias: SignalBias): String = when (bias) {
        SignalBias.BULLISH -> bullishLabel
        SignalBias.NEUTRAL -> neutralLabel
        SignalBias.BEARISH -> bearishLabel
    }
}

enum class AlignmentTier(val title: String, val subtitle: String) {
    HIGH("High Alignment (5–6/6)", "Strong directional agreement across checklist"),
    MODERATE("Moderate (4/6)", "Majority alignment with minor divergence"),
    CONFLICTED("Stay Neutral (≤3/6)", "Mixed or balanced checklist signals"),
}

data class ConfluenceSummary(
    val bullishCount: Int,
    val neutralCount: Int,
    val bearishCount: Int,
    val dominantBias: SignalBias,
    val alignedCount: Int,
    val tier: AlignmentTier,
    val verdictTitle: String,
    val verdictDetail: String,
)

fun evaluateConfluence(
    fii: SignalBias,
    global: SignalBias,
    pcr: SignalBias,
    oiHigh: SignalBias,
    emaSignal: SignalBias,
    zone: SignalBias,
): ConfluenceSummary {
    val signals = listOf(fii, global, pcr, oiHigh, emaSignal, zone)
    val bull = signals.count { it == SignalBias.BULLISH }
    val bear = signals.count { it == SignalBias.BEARISH }
    val neut = signals.count { it == SignalBias.NEUTRAL }

    val dominantBias = when {
        bull > bear && bull >= 4 -> SignalBias.BULLISH
        bear > bull && bear >= 4 -> SignalBias.BEARISH
        else -> SignalBias.NEUTRAL
    }
    val alignedCount = maxOf(bull, bear)
    val tier = when {
        dominantBias != SignalBias.NEUTRAL && alignedCount >= 5 -> AlignmentTier.HIGH
        dominantBias != SignalBias.NEUTRAL && alignedCount == 4 -> AlignmentTier.MODERATE
        else -> AlignmentTier.CONFLICTED
    }

    val (verdictTitle, verdictDetail) = when {
        dominantBias == SignalBias.BULLISH && alignedCount >= 5 ->
            "$alignedCount/6 Bullish · High Conviction" to "Checklist strongly favors long setups."
        dominantBias == SignalBias.BULLISH ->
            "$alignedCount/6 Bullish · Moderate Bias" to "Mild bullish edge; manage risk tightly."
        dominantBias == SignalBias.BEARISH && alignedCount >= 5 ->
            "$alignedCount/6 Bearish · High Conviction" to "Checklist strongly favors short setups."
        dominantBias == SignalBias.BEARISH ->
            "$alignedCount/6 Bearish · Moderate Bias" to "Mild bearish edge; watch conflicting cues."
        else ->
            "Stay Neutral · Mixed ($bull B / $neut N / $bear S)" to "No clear 4+ confluence — protect capital."
    }

    return ConfluenceSummary(
        bullishCount = bull,
        neutralCount = neut,
        bearishCount = bear,
        dominantBias = dominantBias,
        alignedCount = alignedCount,
        tier = tier,
        verdictTitle = verdictTitle,
        verdictDetail = verdictDetail,
    )
}
