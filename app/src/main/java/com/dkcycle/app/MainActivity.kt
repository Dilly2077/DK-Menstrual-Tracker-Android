package com.dkcycle.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DKCycleApp() }
    }
}

internal enum class Screen(val label: String, val icon: ImageVector) {
    TODAY("Today", Icons.Default.Home),
    CALENDAR("Calendar", Icons.Default.CalendarMonth),
    LOG("Log", Icons.Default.AddCircle),
    INSIGHTS("Insights", Icons.Default.Insights),
    LEARN("Learn", Icons.Default.MenuBook),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun DKCycleApp() {
    val colors = lightColorScheme(
        primary = Color(0xFF8E4F6B),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFF7D8E5),
        onPrimaryContainer = Color(0xFF3A1022),
        secondary = Color(0xFF725B79),
        secondaryContainer = Color(0xFFF2DDF4),
        background = Color(0xFFFFF9FB),
        surface = Color(0xFFFFF9FB),
        surfaceVariant = Color(0xFFF6EEF1),
        outline = Color(0xFF8A7A80)
    )

    MaterialTheme(colorScheme = colors) {
        val context = LocalContext.current
        val store = remember { LocalStore(context.applicationContext) }
        var logs by remember { mutableStateOf(store.loadLogs()) }
        var screenName by rememberSaveable { mutableStateOf(Screen.TODAY.name) }
        val screen = Screen.valueOf(screenName)
        val analysis = remember(logs) { CycleEngine.analyse(logs.values.toList()) }
        val snackbar = remember { SnackbarHostState() }

        fun save(updated: Map<LocalDate, DailyLog>) {
            logs = updated
            store.saveLogs(updated)
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                NavigationBar {
                    Screen.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = screen == destination,
                            onClick = { screenName = destination.name },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        ) { padding ->
            Surface(modifier = Modifier.fillMaxSize().padding(padding)) {
                when (screen) {
                    Screen.TODAY -> TodayScreen(analysis, logs) { screenName = Screen.LOG.name }
                    Screen.CALENDAR -> CalendarScreen(logs, analysis)
                    Screen.LOG -> LogScreen(logs, ::save, snackbar)
                    Screen.INSIGHTS -> InsightsScreen(logs, analysis)
                    Screen.LEARN -> LearnScreen()
                    Screen.SETTINGS -> SettingsScreen(logs, ::save, store, snackbar)
                }
            }
        }
    }
}

@Composable
internal fun PageHeader(title: String, subtitle: String? = null, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            if (subtitle != null) Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing?.invoke()
    }
}

@Composable
internal fun TodayScreen(analysis: CycleAnalysis, logs: Map<LocalDate, DailyLog>, onLog: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item { PageHeader("DKCycle", "Private, local cycle tracking") }
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            analysis.cycleDay?.let { "Cycle day $it" } ?: "Start by logging your period",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(analysis.phase, style = MaterialTheme.typography.headlineSmall)
                        Text(analysis.phaseExplanation)
                        Button(onClick = onLog) { Text("Log today") }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Next period",
                        value = analysis.nextPeriodStart?.format(shortDate()) ?: "—",
                        note = "${analysis.confidence} confidence"
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Typical cycle",
                        value = "${analysis.averageCycleLength} days",
                        note = if (analysis.periodStarts.size < 2) "Using default until more data" else "Based on recent cycles"
                    )
                }

                Spacer(Modifier.height(12.dp))
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Estimated fertile window", fontWeight = FontWeight.Bold)
                        Text(
                            if (analysis.fertileStart != null && analysis.fertileEnd != null)
                                "${analysis.fertileStart.format(shortDate())} – ${analysis.fertileEnd.format(shortDate())}"
                            else "Add period data to estimate a window"
                        )
                        Text(
                            "This is a calendar estimate, not a contraceptive method and not confirmation of ovulation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (logs[LocalDate.now()] != null) {
                    Spacer(Modifier.height(12.dp))
                    Text("Today has been logged.", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
internal fun MetricCard(modifier: Modifier = Modifier, title: String, value: String, note: String) {
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(note, style = MaterialTheme.typography.bodySmall)
        }
    }
}
