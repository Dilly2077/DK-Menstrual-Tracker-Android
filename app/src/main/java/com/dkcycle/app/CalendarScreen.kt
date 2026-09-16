package com.dkcycle.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

internal val LoggedPeriodColor = Color(0xFFAD4B6B)
internal val PredictedPeriodColor = Color(0xFFF9D5E2)
internal val FertileWindowColor = Color(0xFFEADCF6)
internal val OvulationEstimateColor = Color(0xFF8B6AA3)
internal val LutealPhaseColor = Color(0xFFFFE5D5)
private val CalendarPastelText = Color(0xFF38262D)
private const val CALENDAR_CENTER_PAGE = 1200
private const val CALENDAR_PAGE_COUNT = 2401

@Composable
internal fun CalendarScreen(
    logs: Map<LocalDate, DailyLog>,
    analysis: CycleAnalysis,
    showIntimacyMarker: Boolean,
    onSave: (Map<LocalDate, DailyLog>) -> Unit,
    onMoreDetails: (LocalDate) -> Unit
) {
    val anchorMonth = remember { YearMonth.now() }
    val pagerState = rememberPagerState(
        initialPage = CALENDAR_CENTER_PAGE,
        pageCount = { CALENDAR_PAGE_COUNT }
    )
    val scope = rememberCoroutineScope()
    var selectedDateRaw by rememberSaveable { mutableStateOf<String?>(null) }
    val month = anchorMonth.plusMonths((pagerState.currentPage - CALENDAR_CENTER_PAGE).toLong())

    Column(modifier = Modifier.fillMaxSize()) {
        PageHeader("Calendar", "Swipe months • tap a day to log")
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = {
                    if (pagerState.currentPage > 0) scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                }
            ) { Text("‹") }
            Text(
                "${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            OutlinedButton(
                onClick = {
                    if (pagerState.currentPage < CALENDAR_PAGE_COUNT - 1) scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            ) { Text("›") }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) { page ->
            val pageMonth = anchorMonth.plusMonths((page - CALENDAR_CENTER_PAGE).toLong())
            MonthPage(
                month = pageMonth,
                logs = logs,
                analysis = analysis,
                showIntimacyMarker = showIntimacyMarker,
                onDateClick = { date -> if (!date.isAfter(LocalDate.now())) selectedDateRaw = date.toString() }
            )
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
                    ) { Text(if (current.flow == FlowIntensity.NONE) "Mark period" else "Remove period") }
                    if (showIntimacyMarker) {
                        OutlinedButton(
                            onClick = {
                                val updated = current.copy(intimacy = !current.intimacy)
                                onSave(if (updated.hasMeaningfulData()) logs + (selectedDate to updated) else logs - selectedDate)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (current.intimacy) "Remove private marker" else "Add private marker") }
                    }
                    Text(
                        "Use Detailed log for flow level, symptoms, mood, pain or notes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = { Button(onClick = { selectedDateRaw = null }) { Text("Done") } },
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
private fun MonthPage(
    month: YearMonth,
    logs: Map<LocalDate, DailyLog>,
    analysis: CycleAnalysis,
    showIntimacyMarker: Boolean,
    onDateClick: (LocalDate) -> Unit
) {
    val cells = remember(month) {
        val first = month.atDay(1)
        val offset = first.dayOfWeek.value - 1
        List<LocalDate?>(offset) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
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
                    onClick = { if (date != null) onDateClick(date) }
                )
            }
        }
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
        Box(Modifier.fillMaxWidth().aspectRatio(1f))
        return
    }

    val predictedPeriod = analysis.nextPeriodStart != null && analysis.predictedPeriodEnd != null &&
        !date.isBefore(analysis.nextPeriodStart) && !date.isAfter(analysis.predictedPeriodEnd)
    val predictedStart = date == analysis.nextPeriodStart
    val estimatedOvulation = analysis.estimatedOvulation != null && date == analysis.estimatedOvulation
    val fertileWindow = analysis.fertileStart != null && analysis.fertileEnd != null &&
        !date.isBefore(analysis.fertileStart) && !date.isAfter(analysis.fertileEnd) && !estimatedOvulation
    val lutealPhase = analysis.lutealStart != null && analysis.lutealEnd != null &&
        !date.isBefore(analysis.lutealStart) && !date.isAfter(analysis.lutealEnd)
    val loggedPeriod = log?.flow != null && log.flow != FlowIntensity.NONE

    val background = when {
        loggedPeriod -> LoggedPeriodColor
        predictedPeriod -> PredictedPeriodColor
        estimatedOvulation -> OvulationEstimateColor
        fertileWindow -> FertileWindowColor
        lutealPhase -> LutealPhaseColor
        else -> Color.Transparent
    }
    val foreground = when {
        loggedPeriod || estimatedOvulation -> Color.White
        predictedPeriod || fertileWindow || lutealPhase -> CalendarPastelText
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = Modifier
            .padding(3.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(background, CircleShape)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(date.dayOfMonth.toString(), color = foreground)
        Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
            if (loggedPeriod) Text("•", color = Color.White, style = MaterialTheme.typography.labelSmall)
            if (showIntimacyMarker && log?.intimacy == true) Text("💗", style = MaterialTheme.typography.labelSmall)
            if (predictedStart && !loggedPeriod) {
                Text("✦", color = LoggedPeriodColor, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
internal fun CalendarLegend(showIntimacyMarker: Boolean = false) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text("Calendar key", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LegendItem(Modifier.weight(1f), LoggedPeriodColor, null, "Logged period", lightText = true)
            LegendItem(Modifier.weight(1f), PredictedPeriodColor, "✦", "Predicted start")
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LegendItem(Modifier.weight(1f), PredictedPeriodColor, null, "Predicted period")
            LegendItem(Modifier.weight(1f), FertileWindowColor, null, "Fertile window")
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LegendItem(Modifier.weight(1f), OvulationEstimateColor, null, "Ovulation estimate", lightText = true)
            LegendItem(Modifier.weight(1f), LutealPhaseColor, null, "Luteal phase")
        }
        if (showIntimacyMarker) {
            Row(modifier = Modifier.fillMaxWidth()) {
                LegendItem(Modifier.weight(1f), Color.Transparent, "💗", "Private marker")
                Box(Modifier.weight(1f))
            }
        }
        Text(
            "Fertile window, ovulation and luteal phase colours are date-based estimates.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LegendItem(
    modifier: Modifier,
    background: Color,
    symbol: String?,
    label: String,
    lightText: Boolean = false
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(
            modifier = Modifier.size(24.dp).background(background, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (symbol != null) {
                Text(
                    symbol,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (lightText) Color.White else CalendarPastelText
                )
            }
        }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
