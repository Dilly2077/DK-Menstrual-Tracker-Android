package com.dkcycle.app

import java.time.LocalDate

enum class FlowIntensity { NONE, SPOTTING, LIGHT, MEDIUM, HEAVY }
enum class AppearanceMode { SYSTEM, LIGHT, DARK }

data class DailyLog(
    val date: LocalDate,
    val flow: FlowIntensity = FlowIntensity.NONE,
    val symptoms: Set<String> = emptySet(),
    val mood: String = "",
    val pain: Int = 0,
    val temperatureC: Double? = null,
    val intimacy: Boolean = false,
    val notes: String = ""
)

fun DailyLog.hasMeaningfulData(): Boolean =
    flow != FlowIntensity.NONE ||
        symptoms.isNotEmpty() ||
        mood.isNotBlank() ||
        pain > 0 ||
        temperatureC != null ||
        intimacy ||
        notes.isNotBlank()

data class CycleAnalysis(
    val periodStarts: List<LocalDate>,
    val averageCycleLength: Int,
    val averagePeriodLength: Int,
    val variabilityDays: Double,
    val nextPeriodStart: LocalDate?,
    val predictedPeriodEnd: LocalDate?,
    val predictionWindowStart: LocalDate?,
    val predictionWindowEnd: LocalDate?,
    val estimatedOvulation: LocalDate?,
    val ovulationWindowStart: LocalDate?,
    val ovulationWindowEnd: LocalDate?,
    val fertileStart: LocalDate?,
    val fertileEnd: LocalDate?,
    val lutealStart: LocalDate?,
    val lutealEnd: LocalDate?,
    val confidence: String,
    val cycleDay: Int?,
    val phase: String,
    val phaseExplanation: String,
    val inferredMissedCycles: Int,
    val usableCycleCount: Int
)
