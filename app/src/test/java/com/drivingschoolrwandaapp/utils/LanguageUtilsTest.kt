package com.drivingschoolrwandaapp.utils

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.drivingschoolrwandaapp.R
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.MockedStatic
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.never
import org.mockito.Mockito.verify

/**
 * Unit tests for [LanguageUtils.loadAppLanguage].
 *
 * NOTE: [LanguageUtils.showLanguageDialog] is intentionally NOT tested here —
 * it constructs and shows a real [androidx.appcompat.app.AlertDialog], which
 * requires a Looper / Robolectric environment. Only the pure preference +
 * locale-application logic of `loadAppLanguage` is covered.
 *
 * [AppCompatDelegate.setApplicationLocales] is a static call, so it is mocked
 * with [mockStatic] and verified via an [ArgumentCaptor] on [LocaleListCompat].
 */
class LanguageUtilsTest {

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var resources: Resources
    private lateinit var configuration: Configuration
    private var appCompatMock: MockedStatic<AppCompatDelegate>? = null

    @Before
    fun setUp() {
        context = mock(Context::class.java)
        sharedPreferences = mock(SharedPreferences::class.java)
        editor = mock(SharedPreferences.Editor::class.java)
        resources = mock(Resources::class.java)
        configuration = mock(Configuration::class.java)

        `when`(context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)).thenReturn(sharedPreferences)
        `when`(sharedPreferences.edit()).thenReturn(editor)
        `when`(editor.putString(anyString(), anyString())).thenReturn(editor)
        `when`(context.resources).thenReturn(resources)
        `when`(resources.configuration).thenReturn(configuration)

        appCompatMock = mockStatic(AppCompatDelegate::class.java)
    }

    @After
    fun tearDown() {
        appCompatMock?.close()
    }

    private fun capturedLocales(): LocaleListCompat {
        val captor = ArgumentCaptor.forClass(LocaleListCompat::class.java)
        appCompatMock!!.verify { AppCompatDelegate.setApplicationLocales(captor.capture()) }
        assertNotNull("setApplicationLocales must have been called", captor.value)
        return captor.value
    }

    // ---------------------------------------------------------------------------
    // Default language when unset
    // ---------------------------------------------------------------------------

    @Test
    fun `loadAppLanguage sets default rw when no language stored`() {
        `when`(sharedPreferences.contains("language_code")).thenReturn(false)
        `when`(sharedPreferences.getString("language_code", "rw")).thenReturn("rw")

        LanguageUtils.loadAppLanguage(context)

        // Default language was persisted.
        verify(editor).putString("language_code", "rw")
        // And applied to the app locales.
        val locales = capturedLocales()
        assertEquals("rw", locales.get(0)?.language)
    }

    // ---------------------------------------------------------------------------
    // Stored language applied
    // ---------------------------------------------------------------------------

    @Test
    fun `loadAppLanguage applies the stored language`() {
        `when`(sharedPreferences.contains("language_code")).thenReturn(true)
        `when`(sharedPreferences.getString("language_code", "rw")).thenReturn("en")

        LanguageUtils.loadAppLanguage(context)

        // Already set → must not overwrite.
        verify(editor, never()).putString("language_code", "rw")
        verify(editor, never()).putString(anyString(), anyString())

        val locales = capturedLocales()
        assertEquals("en", locales.get(0)?.language)
    }

    @Test
    fun `loadAppLanguage applies stored french language`() {
        `when`(sharedPreferences.contains("language_code")).thenReturn(true)
        `when`(sharedPreferences.getString("language_code", "rw")).thenReturn("fr")

        LanguageUtils.loadAppLanguage(context)

        val locales = capturedLocales()
        assertEquals("fr", locales.get(0)?.language)
    }

    // ---------------------------------------------------------------------------
    // Null / empty language — nothing applied
    // ---------------------------------------------------------------------------

    @Test
    fun `loadAppLanguage ignores a null stored language`() {
        `when`(sharedPreferences.contains("language_code")).thenReturn(true)
        `when`(sharedPreferences.getString("language_code", "rw")).thenReturn(null)

        LanguageUtils.loadAppLanguage(context)

        appCompatMock!!.verifyNoInteractions()
        verify(editor, never()).putString(anyString(), anyString())
    }

    @Test
    fun `loadAppLanguage ignores an empty stored language`() {
        `when`(sharedPreferences.contains("language_code")).thenReturn(true)
        `when`(sharedPreferences.getString("language_code", "rw")).thenReturn("")

        LanguageUtils.loadAppLanguage(context)

        appCompatMock!!.verifyNoInteractions()
        verify(editor, never()).putString(anyString(), anyString())
    }
}
