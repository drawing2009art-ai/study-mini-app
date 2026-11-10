package com.piptrack.app

import org.junit.Assert.*
import org.junit.Test

class TradingAnalyticsTest {
    private fun trade(direction: String, entry: Double, exit: Double, pip: Double, manual: Double? = null) =
        Trade(instrument = "TEST", direction = direction, openedAt = 1L, entryPrice = entry,
            exitPrice = exit, pipSize = pip, manualPips = manual, riskPips = 10.0, strategy = "", notes = "")

    @Test fun eurusdBuyWinAndLoss() {
        assertEquals(10.0, trade("BUY", 1.1000, 1.1010, 0.0001).resultPips()!!, 0.000001)
        assertEquals(-10.0, trade("BUY", 1.1000, 1.0990, 0.0001).resultPips()!!, 0.000001)
    }
    @Test fun eurusdSellWinAndLoss() {
        assertEquals(10.0, trade("SELL", 1.1000, 1.0990, 0.0001).resultPips()!!, 0.000001)
        assertEquals(-10.0, trade("SELL", 1.1000, 1.1010, 0.0001).resultPips()!!, 0.000001)
    }
    @Test fun jpyAndCustomPipSizes() {
        assertEquals(10.0, trade("BUY", 150.00, 150.10, 0.01).resultPips()!!, 0.000001)
        assertEquals(5.0, trade("BUY", 100.0, 100.5, 0.1).resultPips()!!, 0.000001)
    }
    @Test fun manualResultOverridesPriceCalculation() {
        assertEquals(-7.5, trade("BUY", 1.0, 1.5, 0.1, -7.5).resultPips()!!, 0.000001)
    }
    @Test fun openTradeIsNotRealized() {
        val t = trade("BUY", 1.0, 1.1, 0.01).copy(isClosed = false, currentPrice = 1.05)
        assertNull(t.resultPips())
        assertEquals(5.0, t.floatingPips()!!, 0.000001)
    }
    @Test fun statsExcludeBreakevenFromWinRate() {
        val list = listOf(trade("BUY", 1.0, 1.1, 0.01), trade("BUY", 1.0, 0.9, 0.01),
            trade("BUY", 1.0, 1.0, 0.01))
        val stats = TradingAnalytics.stats(list)
        assertEquals(3, stats.closed)
        assertEquals(50.0, stats.winRate!!, 0.000001)
        assertEquals(0.0, stats.netPips, 0.000001)
    }
    @Test fun emptyDataDoesNotInventWinRate() {
        assertNull(TradingAnalytics.stats(emptyList()).winRate)
    }
}
