package com.dkcycle.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
internal fun SettingsScreen(
    logs: Map<LocalDate, DailyLog>,
    save: (Map<LocalDate, DailyLog>) -> Unit,
    store: LocalStore,
    snackbar: SnackbarHostState,
    intimacyMarkerEnabled: Boolean,
    onIntimacyMarkerChanged: (Boolean) -> Unit,
    appearance: AppearanceMode,
    onAppearanceChanged: (AppearanceMode) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirmDelete by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(store.exportJson(logs)) }
            }.onSuccess { scope.launch { snackbar.showSnackbar("Export complete") } }
                .onFailure { scope.launch { snackbar.showSnackbar("Export failed") } }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            runCatching {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Could not read file")
                store.importJson(text)
            }.onSuccess {
                save(it)
                scope.launch { snackbar.showSnackbar("Import complete") }
            }.onFailure { scope.launch { snackbar.showSnackbar("Import failed: ${it.message ?: "invalid file"}") } }
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 30.dp)) {
        item { PageHeader("Settings", "Privacy, appearance and data controls") }
        item {
            SettingsCard("Appearance", "Follow your phone or choose a fixed theme.") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppearanceMode.entries.forEach { mode ->
                        FilterChip(
                            selected = appearance == mode,
                            onClick = { onAppearanceChanged(mode) },
                            label = {
                                Text(when (mode) {
                                    AppearanceMode.SYSTEM -> "System"
                                    AppearanceMode.LIGHT -> "Light"
                                    AppearanceMode.DARK -> "Dark"
                                })
                            }
                        )
                    }
                }
            }
        }
        item {
            SettingsCard(
                "Home widgets",
                "Add a live cycle summary or a colour-coded month calendar. Both read only from Lunara's local data."
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        if (!requestLunaraWidget(context, LunaraWidgetKind.STATUS)) {
                            scope.launch { snackbar.showSnackbar("Open your launcher's widget picker to add Lunara") }
                        }
                    }) { Text("Add cycle widget") }
                    OutlinedButton(onClick = {
                        if (!requestLunaraWidget(context, LunaraWidgetKind.CALENDAR)) {
                            scope.launch { snackbar.showSnackbar("Open your launcher's widget picker to add Lunara") }
                        }
                    }) { Text("Add calendar widget") }
                }
            }
        }
        item {
            SettingsCard(
                "Optional private marker",
                "Show a discreet heart option for close moments. It stays out of the partner view."
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Show 💗 marker")
                    Switch(checked = intimacyMarkerEnabled, onCheckedChange = onIntimacyMarkerChanged)
                }
            }
        }
        item {
            SettingsCard(
                "Privacy by design",
                "Cycle data stays on this device. There is no INTERNET permission, advertising or analytics."
            )
        }
        item {
            SettingsCard(
                "Prediction model",
                "Predictions use period-start history only. Ovulation and fertile timing remain calendar estimates, not measurements."
            )
        }
        item {
            SettingsCard("Home shortcut", "Pin the Lunara app shortcut.") {
                Button(onClick = {
                    if (!requestLunaraHomeShortcut(context)) {
                        scope.launch { snackbar.showSnackbar("Your launcher does not support app-requested pinning") }
                    }
                }) { Text("Add shortcut") }
            }
        }
        item {
            SettingsCard("Export & restore", "Create a JSON backup or restore an existing Lunara/DKCycle backup.") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { exportLauncher.launch("Lunara-backup-${LocalDate.now()}.json") }) { Text("Export") }
                    OutlinedButton(onClick = { importLauncher.launch("application/json") }) { Text("Import") }
                }
            }
        }
        item {
            SettingsCard(
                "Medical limitations",
                "Predictions are estimates. Do not use them as contraception, diagnosis, pregnancy testing or a substitute for clinical advice."
            )
        }
        item {
            SettingsCard("Data", "Delete all locally stored cycle logs from this installation.") {
                Button(
                    onClick = { confirmDelete = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete all data") }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete all Lunara data?") },
            text = { Text("This cannot be undone unless you have an exported backup.") },
            confirmButton = {
                Button(onClick = {
                    save(emptyMap())
                    store.clearLegacyPartnerPass()
                    confirmDelete = false
                    scope.launch { snackbar.showSnackbar("All local health data deleted") }
                }) { Text("Delete") }
            },
            dismissButton = { OutlinedButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}

@Composable
internal fun SettingsCard(title: String, body: String, content: (@Composable () -> Unit)? = null) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 7.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(body)
            content?.invoke()
        }
    }
}

internal fun shortDate(): DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")
internal fun longDate(): DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")
