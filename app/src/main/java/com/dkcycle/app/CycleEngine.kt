package com.dkcycle.app

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Minimal-input personalised menstrual prediction.
 *
 * The model needs period dates only. It deliberately does not require temperature,
 * heart rate, LH tests, age, weight, symptoms, or an account.
 *
 * Long observed gaps may represent one genuinely long cycle or multiple cycles with
 * missed tracking. Lunara weighs those possibilities instead of forcing every gap
 * into a single cycle. Population information is only a weak prior while the user's
 * own history progressively dominates.
 *
 * The next-period prediction is a probability distribution conditioned on the
 * current cycle having reached today. Ovulation and fertile dates remain calendar-
 * only estimates and are never presented as measured events.
 */
object CycleEngine {
    private const val POPULATION_MEAN = 29.3
    private const val POPULATION_SD = 7.0
    private const val MIN_CYCLE = 15
    private const val MAX_HISTORY_GAP = 180
    private const val MAX_PREDICTED_CYCLE = 120
    private const val MAX_LATENT_CYCLES = 4
    private const val EM_ITERATIONS = 8

    private data class WeightedCycle(val days: Double, val weight: Double)
    private data class GapCandidate(val cycles: Int, val probability: Double)

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
        val rawIntervals = periodStarts.zipWithNext { a, b ->
            ChronoUnit.DAYS.between(a, b).toInt()
        }.filter { it in MIN_CYCLE..MAX_HISTORY_GAP }

        val plausibleSingleCycles = rawIntervals.filter { it <= 45 }
        var mean = when {
            plausibleSingleCycles.isNotEmpty() -> medianInts(plausibleSingleCycles)
            rawIntervals.size >= 3 -> min(45.0, medianInts(rawIntervals))
            else -> POPULATION_MEAN
        }
        var sd = max(3.5, robustSpread(rawIntervals.map(Int::toDouble), mean))
        var skipProbability = 0.06
        var weightedCycles = emptyList<WeightedCycle>()
        var expectedSkipped = 0.0

        repeat(EM_ITERATIONS) {
            val inferred = mutableListOf<WeightedCycle>()
            var skipped = 0.0
            var latentCycles = 0.0

            rawIntervals.forEachIndexed { index, observed ->
                val age = rawIntervals.lastIndex - index
                val recencyWeight = exp(-0.10 * age)
                val candidates = gapCandidates(observed, mean, sd, skipProbability)

                candidates.forEach { candidate ->
                    val impliedLength = observed.toDouble() / candidate.cycles
                    val reliability = recencyWeight * candidate.probability / sqrt(candidate.cycles.toDouble())
                    if (impliedLength in MIN_CYCLE.toDouble()..60.0) {
                        inferred += WeightedCycle(impliedLength, reliability)
                    }
                    skipped += candidate.probability * (candidate.cycles - 1)
                    latentCycles += candidate.probability * candidate.cycles
                }
            }

            weightedCycles = robustWeights(inferred)
            expectedSkipped = skipped
            val personalWeight = weightedCycles.sumOf { it.weight }
            val personalMean = if (personalWeight > 0.0) {
                weightedCycles.sumOf { it.days * it.weight } / personalWeight
            } else POPULATION_MEAN

            val priorStrength = when {
                rawIntervals.size >= 6 -> 0.35
                rawIntervals.size >= 3 -> 0.65
                rawIntervals.isNotEmpty() -> 1.25
                else -> 2.25
            }
            mean = (POPULATION_MEAN * priorStrength + personalMean * personalWeight) /
                (priorStrength + personalWeight).coerceAtLeast(0.0001)

            val personalVariance = if (personalWeight > 0.0) {
                weightedCycles.sumOf { it.weight * (it.days - mean).pow(2) } / personalWeight
            } else POPULATION_SD.pow(2)
            val variancePriorWeight = 1.4
            val variance = (POPULATION_SD.pow(2) * variancePriorWeight + personalVariance * personalWeight) /
                (variancePriorWeight + personalWeight).coerceAtLeast(0.0001)
            sd = sqrt(variance).coerceIn(1.5, 12.0)

            // Conservative prior: missed tracking is possible, but ordinary
            // single-cycle intervals remain the default expectation.
            skipProbability = ((1.0 + skipped) / (16.0 + latentCycles)).coerceIn(0.01, 0.35)
        }

