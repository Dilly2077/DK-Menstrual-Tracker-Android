package com.dkcycle.app

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon as AndroidIcon
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.darkColorScheme
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

private val LunaraLightColors = lightColorScheme(
    primary = Color(0xFF9B4965),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9E4),
    onPrimaryContainer = Color(0xFF3D0D20),
    secondary = Color(0xFF6F5A73),
    secondaryContainer = Color(0xFFF5DDF4),
    background = Color(0xFFFFFAFB),
    surface = Color(0xFFFFFAFB),
    surfaceVariant = Color(0xFFF8EEF2),
    onSurface = Color(0xFF23191D),
    onSurfaceVariant = Color(0xFF58474E),
    outline = Color(0xFF8A7A80)
)

private val LunaraDarkColors = darkColorScheme(
    primary = Color(0xFFFFB0C7),
    onPrimary = Color(0xFF5F1834),
    primaryContainer = Color(0xFF7C304E),
    onPrimaryContainer = Color(0xFFFFD9E4),
    secondary = Color(0xFFD7BED7),
    onSecondary = Color(0xFF3D2C40),
    secondaryContainer = Color(0xFF564258),
    onSecondaryContainer = Color(0xFFF5DDF4),
    background = Color(0xFF171216),
    surface = Color(0xFF171216),
    surfaceVariant = Color(0xFF332A2F),
    onSurface = Color(0xFFF0E5E9),
    onSurfaceVariant = Color(0xFFD7C4CB),
    outline = Color(0xFFA18C94)
)

