package com.drivingschoolrwandaapp.utils

import android.util.Log
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic

/**
 * Unit tests for [PhoneUtils].
 *
 * These tests run on the JVM without a device or emulator. The only Android
 * framework dependency is [android.util.Log], which is mocked via
 * [Mockito.mockStatic].
 *
 * Covers:
 * - Rwandan local numbers (default region RW)
 * - International numbers with +, 00, and bare country code
 * - Formatted numbers (spaces, dashes)
 * - Normalization idempotency
 * - Invalid / unparseable inputs
 * - Edge cases (null, empty, whitespace)
 * - Non-Rwandan valid international numbers
 */
class PhoneUtilsTest {

    private var logMock: MockedStatic<Log>? = null

    @Before
    fun setUp() {
        logMock = mockStatic(Log::class.java)
    }

    @After
    fun tearDown() {
        logMock?.close()
    }

    // ---------------------------------------------------------------------------
    // normalize()
    // ---------------------------------------------------------------------------

    // ── Rwandan local numbers ──

    @Test
    fun `normalize Rwandan local number converts to E164`() {
        assertEquals("+250782877442", PhoneUtils.normalize("0782877442"))
    }

    @Test
    fun `normalize Rwandan local number with leading spaces`() {
        assertEquals("+250782877442", PhoneUtils.normalize("  0782877442"))
    }

    @Test
    fun `normalize Rwandan local number with trailing spaces`() {
        assertEquals("+250782877442", PhoneUtils.normalize("0782877442  "))
    }

    @Test
    fun `normalize Rwandan local number for 07X prefix`() {
        // 0722 → +250722...
        assertEquals("+250722000000", PhoneUtils.normalize("0722000000"))
    }

    @Test
    fun `normalize Rwandan landline number`() {
        // Landlines start with 025x
        assertEquals("+250252123456", PhoneUtils.normalize("0252123456"))
    }

    // ── International numbers with + ──

    @Test
    fun `normalize international with plus prefix`() {
        assertEquals("+250782877442", PhoneUtils.normalize("+250782877442"))
    }

    @Test
    fun `normalize US number with plus prefix`() {
        assertEquals("+12024567890", PhoneUtils.normalize("+12024567890"))
    }

    @Test
    fun `normalize UK mobile with plus prefix`() {
        assertEquals("+447911123456", PhoneUtils.normalize("+447911123456"))
    }

    // ── International numbers with 00 ──

    @Test
    fun `normalize international with 00 prefix`() {
        assertEquals("+250782877442", PhoneUtils.normalize("00250782877442"))
    }

    @Test
    fun `normalize US number with 00 prefix`() {
        assertEquals("+12024567890", PhoneUtils.normalize("0012024567890"))
    }

    // ── International numbers without + or 00 (bare country code) ──

    @Test
    fun `normalize international with bare country code`() {
        assertEquals("+250782877442", PhoneUtils.normalize("250782877442"))
    }

    // ── Formatted numbers ──

    @Test
    fun `normalize number with spaces`() {
        assertEquals("+250782877442", PhoneUtils.normalize("+250 78 287 7442"))
    }

    @Test
    fun `normalize number with dashes`() {
        assertEquals("+250782877442", PhoneUtils.normalize("078-287-7442"))
    }

    @Test
    fun `normalize number with parentheses`() {
        assertEquals("+250782877442", PhoneUtils.normalize("+250 (78) 287-7442"))
    }

    @Test
    fun `normalize number with mixed formatting`() {
        assertEquals("+250782877442", PhoneUtils.normalize("  +250 (78) 287-7442  "))
    }

    // ── Normalization idempotency ──

    @Test
    fun `normalize is idempotent for E164 already`() {
        val e164 = "+250782877442"
        assertEquals(e164, PhoneUtils.normalize(e164))
    }

    @Test
    fun `normalize local and international formats produce same result`() {
        val local = "0782877442"
        val international = "+250782877442"
        val bare = "250782877442"
        val with00 = "00250782877442"
        assertEquals(
            "All formats of the same number should normalize identically",
            PhoneUtils.normalize(international),
            PhoneUtils.normalize(local)
        )
        assertEquals(PhoneUtils.normalize(international), PhoneUtils.normalize(bare))
        assertEquals(PhoneUtils.normalize(international), PhoneUtils.normalize(with00))
    }

    // ── Edge cases ──

    @Test
    fun `normalize null returns empty string`() {
        assertEquals("", PhoneUtils.normalize(null))
    }

    @Test
    fun `normalize empty string returns empty string`() {
        assertEquals("", PhoneUtils.normalize(""))
    }

    @Test
    fun `normalize whitespace only returns empty string`() {
        assertEquals("", PhoneUtils.normalize("   "))
    }

    @Test
    fun `normalize short number still parses and formats`() {
        // libphonenumber.parse("123", "RW") succeeds and formats as +250123
        assertEquals("+250123", PhoneUtils.normalize("123"))
    }

    @Test
    fun `normalize non-numeric returns raw input`() {
        assertEquals("abc", PhoneUtils.normalize("abc"))
    }

