package com.piptrack.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*

private val Bg = Color(0xFF0B1017)
private val Panel = Color(0xFF151E29)
private val Mint = Color(0xFF35D39A)
private val Red = Color(0xFFFF6577)
private val Muted = Color(0xFF91A0B2)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PipTrackTheme { PipTrackApp() } }
    }
}

@Composable fun PipTrackTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(
        primary = Mint, secondary = Color(0xFF8EA8FF), background = Bg,
        surface = Panel, onBackground = Color.White, onSurface = Color.White
    ), content = content)
}

@Composable fun PipTrackApp(vm: TradeViewModel = viewModel()) {
    val trades by vm.trades.collectAsState()
    var tab by remember { mutableStateOf("Dashboard") }
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Trade?>(null) }
    var showOpen by remember { mutableStateOf(false) }

    Scaffold(containerColor = Bg, floatingActionButton = {
        FloatingActionButton(onClick = { editing = null; adding = true }, containerColor = Mint, contentColor = Bg) {
            Icon(Icons.Default.Add, contentDescription = "Add trade")
        }
    }, bottomBar = {
        NavigationBar(containerColor = Panel) {
            listOf("Dashboard", "Trades", "Calendar", "Analytics", "Settings").forEach { item ->
                NavigationBarItem(selected = tab == item, onClick = { tab = item },
                    icon = { Icon(when(item) {
                        "Dashboard" -> Icons.Default.Dashboard
                        "Trades" -> Icons.Default.ReceiptLong
                        "Calendar" -> Icons.Default.CalendarMonth
                        "Analytics" -> Icons.Default.BarChart
                        else -> Icons.Default.Settings
                    }, contentDescription = item) }, label = { Text(item, fontSize = 10.sp) })
            }
        }
    }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            when(tab) {
                "Dashboard" -> Dashboard(trades, onAdd = { adding = true }, onTrades = { tab = "Trades" })
                "Trades" -> TradesScreen(trades, onEdit = { editing = it; adding = true }, onDelete = vm::remove)
                "Calendar" -> CalendarScreen(trades)
                "Analytics" -> AnalyticsScreen(trades)
                else -> SettingsScreen(vm, trades)
            }
        }
    }
    if (adding) TradeEditor(existing = editing, onDismiss = { adding = false; editing = null },
        onSave = { vm.save(it) { adding = false; editing = null } })
}

@Composable fun Header(title: String, subtitle: String = "") {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text(title, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        if (subtitle.isNotBlank()) Text(subtitle, color = Muted, fontSize = 12.sp)
    }
}
@Composable
fun MetricCard(label: String, metricValue: String, accent: Color = Color.White, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = label, color = Muted, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(7.dp))
            Text(text = metricValue, color = accent, fontWeight = FontWeight.Bold, fontSize = 21.sp)
        }
    }
}
fun closed(trades: List<Trade>) = trades.filter { it.isClosed && it.resultPips() != null }
fun fmt(n: Double) = String.format(Locale.US, "%.1f", n)

