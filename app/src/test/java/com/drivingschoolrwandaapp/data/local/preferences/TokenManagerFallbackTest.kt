package com.drivingschoolrwandaapp.data.local.preferences

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

/**
 * Tests for [TokenManager]'s StackOverflowError fallback behavior.
 *
 * When EncryptedSharedPreferences is corrupted (a known Android Keystore bug),
 * every read operation throws StackOverflowError. These tests verify that TokenManager
 * catches the error, switches to a plain SharedPreferences fallback, and returns safe
 * defaults instead of crashing.
 *
 * We use the package-private constructor to inject a mock SharedPreferences,
 * simulating the real-world corruption scenario.
 */
class TokenManagerFallbackTest {

    private lateinit var tokenManager: TokenManager
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockContext: Context
    private lateinit var mockFallbackPrefs: SharedPreferences

    @Before
    fun setUp() {
        mockPrefs = mock(SharedPreferences::class.java)
        mockContext = mock(Context::class.java)
        mockFallbackPrefs = mock(SharedPreferences::class.java)

        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockFallbackPrefs)

        // Stub the fallback prefs editor so switchToFallback() can clear it
        val fallbackEditor = mock(SharedPreferences.Editor::class.java)
        `when`(mockFallbackPrefs.edit()).thenReturn(fallbackEditor)
        `when`(fallbackEditor.clear()).thenReturn(fallbackEditor)

