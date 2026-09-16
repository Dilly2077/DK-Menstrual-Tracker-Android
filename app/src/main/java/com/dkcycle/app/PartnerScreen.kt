package com.dkcycle.app

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Base64
import java.util.Locale

data class PartnerSnapshot(
    val generatedAt: Instant,
    val logs: Map<LocalDate, DailyLog>
)

object PartnerPassCodec {
    private const val PREFIX = "LUNARA1-"

    fun encode(logs: Map<LocalDate, DailyLog>, now: Instant = Instant.now()): String {
        val cutoff = LocalDate.now().minusMonths(24)
        val sharedDays = logs.values
            .filter { it.flow != FlowIntensity.NONE && !it.date.isBefore(cutoff) }
            .sortedBy { it.date }
            .joinToString(";") { "${it.date},${it.flow.name}" }
        val payload = "v1|${now.toEpochMilli()}|$sharedDays"
        return PREFIX + Base64.getUrlEncoder().withoutPadding()
            .encodeToString(payload.toByteArray(Charsets.UTF_8))
    }

    fun decode(raw: String): PartnerSnapshot {
        val code = extract(raw)
        require(code.startsWith(PREFIX)) { "No Lunara Partner Pass found" }
        val encoded = code.removePrefix(PREFIX)
        val payload = String(Base64.getUrlDecoder().decode(encoded), Charsets.UTF_8)
        val parts = payload.split("|", limit = 3)
        require(parts.size == 3 && parts[0] == "v1") { "Unsupported Partner Pass" }
        val generatedAt = Instant.ofEpochMilli(parts[1].toLong())
        val logs = linkedMapOf<LocalDate, DailyLog>()
        if (parts[2].isNotBlank()) {
            parts[2].split(';').forEach { item ->
                val fields = item.split(',', limit = 2)
                require(fields.size == 2) { "Invalid shared cycle day" }
                val date = LocalDate.parse(fields[0])
                val flow = FlowIntensity.valueOf(fields[1])
                logs[date] = DailyLog(date = date, flow = flow)
            }
        }
        return PartnerSnapshot(generatedAt, logs)
    }

    private fun extract(raw: String): String {
        val start = raw.indexOf(PREFIX)
        require(start >= 0) { "No Lunara Partner Pass found" }
        return raw.substring(start)
            .takeWhile { !it.isWhitespace() }
            .trimEnd('.', ',', ';')
    }
}

@Composable
internal fun PartnerScreen(
    logs: Map<LocalDate, DailyLog>,
    store: LocalStore,
    snackbar: SnackbarHostState,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var passInput by rememberSaveable { mutableStateOf("") }
    var snapshot by remember {
        mutableStateOf(store.loadPartnerPass()?.let { runCatching { PartnerPassCodec.decode(it) }.getOrNull() })
    }
    val sharedAnalysis = remember(snapshot) { snapshot?.let { CycleEngine.analyse(it.logs.values.toList()) } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            PageHeader("Partner", "Read-only sharing without a Lunara account") {
                OutlinedButton(onClick = onBack) { Text("Done") }
            }
        }

        item {
            PartnerCard(
                title = "Share your cycle",
                body = "Create a Partner Pass containing only bleeding dates needed for the shared calendar and cycle estimates. Symptoms, mood, pain, temperature and notes are not included."
            ) {
                Button(onClick = {
                    val analysis = CycleEngine.analyse(logs.values.toList())
                    if (analysis.periodStarts.isEmpty()) {
                        scope.launch { snackbar.showSnackbar("Log at least one period before sharing") }
                    } else {
                        val pass = PartnerPassCodec.encode(logs)
                        val message = buildString {
                            appendLine("I’m sharing a read-only Lunara cycle snapshot with you.")
                            appendLine()
                            appendLine("Open Lunara → Partner and paste this Partner Pass:")
                            appendLine(pass)
                            appendLine()
                            append("This is a snapshot, not a live account link. Only share it with someone you trust.")
                        }
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Lunara Partner Pass")
                            putExtra(Intent.EXTRA_TEXT, message)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Partner Pass"))
                    }
                }) { Text("Create & share Partner Pass") }
            }
        }

        item {
            PartnerCard(
                title = "View a partner’s cycle",
                body = "Paste a Partner Pass you received. It is stored only on this device and opens as a read-only shared view."
            ) {
                OutlinedTextField(
                    value = passInput,
                    onValueChange = { passInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                    label = { Text("Partner Pass") },
                    placeholder = { Text("LUNARA1-…") }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        passInput = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                    }) { Text("Paste") }
                    Button(onClick = {
                        runCatching { PartnerPassCodec.decode(passInput) }
                            .onSuccess {
                                snapshot = it
                                store.savePartnerPass(passInput)
                                scope.launch { snackbar.showSnackbar("Partner view updated") }
                            }
                            .onFailure {
                                scope.launch { snackbar.showSnackbar(it.message ?: "Invalid Partner Pass") }
                            }
                    }) { Text("Open") }
                }
            }
        }

        if (snapshot != null && sharedAnalysis != null) {
            item {
                val updated = snapshot!!.generatedAt.atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm"))
                PartnerCard(
                    title = "Shared overview",
                    body = "Snapshot updated $updated. Ask your partner for a new pass whenever you want refreshed data."
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricCard(
                            Modifier.weight(1f),
                            "Cycle day",
                            sharedAnalysis!!.cycleDay?.toString() ?: "—",
                            sharedAnalysis!!.phase
                        )
                        MetricCard(
                            Modifier.weight(1f),
                            "Next period",
                            sharedAnalysis!!.nextPeriodStart?.format(shortDate()) ?: "—",
                            "${sharedAnalysis!!.confidence} confidence"
                        )
                    }
                    Text(
                        "This view deliberately excludes symptoms, mood, pain, notes and temperature.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(onClick = {
                        store.clearPartnerPass()
                        snapshot = null
                        scope.launch { snackbar.showSnackbar("Shared partner view cleared") }
                    }) { Text("Clear partner view") }
                }
            }
            item { PartnerCalendar(snapshot!!.logs, sharedAnalysis!!) }
        }

        item {
            PartnerCard(
                title = "Privacy note",
                body = "Partner Pass is an offline snapshot. Lunara does not upload it and cannot remotely revoke a copy after you send it. Live auto-updating and remotely revocable sharing would require an opt-in encrypted network service in a future version."
            )
        }
    }
}

@Composable
private fun PartnerCard(title: String, body: String, content: (@Composable () -> Unit)? = null) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(body)
            content?.invoke()
        }
    }
}

@Composable
private fun PartnerCalendar(logs: Map<LocalDate, DailyLog>, analysis: CycleAnalysis) {
    var monthRaw by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val month = YearMonth.parse(monthRaw)
    val first = month.atDay(1)
    val offset = first.dayOfWeek.value - 1
    val cells: List<LocalDate?> = List(offset) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Text(
                "Shared calendar",
                modifier = Modifier.padding(horizontal = 18.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = { monthRaw = month.minusMonths(1).toString() }) { Text("‹") }
                Text(
                    "${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}",
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedButton(onClick = { monthRaw = month.plusMonths(1).toString() }) { Text("›") }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                    Text(it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.fillMaxWidth().height(300.dp).padding(horizontal = 12.dp),
                userScrollEnabled = false
            ) {
                items(cells) { date -> CalendarDay(date, logs[date], analysis) }
            }
            CalendarLegend()
        }
    }
}
