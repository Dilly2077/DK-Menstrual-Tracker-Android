package com.dkcycle.app

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
internal fun LogScreen(
    logs: Map<LocalDate, DailyLog>,
    save: (Map<LocalDate, DailyLog>) -> Unit,
    snackbar: SnackbarHostState,
    initialDate: LocalDate = LocalDate.now(),
    showIntimacyMarker: Boolean = false
) {
    var dateRaw by rememberSaveable(initialDate.toString()) { mutableStateOf(initialDate.toString()) }
    val date = LocalDate.parse(dateRaw)
    val existing = logs[date] ?: DailyLog(date)
    var flowName by remember(dateRaw, existing) { mutableStateOf(existing.flow.name) }
    var symptoms by remember(dateRaw, existing) { mutableStateOf(existing.symptoms) }
    var mood by remember(dateRaw, existing) { mutableStateOf(existing.mood) }
    var pain by remember(dateRaw, existing) { mutableStateOf(existing.pain.toFloat()) }
    var intimacy by remember(dateRaw, existing) { mutableStateOf(existing.intimacy) }
    var notes by remember(dateRaw, existing) { mutableStateOf(existing.notes) }
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            PageHeader("Detailed log", date.format(longDate()))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(onClick = { dateRaw = date.minusDays(1).toString() }) { Text("Previous") }
                OutlinedButton(onClick = { dateRaw = LocalDate.now().toString() }, enabled = date != LocalDate.now()) { Text("Today") }
                OutlinedButton(onClick = { dateRaw = date.plusDays(1).toString() }, enabled = date.isBefore(LocalDate.now())) { Text("Next") }
            }
        }
        item {
            LogSection("Bleeding") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(FlowIntensity.entries) { intensity ->
                        FilterChip(
                            selected = flowName == intensity.name,
                            onClick = { flowName = intensity.name },
                            label = { Text(intensity.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }
        }
        if (showIntimacyMarker) {
            item {
                LogSection("Private marker") {
                    FilterChip(
                        selected = intimacy,
                        onClick = { intimacy = !intimacy },
                        label = { Text("💗 Close moment") }
                    )
                    Text(
                        "Optional and private. This marker is not included in the partner view.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            val allSymptoms = listOf("Cramps", "Headache", "Bloating", "Breast tenderness", "Acne", "Fatigue", "Nausea", "Back pain")
            LogSection("Symptoms") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    allSymptoms.chunked(2).forEach { rowSymptoms ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowSymptoms.forEach { symptom ->
                                FilterChip(
                                    selected = symptom in symptoms,
                                    onClick = { symptoms = if (symptom in symptoms) symptoms - symptom else symptoms + symptom },
                                    label = { Text(symptom) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowSymptoms.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        item {
            val moods = listOf("Low", "Irritable", "Okay", "Good", "Energetic")
            LogSection("Mood") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(moods) { value ->
                        FilterChip(selected = mood == value, onClick = { mood = value }, label = { Text(value) })
                    }
                }
            }
        }
        item {
            LogSection("Pain: ${pain.toInt()}/10") {
                Slider(value = pain, onValueChange = { pain = it }, valueRange = 0f..10f, steps = 9)
            }
        }
        item {
            LogSection("Notes") {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    placeholder = { Text("Anything else you want to remember") }
                )
            }
        }
        item {
            Button(
                onClick = {
                    val updatedLog = DailyLog(
                        date = date,
                        flow = FlowIntensity.valueOf(flowName),
                        symptoms = symptoms,
                        mood = mood,
                        pain = pain.toInt(),
                        intimacy = intimacy,
                        notes = notes.trim()
                    )
                    val updated = if (updatedLog.hasMeaningfulData()) logs + (date to updatedLog) else logs - date
                    save(updated)
                    scope.launch { snackbar.showSnackbar(if (updatedLog.hasMeaningfulData()) "Saved ${date.format(shortDate())}" else "Empty day cleared") }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(52.dp)
            ) { Text("Save day") }
        }
    }
}

@Composable
internal fun LogSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        content()
    }
}
