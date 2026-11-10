package com.piptrack.app

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "instruments", indices = [Index(value = ["symbol"], unique = true)])
data class Instrument(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val symbol: String,
    val pipSize: Double,
    val precision: Int = 5,
    val archived: Boolean = false
)

@Entity(tableName = "strategies", indices = [Index(value = ["name"], unique = true)])
data class Strategy(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val archived: Boolean = false)

@Entity(tableName = "tags", indices = [Index(value = ["name"], unique = true)])
data class Tag(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val color: String = "#91A0B2")

@Entity(tableName = "trades", indices = [Index("openedAt"), Index("instrument")])
data class Trade(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val instrument: String,
    val direction: String,
    val openedAt: Long,
    val entryPrice: Double,
    val exitPrice: Double?,
    val pipSize: Double,
    val manualPips: Double?,
    val riskPips: Double?,
    val strategy: String,
    val notes: String,
    val isClosed: Boolean = true,
    val currentPrice: Double? = null,
    val plannedStopLoss: Double? = null,
    val plannedTakeProfit: Double? = null,
    val timeframe: String = "",
    val session: String = "",
    val setupType: String = "",
    val entryReason: String = "",
    val marketConditions: String = "",
    val confirmation: String = "",
    val followedPlan: Boolean? = null,
    val emotionalState: String = "",
    val mistakes: String = "",
    val grossMoney: Double? = null,
    val netMoney: Double? = null,
    val currency: String = "",
    val commission: Double? = null,
    val swap: Double? = null,
    val otherFees: Double? = null,
    val positionSize: Double? = null,
    val closedAt: Long? = null,
    val timezoneId: String = java.util.TimeZone.getDefault().id,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun resultPips(): Double? {
        if (!isClosed) return null
        manualPips?.let { return it }
        val exit = exitPrice ?: return null
        return if (direction == "BUY") (exit - entryPrice) / pipSize
        else (entryPrice - exit) / pipSize
    }
    fun floatingPips(): Double? {
        if (isClosed || currentPrice == null || pipSize <= 0.0) return null
        return if (direction == "BUY") (currentPrice - entryPrice) / pipSize
        else (entryPrice - currentPrice) / pipSize
    }
    fun realizedR(): Double? = resultPips()?.let { r -> riskPips?.takeIf { it > 0.0 }?.let { r / it } }
}

@Entity(tableName = "price_snapshots", indices = [Index("tradeId")])
data class TradePriceSnapshot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tradeId: Long,
    val price: Double,
    val floatingPips: Double,
    val recordedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_journal", indices = [Index(value = ["dateKey"], unique = true)])
data class DailyJournal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateKey: String,
    val preSessionPlan: String = "",
    val postSessionReview: String = "",
    val observations: String = "",
    val mistakes: String = "",
    val lessons: String = "",
    val personalRules: String = ""
)

@Entity(tableName = "checklist_items")
data class ChecklistItem(@PrimaryKey(autoGenerate = true) val id: Long = 0, val question: String, val sortOrder: Int = 0, val strategy: String = "")

@Entity(tableName = "checklist_responses", indices = [Index("tradeId")])
data class ChecklistResponse(@PrimaryKey(autoGenerate = true) val id: Long = 0, val tradeId: Long, val itemId: Long, val checked: Boolean)

@Entity(tableName = "attachments", indices = [Index("tradeId")])
data class TradeAttachment(@PrimaryKey(autoGenerate = true) val id: Long = 0, val tradeId: Long, val localUri: String, val displayName: String)

@Dao
interface TradeDao {
    @Query("SELECT * FROM trades ORDER BY openedAt DESC") fun observeAll(): Flow<List<Trade>>
    @Query("SELECT * FROM trades WHERE id = :id") suspend fun get(id: Long): Trade?
    @Insert suspend fun insert(trade: Trade): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(trades: List<Trade>)
    @Update suspend fun update(trade: Trade)
    @Query("DELETE FROM trades WHERE id = :id") suspend fun delete(id: Long)
    @Query("DELETE FROM trades") suspend fun clear()
    @Query("SELECT * FROM trades WHERE isClosed = 0 ORDER BY openedAt DESC") fun observeOpen(): Flow<List<Trade>>
}
@Dao interface InstrumentDao {
    @Query("SELECT * FROM instruments WHERE archived = 0 ORDER BY symbol") fun observeActive(): Flow<List<Instrument>>
    @Query("SELECT * FROM instruments ORDER BY symbol") suspend fun all(): List<Instrument>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(item: Instrument)
    @Update suspend fun update(item: Instrument)
}
@Dao interface StrategyDao {
    @Query("SELECT * FROM strategies WHERE archived = 0 ORDER BY name") fun observeActive(): Flow<List<Strategy>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(item: Strategy)
}
@Dao interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name") fun observeAll(): Flow<List<Tag>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(item: Tag)
}
@Dao interface SnapshotDao {
    @Query("SELECT * FROM price_snapshots WHERE tradeId = :tradeId ORDER BY recordedAt") fun forTrade(tradeId: Long): Flow<List<TradePriceSnapshot>>
    @Insert suspend fun add(snapshot: TradePriceSnapshot)
}
@Dao interface JournalDao {
    @Query("SELECT * FROM daily_journal ORDER BY dateKey DESC") fun observeAll(): Flow<List<DailyJournal>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(journal: DailyJournal)
}
@Dao interface ChecklistDao {
    @Query("SELECT * FROM checklist_items ORDER BY sortOrder") fun items(): Flow<List<ChecklistItem>>
    @Insert suspend fun add(item: ChecklistItem): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveResponse(response: ChecklistResponse)
}
@Dao interface AttachmentDao {
    @Query("SELECT * FROM attachments WHERE tradeId = :tradeId") fun forTrade(tradeId: Long): Flow<List<TradeAttachment>>
    @Insert suspend fun add(item: TradeAttachment)
}

