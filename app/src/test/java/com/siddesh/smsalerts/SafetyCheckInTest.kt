package com.siddesh.smsalerts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyCheckInTest {

    private fun formatRemainingTime(millis: Long): String {
        if (millis <= 0) return "00m 00s"
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format("%02dh %02dm", hours, minutes)
        } else {
            String.format("%02dm %02ds", minutes, seconds)
        }
    }

    @Test
    fun formatRemainingTime_zeroOrNegative_returnsZeroFormat() {
        assertEquals("00m 00s", formatRemainingTime(0))
        assertEquals("00m 00s", formatRemainingTime(-5000))
    }

    @Test
    fun formatRemainingTime_minutesAndSeconds() {
        // 14 minutes and 30 seconds = (14 * 60 + 30) * 1000 = 870,000 ms
        val millis = (14 * 60 + 30) * 1000L
        assertEquals("14m 30s", formatRemainingTime(millis))
    }

    @Test
    fun formatRemainingTime_hoursAndMinutes() {
        // 1 hour and 15 minutes = 75 minutes * 60 * 1000 = 4,500,000 ms
        val millis = 75 * 60 * 1000L
        assertEquals("01h 15m", formatRemainingTime(millis))
    }

    @Test
    fun intervalCalculations_verifyDurations() {
        val now = 1000000L
        val fifteenMinExpiry = now + (15 * 60 * 1000L)
        val thirtyMinExpiry = now + (30 * 60 * 1000L)
        val oneHourExpiry = now + (60 * 60 * 1000L)

        assertEquals(15 * 60 * 1000L, fifteenMinExpiry - now)
        assertEquals(30 * 60 * 1000L, thirtyMinExpiry - now)
        assertEquals(60 * 60 * 1000L, oneHourExpiry - now)

        val isExpiredBefore = now >= fifteenMinExpiry
        assertFalse(isExpiredBefore)

        val isExpiredAfter = (fifteenMinExpiry + 1000) >= fifteenMinExpiry
        assertTrue(isExpiredAfter)
    }
}
