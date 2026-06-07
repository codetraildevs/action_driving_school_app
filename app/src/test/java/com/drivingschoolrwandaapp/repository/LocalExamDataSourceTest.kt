package com.drivingschoolrwandaapp.repository

import android.content.Context
import android.content.SharedPreferences
import android.content.res.AssetManager
import android.util.Log
import com.drivingschoolrwandaapp.models.LocalExam
import com.drivingschoolrwandaapp.models.LocalExamWrapper
import com.google.gson.Gson
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.MockedStatic
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.any
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import java.io.ByteArrayInputStream
import java.io.IOException

/**
 * Unit tests for [LocalExamDataSource].
 *
 * Uses Mockito to mock Android framework dependencies ([Context], [AssetManager],
 * [SharedPreferences]) so tests run on the JVM without a device or emulator.
 *
 * Covers three main areas:
 * 1. **Cache behavior** — single-load, double-load (cache hit), cache clear,
 *    separate cache entries per language
 * 2. **Language normalization** — lowercase, uppercase, "kin"→"rw", unknown→"en"
 * 3. **Error handling** — invalid JSON returns empty list, missing file returns empty list
 */
class LocalExamDataSourceTest {

    private lateinit var context: Context
    private lateinit var assetManager: AssetManager
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var dataSource: LocalExamDataSource
    private val gson = Gson()
    private var logMock: MockedStatic<Log>? = null

    @Before
    fun setUp() {
        // Mock android.util.Log so tests don't throw RuntimeException on JVM
        logMock = mockStatic(Log::class.java)

        context = mock(Context::class.java)
        assetManager = mock(AssetManager::class.java)
        sharedPreferences = mock(SharedPreferences::class.java)

        // Stub Context.assets so AssetManager.open() can be mocked
        `when`(context.assets).thenReturn(assetManager)
        // Stub getSharedPreferences so the init block's listener registration works
        `when`(context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE))
            .thenReturn(sharedPreferences)

