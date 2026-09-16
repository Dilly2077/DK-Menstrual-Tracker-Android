package com.dkcycle.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CycleEngineTest {
    @Test
    fun stableTwentyEightDayHistoryProducesPersonalisedPrediction() {
        val starts = listOf(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 29),
            LocalDate.of(2026, 2, 26),
            LocalDate.of(2026, 3, 26),
            LocalDate.of(2026, 4, 23),
            LocalDate.of(2026, 5, 21)
        )
        val logs = starts.flatMap { start ->
            (0L..4L).map { offset -> DailyLog(start.plusDays(offset), flow = FlowIntensity.MEDIUM) }
        }
        val analysis = CycleEngine.analyse(logs, LocalDate.of(2026, 6, 1))
        assertTrue(analysis.averageCycleLength in 28..29)
        assertEquals(5, analysis.averagePeriodLength)
        assertNotNull(analysis.nextPeriodStart)
        assertNotNull(analysis.predictionWindowStart)
        assertNotNull(analysis.predictionWindowEnd)
        assertTrue(!analysis.predictionWindowStart!!.isAfter(analysis.nextPeriodStart))
        assertTrue(!analysis.predictionWindowEnd!!.isBefore(analysis.nextPeriodStart))
    }

    @Test
    fun likelyMissedPeriodDoesNotBecomeFiftySixDayTypicalCycle() {
        val starts = listOf(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 29),
            LocalDate.of(2026, 3, 26),
            LocalDate.of(2026, 4, 23)
        )
        val logs = starts.map { DailyLog(it, flow = FlowIntensity.MEDIUM) }
        val analysis = CycleEngine.analyse(logs, LocalDate.of(2026, 5, 1))
        assertTrue(analysis.averageCycleLength in 27..31)
        assertTrue(analysis.inferredMissedCycles >= 1)
    }

    @Test
    fun ongoingCycleNeverPredictsPeriodInThePast() {
        val starts = listOf(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 29),
            LocalDate.of(2026, 2, 26),
            LocalDate.of(2026, 3, 26)
        )
        val logs = starts.map { DailyLog(it, flow = FlowIntensity.MEDIUM) }
        val today = LocalDate.of(2026, 5, 5)
        val analysis = CycleEngine.analyse(logs, today)
        assertNotNull(analysis.nextPeriodStart)
        assertTrue(!analysis.nextPeriodStart!!.isBefore(today))
    }

    @Test
    fun spottingAloneDoesNotCreatePeriodStart() {
        val analysis = CycleEngine.analyse(
            listOf(
                DailyLog(LocalDate.of(2026, 1, 10), flow = FlowIntensity.SPOTTING),
                DailyLog(LocalDate.of(2026, 1, 20), flow = FlowIntensity.MEDIUM)
            ),
            LocalDate.of(2026, 1, 25)
        )
        assertEquals(listOf(LocalDate.of(2026, 1, 20)), analysis.periodStarts)
    }

    @Test
    fun onePeriodKeepsConfidenceLimited() {
        val analysis = CycleEngine.analyse(
            listOf(DailyLog(LocalDate.of(2026, 8, 1), flow = FlowIntensity.MEDIUM)),
            LocalDate.of(2026, 8, 10)
        )
        assertEquals("Limited", analysis.confidence)
    }
}
