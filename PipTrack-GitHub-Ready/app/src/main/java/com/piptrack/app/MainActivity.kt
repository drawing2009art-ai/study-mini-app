
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
fun Metric(
    label: String,
