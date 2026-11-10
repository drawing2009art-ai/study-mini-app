package com.piptrack.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TradeViewModel(app: Application) : AndroidViewModel(app) {
    private val db = PipTrackDatabase.get(app)
    private val dao = db.tradeDao()
    val trades = dao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val instruments = db.instrumentDao().observeActive().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val strategies = db.strategyDao().observeActive().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val tags = db.tagDao().observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val journals = db.journalDao().observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(trade: Trade, done: () -> Unit = {}) = viewModelScope.launch {
        if (trade.id == 0L) dao.insert(trade) else dao.update(trade)
        done()
    }
    fun remove(id: Long) = viewModelScope.launch { dao.delete(id) }
    fun updatePrice(trade: Trade, price: Double) = viewModelScope.launch {
        val updated = trade.copy(currentPrice = price, updatedAt = System.currentTimeMillis())
        dao.update(updated)
        updated.floatingPips()?.let { db.snapshotDao().add(TradePriceSnapshot(tradeId = trade.id, price = price, floatingPips = it)) }
    }
    fun closeTrade(trade: Trade, exit: Double, closeTime: Long = System.currentTimeMillis()) = viewModelScope.launch {
        dao.update(trade.copy(exitPrice = exit, manualPips = null, isClosed = true, closedAt = closeTime, currentPrice = null))
    }
    fun saveJournal(journal: DailyJournal) = viewModelScope.launch { db.journalDao().save(journal) }
    fun addInstrument(symbol: String, pipSize: Double, precision: Int) = viewModelScope.launch {
        db.instrumentDao().upsert(Instrument(symbol = symbol.uppercase().trim(), pipSize = pipSize, precision = precision))
    }
    fun addStrategy(name: String) = viewModelScope.launch { db.strategyDao().upsert(Strategy(name = name.trim())) }
    fun addTag(name: String) = viewModelScope.launch { db.tagDao().upsert(Tag(name = name.trim())) }
    fun clearAll(done: () -> Unit = {}) = viewModelScope.launch { dao.clear(); done() }
    suspend fun find(id: Long) = dao.get(id)
}
