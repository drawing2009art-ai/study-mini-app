package com.piptrack.app

import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class PerformanceStats(
    val closed: Int, val wins: Int, val losses: Int, val breakeven: Int,
    val winRate: Double?, val grossWinPips: Double, val grossLossPips: Double,
    val netPips: Double, val averageWin: Double?, val averageLoss: Double?,
    val expectancy: Double?, val averageR: Double?, val pipProfitFactor: Double?
)

object TradingAnalytics {
    fun stats(trades: List<Trade>): PerformanceStats {
        val results = trades.filter { it.isClosed }.mapNotNull { it.resultPips() }
        val wins = results.filter { it > 0.0 }
        val losses = results.filter { it < 0.0 }
        val be = results.count { it == 0.0 }
        val gw = wins.sum()
        val gl = losses.sumOf { abs(it) }
        val rate = if (wins.size + losses.size == 0) null else wins.size.toDouble() / (wins.size + losses.size)
        val avgW = wins.takeIf { it.isNotEmpty() }?.average()
        val avgL = losses.takeIf { it.isNotEmpty() }?.let { it.sumOf { v -> abs(v) } / it.size }
        val expectancy = if (rate != null && avgW != null && avgL != null)
            rate * avgW - (1.0 - rate) * avgL else null
        val rs = trades.mapNotNull { it.realizedR() }
        return PerformanceStats(results.size, wins.size, losses.size, be, rate?.times(100.0),
            gw, gl, gw - gl, avgW, avgL, expectancy, rs.takeIf { it.isNotEmpty() }?.average(),
            if (gl > 0.0) gw / gl else null)
    }

    fun byInstrument(trades: List<Trade>) = trades.filter { it.isClosed && it.resultPips() != null }
        .groupBy { it.instrument }.toSortedMap().mapValues { stats(it.value) }

    fun streaks(trades: List<Trade>): Pair<Int, Int> {
        var bestWin = 0; var bestLoss = 0; var win = 0; var loss = 0
        trades.filter { it.isClosed }.sortedBy { it.closedAt ?: it.openedAt }.forEach {
            val result = it.resultPips() ?: return@forEach
            if (result > 0) { win++; loss = 0 } else if (result < 0) { loss++; win = 0 } else { win = 0; loss = 0 }
            bestWin = max(bestWin, win); bestLoss = max(bestLoss, loss)
        }
        return bestWin to bestLoss
    }

    fun dailyNet(trades: List<Trade>): Map<String, Double> =
        trades.filter { it.isClosed && it.resultPips() != null }.groupBy {
            java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date(it.closedAt ?: it.openedAt))
        }.mapValues { (_, v) -> v.sumOf { it.resultPips() ?: 0.0 } }.toSortedMap()
}