        val periodLengths = runs.map { it.size }.filter { it in 1..10 }.takeLast(8)
        val averagePeriod = if (periodLengths.isEmpty()) {
            5
        } else {
            medianInts(periodLengths).roundToInt().coerceIn(1, 10)
        }

        val lastStart = periodStarts.lastOrNull()
        val currentDay = lastStart?.let {
            ChronoUnit.DAYS.between(it, today).toInt().plus(1).takeIf { day -> day > 0 }
        }

        val baseDistribution = physiologicalDistribution(mean, sd)
        val survivalProbability = if (currentDay != null) {
            baseDistribution.filter { it.first >= currentDay }.sumOf { it.second }
        } else 1.0
        val timingVeryUncertain = currentDay != null &&
            (currentDay > MAX_PREDICTED_CYCLE || survivalProbability < 0.005)
        val timingUncertain = currentDay != null &&
            (timingVeryUncertain || survivalProbability < 0.03)

        val conditionalDistribution = if (currentDay != null && !timingVeryUncertain) {
            normalize(baseDistribution.filter { it.first >= currentDay })
        } else emptyList()

        val medianLength = conditionalDistribution.quantile(0.50)
        val lowLength = conditionalDistribution.quantile(0.10)
        val highLength = conditionalDistribution.quantile(0.90)

        val nextStart = if (lastStart != null && medianLength != null) lastStart.plusDays(medianLength.toLong()) else null
        val predictionLow = if (lastStart != null && lowLength != null) lastStart.plusDays(lowLength.toLong()) else null
        val predictionHigh = if (lastStart != null && highLength != null) lastStart.plusDays(highLength.toLong()) else null
        val predictedPeriodEnd = nextStart?.plusDays((averagePeriod - 1).toLong())

        // Dates alone cannot identify ovulation precisely. Use a centre estimate and
        // retain a much wider plausible range internally rather than assuming a fixed
        // universal 14-day luteal phase.
        val estimatedOvulation = nextStart?.minusDays(13)
        val ovulationWindowStart = predictionLow?.minusDays(17)
        val ovulationWindowEnd = predictionHigh?.minusDays(7)
        val fertileStart = estimatedOvulation?.minusDays(5)
        val fertileEnd = estimatedOvulation?.plusDays(1)
        val lutealStart = estimatedOvulation?.plusDays(1)
        val lutealEnd = nextStart?.minusDays(1)

        val predictionWidth = if (predictionLow != null && predictionHigh != null) {
            ChronoUnit.DAYS.between(predictionLow, predictionHigh).toInt()
        } else 99
        val confidence = when {
            rawIntervals.size >= 6 && predictionWidth <= 6 -> "Higher"
            rawIntervals.size >= 3 && predictionWidth <= 10 -> "Moderate"
            else -> "Limited"
        }

        val phase = when {
            lastStart == null -> "Learning your cycle"
            timingUncertain -> "Cycle timing uncertain"
            currentDay != null && currentDay <= averagePeriod -> "Menstrual phase"
            fertileStart != null && fertileEnd != null && !today.isBefore(fertileStart) && !today.isAfter(fertileEnd) -> "Estimated fertile window"
            estimatedOvulation != null && today.isBefore(estimatedOvulation) -> "Follicular phase"
            nextStart != null && today.isBefore(nextStart) -> "Luteal phase"
            else -> "Period may be later than estimated"
        }

        val explanation = when (phase) {
            "Menstrual phase" -> "Oestrogen and progesterone are relatively low while the uterine lining is shed."
            "Follicular phase" -> "Follicles develop and oestrogen usually rises. Calendar timing varies from cycle to cycle."
            "Estimated fertile window" -> "This window is inferred from period timing only. Lunara has not measured or confirmed ovulation."
            "Luteal phase" -> "After ovulation, progesterone usually rises. The displayed timing is an estimate from cycle history."
            "Cycle timing uncertain" -> "This cycle has moved beyond most of your predicted range. It may genuinely be longer, or a period may not have been logged. Lunara avoids pretending it knows which."
            "Period may be later than estimated" -> "The prediction is updated as the cycle continues instead of treating the original date as certain."
            else -> "Mark the first day of a period to start learning. Several cycles narrow the prediction range."
        }