@Composable
fun LunaraApp() {
    val context = LocalContext.current
    val store = remember { LocalStore(context.applicationContext) }
    var appearance by remember { mutableStateOf(store.appearanceMode()) }
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (appearance) {
        AppearanceMode.SYSTEM -> systemDark
        AppearanceMode.LIGHT -> false
        AppearanceMode.DARK -> true
    }
    val colors = if (darkTheme) LunaraDarkColors else LunaraLightColors

    MaterialTheme(colorScheme = colors) {
        var logs by remember { mutableStateOf(store.loadLogs()) }
        var screenName by rememberSaveable { mutableStateOf(Screen.TODAY.name) }
        var previousScreenName by rememberSaveable { mutableStateOf(Screen.TODAY.name) }
        var logDateRaw by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
        var intimacyMarkerEnabled by remember { mutableStateOf(store.intimacyMarkerEnabled()) }
        val screen = Screen.valueOf(screenName)
        val analysis = remember(logs) { CycleEngine.analyse(logs.values.toList()) }
        val snackbar = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        var showHomePrompt by remember { mutableStateOf(!store.hasShownHomeShortcutPrompt()) }
        val primaryScreens = remember {
            listOf(Screen.TODAY, Screen.CALENDAR, Screen.LOG, Screen.INSIGHTS, Screen.LEARN, Screen.SETTINGS)
        }

        fun save(updated: Map<LocalDate, DailyLog>) {
            logs = updated.filterValues { it.hasMeaningfulData() }
            store.saveLogs(logs)
        }

        fun navigate(destination: Screen) {
            if (destination != screen) {
                previousScreenName = screenName
                screenName = destination.name
            }
        }

        fun openLog(date: LocalDate) {
            logDateRaw = date.toString()
            navigate(Screen.LOG)
        }

        BackHandler(enabled = screen != Screen.TODAY) {
            val destination = runCatching { Screen.valueOf(previousScreenName) }.getOrDefault(Screen.TODAY)
            screenName = destination.name
            previousScreenName = Screen.TODAY.name
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                NavigationBar {
                    primaryScreens.forEach { destination ->
                        NavigationBarItem(
                            selected = screen == destination,
                            onClick = { navigate(destination) },
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
                        onLog = { openLog(LocalDate.now()) },
                        onPartner = { navigate(Screen.PARTNER) }
                    )
                    Screen.CALENDAR -> CalendarScreen(
                        logs = logs,
                        analysis = analysis,
                        showIntimacyMarker = intimacyMarkerEnabled,
                        onSave = ::save,
                        onMoreDetails = ::openLog
                    )
                    Screen.LOG -> LogScreen(
                        logs = logs,
                        save = ::save,
                        snackbar = snackbar,
                        initialDate = LocalDate.parse(logDateRaw),
                        showIntimacyMarker = intimacyMarkerEnabled
                    )
                    Screen.INSIGHTS -> InsightsScreen(logs, analysis)
                    Screen.LEARN -> LearnScreen()
                    Screen.SETTINGS -> SettingsScreen(
                        logs = logs,
                        save = ::save,
                        store = store,
                        snackbar = snackbar,
                        intimacyMarkerEnabled = intimacyMarkerEnabled,
                        onIntimacyMarkerChanged = {
                            intimacyMarkerEnabled = it
                            store.setIntimacyMarkerEnabled(it)
                        },
                        appearance = appearance,
                        onAppearanceChanged = {
                            appearance = it
                            store.setAppearanceMode(it)
                        }
                    )
                    Screen.PARTNER -> PartnerScreen(logs) {
                        screenName = previousScreenName
                        previousScreenName = Screen.TODAY.name
                    }
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
                text = { Text("Lunara can ask Android to pin a Home-screen shortcut so it is easier to find.") },
                confirmButton = {
                    Button(onClick = {
                        store.markHomeShortcutPromptShown()
                        showHomePrompt = false
                        if (!requestLunaraHomeShortcut(context)) {
                            scope.launch { snackbar.showSnackbar("Your launcher does not support app-requested pinning") }
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
    val todayLog = logs[LocalDate.now()]?.takeIf { it.hasMeaningfulData() }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { PageHeader("Lunara", "Simple cycle tracking, detail when you want it") }
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            analysis.cycleDay?.let { "Cycle day $it" } ?: "Start by marking a bleeding day",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(analysis.phase, style = MaterialTheme.typography.headlineSmall)
                        Text(analysis.phaseExplanation)
                        Button(onClick = onLog) { Text("Detailed log") }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Next period",
                        value = analysis.nextPeriodStart?.format(shortDate()) ?: "Learning",
                        note = when {
                            analysis.nextPeriodStart == null -> "Add or continue logging periods"
                            else -> "${analysis.confidence} confidence"
                        }
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Typical cycle",
                        value = "${analysis.averageCycleLength} days",
                        note = if (analysis.usableCycleCount == 0) "Prior while learning" else "Personalised estimate"
                    )
                }

                if (analysis.predictionWindowStart != null && analysis.predictionWindowEnd != null) {
                    Spacer(Modifier.height(12.dp))
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Prediction range", fontWeight = FontWeight.Bold)
                            Text(
                                "Most likely start ${analysis.nextPeriodStart?.format(shortDate())}. Approximate 80% range ${analysis.predictionWindowStart.format(shortDate())}–${analysis.predictionWindowEnd.format(shortDate())}."
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Calendar quick log", fontWeight = FontWeight.Bold)
                        Text("Tap a date in Calendar to mark 🩸 bleeding in one tap. Use Detailed log only when you want more information.")
                    }
                }

                Spacer(Modifier.height(12.dp))
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Partner", fontWeight = FontWeight.Bold)
                        Text("Partner sharing is moving to an ongoing paired view instead of one-off snapshots.")
                        OutlinedButton(onClick = onPartner) { Text("Open Partner") }
                    }
                }

                if (todayLog != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        buildString {
                            append("Today: ")
                            if (todayLog.flow != FlowIntensity.NONE) append("🩸 ")
                            if (todayLog.intimacy) append("💗 ")
                            if (todayLog.symptoms.isNotEmpty() || todayLog.mood.isNotBlank() || todayLog.pain > 0 || todayLog.notes.isNotBlank()) append("details saved")
                        }.trim(),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
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
