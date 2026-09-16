package com.dkcycle.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class PartnerPassCodecTest {
    @Test
    fun partnerPassSharesFlowButNotPrivateLogFields() {
        val date = LocalDate.now().minusDays(4)
        val logs = mapOf(
            date to DailyLog(
                date = date,
                flow = FlowIntensity.HEAVY,
                symptoms = setOf("Cramps"),
                mood = "Low",
                pain = 8,
                temperatureC = 37.1,
                notes = "private note"
            )
        )

        val code = PartnerPassCodec.encode(logs, Instant.ofEpochMilli(1_700_000_000_000L))
        val decoded = PartnerPassCodec.decode(code)
        val shared = decoded.logs.getValue(date)

        assertEquals(FlowIntensity.HEAVY, shared.flow)
        assertTrue(shared.symptoms.isEmpty())
        assertTrue(shared.mood.isEmpty())
        assertEquals(0, shared.pain)
        assertEquals(null, shared.temperatureC)
        assertTrue(shared.notes.isEmpty())
        assertFalse(code.contains("private note"))
    }

    @Test
    fun decoderCanExtractPassFromSharedMessage() {
        val date = LocalDate.now().minusDays(2)
        val code = PartnerPassCodec.encode(mapOf(date to DailyLog(date, flow = FlowIntensity.MEDIUM)))
        val decoded = PartnerPassCodec.decode("Here is my pass:\n$code\nThanks")
        assertEquals(FlowIntensity.MEDIUM, decoded.logs.getValue(date).flow)
    }
}
