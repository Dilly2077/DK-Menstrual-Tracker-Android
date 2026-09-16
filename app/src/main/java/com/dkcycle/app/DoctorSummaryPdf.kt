package com.dkcycle.app

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

internal object DoctorSummaryPdf {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 48f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2)

    private val dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.UK)

    fun write(
        output: OutputStream,
        logs: Map<LocalDate, DailyLog>,
        analysis: CycleAnalysis,
        generatedOn: LocalDate = LocalDate.now()
    ) {
        val document = PdfDocument()
        try {
            val writer = ReportWriter(document)
            val meaningfulLogs = logs.values.filter { it.hasMeaningfulData() }.sortedBy { it.date }
            val periods = periodEpisodes(logs)

            writer.title("Averelle menstrual history summary")
            writer.body("Generated ${generatedOn.format(dateFormat)} from information entered by the user.")
            writer.note("This is a factual tracking summary for discussion with a healthcare professional. It is not a diagnosis, a clinical record or a substitute for medical assessment.")

            writer.heading("Tracking overview")
            writer.keyValue("Tracked period", if (meaningfulLogs.isEmpty()) "No logged data" else "${meaningfulLogs.first().date.format(dateFormat)} to ${meaningfulLogs.last().date.format(dateFormat)}")
            writer.keyValue("Recorded period starts", analysis.periodStarts.size.toString())
            writer.keyValue("Typical cycle length", "${analysis.averageCycleLength} days")
            writer.keyValue("Typical bleeding duration", "${analysis.averagePeriodLength} days")
            writer.keyValue("Cycle timing variability", "approximately ${analysis.variabilityDays.roundToInt()} days")
            analysis.periodStarts.lastOrNull()?.let { writer.keyValue("Most recent recorded period start", it.format(dateFormat)) }

            writer.heading("Current estimate")
            if (analysis.nextPeriodStart == null) {
                writer.body("There is not yet enough usable tracking history for a current next-period estimate.")
            } else {
                writer.keyValue("Estimated next period start", analysis.nextPeriodStart.format(dateFormat))
                if (analysis.predictionWindowStart != null && analysis.predictionWindowEnd != null) {
                    writer.keyValue(
                        "Estimated range",
                        "${analysis.predictionWindowStart.format(dateFormat)} to ${analysis.predictionWindowEnd.format(dateFormat)}"
                    )
                }
                writer.keyValue("Prediction confidence", analysis.confidence)
                writer.note("Period and fertile-window predictions are calendar estimates based on logged period timing; they do not confirm ovulation and must not be used as contraception.")
            }

            writer.heading("Recent recorded periods")
            if (periods.isEmpty()) {
                writer.body("No non-spotting bleeding episodes are recorded.")
            } else {
                periods.takeLast(12).reversed().forEach { period ->
                    val cycleLength = analysis.periodStarts.indexOf(period.start).takeIf { it >= 0 }?.let { index ->
                        analysis.periodStarts.getOrNull(index + 1)?.let { next -> ChronoUnit.DAYS.between(period.start, next).toInt() }
                    }
                    val parts = buildList {
                        add("${period.start.format(dateFormat)} – ${period.end.format(dateFormat)}")
                        add("${period.days} bleeding day${if (period.days == 1) "" else "s"}")
                        cycleLength?.let { add("next cycle start after $it days") }
                        if (period.maxPain > 0) add("highest logged pain ${period.maxPain}/10")
                        if (period.heavyDays > 0) add("${period.heavyDays} heavy-flow day${if (period.heavyDays == 1) "" else "s"}")
                    }
                    writer.bullet(parts.joinToString("; "))
                }
            }

            writer.heading("Symptoms and pain")
            val symptomCounts = meaningfulLogs
                .flatMap { it.symptoms }
                .groupingBy { it }
                .eachCount()
                .toList()
                .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })
                .take(8)
            if (symptomCounts.isEmpty()) {
                writer.body("No symptoms have been logged.")
            } else {
                writer.body("Most frequently logged symptoms:")
                symptomCounts.forEach { (symptom, count) -> writer.bullet("$symptom — $count day${if (count == 1) "" else "s"}") }
            }
            val highPainDays = meaningfulLogs.count { it.pain >= 7 }
            val spottingDays = meaningfulLogs.count { it.flow == FlowIntensity.SPOTTING }
            writer.keyValue("Days with pain rated 7/10 or higher", highPainDays.toString())
            writer.keyValue("Logged spotting days", spottingDays.toString())

            writer.heading("Privacy note")
            writer.body("Free-text notes and the optional private intimacy marker are intentionally excluded from this PDF. The report contains only the cycle, bleeding, symptom and pain information summarised above.")

            writer.finish()
            document.writeTo(output)
        } finally {
            document.close()
        }
    }

    private data class PeriodEpisode(
        val start: LocalDate,
        val end: LocalDate,
        val days: Int,
        val maxPain: Int,
        val heavyDays: Int
    )

    private fun periodEpisodes(logs: Map<LocalDate, DailyLog>): List<PeriodEpisode> {
        val bleeding = logs.values
            .filter { it.flow != FlowIntensity.NONE && it.flow != FlowIntensity.SPOTTING }
            .sortedBy { it.date }
        if (bleeding.isEmpty()) return emptyList()

        val groups = mutableListOf<MutableList<DailyLog>>()
        bleeding.forEach { log ->
            if (groups.isEmpty() || ChronoUnit.DAYS.between(groups.last().last().date, log.date) > 1) {
                groups += mutableListOf(log)
            } else {
                groups.last() += log
            }
        }
        return groups.map { group ->
            PeriodEpisode(
                start = group.first().date,
                end = group.last().date,
                days = group.size,
                maxPain = group.maxOf { it.pain },
                heavyDays = group.count { it.flow == FlowIntensity.HEAVY }
            )
        }
    }

    private class ReportWriter(private val document: PdfDocument) {
        private var page: PdfDocument.Page? = null
        private var pageNumber = 0
        private var y = 0f

        private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(50, 28, 37)
            textSize = 23f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        private val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(104, 46, 69)
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(42, 38, 40)
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        private val boldPaint = Paint(bodyPaint).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        private val notePaint = Paint(bodyPaint).apply {
            color = Color.rgb(92, 82, 87)
            textSize = 9.5f
        }

        init {
            newPage()
        }

        fun title(text: String) {
            wrapped(text, titlePaint, 28f, 12f)
        }

        fun heading(text: String) {
            ensure(38f)
            y += 12f
            wrapped(text, headingPaint, 19f, 4f)
        }

        fun body(text: String) = wrapped(text, bodyPaint, 15f, 6f)

        fun note(text: String) = wrapped(text, notePaint, 13.5f, 8f)

        fun bullet(text: String) = wrapped("• $text", bodyPaint, 15f, 4f, indent = 10f)

        fun keyValue(label: String, value: String) {
            ensure(20f)
            val canvas = requireNotNull(page).canvas
            canvas.drawText("$label:", MARGIN, y, boldPaint)
            val labelWidth = boldPaint.measureText("$label: ")
            val remaining = CONTENT_WIDTH - labelWidth
            if (bodyPaint.measureText(value) <= remaining) {
                canvas.drawText(value, MARGIN + labelWidth, y, bodyPaint)
                y += 16f
            } else {
                y += 15f
                wrapped(value, bodyPaint, 15f, 4f, indent = 12f)
            }
        }

        fun finish() {
            page?.let { document.finishPage(it) }
            page = null
        }

        private fun wrapped(
            text: String,
            paint: Paint,
            lineHeight: Float,
            after: Float,
            indent: Float = 0f
        ) {
            val lines = wrap(text, paint, CONTENT_WIDTH - indent)
            lines.forEach { line ->
                ensure(lineHeight + 2f)
                requireNotNull(page).canvas.drawText(line, MARGIN + indent, y, paint)
                y += lineHeight
            }
            y += after
        }

        private fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
            if (text.isBlank()) return listOf("")
            val output = mutableListOf<String>()
            text.split("\n").forEach { paragraph ->
                var current = ""
                paragraph.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.forEach { word ->
                    val candidate = if (current.isEmpty()) word else "$current $word"
                    if (paint.measureText(candidate) <= maxWidth || current.isEmpty()) {
                        current = candidate
                    } else {
                        output += current
                        current = word
                    }
                }
                if (current.isNotEmpty()) output += current
            }
            return output
        }

        private fun ensure(height: Float) {
            if (y + height > PAGE_HEIGHT - MARGIN) newPage()
        }

        private fun newPage() {
            page?.let { document.finishPage(it) }
            pageNumber += 1
            val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            page = document.startPage(info)
            y = MARGIN
        }
    }
}
