package com.dkcycle.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalDate

class CycleEngineTest {
    @Test
    fun predictsFromRecentPeriodStarts() {
        val starts = listOf(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 29),
            LocalDate.of(2026, 2, 26),
            LocalDate.of(2026, 3, 26)
        )
        val logs = starts.flatMap { start ->
            (0L..4L).map { offset -> DailyLog(start.plusDays(offset), flow = FlowIntensity.MEDIUM) }
        }
        val analysis = CycleEngine.analyse(logs, LocalDate.of(2026, 4, 10))
        assertEquals(28, analysis.averageCycleLength)
        assertEquals(5, analysis.averagePeriodLength)
        assertEquals(LocalDate.of(2026, 4, 23), analysis.nextPeriodStart)
        assertEquals(LocalDate.of(2026, 4, 9), analysis.estimatedOvulation)
        assertNotNull(analysis.fertileStart)
    }

    @Test
    fun ignoresSpottingAsPeriodStart() {
        val logs = listOf(
            DailyLog(LocalDate.of(2026, 1, 10), flow = FlowIntensity.SPOTTING),
            DailyLog(LocalDate.of(2026, 1, 20), flow = FlowIntensity.MEDIUM)
        )
        val analysis = CycleEngine.analyse(logs)
        assertEquals(listOf(LocalDate.of(2026, 1, 20)), analysis.periodStarts)
    }
}
