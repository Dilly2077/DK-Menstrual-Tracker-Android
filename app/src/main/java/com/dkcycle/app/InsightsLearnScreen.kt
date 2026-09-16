package com.dkcycle.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
internal fun InsightsScreen(logs: Map<LocalDate, DailyLog>, analysis: CycleAnalysis) {
    val cycleLengths = analysis.periodStarts.zipWithNext { a, b -> ChronoUnit.DAYS.between(a, b).toInt() }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 30.dp)) {
        item { PageHeader("Insights", "Your patterns, kept on this device") }
        item {
            Row(modifier = Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(Modifier.weight(1f), "Cycle", "${analysis.averageCycleLength} days", "Personalised estimate")
                MetricCard(Modifier.weight(1f), "Period", "${analysis.averagePeriodLength} days", "Recent median")
            }
        }
        item {
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(Modifier.weight(1f), "Uncertainty", "%.1f days".format(analysis.variabilityDays), "Model spread")
                MetricCard(Modifier.weight(1f), "Prediction", analysis.confidence, "Narrows with history")
            }
        }
        if (analysis.predictionWindowStart != null && analysis.predictionWindowEnd != null) {
            item {
                Spacer(Modifier.height(12.dp))
                LearnCard(
                    "Current prediction range",
                    "Most likely next period: ${analysis.nextPeriodStart?.format(shortDate())}. Approximate 80% model window: ${analysis.predictionWindowStart.format(shortDate())} – ${analysis.predictionWindowEnd.format(shortDate())}."
                )
            }
        }
        item {
            Spacer(Modifier.height(20.dp))
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text("Observed start-to-start gaps", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                if (cycleLengths.isEmpty()) {
                    Text("Log at least two period starts to calculate a cycle interval.")
                } else {
                    cycleLengths.takeLast(8).forEachIndexed { index, days ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
                            Text("Interval ${index + 1}", modifier = Modifier.weight(1f))
                            Text("$days days", fontWeight = FontWeight.SemiBold)
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(20.dp))
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text("Commonly logged symptoms", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                val counts = logs.values.flatMap { it.symptoms }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }
                if (counts.isEmpty()) Text("No symptom trends yet.")
                counts.take(6).forEach { (symptom, count) ->
                    Text("$symptom · $count day${if (count == 1) "" else "s"}", modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

private data class LearnTopic(
    val title: String,
    val shortTitle: String,
    val imageRes: Int,
    val summary: String,
    val timing: String,
    val hormonePattern: String,
    val details: String,
    val note: String
)

@Composable
internal fun LearnScreen() {
    val topics = remember {
        listOf(
            LearnTopic(
                title = "Cycle overview",
                shortTitle = "Overview",
                imageRes = R.drawable.learn_overview,
                summary = "A cycle moves through menstrual, follicular, ovulation and luteal phases. The timing is different for everyone.",
                timing = "Whole cycle",
                hormonePattern = "Hormones rise and fall",
                details = "Day 1 is the first day of menstrual bleeding. Follicles then develop in the ovary, ovulation may occur later in the cycle, and the luteal phase follows before the next period.",
                note = "Lunara estimates timing from period history. It cannot confirm ovulation from dates alone."
            ),
            LearnTopic(
                title = "Menstrual phase",
                shortTitle = "Menstrual",
                imageRes = R.drawable.learn_menstrual,
                summary = "Bleeding begins as the uterine lining sheds. This is counted as the start of a new cycle.",
                timing = "First days of the cycle",
                hormonePattern = "Oestrogen + progesterone low",
                details = "Oestrogen and progesterone are relatively low. The endometrium built during the previous cycle is shed while the next ovarian cycle begins in the background.",
                note = "Flow and duration vary between people and between cycles."
            ),
            LearnTopic(
                title = "Follicular phase",
                shortTitle = "Follicular",
                imageRes = R.drawable.learn_follicular,
                summary = "Follicles in the ovary develop while the uterine lining starts building again.",
                timing = "After bleeding to ovulation",
                hormonePattern = "Oestrogen usually rises",
                details = "FSH supports follicle development. As a dominant follicle develops, oestrogen usually rises and the endometrium becomes more proliferative.",
                note = "This phase can vary considerably in length, which limits date-only prediction."
            ),
            LearnTopic(
                title = "Ovulation",
                shortTitle = "Ovulation",
                imageRes = R.drawable.learn_ovulation,
                summary = "Ovulation is the release of an egg from the ovary. Calendar apps can only estimate when it happens.",
                timing = "Often around mid-cycle",
                hormonePattern = "LH surge precedes ovulation",
                details = "A sustained rise in oestradiol helps trigger the LH surge. Ovulation usually follows, but the exact day can shift even in otherwise regular cycles.",
                note = "Lunara does not confirm ovulation. Its fertile and ovulation dates are estimates only."
            ),
            LearnTopic(
                title = "Luteal phase",
                shortTitle = "Luteal",
                imageRes = R.drawable.learn_luteal,
                summary = "After ovulation, the corpus luteum produces progesterone until the next period begins.",
                timing = "Ovulation to next period",
                hormonePattern = "Progesterone usually rises",
                details = "Progesterone helps support the endometrium after ovulation. If pregnancy does not occur, progesterone and oestrogen fall and menstruation begins.",
                note = "A late period can have many causes. Lunara cannot diagnose why a cycle changes."
            )
        )
    }
    var selectedIndex by remember { mutableIntStateOf(0) }
    val topic = topics[selectedIndex]

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { PageHeader("Learn", "Pick a phase") }
        item {
            ScrollableTabRow(
                selectedTabIndex = selectedIndex,
                edgePadding = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                topics.forEachIndexed { index, item ->
                    Tab(
                        selected = selectedIndex == index,
                        onClick = { selectedIndex = index },
                        text = { Text(item.shortTitle) }
                    )
                }
            }
        }
        item {
            LearnPhaseCard(topic)
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LearnFactCard(Modifier.weight(1f), "Typical timing", topic.timing)
                LearnFactCard(Modifier.weight(1f), "Hormone pattern", topic.hormonePattern)
            }
        }
        item {
            LearnMoreDetails(topic)
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Keep in mind", fontWeight = FontWeight.Bold)
                    Text(topic.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun LearnPhaseCard(topic: LearnTopic) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(
                painter = painterResource(topic.imageRes),
                contentDescription = "${topic.title} illustration",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(topic.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(topic.summary)
            }
        }
    }
}

@Composable
private fun LearnFactCard(modifier: Modifier, label: String, value: String) {
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun LearnMoreDetails(topic: LearnTopic) {
    var expanded by remember(topic.title) { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
                Text(if (expanded) "Hide details" else "More details", modifier = Modifier.weight(1f))
                Icon(
                    if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null
                )
            }
            AnimatedVisibility(expanded) {
                Text(topic.details)
            }
        }
    }
}

@Composable
internal fun LearnCard(title: String, body: String) {
    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(body)
        }
    }
}