        return CycleAnalysis(
            periodStarts = periodStarts,
            averageCycleLength = mean.roundToInt().coerceIn(MIN_CYCLE, 60),
            averagePeriodLength = averagePeriod,
            variabilityDays = sd,
            nextPeriodStart = nextStart,
            predictedPeriodEnd = predictedPeriodEnd,
            predictionWindowStart = predictionLow,
            predictionWindowEnd = predictionHigh,
            estimatedOvulation = estimatedOvulation,
            ovulationWindowStart = ovulationWindowStart,
            ovulationWindowEnd = ovulationWindowEnd,
            fertileStart = fertileStart,
            fertileEnd = fertileEnd,
            lutealStart = lutealStart,
            lutealEnd = lutealEnd,
            confidence = confidence,
            cycleDay = currentDay,
            phase = phase,
            phaseExplanation = explanation,
            inferredMissedCycles = expectedSkipped.roundToInt().coerceAtLeast(0),
            usableCycleCount = rawIntervals.size
        )
    }

    private fun gapCandidates(observed: Int, mean: Double, sd: Double, skipProbability: Double): List<GapCandidate> {
        val raw = mutableListOf<Pair<Int, Double>>()
        for (cycles in 1..MAX_LATENT_CYCLES) {
            val implied = observed.toDouble() / cycles
            if (implied !in MIN_CYCLE.toDouble()..60.0) continue
            val prior = (1.0 - skipProbability) * skipProbability.pow(cycles - 1)
            val spread = max(2.0, sqrt(cycles.toDouble()) * sd)
            val likelihood = normalPdf(observed.toDouble(), cycles * mean, spread)
            raw += cycles to (prior * likelihood)
        }
        val total = raw.sumOf { it.second }
        if (raw.isEmpty() || total <= 0.0) return listOf(GapCandidate(1, 1.0))
        return raw.map { GapCandidate(it.first, it.second / total) }
    }

    private fun physiologicalDistribution(mean: Double, sd: Double): List<Pair<Int, Double>> {
        val raw = (MIN_CYCLE..MAX_PREDICTED_CYCLE).map { day ->
            val core = normalPdf(day.toDouble(), mean, sd)
            val broadTail = normalPdf(day.toDouble(), mean, max(7.0, sd * 2.2))
            day to (0.88 * core + 0.12 * broadTail)
        }
        return normalize(raw)
    }

    private fun normalize(values: List<Pair<Int, Double>>): List<Pair<Int, Double>> {
        val total = values.sumOf { it.second }
        return if (total <= 0.0) emptyList() else values.map { it.first to (it.second / total) }
    }

    private fun robustWeights(values: List<WeightedCycle>): List<WeightedCycle> {
        if (values.size < 3) return values
        val median = medianDoubles(values.map { it.days })
        val mad = medianDoubles(values.map { abs(it.days - median) }).coerceAtLeast(1.0)
        val scale = 1.4826 * mad
        return values.map { item ->
            val z = abs(item.days - median) / scale
            val huber = if (z <= 2.5) 1.0 else 2.5 / z
            item.copy(weight = item.weight * huber)
        }
    }

    private fun normalPdf(x: Double, mean: Double, sd: Double): Double {
        val z = (x - mean) / sd
        return exp(-0.5 * z * z) / (sd * sqrt(2.0 * PI))
    }

    private fun robustSpread(values: List<Double>, centre: Double): Double {
        if (values.isEmpty()) return POPULATION_SD
        val mad = medianDoubles(values.map { abs(it - centre) })
        return (1.4826 * mad).coerceAtLeast(2.0)
    }

    private fun List<Pair<Int, Double>>.quantile(q: Double): Int? {
        if (isEmpty()) return null
        var cumulative = 0.0
        for ((day, probability) in this) {
            cumulative += probability
            if (cumulative >= q) return day
        }
        return last().first
    }

    private fun medianInts(values: List<Int>): Double = medianDoubles(values.map(Int::toDouble))

    private fun medianDoubles(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) (sorted[mid - 1] + sorted[mid]) / 2.0 else sorted[mid]
    }
}
