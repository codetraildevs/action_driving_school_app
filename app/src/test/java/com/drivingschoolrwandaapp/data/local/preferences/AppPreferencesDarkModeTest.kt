package com.drivingschoolrwandaapp.data.local.preferences

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

/**
 * Unit tests for [AppPreferences] dark-mode preference methods.
 *
 * Uses a mock [SharedPreferences] injected via a mock [Context] so we
 * can verify read/write behaviour without hitting disk.
 *
 * Dark mode values:
 *   0 = Follow system (default)
 *   1 = Off  (light)
 *   2 = On   (dark)
 */
class AppPreferencesDarkModeTest {

    private lateinit var appPreferences: AppPreferences
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor

    @Before
    fun setUp() {
        mockPrefs = mock(SharedPreferences::class.java)
        mockEditor = mock(SharedPreferences.Editor::class.java)
        val mockContext = mock(Context::class.java)

        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPrefs)
        `when`(mockPrefs.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putInt(anyString(), anyInt())).thenReturn(mockEditor)

        appPreferences = AppPreferences(mockContext)
    }

    // ── Default value ──

    @Test
    fun `getDarkMode returns follow-system (0) when nothing is stored`() {
        `when`(mockPrefs.getInt("dark_mode", 0)).thenReturn(0)

        assertEquals(0, appPreferences.darkMode)
    }

    // ── Round-trip: set then get ──

    @Test
    fun `setDarkMode stores value via putInt on the editor`() {
        appPreferences.setDarkMode(1)

        verify(mockEditor).putInt("dark_mode", 1)
        verify(mockEditor).apply()
    }

    @Test
    fun `setDarkMode stores follow-system (0)`() {
        appPreferences.setDarkMode(0)

        verify(mockEditor).putInt("dark_mode", 0)
        verify(mockEditor).apply()
    }

    @Test
    fun `setDarkMode stores light (1)`() {
        appPreferences.setDarkMode(1)

        verify(mockEditor).putInt("dark_mode", 1)
        verify(mockEditor).apply()
    }

    @Test
    fun `setDarkMode stores dark (2)`() {
        appPreferences.setDarkMode(2)

        verify(mockEditor).putInt("dark_mode", 2)
        verify(mockEditor).apply()
    }

    @Test
    fun `getDarkMode returns stored follow-system (0)`() {
        `when`(mockPrefs.getInt("dark_mode", 0)).thenReturn(0)

        assertEquals(0, appPreferences.darkMode)
    }

    @Test
    fun `getDarkMode returns stored light mode (1)`() {
        `when`(mockPrefs.getInt("dark_mode", 0)).thenReturn(1)

        assertEquals(1, appPreferences.darkMode)
    }

    @Test
    fun `getDarkMode returns stored dark mode (2)`() {
        `when`(mockPrefs.getInt("dark_mode", 0)).thenReturn(2)

        assertEquals(2, appPreferences.darkMode)
    }

    // ── Full round-trip simulation ──

    @Test
    fun `dark mode round-trip follow-system then light then dark`() {
        // Follow system (0)
        `when`(mockPrefs.getInt("dark_mode", 0)).thenReturn(0)
        assertEquals(0, appPreferences.darkMode)

        // Switch to light (1)
        appPreferences.setDarkMode(1)
        verify(mockEditor).putInt("dark_mode", 1)
        `when`(mockPrefs.getInt("dark_mode", 0)).thenReturn(1)
        assertEquals(1, appPreferences.darkMode)

        // Switch to dark (2)
        appPreferences.setDarkMode(2)
        verify(mockEditor).putInt("dark_mode", 2)
        `when`(mockPrefs.getInt("dark_mode", 0)).thenReturn(2)
        assertEquals(2, appPreferences.darkMode)

        // Back to follow system (0)
        appPreferences.setDarkMode(0)
        verify(mockEditor).putInt("dark_mode", 0)
        `when`(mockPrefs.getInt("dark_mode", 0)).thenReturn(0)
        assertEquals(0, appPreferences.darkMode)
    }

    // ── SharedPreferences key correctness ──

    @Test
    fun `dark mode uses the correct SharedPreferences key`() {
        appPreferences.setDarkMode(2)

        verify(mockEditor).putInt(eq("dark_mode"), anyInt())
    }

    @Test
    fun `getDarkMode reads with the correct key and default`() {
        `when`(mockPrefs.getInt("dark_mode", 0)).thenReturn(0)

        appPreferences.darkMode

        verify(mockPrefs).getInt("dark_mode", 0)
    }
}