        dataSource = LocalExamDataSource(context)
    }

    @After
    fun tearDown() {
        logMock?.close()
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    /**
     * Mock the asset file for the given language to return the provided exams serialized as JSON.
     */
    /**
     * Mock the asset file for the given language to return the provided exams serialized as JSON.
     * Uses [thenAnswer] so each call to [AssetManager.open] gets a fresh [ByteArrayInputStream].
     * Without this, a consumed stream would cause the next call to read an empty string.
     */
    private fun mockAssetFileForLanguage(language: String, exams: List<LocalExam>) {
        val wrapper = LocalExamWrapper(exams)
        val json = gson.toJson(wrapper)
        val assetPath = assetPathForLanguage(language)
        `when`(assetManager.open(assetPath))
            .thenAnswer { ByteArrayInputStream(json.toByteArray()) }
    }

    /**
     * Mock the asset file for the given language to throw IOException (file not found).
     */
    /**
     * Mock the asset file for the given language to throw IOException (file not found).
     */
    private fun mockAssetFileMissing(language: String) {
        val assetPath = assetPathForLanguage(language)
        `when`(assetManager.open(assetPath)).thenThrow(IOException("File not found"))
    }

    /**
     * Return the asset file path for a given language.
     */
    private fun assetPathForLanguage(language: String): String {
        return when (language.lowercase()) {
            "en" -> "json_exams/en_exams.json"
            "fr" -> "json_exams/fr_exams.json"
            "rw" -> "json_exams/rw_exams.json"
            else -> "json_exams/en_exams.json"
        }
    }

    // ---------------------------------------------------------------------------
    // Basic Exam Loading
    // ---------------------------------------------------------------------------

    @Test
    fun `loadExams returns exams from English JSON file`() {
        val exam = LocalExam("177", "RANDOM", "Practice Test A", "img_a.jpg", emptyList())
        mockAssetFileForLanguage("en", listOf(exam))

        val exams = dataSource.loadExams("en")

        assertEquals(1, exams.size)
        assertEquals("177", exams[0].quizId)
        assertEquals("Practice Test A", exams[0].title)
        assertEquals("RANDOM", exams[0].examType)
        assertEquals("img_a.jpg", exams[0].examImgUrl)
    }

    @Test
    fun `loadExams returns exams from French JSON file`() {
        val exam = LocalExam("1", "CLASS", "Examen Français", "img_fr.jpg", emptyList())
        mockAssetFileForLanguage("fr", listOf(exam))

        val exams = dataSource.loadExams("fr")

        assertEquals(1, exams.size)
        assertEquals("Examen Français", exams[0].title)
    }

    @Test
    fun `loadExams returns exams from Kinyarwanda JSON file`() {
        val exam = LocalExam("1", "CLASS", "Ikizamini cy'Impyanya", "img_rw.jpg", emptyList())
        mockAssetFileForLanguage("rw", listOf(exam))

        val exams = dataSource.loadExams("rw")

        assertEquals(1, exams.size)
        assertEquals("Ikizamini cy'Impyanya", exams[0].title)
    }

    @Test
    fun `loadExams with multiple exams parses all of them`() {
        val exam1 = LocalExam("1", "CLASS", "Exam One", "img1.jpg", emptyList())
        val exam2 = LocalExam("177", "RANDOM", "Exam Two", "img2.jpg", emptyList())
        val exam3 = LocalExam("200", "SIGN", "Exam Three", "img3.jpg", emptyList())
        mockAssetFileForLanguage("en", listOf(exam1, exam2, exam3))

        val exams = dataSource.loadExams("en")

        assertEquals(3, exams.size)
        assertEquals("Exam One", exams[0].title)
        assertEquals("Exam Two", exams[1].title)
        assertEquals("Exam Three", exams[2].title)
    }

    // ---------------------------------------------------------------------------
    // Cache Behavior
    // ---------------------------------------------------------------------------

    @Test
    fun `loadExams same language twice uses cache on second call`() {
        val exam = LocalExam("1", "RANDOM", "Cached Test", "img.jpg", emptyList())
        mockAssetFileForLanguage("en", listOf(exam))

        dataSource.loadExams("en")
        dataSource.loadExams("en")

        // Asset file should only be opened once; second call hits cache
        verify(assetManager, times(1)).open("json_exams/en_exams.json")
    }

    @Test
    fun `loadExams different languages use separate cache entries`() {
        mockAssetFileForLanguage("en", listOf(LocalExam("1", "RANDOM", "English", "img.jpg", emptyList())))
        mockAssetFileForLanguage("fr", listOf(LocalExam("2", "RANDOM", "French", "img.jpg", emptyList())))

        dataSource.loadExams("en")
        dataSource.loadExams("fr")

        // Both asset files should be opened exactly once
        verify(assetManager, times(1)).open("json_exams/en_exams.json")
        verify(assetManager, times(1)).open("json_exams/fr_exams.json")
    }

    @Test
    fun `loadExams each language has independent cache entry`() {
        mockAssetFileForLanguage("en", listOf(LocalExam("1", "RANDOM", "English", "img.jpg", emptyList())))
        mockAssetFileForLanguage("fr", listOf(LocalExam("2", "RANDOM", "French", "img.jpg", emptyList())))

        dataSource.loadExams("en")
        dataSource.loadExams("fr")
        dataSource.loadExams("en")
        dataSource.loadExams("fr")

        // Each file opened exactly once — both languages' data is cached independently
        verify(assetManager, times(1)).open("json_exams/en_exams.json")
        verify(assetManager, times(1)).open("json_exams/fr_exams.json")
    }

    @Test
    fun `clearCache forces reload on next access`() {
        val exam = LocalExam("1", "RANDOM", "Test", "img.jpg", emptyList())
        mockAssetFileForLanguage("en", listOf(exam))

        dataSource.loadExams("en")
        dataSource.clearCache()
        dataSource.loadExams("en")

        // Asset file opened twice: first call + after cache clear
        verify(assetManager, times(2)).open("json_exams/en_exams.json")
    }

    @Test
    fun `clearCache with untouched cache does not throw`() {
        // Call clearCache before any loadExams — should be a no-op
        dataSource.clearCache()
        // After clearing, a load should still work
        mockAssetFileForLanguage("en", listOf(LocalExam("1", "RANDOM", "Test", "img.jpg", emptyList())))
        val exams = dataSource.loadExams("en")
        assertEquals(1, exams.size)
    }

    @Test
    fun `sharedpreferences listener clears cache when language_code changes`() {
        // Capture the OnSharedPreferenceChangeListener registered in the init block
        val captor = ArgumentCaptor.forClass(SharedPreferences.OnSharedPreferenceChangeListener::class.java)
        verify(sharedPreferences).registerOnSharedPreferenceChangeListener(captor.capture())
        val languageListener = captor.value

        // Populate cache: load English exams once
        val exam = LocalExam("1", "RANDOM", "Test", "img.jpg", emptyList())
        mockAssetFileForLanguage("en", listOf(exam))
        dataSource.loadExams("en")
        verify(assetManager, times(1)).open("json_exams/en_exams.json")

        // Simulate language change via SharedPreferences callback
        languageListener.onSharedPreferenceChanged(sharedPreferences, "language_code")

        // Load English again — cache was invalidated, so asset must be re-opened
        dataSource.loadExams("en")
        verify(assetManager, times(2)).open("json_exams/en_exams.json")
    }

    @Test
    fun `sharedpreferences listener does not clear cache for other preference keys`() {
        // Capture the OnSharedPreferenceChangeListener
        val captor = ArgumentCaptor.forClass(SharedPreferences.OnSharedPreferenceChangeListener::class.java)
        verify(sharedPreferences).registerOnSharedPreferenceChangeListener(captor.capture())
        val languageListener = captor.value

        // Populate cache
        val exam = LocalExam("1", "RANDOM", "Test", "img.jpg", emptyList())
        mockAssetFileForLanguage("en", listOf(exam))
        dataSource.loadExams("en")
        verify(assetManager, times(1)).open("json_exams/en_exams.json")

        // Simulate a change to a different preference key (e.g., "is_grid_layout")
        languageListener.onSharedPreferenceChanged(sharedPreferences, "is_grid_layout")

        // Load English again — cache should still be valid, so asset is NOT re-opened
        dataSource.loadExams("en")
        verify(assetManager, times(1)).open("json_exams/en_exams.json")
    }

    @Test
    fun `sharedpreferences listener is registered against correct sharedPreferences instance`() {
        // Verify the listener was registered on the expected SharedPreferences instance
        // (the one returned by context.getSharedPreferences("app_prefs", MODE_PRIVATE))
        verify(context).getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        verify(sharedPreferences).registerOnSharedPreferenceChangeListener(any())
    }

    // ---------------------------------------------------------------------------
    // Language Normalization
    // ---------------------------------------------------------------------------

    @Test
    fun `uppercase EN normalizes to en`() {
        mockAssetFileForLanguage("en", listOf(LocalExam("1", "RANDOM", "Uppercase Test", "img.jpg", emptyList())))

        val exams = dataSource.loadExams("EN")

        assertEquals(1, exams.size)
        verify(assetManager).open("json_exams/en_exams.json")
    }

    @Test
    fun `mixedcase Fr normalizes to fr`() {
        mockAssetFileForLanguage("fr", listOf(LocalExam("1", "RANDOM", "Mixed Case", "img.jpg", emptyList())))

        val exams = dataSource.loadExams("Fr")

        assertEquals(1, exams.size)
        verify(assetManager).open("json_exams/fr_exams.json")
    }

    @Test
    fun `kin language code maps to rw`() {
        mockAssetFileForLanguage("rw", listOf(LocalExam("1", "RANDOM", "Kinyarwanda", "img.jpg", emptyList())))

        val exams = dataSource.loadExams("kin")

        assertEquals(1, exams.size)
        assertEquals("Kinyarwanda", exams[0].title)
        verify(assetManager).open("json_exams/rw_exams.json")
    }

    @Test
    fun `KIN uppercase maps to rw`() {
        mockAssetFileForLanguage("rw", listOf(LocalExam("1", "RANDOM", "Test", "img.jpg", emptyList())))

        dataSource.loadExams("KIN")

        verify(assetManager).open("json_exams/rw_exams.json")
    }

    @Test
    fun `unknown language code falls back to English`() {
        mockAssetFileForLanguage("en", listOf(LocalExam("1", "RANDOM", "Fallback", "img.jpg", emptyList())))

        val exams = dataSource.loadExams("de")

        assertEquals(1, exams.size)
        assertEquals("Fallback", exams[0].title)
        verify(assetManager).open("json_exams/en_exams.json")
    }

    @Test
    fun `empty language code falls back to English`() {
        mockAssetFileForLanguage("en", listOf(LocalExam("1", "RANDOM", "Empty Fallback", "img.jpg", emptyList())))

        val exams = dataSource.loadExams("")

        assertEquals(1, exams.size)
        assertEquals("Empty Fallback", exams[0].title)
        verify(assetManager).open("json_exams/en_exams.json")
    }

    @Test
    fun `null-safe language normalization does not crash`() {
        // Note: Kotlin's type system prevents passing null to loadExams,
        // but the normalizeLanguage function should handle edge cases via when.
        // This test verifies that an unexpected code gets the English fallback.
        mockAssetFileForLanguage("en", listOf(LocalExam("1", "RANDOM", "Safe", "img.jpg", emptyList())))

        val exams = dataSource.loadExams("x")

        assertEquals(1, exams.size)
        verify(assetManager).open("json_exams/en_exams.json")
    }

    // ---------------------------------------------------------------------------
    // Error Handling
    // ---------------------------------------------------------------------------

    @Test
    fun `invalid JSON returns empty exam list`() {
        `when`(assetManager.open("json_exams/en_exams.json"))
            .thenReturn(ByteArrayInputStream("not valid json at all".toByteArray()))

        val exams = dataSource.loadExams("en")

        assertTrue("Expected empty list for invalid JSON", exams.isEmpty())
    }

    @Test
    fun `malformed JSON array returns empty exam list`() {
        `when`(assetManager.open("json_exams/en_exams.json"))
            .thenReturn(ByteArrayInputStream("{\"exams\": broken}".toByteArray()))

        val exams = dataSource.loadExams("en")

        assertTrue("Expected empty list for malformed JSON", exams.isEmpty())
    }

    @Test
    fun `missing asset file returns empty exam list`() {
        mockAssetFileMissing("en")

        val exams = dataSource.loadExams("en")

        assertTrue("Expected empty list when asset file is missing", exams.isEmpty())
    }

    @Test
    fun `IOError during read returns empty exam list`() {
        `when`(assetManager.open("json_exams/en_exams.json")).thenThrow(IOException("I/O error"))

        val exams = dataSource.loadExams("en")

        assertTrue("Expected empty list on IO error", exams.isEmpty())
    }

    // ---------------------------------------------------------------------------
    // Utility Methods
    // ---------------------------------------------------------------------------

    @Test
    fun `getImageAssetPath constructs correct file URI`() {
        val path = dataSource.getImageAssetPath("question_001.png")
        assertEquals("file:///android_asset/json_questions_images/question_001.png", path)
    }

    @Test
    fun `getImageAssetPath handles image in subdirectory`() {
        val path = dataSource.getImageAssetPath("subfolder/image.png")
        assertEquals("file:///android_asset/json_questions_images/subfolder/image.png", path)
    }

    // ---------------------------------------------------------------------------
    // Lookup Methods
    // ---------------------------------------------------------------------------

    @Test
    fun `loadExamByQuizId returns correct exam for matching quizId`() {
        val exam1 = LocalExam("1", "CLASS", "Exam Alpha", "img1.jpg", emptyList())
        val exam2 = LocalExam("177", "RANDOM", "Exam Beta", "img2.jpg", emptyList())
        mockAssetFileForLanguage("en", listOf(exam1, exam2))

        val result = dataSource.loadExamByQuizId("177", "en")

        assertNotNull(result)
        assertEquals("Exam Beta", result!!.title)
    }

    @Test
    fun `loadExamByQuizId returns null for non existent quizId`() {
        val exam = LocalExam("1", "CLASS", "Exam Alpha", "img.jpg", emptyList())
        mockAssetFileForLanguage("en", listOf(exam))

        val result = dataSource.loadExamByQuizId("999", "en")

        assertNull("Expected null for non-existent quizId", result)
    }

    @Test
    fun `loadExamByQuizId returns null when exam list is empty`() {
        mockAssetFileForLanguage("en", emptyList())

        val result = dataSource.loadExamByQuizId("1", "en")

        assertNull("Expected null when no exams loaded", result)
    }

    @Test
    fun `loadExamByIndex returns exam at valid index`() {
        val exam1 = LocalExam("1", "CLASS", "First Exam", "img1.jpg", emptyList())
        val exam2 = LocalExam("2", "RANDOM", "Second Exam", "img2.jpg", emptyList())
        mockAssetFileForLanguage("en", listOf(exam1, exam2))

        val result = dataSource.loadExamByIndex(1, "en")

        assertNotNull(result)
        assertEquals("Second Exam", result!!.title)
    }

    @Test
    fun `loadExamByIndex returns null for negative index`() {
        val exam = LocalExam("1", "CLASS", "Exam", "img.jpg", emptyList())
        mockAssetFileForLanguage("en", listOf(exam))

        val result = dataSource.loadExamByIndex(-1, "en")

        assertNull("Expected null for negative index", result)
    }

    @Test
    fun `loadExamByIndex returns null for out of bounds index`() {
        val exam = LocalExam("1", "CLASS", "Exam", "img.jpg", emptyList())
        mockAssetFileForLanguage("en", listOf(exam))

        val result = dataSource.loadExamByIndex(10, "en")

        assertNull("Expected null for out-of-bounds index", result)
    }
}
