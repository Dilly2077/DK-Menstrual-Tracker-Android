package com.dkcycle.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
internal fun CalendarScreen(
    logs: Map<LocalDate, DailyLog>,
    analysis: CycleAnalysis,
    showIntimacyMarker: Boolean,
    onSave: (Map<LocalDate, DailyLog>) -> Unit,
    onMoreDetails: (LocalDate) -> Unit
) {
    var monthRaw by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    var selectedDateRaw by rememberSaveable { mutableStateOf<String?>(null) }
    val month = YearMonth.parse(monthRaw)
    val first = month.atDay(1)
    val offset = first.dayOfWeek.value - 1
    val cells: List<LocalDate?> = List(offset) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }

    Column(modifier = Modifier.fillMaxSize()) {
        PageHeader("Calendar", "Tap any day for a quick log")
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = { monthRaw = month.minusMonths(1).toString() }) { Text("‹") }
            Text(
                "${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            OutlinedButton(onClick = { monthRaw = month.plusMonths(1).toString() }) { Text("›") }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Text(it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp),
            userScrollEnabled = false
        ) {
            items(cells) { date ->
                CalendarDay(
                    date = date,
                    log = date?.let { logs[it] },
                    analysis = analysis,
                    showIntimacyMarker = showIntimacyMarker,
                    onClick = { if (date != null && !date.isAfter(LocalDate.now())) selectedDateRaw = date.toString() }
                )
            }
        }
        CalendarLegend(showIntimacyMarker)
    }

    val selectedDate = selectedDateRaw?.let(LocalDate::parse)
    if (selectedDate != null) {
        val current = logs[selectedDate] ?: DailyLog(selectedDate)
        AlertDialog(
            onDismissRequest = { selectedDateRaw = null },
            title = { Text(selectedDate.format(longDate())) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Quick log", fontWeight = FontWeight.Bold)
                    Button(
                        onClick = {
                            val updated = current.copy(
                                flow = if (current.flow == FlowIntensity.NONE) FlowIntensity.MEDIUM else FlowIntensity.NONE
                            )
                            onSave(if (updated.hasMeaningfulData()) logs + (selectedDate to updated) else logs - selectedDate)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (current.flow == FlowIntensity.NONE) "🩸  Mark bleeding" else "🩸  Remove bleeding") }
                    if (showIntimacyMarker) {
                        OutlinedButton(
                            onClick = {
                                val updated = current.copy(intimacy = !current.intimacy)
                                onSave(if (updated.hasMeaningfulData()) logs + (selectedDate to updated) else logs - selectedDate)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (current.intimacy) "💗  Remove private marker" else "💗  Add private marker") }
                    }
                    Text(
                        "Use the full Log screen only when you want to add flow level, symptoms, mood, pain or notes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { selectedDateRaw = null }) { Text("Done") }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    selectedDateRaw = null
                    onMoreDetails(selectedDate)
                }) { Text("More details") }
            }
        )
    }
}

@Composable
internal fun CalendarDay(
    date: LocalDate?,
    log: DailyLog?,
    analysis: CycleAnalysis,
    showIntimacyMarker: Boolean = false,
    onClick: () -> Unit = {}
) {
    if (date == null) {
        Box(Modifier.size(52.dp))
        return
    }
    val predictedPeriod = analysis.nextPeriodStart != null && analysis.predictedPeriodEnd != null &&
        !date.isBefore(analysis.nextPeriodStart) && !date.isAfter(analysis.predictedPeriodEnd)
    val fertile = analysis.fertileStart != null && analysis.fertileEnd != null &&
        !date.isBefore(analysis.fertileStart) && !date.isAfter(analysis.fertileEnd)
    val loggedPeriod = log?.flow != null && log.flow != FlowIntensity.NONE
    val predictedStart = date == analysis.nextPeriodStart
    val background = when {
        loggedPeriod -> MaterialTheme.colorScheme.primary
        predictedPeriod -> MaterialTheme.colorScheme.primaryContainer
        fertile -> MaterialTheme.colorScheme.secondaryContainer
        else -> Color.Transparent
    }
    val foreground = if (loggedPeriod) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier.padding(2.dp).size(52.dp).background(background, CircleShape).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(date.dayOfMonth.toString(), color = foreground)
        Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
            if (loggedPeriod) Text("🩸", style = MaterialTheme.typography.labelSmall)
            if (showIntimacyMarker && log?.intimacy == true) Text("💗", style = MaterialTheme.typography.labelSmall)
            if (predictedStart && !loggedPeriod) Text("✦", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
internal fun CalendarLegend(showIntimacyMarker: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Text("🩸 logged", style = MaterialTheme.typography.labelSmall)
            Text("✦ predicted start", style = MaterialTheme.typography.labelSmall)
            Text("Pale = estimate", style = MaterialTheme.typography.labelSmall)
        }
        if (showIntimacyMarker) {
            Text("💗 private marker", style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}
