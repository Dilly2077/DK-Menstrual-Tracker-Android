package com.dkcycle.app

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.pow
import kotlin.math.sqrt

object CycleEngine {
    fun analyse(logs: List<DailyLog>, today: LocalDate = LocalDate.now()): CycleAnalysis {
        val bleedingDates = logs
            .filter { it.flow != FlowIntensity.NONE && it.flow != FlowIntensity.SPOTTING }
            .map { it.date }
            .distinct()
            .sorted()

        val runs = bleedingDates.fold(mutableListOf<MutableList<LocalDate>>()) { acc, date ->
            if (acc.isEmpty() || ChronoUnit.DAYS.between(acc.last().last(), date) > 1) {
                acc += mutableListOf(date)
            } else {
                acc.last() += date
            }
            acc
        }

        val periodStarts = runs.map { it.first() }
        val cycleLengths = periodStarts.zipWithNext { a, b -> ChronoUnit.DAYS.between(a, b).toInt() }
            .filter { it in 15..60 }
            .takeLast(6)

        val averageCycle = when {
            cycleLengths.isEmpty() -> 28
            else -> weightedAverage(cycleLengths)
        }.coerceIn(15, 60)

        val periodLengths = runs.map { it.size }.filter { it in 1..10 }.takeLast(6)
        val averagePeriod = if (periodLengths.isEmpty()) 5 else periodLengths.average().toInt().coerceIn(1, 10)

        val variability = if (cycleLengths.size > 1) {
            val mean = cycleLengths.average()
            sqrt(cycleLengths.sumOf { (it - mean).pow(2) } / cycleLengths.size)
        } else 0.0

        val confidence = when {
            cycleLengths.size >= 4 && variability <= 2.0 -> "Higher"
            cycleLengths.size >= 3 && variability <= 4.0 -> "Moderate"
            else -> "Limited"
        }

        val lastStart = periodStarts.lastOrNull()
        val nextStart = lastStart?.plusDays(averageCycle.toLong())
        val periodEnd = nextStart?.plusDays((averagePeriod - 1).toLong())
        val ovulation = nextStart?.minusDays(14)
        val fertileStart = ovulation?.minusDays(5)
        val fertileEnd = ovulation?.plusDays(1)

        val cycleDay = lastStart?.let {
            ChronoUnit.DAYS.between(it, today).toInt().plus(1).takeIf { day -> day > 0 }
        }

        val phase = when {
            lastStart == null -> "Not enough data"
            today.isBefore(lastStart.plusDays(averagePeriod.toLong())) -> "Menstrual phase"
            fertileStart != null && fertileEnd != null && !today.isBefore(fertileStart) && !today.isAfter(fertileEnd) -> "Fertile / ovulatory window"
            fertileStart != null && today.isBefore(fertileStart) -> "Follicular phase"
            nextStart != null && today.isBefore(nextStart) -> "Luteal phase"
            else -> "Cycle may be running longer than predicted"
        }

        val explanation = when (phase) {
            "Menstrual phase" -> "Oestrogen and progesterone are relatively low. FSH begins recruiting follicles for the next cycle while the uterine lining is shed."
            "Follicular phase" -> "FSH supports follicle development. Oestrogen usually rises as the dominant follicle matures and the endometrium rebuilds."
            "Fertile / ovulatory window" -> "Rising oestrogen helps trigger the LH surge that precedes ovulation. This is only a date-based estimate; DKCycle does not measure ovulation or hormone concentrations."
            "Luteal phase" -> "After ovulation, the corpus luteum produces progesterone and some oestrogen. If pregnancy does not occur, both fall before the next period."
            "Cycle may be running longer than predicted" -> "Your logged cycle has passed the predicted start date. Predictions are estimates and naturally become less certain when cycles vary."
            else -> "Log at least one period start to begin cycle estimates; several cycles are needed before personalised predictions become more useful."
        }

        return CycleAnalysis(
            periodStarts = periodStarts,
            averageCycleLength = averageCycle,
            averagePeriodLength = averagePeriod,
            variabilityDays = variability,
            nextPeriodStart = nextStart,
            predictedPeriodEnd = periodEnd,
            estimatedOvulation = ovulation,
            fertileStart = fertileStart,
            fertileEnd = fertileEnd,
            confidence = confidence,
            cycleDay = cycleDay,
            phase = phase,
            phaseExplanation = explanation
        )
    }

    private fun weightedAverage(values: List<Int>): Int {
        var weightedSum = 0.0
        var totalWeight = 0.0
        values.forEachIndexed { index, value ->
            val weight = (index + 1).toDouble()
            weightedSum += value * weight
            totalWeight += weight
        }
        return (weightedSum / totalWeight).toInt()
    }
}
