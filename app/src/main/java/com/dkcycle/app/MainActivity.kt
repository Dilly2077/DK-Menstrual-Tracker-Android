package com.dkcycle.app

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon as AndroidIcon
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LunaraApp() }
    }
}

internal enum class Screen(val label: String, val icon: ImageVector) {
    TODAY("Today", Icons.Default.Home),
    CALENDAR("Calendar", Icons.Default.CalendarMonth),
    LOG("Log", Icons.Default.AddCircle),
    INSIGHTS("Insights", Icons.Default.Insights),
    LEARN("Learn", Icons.Default.MenuBook),
    SETTINGS("Settings", Icons.Default.Settings),
    PARTNER("Partner", Icons.Default.Favorite)
}

@Composable
fun LunaraApp() {
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
        val scope = rememberCoroutineScope()
        var showHomePrompt by remember { mutableStateOf(!store.hasShownHomeShortcutPrompt()) }
        val primaryScreens = remember {
            listOf(Screen.TODAY, Screen.CALENDAR, Screen.LOG, Screen.INSIGHTS, Screen.LEARN, Screen.SETTINGS)
        }

        fun save(updated: Map<LocalDate, DailyLog>) {
            logs = updated
            store.saveLogs(updated)
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                NavigationBar {
                    primaryScreens.forEach { destination ->
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
                    Screen.TODAY -> TodayScreen(
                        analysis = analysis,
                        logs = logs,
                        onLog = { screenName = Screen.LOG.name },
                        onPartner = { screenName = Screen.PARTNER.name }
                    )
                    Screen.CALENDAR -> CalendarScreen(logs, analysis)
                    Screen.LOG -> LogScreen(logs, ::save, snackbar)
                    Screen.INSIGHTS -> InsightsScreen(logs, analysis)
                    Screen.LEARN -> LearnScreen()
                    Screen.SETTINGS -> SettingsScreen(logs, ::save, store, snackbar)
                    Screen.PARTNER -> PartnerScreen(logs, store, snackbar) { screenName = Screen.TODAY.name }
                }
            }
        }

        if (showHomePrompt) {
            AlertDialog(
                onDismissRequest = {
                    store.markHomeShortcutPromptShown()
                    showHomePrompt = false
                },
                title = { Text("Add Lunara to your Home screen?") },
                text = {
                    Text("Android has installed Lunara in your app drawer. Lunara can ask your launcher to add a Home-screen shortcut so it is easier to find.")
                },
                confirmButton = {
                    Button(onClick = {
                        store.markHomeShortcutPromptShown()
                        showHomePrompt = false
                        if (!requestLunaraHomeShortcut(context)) {
                            scope.launch { snackbar.showSnackbar("Your current launcher does not support app-requested pinning") }
                        }
                    }) { Text("Add to Home") }
                },
                dismissButton = {
                    OutlinedButton(onClick = {
                        store.markHomeShortcutPromptShown()
                        showHomePrompt = false
                    }) { Text("Not now") }
                }
            )
        }
    }
}

internal fun requestLunaraHomeShortcut(context: Context): Boolean {
    val manager = context.getSystemService(ShortcutManager::class.java) ?: return false
    if (!manager.isRequestPinShortcutSupported) return false
    val launchIntent = Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_MAIN
        addCategory(Intent.CATEGORY_LAUNCHER)
    }
    val shortcut = ShortcutInfo.Builder(context, "lunara-home")
        .setShortLabel("Lunara")
        .setLongLabel("Open Lunara")
        .setIcon(AndroidIcon.createWithResource(context, R.mipmap.ic_launcher))
        .setIntent(launchIntent)
        .build()
    return manager.requestPinShortcut(shortcut, null)
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
internal fun TodayScreen(
    analysis: CycleAnalysis,
    logs: Map<LocalDate, DailyLog>,
    onLog: () -> Unit,
    onPartner: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item { PageHeader("Lunara", "Private, local cycle tracking") }
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
                        Text(analysis.phaseExplanation.replace("DKCycle", "Lunara"))
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

                Spacer(Modifier.height(12.dp))
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Partner", fontWeight = FontWeight.Bold)
                        Text("Share a read-only cycle snapshot with someone you trust, or import a Partner Pass they sent you.")
                        OutlinedButton(onClick = onPartner) { Text("Open Partner") }
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
