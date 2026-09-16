package com.dkcycle.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@Composable
internal fun PartnerScreen(
    logs: Map<LocalDate, DailyLog>,
    onBack: () -> Unit
) {
    val analysis = CycleEngine.analyse(logs.values.toList())

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            PageHeader("Partner", "Pair once, keep the shared view up to date") {
                OutlinedButton(onClick = onBack) { Text("Done") }
            }
        }
        item {
            PartnerCard(
                title = "Live partner sharing",
                body = "The old one-off Partner Pass has been removed. The replacement is an ongoing pairing: once linked, Lunara will sync the shared cycle view when data changes rather than asking you to send another snapshot."
            )
        }
        item {
            PartnerCard(
                title = "What a partner will see",
                body = "Only cycle timing needed for a read-only calendar and predictions. Detailed notes, symptoms, mood, pain, temperature and the optional private heart marker are excluded."
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard(
                        Modifier.weight(1f),
                        "Next period",
                        analysis.nextPeriodStart?.format(shortDate()) ?: "Learning",
                        "Estimated"
                    )
                    MetricCard(
                        Modifier.weight(1f),
                        "Cycle day",
                        analysis.cycleDay?.toString() ?: "—",
                        analysis.phase
                    )
                }
                Text(
                    "✦ Predicted period starts are shown as estimates in the shared calendar once enough cycle data exists.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            PartnerCard(
                title = "Secure sync setup",
                body = "Live pairing needs a small encrypted network service so two different phones can keep the same read-only view updated. Lunara will not fall back to an unencrypted public relay or pretend a local snapshot is live. The app-side snapshot flow is removed; secure live pairing is the remaining backend step for V0.3."
            )
        }
        item {
            PartnerCard(
                title = "No logging prerequisite",
                body = "Pairing will not require a period or daily log first. A partner can be linked immediately; the shared view simply starts in a learning state until cycle data is available."
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