        // Use package-private constructor to inject mock prefs directly
        tokenManager = TokenManager(mockContext, mockPrefs)
    }

    // ── getAccessToken ──

    @Test
    fun `getAccessToken returns token when prefs work normally`() {
        `when`(mockPrefs.getString("access_token", null)).thenReturn("test_access_token")

        assertEquals("test_access_token", tokenManager.accessToken)
    }

    @Test
    fun `getAccessToken returns null on StackOverflowError`() {
        `when`(mockPrefs.getString("access_token", null)).thenThrow(StackOverflowError("keystore corrupted"))

        val result = tokenManager.accessToken

        assertNull(result)
    }

    @Test
    fun `getAccessToken switches to fallback after StackOverflowError`() {
        `when`(mockPrefs.getString("access_token", null)).thenThrow(StackOverflowError("keystore corrupted"))

        tokenManager.accessToken

        // Verify fallback SharedPreferences was created
        verify(mockContext).getSharedPreferences("encrypted_tokens_fallback", Context.MODE_PRIVATE)
    }

    // ── getRefreshToken ──

    @Test
    fun `getRefreshToken returns token when prefs work normally`() {
        `when`(mockPrefs.getString("refresh_token", null)).thenReturn("test_refresh_token")

        assertEquals("test_refresh_token", tokenManager.refreshToken)
    }

    @Test
    fun `getRefreshToken returns null on StackOverflowError`() {
        `when`(mockPrefs.getString("refresh_token", null)).thenThrow(StackOverflowError("keystore corrupted"))

        val result = tokenManager.refreshToken

        assertNull(result)
    }

    // ── isTokenExpired ──

    @Test
    fun `isTokenExpired returns false when token is valid`() {
        val futureTime = System.currentTimeMillis() + 86_400_000 // 24 hours from now
        `when`(mockPrefs.getLong("token_expiry", 0)).thenReturn(futureTime)

        assertFalse(tokenManager.isTokenExpired)
    }

    @Test
    fun `isTokenExpired returns true when token is expired`() {
        val pastTime = System.currentTimeMillis() - 86_400_000 // 24 hours ago
        `when`(mockPrefs.getLong("token_expiry", 0)).thenReturn(pastTime)

        assertTrue(tokenManager.isTokenExpired)
    }

    @Test
    fun `isTokenExpired returns true on StackOverflowError (forces re-auth)`() {
        `when`(mockPrefs.getLong("token_expiry", 0)).thenThrow(StackOverflowError("keystore corrupted"))

        assertTrue(tokenManager.isTokenExpired)
    }

    // ── getRoleId ──

    @Test
    fun `getRoleId returns stored role when prefs work normally`() {
        `when`(mockPrefs.getInt("role_id", 0)).thenReturn(2)

        assertEquals(2, tokenManager.roleId)
    }

    @Test
    fun `getRoleId returns 0 on StackOverflowError`() {
        `when`(mockPrefs.getInt("role_id", 0)).thenThrow(StackOverflowError("keystore corrupted"))

        assertEquals(0, tokenManager.roleId)
    }

    // ── isLoggedIn ──

    @Test
    fun `isLoggedIn returns true when token exists and not expired`() {
        val futureTime = System.currentTimeMillis() + 86_400_000
        `when`(mockPrefs.getString("access_token", null)).thenReturn("valid_token")
        `when`(mockPrefs.getLong("token_expiry", 0)).thenReturn(futureTime)

        assertTrue(tokenManager.isLoggedIn)
    }

    @Test
    fun `isLoggedIn returns false when getAccessToken throws StackOverflowError`() {
        `when`(mockPrefs.getString("access_token", null)).thenThrow(StackOverflowError("keystore corrupted"))

        assertFalse(tokenManager.isLoggedIn)
    }

    @Test
    fun `isLoggedIn returns false when no token`() {
        `when`(mockPrefs.getString("access_token", null)).thenReturn(null)

        assertFalse(tokenManager.isLoggedIn)
    }

    // ── getTokenExpiryTime ──

    @Test
    fun `getTokenExpiryTime returns stored value when prefs work normally`() {
        val expected = 1700000000000L
        `when`(mockPrefs.getLong("token_expiry", 0)).thenReturn(expected)

        assertEquals(expected, tokenManager.tokenExpiryTime)
    }

    @Test
    fun `getTokenExpiryTime returns 0 on StackOverflowError`() {
        `when`(mockPrefs.getLong("token_expiry", 0)).thenThrow(StackOverflowError("keystore corrupted"))

        assertEquals(0L, tokenManager.tokenExpiryTime)
    }

    // ── isRememberMe ──

    @Test
    fun `isRememberMe returns stored value when prefs work normally`() {
        `when`(mockPrefs.getBoolean("remember_me", true)).thenReturn(false)

        assertFalse(tokenManager.isRememberMe)
    }

    @Test
    fun `isRememberMe returns true (default) on StackOverflowError`() {
        `when`(mockPrefs.getBoolean("remember_me", true)).thenThrow(StackOverflowError("keystore corrupted"))

        assertTrue(tokenManager.isRememberMe)
    }

    // ── Fallback only triggers once ──

    @Test
    fun `switchToFallback only creates fallback prefs once`() {
        `when`(mockPrefs.getString("access_token", null)).thenThrow(StackOverflowError("keystore corrupted"))

        // Call twice — second call should use already-created fallback, not create another
        tokenManager.accessToken
        tokenManager.refreshToken

        // getSharedPreferences should be called only once (from switchToFallback)
        verify(mockContext, times(1)).getSharedPreferences("encrypted_tokens_fallback", Context.MODE_PRIVATE)
    }

    // ── saveTokens ──

    @Test
    fun `saveTokens stores tokens and expiry`() {
        val editor = mock(SharedPreferences.Editor::class.java)
        `when`(mockPrefs.edit()).thenReturn(editor)
        `when`(editor.putString(anyString(), anyString())).thenReturn(editor)
        `when`(editor.putLong(anyString(), anyLong())).thenReturn(editor)
        `when`(editor.putBoolean(anyString(), anyBoolean())).thenReturn(editor)

        tokenManager.saveTokens("new_access", "new_refresh", true)

        verify(editor).putString("access_token", "new_access")
        verify(editor).putString("refresh_token", "new_refresh")
        verify(editor).putBoolean("remember_me", true)
        verify(editor).apply()
    }

    // ── clearTokens ──

    @Test
    fun `clearTokens removes all token data`() {
        val editor = mock(SharedPreferences.Editor::class.java)
        `when`(mockPrefs.edit()).thenReturn(editor)
        `when`(editor.remove(anyString())).thenReturn(editor)

        tokenManager.clearTokens()

        verify(editor).remove("access_token")
        verify(editor).remove("refresh_token")
        verify(editor).remove("token_expiry")
        verify(editor).remove("role_id")
        verify(editor).apply()
    }

    @Test
    fun `clearTokens does not crash on exception`() {
        `when`(mockPrefs.edit()).thenThrow(RuntimeException("disk full"))

        // Should not throw
        tokenManager.clearTokens()
    }

    // ── saveRole ──

    @Test
    fun `saveRole stores role id`() {
        val editor = mock(SharedPreferences.Editor::class.java)
        `when`(mockPrefs.edit()).thenReturn(editor)
        `when`(editor.putInt(anyString(), anyInt())).thenReturn(editor)

        tokenManager.saveRole(5)

        verify(editor).putInt("role_id", 5)
        verify(editor).apply()
    }
}