@Database(
    entities = [Trade::class, Instrument::class, Strategy::class, Tag::class, TradePriceSnapshot::class,
        DailyJournal::class, ChecklistItem::class, ChecklistResponse::class, TradeAttachment::class],
    version = 2, exportSchema = true
)
abstract class PipTrackDatabase : RoomDatabase() {
    abstract fun tradeDao(): TradeDao
    abstract fun instrumentDao(): InstrumentDao
    abstract fun strategyDao(): StrategyDao
    abstract fun tagDao(): TagDao
    abstract fun snapshotDao(): SnapshotDao
    abstract fun journalDao(): JournalDao
    abstract fun checklistDao(): ChecklistDao
    abstract fun attachmentDao(): AttachmentDao
    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trades ADD COLUMN currentPrice REAL")
                db.execSQL("ALTER TABLE trades ADD COLUMN plannedStopLoss REAL")
                db.execSQL("ALTER TABLE trades ADD COLUMN plannedTakeProfit REAL")
                db.execSQL("ALTER TABLE trades ADD COLUMN timeframe TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE trades ADD COLUMN session TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE trades ADD COLUMN setupType TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE trades ADD COLUMN entryReason TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE trades ADD COLUMN marketConditions TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE trades ADD COLUMN confirmation TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE trades ADD COLUMN followedPlan INTEGER")
                db.execSQL("ALTER TABLE trades ADD COLUMN emotionalState TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE trades ADD COLUMN mistakes TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE trades ADD COLUMN grossMoney REAL")
                db.execSQL("ALTER TABLE trades ADD COLUMN netMoney REAL")
                db.execSQL("ALTER TABLE trades ADD COLUMN currency TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE trades ADD COLUMN commission REAL")
                db.execSQL("ALTER TABLE trades ADD COLUMN swap REAL")
                db.execSQL("ALTER TABLE trades ADD COLUMN otherFees REAL")
                db.execSQL("ALTER TABLE trades ADD COLUMN positionSize REAL")
                db.execSQL("ALTER TABLE trades ADD COLUMN closedAt INTEGER")
                db.execSQL("ALTER TABLE trades ADD COLUMN timezoneId TEXT NOT NULL DEFAULT 'UTC'")
                db.execSQL("ALTER TABLE trades ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE TABLE IF NOT EXISTS instruments (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, symbol TEXT NOT NULL, pipSize REAL NOT NULL, precision INTEGER NOT NULL, archived INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_instruments_symbol ON instruments(symbol)")
                db.execSQL("CREATE TABLE IF NOT EXISTS strategies (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, archived INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_strategies_name ON strategies(name)")
                db.execSQL("CREATE TABLE IF NOT EXISTS tags (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, color TEXT NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_tags_name ON tags(name)")
                db.execSQL("CREATE TABLE IF NOT EXISTS price_snapshots (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, tradeId INTEGER NOT NULL, price REAL NOT NULL, floatingPips REAL NOT NULL, recordedAt INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_price_snapshots_tradeId ON price_snapshots(tradeId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS daily_journal (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, dateKey TEXT NOT NULL, preSessionPlan TEXT NOT NULL, postSessionReview TEXT NOT NULL, observations TEXT NOT NULL, mistakes TEXT NOT NULL, lessons TEXT NOT NULL, personalRules TEXT NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_daily_journal_dateKey ON daily_journal(dateKey)")
                db.execSQL("CREATE TABLE IF NOT EXISTS checklist_items (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, question TEXT NOT NULL, sortOrder INTEGER NOT NULL, strategy TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS checklist_responses (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, tradeId INTEGER NOT NULL, itemId INTEGER NOT NULL, checked INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_checklist_responses_tradeId ON checklist_responses(tradeId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS attachments (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, tradeId INTEGER NOT NULL, localUri TEXT NOT NULL, displayName TEXT NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_attachments_tradeId ON attachments(tradeId)")
            }
        }
        @Volatile private var INSTANCE: PipTrackDatabase? = null
        fun get(context: android.content.Context): PipTrackDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(context.applicationContext, PipTrackDatabase::class.java, "piptrack.db")
                    .addMigrations(MIGRATION_1_2)
                    .build().also { INSTANCE = it }
            }
    }
}
