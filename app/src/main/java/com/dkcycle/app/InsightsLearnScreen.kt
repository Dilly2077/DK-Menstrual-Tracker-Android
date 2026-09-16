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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
                    "Most likely next period: ${analysis.nextPeriodStart?.format(shortDate())}. Approximate 80% model window: ${analysis.predictionWindowStart.format(shortDate())} – ${analysis.predictionWindowEnd.format(shortDate())}. This range updates as the current cycle continues."
                )
            }
        }
        if (analysis.inferredMissedCycles > 0) {
            item {
                LearnCard(
                    "Possible missed tracking",
                    "Lunara found ${analysis.inferredMissedCycles} historical gap${if (analysis.inferredMissedCycles == 1) "" else "s"} that fit multiple typical cycles better than one unusually long cycle. They are down-weighted instead of being treated as ordinary cycle lengths."
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

@Composable
internal fun LearnScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Learn", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Cycle physiology without the paywall", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            LearnCard(
                "The cycle in one minute",
                "Day 1 is the first day of menstrual bleeding. In the follicular phase, FSH supports follicle development and oestrogen tends to rise. A mid-cycle LH surge precedes ovulation. In the luteal phase, progesterone from the corpus luteum predominates. If pregnancy does not occur, progesterone and oestrogen fall and menstruation begins."
            )
        }
        item {
            LearnCard(
                "Menstrual phase",
                "The functional endometrium is shed after ovarian steroid hormone concentrations fall. FSH begins to rise enough to recruit a new cohort of ovarian follicles."
            )
        }
        item {
            LearnCard(
                "Follicular phase",
                "Developing follicles produce increasing oestradiol. Oestrogen promotes proliferative growth of the endometrium. The dominant follicle becomes increasingly responsive to FSH and LH."
            )
        }
        item {
            LearnCard(
                "Ovulation",
                "Sustained high oestradiol switches to positive feedback at the hypothalamic-pituitary axis, producing the LH surge. Ovulation usually follows the surge; calendar timing varies between people and between cycles."
            )
        }
        item {
            LearnCard(
                "Luteal phase",
                "The ruptured follicle becomes the corpus luteum, producing progesterone and oestrogen. Progesterone changes the endometrium from proliferative to secretory. Without pregnancy, corpus luteum function declines and steroid concentrations fall."
            )
        }
        item {
            LearnCard(
                "How Lunara predicts",
                "Period-start history is enough to run the model. Lunara learns your typical cycle distribution, gives recent cycles slightly more influence, down-weights outliers, considers whether long gaps may contain missed tracking, and updates its prediction range as the current cycle continues."
            )
        }
        item {
            LearnCard(
                "What Lunara cannot know from dates alone",
                "Lunara cannot measure FSH, LH, oestrogen or progesterone, confirm ovulation, diagnose a condition, or determine whether pregnancy is possible on a particular day. Ovulation and fertile-window colours are calendar estimates only."
            )
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
