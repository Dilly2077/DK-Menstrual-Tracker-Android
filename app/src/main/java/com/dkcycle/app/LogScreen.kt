package com.dkcycle.app

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.time.LocalDate

private data class MoodOption(val value: String, val emoji: String, val color: Color)
private data class SymptomOption(val value: String, val emoji: String, val color: Color)

private val moodOptions = listOf(
    MoodOption("Terrible", "😭", Color(0xFFF3D6DE)),
    MoodOption("Low", "😔", Color(0xFFE8DDF3)),
    MoodOption("Anxious", "😟", Color(0xFFDDE8F3)),
    MoodOption("Irritable", "😠", Color(0xFFF6DDD4)),
    MoodOption("Sensitive", "🥺", Color(0xFFF6E2EB)),
    MoodOption("Okay", "😐", Color(0xFFE6E3E4)),
    MoodOption("Calm", "😌", Color(0xFFDDEDE4)),
    MoodOption("Good", "🙂", Color(0xFFE7EED7)),
    MoodOption("Happy", "😊", Color(0xFFFFE8B8)),
    MoodOption("Energetic", "🤩", Color(0xFFFFDFB8)),
    MoodOption("Loved", "🥰", Color(0xFFFFDCE5)),
    MoodOption("Tired", "😴", Color(0xFFDCE0EE))
)

private val symptomOptions = listOf(
    SymptomOption("Cramps", "〰️", Color(0xFFF8DCE4)),
    SymptomOption("Headache", "🤕", Color(0xFFE4DDF3)),
    SymptomOption("Bloating", "🎈", Color(0xFFFBE5CC)),
    SymptomOption("Breast tenderness", "💗", Color(0xFFF8DFEA)),
    SymptomOption("Acne", "✨", Color(0xFFE8E2F3)),
    SymptomOption("Fatigue", "🥱", Color(0xFFDDE4F2)),
    SymptomOption("Nausea", "🤢", Color(0xFFE3EDD8)),
    SymptomOption("Back pain", "😣", Color(0xFFF2E1D8)),
    SymptomOption("Cravings", "🍫", Color(0xFFF5E4D3)),
    SymptomOption("Dizziness", "😵‍💫", Color(0xFFE0E8F0)),
    SymptomOption("Trouble sleeping", "🌙", Color(0xFFE4DFF3)),
    SymptomOption("Digestive changes", "🌿", Color(0xFFDDEBDD))
)

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
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            PageHeader("Log", date.format(longDate()))
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
            LogSection("Period", Color(0x18C65F7C)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(FlowIntensity.entries) { intensity ->
                        FilterChip(
                            selected = flowName == intensity.name,
                            onClick = { flowName = intensity.name },
                            label = { Text(flowLabel(intensity)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }
        if (showIntimacyMarker) {
            item {
                LogSection("Private marker", Color(0x18D66E9B)) {
                    FilterChip(
                        selected = intimacy,
                        onClick = { intimacy = !intimacy },
                        label = { Text("💗 Close moment") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                }
            }
        }
        item {
            LogSection("Symptoms", Color(0x148B6AA3)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    symptomOptions.chunked(2).forEach { rowSymptoms ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowSymptoms.forEach { option ->
                                FilterChip(
                                    selected = option.value in symptoms,
                                    onClick = {
                                        symptoms = if (option.value in symptoms) symptoms - option.value else symptoms + option.value
                                    },
                                    label = { Text("${option.emoji} ${option.value}") },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = option.color.copy(alpha = 0.55f),
                                        selectedContainerColor = option.color
                                    )
                                )
                            }
                            if (rowSymptoms.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        item {
            LogSection("Mood", Color(0x14E1A94D)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(moodOptions) { option ->
                        FilterChip(
                            selected = mood == option.value,
                            onClick = { mood = if (mood == option.value) "" else option.value },
                            label = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(option.emoji, style = MaterialTheme.typography.titleMedium)
                                    Text(option.value, style = MaterialTheme.typography.labelSmall)
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = option.color.copy(alpha = 0.55f),
                                selectedContainerColor = option.color
                            )
                        )
                    }
                }
            }
        }
        item {
            LogSection("Pain", Color(0x14D27679)) {
                PainScale(pain = pain, onPainChanged = { pain = it })
            }
        }
        item {
            LogSection("Notes", Color(0x126F5A73)) {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    placeholder = { Text("Anything else you want to remember") },
                    shape = RoundedCornerShape(20.dp)
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
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(52.dp),
                shape = RoundedCornerShape(18.dp)
            ) { Text("Save day") }
        }
    }
}

private fun flowLabel(intensity: FlowIntensity): String = when (intensity) {
    FlowIntensity.NONE -> "None"
    FlowIntensity.SPOTTING -> "Spotting"
    FlowIntensity.LIGHT -> "Light"
    FlowIntensity.MEDIUM -> "Medium"
    FlowIntensity.HEAVY -> "Heavy"
}

@Composable
private fun PainScale(pain: Float, onPainChanged: (Float) -> Unit) {
    val emoji = when (pain.toInt()) {
        0, 1 -> "😌"
        2, 3 -> "🙂"
        4, 5 -> "😐"
        6, 7 -> "😣"
        8, 9 -> "😖"
        else -> "😭"
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface, CircleShape)
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, style = MaterialTheme.typography.headlineSmall)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("${pain.toInt()}/10", fontWeight = FontWeight.Bold)
            Slider(value = pain, onValueChange = onPainChanged, valueRange = 0f..10f, steps = 9)
        }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf("😌", "🙂", "😐", "😣", "😖", "😭").forEach { face ->
            Text(face, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf("0", "2", "4", "6", "8", "10").forEach { value ->
            Text(value, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
internal fun LogSection(title: String, tint: Color = MaterialTheme.colorScheme.surfaceVariant, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .background(tint, RoundedCornerShape(28.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        content()
    }
}