@Composable fun Dashboard(trades: List<Trade>, onAdd: () -> Unit, onTrades: () -> Unit) {
    val c = closed(trades)
    val wins = c.count { (it.resultPips() ?: 0.0) > 0 }
    val losses = c.count { (it.resultPips() ?: 0.0) < 0 }
    val net = c.sumOf { it.resultPips() ?: 0.0 }
    val winRate = if (wins + losses == 0) null else wins * 100.0 / (wins + losses)
    LazyColumn(contentPadding = PaddingValues(bottom = 90.dp)) {
        item { Header("PipTrack", "PRACTICE ACCOUNT  •  ALL TIME") }
        item {
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                Column(Modifier.padding(18.dp)) {
                    Text("Net pips", color = Muted)
                    Text(if (c.isEmpty()) "—" else fmt(net), fontSize = 34.sp, fontWeight = FontWeight.Bold,
                        color = if (net >= 0) Mint else Red)
                    Text(if (c.isEmpty()) "Add your first closed trade to see performance" else "Closed trades only · unrealized results excluded",
                        color = Muted, fontSize = 11.sp)
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
        item {
            Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("Closed trades", "${c.size}", modifier = Modifier.weight(1f))
                MetricCard("Win rate", winRate?.let { "${fmt(it)}%" } ?: "—", Mint, Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("Wins", "$wins", Mint, Modifier.weight(1f))
                MetricCard("Losses", "$losses", Red, Modifier.weight(1f))
                MetricCard("Breakeven", "${c.count { it.resultPips() == 0.0 }}", modifier = Modifier.weight(1f))
            }
        }
        item { Header("Recent trades", "Your latest recorded activity") }
        if (trades.isEmpty()) item { EmptyState("No trades yet", "Tap + to record your first practice trade.") }
        items(trades.take(5)) { TradeRow(it, {}) }
        item { TextButton(onClick = onTrades, modifier = Modifier.fillMaxWidth()) { Text("View all trades") } }
    }
}

@Composable fun TradesScreen(trades: List<Trade>, onEdit: (Trade) -> Unit, onDelete: (Long) -> Unit) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }
    var pendingDelete by remember { mutableStateOf<Trade?>(null) }
    val shown = trades.filter { it.instrument.contains(query, true) || it.strategy.contains(query, true) }
        .filter { filter == "All" || (if (filter == "Open") !it.isClosed else it.isClosed) }
    Column {
        Header("Trades", "${trades.size} total records")
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text("Search instrument or strategy") }, singleLine = true)
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All", "Closed", "Open").forEach { FilterChip(selected = filter == it, onClick = { filter = it }, label = { Text(it) }) }
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 90.dp)) {
            if (shown.isEmpty()) item { EmptyState("No matching trades", "Try another search or add a trade.") }
            items(shown, key = { it.id }) { trade ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { TradeRow(trade) { onEdit(trade) } }
                    IconButton(onClick = { onEdit(trade) }) { Icon(Icons.Default.Edit, "Edit", tint = Muted) }
                    IconButton(onClick = { pendingDelete = trade }) { Icon(Icons.Default.Delete, "Delete", tint = Red) }
                }
            }
        }
    }
    pendingDelete?.let { t ->
        AlertDialog(onDismissRequest = { pendingDelete = null }, title = { Text("Delete trade?") },
            text = { Text("This permanently deletes ${t.instrument} from this device.") },
            confirmButton = { TextButton(onClick = { onDelete(t.id); pendingDelete = null }) { Text("Delete", color = Red) } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } })
    }
}

@Composable fun TradeRow(t: Trade, onClick: () -> Unit) {
    val result = t.resultPips()
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("${t.instrument}  ·  ${t.direction}", fontWeight = FontWeight.SemiBold)
            Text(SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(t.openedAt)) +
                if (t.isClosed) "  ·  CLOSED" else "  ·  OPEN", color = Muted, fontSize = 11.sp)
        }
        Text(result?.let { "${if (it > 0) "+" else ""}${fmt(it)} p" } ?: "Open",
            color = result?.let { if (it > 0) Mint else if (it < 0) Red else Muted } ?: Muted,
            fontWeight = FontWeight.Bold)
    }
    HorizontalDivider(color = Color(0xFF253140))
}

