package com.dkcycle.app

import java.time.LocalDate

enum class FlowIntensity { NONE, SPOTTING, LIGHT, MEDIUM, HEAVY }

data class DailyLog(
    val date: LocalDate,
    val flow: FlowIntensity = FlowIntensity.NONE,
    val symptoms: Set<String> = emptySet(),
    val mood: String = "",
    val pain: Int = 0,
    val temperatureC: Double? = null,
    val notes: String = ""
)

data class CycleAnalysis(
    val periodStarts: List<LocalDate>,
    val averageCycleLength: Int,
    val averagePeriodLength: Int,
    val variabilityDays: Double,
    val nextPeriodStart: LocalDate?,
    val predictedPeriodEnd: LocalDate?,
    val estimatedOvulation: LocalDate?,
    val fertileStart: LocalDate?,
    val fertileEnd: LocalDate?,
    val confidence: String,
    val cycleDay: Int?,
    val phase: String,
    val phaseExplanation: String
)