    // ---------------------------------------------------------------------------
    // isValid()
    // ---------------------------------------------------------------------------

    // ── Valid numbers ──

    @Test
    fun `isValid returns true for local Rwandan number`() {
        assertTrue(PhoneUtils.isValid("0782877442"))
    }

    @Test
    fun `isValid returns true for international Rwandan number`() {
        assertTrue(PhoneUtils.isValid("+250782877442"))
    }

    @Test
    fun `isValid returns true for US number`() {
        assertTrue(PhoneUtils.isValid("+12024567890"))
    }

    @Test
    fun `isValid returns true for UK number`() {
        assertTrue(PhoneUtils.isValid("+447911123456"))
    }

    // ── Invalid numbers ──

    @Test
    fun `isValid returns false for too short number`() {
        assertFalse(PhoneUtils.isValid("123"))
    }

    @Test
    fun `isValid returns false for random text`() {
        assertFalse(PhoneUtils.isValid("not-a-number"))
    }

    @Test
    fun `isValid returns false for number with wrong country code`() {
        // +999 is not a valid country code
        assertFalse(PhoneUtils.isValid("+999000000000"))
    }

    // ── Edge cases ──

    @Test
    fun `isValid null returns false`() {
        assertFalse(PhoneUtils.isValid(null))
    }

    @Test
    fun `isValid empty string returns false`() {
        assertFalse(PhoneUtils.isValid(""))
    }

    @Test
    fun `isValid whitespace only returns false`() {
        assertFalse(PhoneUtils.isValid("   "))
    }

    @Test
    fun `isValid symbols without digits returns false`() {
        assertFalse(PhoneUtils.isValid("!@#$%"))
    }

    // ---------------------------------------------------------------------------
    // getValidationError()
    // ---------------------------------------------------------------------------

    // ── Valid numbers → null ──

    @Test
    fun `getValidationError returns null for valid local number`() {
        assertNull(PhoneUtils.getValidationError("0782877442"))
    }

    @Test
    fun `getValidationError returns null for valid international number`() {
        assertNull(PhoneUtils.getValidationError("+250782877442"))
    }

    @Test
    fun `getValidationError returns null for US number`() {
        assertNull(PhoneUtils.getValidationError("+12024567890"))
    }

    @Test
    fun `getValidationError returns null for formatted valid number`() {
        assertNull(PhoneUtils.getValidationError("  +250 (78) 287-7442  "))
    }

    // ── Invalid / missing → error strings ──

    @Test
    fun `getValidationError null returns phone_required`() {
        assertEquals("phone_required", PhoneUtils.getValidationError(null))
    }

    @Test
    fun `getValidationError empty returns phone_required`() {
        assertEquals("phone_required", PhoneUtils.getValidationError(""))
    }

    @Test
    fun `getValidationError whitespace returns phone_required`() {
        assertEquals("phone_required", PhoneUtils.getValidationError("   "))
    }

    @Test
    fun `getValidationError non-numeric without digits returns invalid_phone`() {
        assertEquals("invalid_phone", PhoneUtils.getValidationError("abc"))
    }

    @Test
    fun `getValidationError symbols without digits returns invalid_phone`() {
        assertEquals("invalid_phone", PhoneUtils.getValidationError("!@#$%"))
    }

    @Test
    fun `getValidationError too short returns invalid_phone`() {
        assertEquals("invalid_phone", PhoneUtils.getValidationError("123"))
    }

    @Test
    fun `getValidationError wrong country code returns invalid_phone`() {
        assertEquals("invalid_phone", PhoneUtils.getValidationError("+999000000000"))
    }

    @Test
    fun `getValidationError random text returns invalid_phone`() {
        assertEquals("invalid_phone", PhoneUtils.getValidationError("not-a-phone"))
    }

    // ---------------------------------------------------------------------------
    // Cross-method consistency: same number, different formats
    // ---------------------------------------------------------------------------

    @Test
    fun `same number in local and international format normalizes identically`() {
        val local = "0782877442"
        val intl = "+250782877442"
        assertEquals(PhoneUtils.normalize(local), PhoneUtils.normalize(intl))
    }

    @Test
    fun `all Rwandan formats of the same number are valid`() {
        assertTrue(PhoneUtils.isValid("0782877442"))
        assertTrue(PhoneUtils.isValid("+250782877442"))
        assertTrue(PhoneUtils.isValid("00250782877442"))
        assertTrue(PhoneUtils.isValid("250782877442"))
        assertTrue(PhoneUtils.isValid("078-287-7442"))
        assertTrue(PhoneUtils.isValid("+250 78 287 7442"))
    }

    @Test
    fun `all Rwandan formats produce null validation error`() {
        assertNull(PhoneUtils.getValidationError("0782877442"))
        assertNull(PhoneUtils.getValidationError("+250782877442"))
        assertNull(PhoneUtils.getValidationError("00250782877442"))
        assertNull(PhoneUtils.getValidationError("250782877442"))
    }
}