@Composable fun TradeEditor(existing: Trade?, onDismiss: () -> Unit, onSave: (Trade) -> Unit) {
    var instrument by remember { mutableStateOf(existing?.instrument ?: "EURUSD") }
    var direction by remember { mutableStateOf(existing?.direction ?: "BUY") }
    var entry by remember { mutableStateOf(existing?.entryPrice?.toString() ?: "") }
    var exit by remember { mutableStateOf(existing?.exitPrice?.toString() ?: "") }
    var pip by remember { mutableStateOf(existing?.pipSize?.toString() ?: "0.0001") }
    var manual by remember { mutableStateOf(existing?.manualPips?.toString() ?: "") }
    var risk by remember { mutableStateOf(existing?.riskPips?.toString() ?: "") }
    var strategy by remember { mutableStateOf(existing?.strategy ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var isClosed by remember { mutableStateOf(existing?.isClosed ?: true) }
    var error by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (existing == null) "Add trade" else "Edit trade") },
        text = {
            Column(Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Field("Instrument", instrument) { instrument = it.uppercase() }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(direction == "BUY", { direction = "BUY" }, label = { Text("BUY") })
                    FilterChip(direction == "SELL", { direction = "SELL" }, label = { Text("SELL") })
                }
                Field("Entry price", entry, { entry = it }, true)
                Field("Exit price (closed trades)", exit, { exit = it }, true)
                Field("Pip size", pip, { pip = it }, true)
                Field("Manual pip result (optional)", manual, { manual = it }, true)
                Field("Initial risk in pips (optional)", risk, { risk = it }, true)
                Field("Strategy (optional)", strategy) { strategy = it }
                Field("Notes (optional)", notes, multiline = true) { notes = it }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Closed trade", Modifier.weight(1f))
                    Switch(isClosed, { isClosed = it })
                }
                if (error.isNotBlank()) Text(error, color = Red, fontSize = 12.sp)
                val preview = runCatching {
                    val e = entry.toDouble(); val p = pip.toDouble()
                    if (manual.isNotBlank()) manual.toDouble() else if (exit.isNotBlank())
                        (if (direction == "BUY") exit.toDouble() - e else e - exit.toDouble()) / p else null
                }.getOrNull()
                Text("Result preview: ${preview?.let { "${fmt(it)} pips" } ?: "Enter valid prices or manual pips"}", color = Mint)
            }
        },
        confirmButton = { Button(onClick = {
            val e = entry.toDoubleOrNull(); val p = pip.toDoubleOrNull()
            val x = exit.toDoubleOrNull(); val m = manual.toDoubleOrNull(); val r = risk.toDoubleOrNull()
            error = when {
                instrument.isBlank() -> "Instrument is required"
                e == null || e <= 0 -> "Enter a valid positive entry price"
                p == null || p <= 0 -> "Pip size must be greater than zero"
                isClosed && x == null && m == null -> "Closed trades need an exit price or manual result"
                exit.isNotBlank() && (x == null || x <= 0) -> "Exit price must be positive"
                risk.isNotBlank() && (r == null || r <= 0) -> "Risk must be greater than zero"
                else -> ""
            }
            if (error.isBlank()) onSave(Trade(id = existing?.id ?: 0, instrument = instrument.trim(), direction = direction,
                openedAt = existing?.openedAt ?: System.currentTimeMillis(), entryPrice = e!!, exitPrice = x,
                pipSize = p!!, manualPips = m, riskPips = r, strategy = strategy, notes = notes, isClosed = isClosed))
        }) { Text("Save trade") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable fun Field(label: String, value: String, onChange: (String) -> Unit, numeric: Boolean = false, multiline: Boolean = false) {
    OutlinedTextField(value, onChange, Modifier.fillMaxWidth(), label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text),
        singleLine = !multiline, minLines = if (multiline) 3 else 1)
}
@Composable fun EmptyState(title: String, body: String) {
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.ShowChart, null, tint = Muted, modifier = Modifier.size(36.dp))
        Spacer(Modifier.height(8.dp)); Text(title, fontWeight = FontWeight.Bold)
        Text(body, color = Muted, fontSize = 12.sp)
    }
}
@Composable fun CalendarScreen(trades: List<Trade>) {
    val days = closed(trades).groupBy { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(it.openedAt)) }
    Column {
        Header("Calendar", "Daily closed-trade pip results")
        if (days.isEmpty()) EmptyState("No trading days yet", "Recorded trades will appear here.")
        else LazyColumn {
            items(days.toList().sortedByDescending { it.first }) { (day, records) ->
                val net = records.sumOf { it.resultPips() ?: 0.0 }
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(day, Modifier.weight(1f), color = Muted)
                        Text("${records.size} trades", color = Muted)
                        Spacer(Modifier.width(14.dp))
                        Text("${fmt(net)} p", color = if (net >= 0) Mint else Red, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
@Composable fun AnalyticsScreen(trades: List<Trade>) {
    val c = closed(trades)
    val groups = c.groupBy { it.instrument }.toList().sortedBy { it.first }
    Column {
        Header("Analytics", "Results are separated by instrument")
        if (groups.isEmpty()) EmptyState("Not enough data", "Close and record trades to unlock analytics.")
        else LazyColumn {
            items(groups) { (symbol, list) ->
                val wins = list.count { (it.resultPips() ?: 0.0) > 0 }
                val losses = list.count { (it.resultPips() ?: 0.0) < 0 }
                val net = list.sumOf { it.resultPips() ?: 0.0 }
                val grossWin = list.sumOf { maxOf(0.0, it.resultPips() ?: 0.0) }
                val grossLoss = list.sumOf { kotlin.math.abs(minOf(0.0, it.resultPips() ?: 0.0)) }
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(symbol, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("${list.size} trades  ·  ${wins} wins  ·  ${losses} losses", color = Muted, fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricCard("Net pips", fmt(net), if (net >= 0) Mint else Red, Modifier.weight(1f))
                            MetricCard("Win rate", if (wins + losses == 0) "—" else fmt(wins * 100.0 / (wins + losses)) + "%", modifier = Modifier.weight(1f))
                        }
                        Text("Gross win ${fmt(grossWin)}  ·  Gross loss ${fmt(grossLoss)}  ·  Pip PF " +
                            if (grossLoss == 0.0) "—" else fmt(grossWin / grossLoss), color = Muted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable fun SettingsScreen(vm: TradeViewModel, trades: List<Trade>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var instrumentName by remember { mutableStateOf("") }
    var pipSize by remember { mutableStateOf("0.0001") }
    var precision by remember { mutableStateOf("5") }
    var strategyName by remember { mutableStateOf("") }
    var tagName by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }

    val jsonExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch { runCatching { DataTransfer.exportJson(context, uri, trades) }
            .onSuccess { message = "JSON backup exported" }.onFailure { message = it.message ?: "Export failed" } }
    }
    val csvExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch { runCatching { DataTransfer.exportCsv(context, uri, trades) }
            .onSuccess { message = "CSV exported" }.onFailure { message = it.message ?: "Export failed" } }
    }
    val jsonImport = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching { DataTransfer.importJson(context, uri, PipTrackDatabase.get(context).tradeDao()) }
                .onSuccess { message = "Backup restored. Imported trades were added to this device." }
                .onFailure { message = it.message ?: "Restore failed" }
        }
    }

    Column {
        Header("Settings", "Manage your local PipTrack workspace")
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Data protection", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Export files to a location you choose. Backups stay under your control.", color = Muted, fontSize = 12.sp)
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { jsonExport.launch("piptrack-backup.json") }, modifier = Modifier.fillMaxWidth()) { Text("Export JSON backup") }
                    OutlinedButton(onClick = { csvExport.launch("piptrack-trades.csv") }, modifier = Modifier.fillMaxWidth()) { Text("Export trades as CSV") }
                    OutlinedButton(onClick = { jsonImport.launch(arrayOf("application/json", "text/*")) }, modifier = Modifier.fillMaxWidth()) { Text("Restore JSON backup") }
                    Text("Restore adds imported trades; it does not overwrite existing records. Check your backup before importing.", color = Muted, fontSize = 11.sp)
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Manage instruments", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Field("Symbol (e.g. EURUSD)", instrumentName) { instrumentName = it }
                    Field("Pip size", pipSize, { pipSize = it }, numeric = true)
                    Field("Display precision", precision, { precision = it }, numeric = true)
                    Button(onClick = {
                        val p = pipSize.toDoubleOrNull(); val d = precision.toIntOrNull()
                        if (instrumentName.isBlank() || p == null || p <= 0 || d == null || d !in 0..10) message = "Enter a symbol, positive pip size and precision 0–10"
                        else { vm.addInstrument(instrumentName, p, d); message = "Instrument saved"; instrumentName = "" }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Add instrument") }
                    Text("Examples: EURUSD 0.0001, USDJPY 0.01. Confirm the pip size for unfamiliar instruments.", color = Muted, fontSize = 11.sp)
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Strategies and tags", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Field("New strategy", strategyName) { strategyName = it }
                    OutlinedButton(onClick = {
                        if (strategyName.isBlank()) message = "Enter a strategy name"
                        else { vm.addStrategy(strategyName); strategyName = ""; message = "Strategy saved" }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Add strategy") }
                    Field("New tag", tagName) { tagName = it }
                    OutlinedButton(onClick = {
                        if (tagName.isBlank()) message = "Enter a tag name"
                        else { vm.addTag(tagName); tagName = ""; message = "Tag saved" }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Add tag") }
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Privacy & storage", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("PipTrack stores trading records in a local Room database on this device. No login, broker connection, ads or cloud sync are used.", color = Muted)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { confirmClear = true }) { Text("Clear all trade records", color = Red) }
                }
            }
            if (message.isNotBlank()) Text(message, color = Mint, fontSize = 12.sp)
            Spacer(Modifier.height(90.dp))
        }
    }
    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text("Permanently clear trades?") },
        text = { Text("This deletes every trade record from this device. Export a backup first if you may need these records.") },
        confirmButton = { TextButton(onClick = { vm.clearAll { message = "Trade records cleared" }; confirmClear = false }) { Text("Delete all", color = Red) } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } }
    )
}
