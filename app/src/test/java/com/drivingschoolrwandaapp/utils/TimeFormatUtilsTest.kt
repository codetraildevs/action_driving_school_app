package com.drivingschoolrwandaapp.utils

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [TimeFormatUtils].
 *
 * Covers the [TimeFormatUtils.formatElapsed] contract: `mm:ss` under an hour
 * and `h:mm:ss` once an hour is reached, including the exact hour boundary and
 * single-digit minute/hour padding rules.
 */
class TimeFormatUtilsTest {

    // ---------------------------------------------------------------------------
    // Sub-minute / sub-hour — mm:ss format
    // ---------------------------------------------------------------------------

    @Test
    fun `formatElapsed zero seconds`() {
        assertEquals("00:00", TimeFormatUtils.formatElapsed(0))
    }

    @Test
    fun `formatElapsed seconds only`() {
        assertEquals("00:59", TimeFormatUtils.formatElapsed(59))
    }

    @Test
    fun `formatElapsed exactly one minute`() {
        assertEquals("01:00", TimeFormatUtils.formatElapsed(60))
    }

    @Test
    fun `formatElapsed minutes and seconds`() {
        assertEquals("01:01", TimeFormatUtils.formatElapsed(61))
    }

    @Test
    fun `formatElapsed pads minutes and seconds to two digits`() {
        assertEquals("05:09", TimeFormatUtils.formatElapsed(309))
    }

    @Test
    fun `formatElapsed just under one hour`() {
        assertEquals("59:59", TimeFormatUtils.formatElapsed(3599))
    }

    // ---------------------------------------------------------------------------
    // One hour or more — h:mm:ss format
    // ---------------------------------------------------------------------------

    @Test
    fun `formatElapsed exactly one hour switches to hours format`() {
        assertEquals("1:00:00", TimeFormatUtils.formatElapsed(3600))
    }

    @Test
    fun `formatElapsed one hour with minutes and seconds`() {
        assertEquals("1:01:01", TimeFormatUtils.formatElapsed(3661))
    }

    @Test
    fun `formatElapsed multiple hours keeps minutes and seconds padded`() {
        assertEquals("2:02:05", TimeFormatUtils.formatElapsed(7325))
    }

    @Test
    fun `formatElapsed large duration`() {
        assertEquals("25:00:00", TimeFormatUtils.formatElapsed(90000))
    }

    // ---------------------------------------------------------------------------
    // Edge cases
    // ---------------------------------------------------------------------------

    @Test
    fun `formatElapsed negative value does not crash and documents current behavior`() {
        // Integer division truncates toward zero: -5/60 = 0, -5%60 = -5 → "00:-5".
        // This documents the current (arguably buggy) behavior for negative input.
        assertEquals("00:-5", TimeFormatUtils.formatElapsed(-5))
    }
}
