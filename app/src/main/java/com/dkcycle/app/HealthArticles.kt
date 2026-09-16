package com.dkcycle.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class OfflineHealthArticle(
    val category: String,
    val title: String,
    val summary: String,
    val body: String,
    val source: String,
    val reviewed: String
)

private val healthArticles = listOf(
    OfflineHealthArticle(
        category = "Periods",
        title = "What counts as a usual period?",
        summary = "Cycle length and bleeding duration vary. Tracking your own pattern is often more useful than chasing a single ‘normal’ number.",
        body = "For many adults, periods occur roughly every 21 to 35 days and bleeding lasts around 2 to 7 days. Your own pattern can still vary. A persistent change in your usual pattern, bleeding between periods or bleeding after sex is worth discussing with a GP or sexual health clinician.",
        source = "NHS: Periods and period problems",
        reviewed = "17 Sep 2026"
    ),
    OfflineHealthArticle(
        category = "Periods",
        title = "When is bleeding considered heavy?",
        summary = "Heavy bleeding is partly about its effect on daily life, not only an exact volume.",
        body = "Signs include needing to change a pad or tampon every 1 to 2 hours, using two period products together, bleeding for more than 7 days, passing large clots, bleeding through clothes or bedding, or avoiding normal activities because of bleeding. See a GP if heavy periods are affecting your life or are accompanied by severe pain, bleeding between periods or bleeding after sex.",
        source = "NHS: Heavy periods",
        reviewed = "17 Sep 2026"
    ),
    OfflineHealthArticle(
        category = "Symptoms",
        title = "Period pain: when to get help",
        summary = "Cramps are common, but severe or changing pain deserves attention.",
        body = "Period pain commonly occurs around the start of a period and often lasts up to a few days. Seek urgent GP advice or NHS 111 if pelvic or period pain is severe or worse than usual and ordinary pain relief has not helped. Arrange a GP review if pain is stopping normal activities, periods become more painful or heavy, or you have pain during sex, urination or bowel movements.",
        source = "NHS: Period pain",
        reviewed = "17 Sep 2026"
    ),
    OfflineHealthArticle(
        category = "Sexual health",
        title = "Condoms and STI protection",
        summary = "Condoms help prevent pregnancy and reduce the risk of sexually transmitted infections.",
        body = "Use a new condom each time you have sex and follow the instructions on the packet. Many sexually transmitted infections can cause no symptoms, so testing is the only way to know for certain. NHS sexual health clinics can provide confidential advice, testing and contraception services.",
        source = "NHS: Condoms and sexually transmitted infections",
        reviewed = "17 Sep 2026"
    )
)

@Composable
internal fun HealthArticlesSection() {
    var selected by remember { mutableStateOf<OfflineHealthArticle?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Health library", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "Short offline guides based on NHS patient information. No feed, tracking or internet connection.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )

        healthArticles.forEach { article ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        article.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(article.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(article.summary, style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { selected = article }) { Text("Read") }
                }
            }
        }

        Text(
            "Content review: 17 Sep 2026",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    selected?.let { article ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(article.title) },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())
                ) {
                    Text(article.body)
                    Spacer(Modifier.height(16.dp))
                    Text(article.source, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Content checked ${article.reviewed}. Educational information only; it does not diagnose a condition or replace clinical advice.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selected = null }) { Text("Close") }
            }
        )
    }
}
