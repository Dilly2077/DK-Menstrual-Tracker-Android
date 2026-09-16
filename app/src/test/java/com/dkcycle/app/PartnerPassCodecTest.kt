package com.dkcycle.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DailyLogBehaviorTest {
    private val date = LocalDate.of(2026, 9, 16)

    @Test
    fun blankDayIsNotTreatedAsLogged() {
        assertFalse(DailyLog(date).hasMeaningfulData())
    }

    @Test
    fun bleedingDayIsMeaningful() {
        assertTrue(DailyLog(date, flow = FlowIntensity.MEDIUM).hasMeaningfulData())
    }

    @Test
    fun privateMarkerIsMeaningfulButIndependentOfBleeding() {
        val log = DailyLog(date, intimacy = true)
        assertTrue(log.hasMeaningfulData())
        assertTrue(log.flow == FlowIntensity.NONE)
    }
}
