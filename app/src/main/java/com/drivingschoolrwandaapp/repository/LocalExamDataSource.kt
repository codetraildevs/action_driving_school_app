package com.drivingschoolrwandaapp.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.drivingschoolrwandaapp.models.LocalExam
import com.drivingschoolrwandaapp.models.LocalExamWrapper
import com.google.gson.Gson

/**
 * Loads exam data from local JSON assets instead of network API.
 * Supports multilingual exams (EN, FR, RW) with caching.
 *
 * Automatically invalidates the cache when the user changes their language
 * preference via [SharedPreferences], ensuring exams are reloaded from the
 * appropriate JSON file on the next access.
 */
class LocalExamDataSource(private val context: Context) {
    private val gson = Gson()
    private val cache = mutableMapOf<String, LocalExamWrapper>()

    companion object {
        private const val TAG = "LocalExamDataSource"
        private const val EXAMS_PATH = "json_exams"
        private const val QUESTIONS_IMAGES_PATH = "json_questions_images"
        private const val PREFS_NAME = "app_prefs"
        private const val KEY_LANGUAGE_CODE = "language_code"

        /** Language code to filename mapping */
        private val LANGUAGE_FILES = mapOf(
            "en" to "en_exams.json",
            "fr" to "fr_exams.json",
            "rw" to "rw_exams.json"
        )
    }

    /**
     * Strong reference to the SharedPreferences change listener.
     * Required because [SharedPreferences.registerOnSharedPreferenceChangeListener]
     * stores listeners in a [WeakHashMap] internally. Without a strong reference
     * held by this singleton, the listener would be garbage-collected and the
     * cache would silently stop invalidating on language changes.
     */
    private val languageChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == KEY_LANGUAGE_CODE) {
            Log.d(TAG, "Language preference changed — clearing exam cache")
            clearCache()
        }
    }

    /**
     * Registers a listener on the app's SharedPreferences so the exam cache
     * is cleared whenever the user changes their language preference.
     * This ensures the next call to [loadExams] re-parses the correct JSON file.
     */
    init {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(languageChangeListener)
    }

    /**
     * Get the image assets path prefix for Glide/AssetManager.
     * Images are stored in [json_questions_images/] under the assets folder.
     */
    fun getImageAssetPath(imageFileName: String): String {
        return "file:///android_asset/$QUESTIONS_IMAGES_PATH/$imageFileName"
    }

    /**
     * Load all exams for the given language code.
     * @param languageCode Language code: "en", "fr", or "rw"
     * @return List of [LocalExam] from the parsed JSON
     */
    fun loadExams(languageCode: String): List<LocalExam> {
        val wrapper = getOrLoad(languageCode)
        return wrapper.exams
    }

    /**
     * Load a specific exam by quizId for the given language.
     * @param quizId The quizId string from JSON (e.g., "1", "177")
     * @param languageCode Language code: "en", "fr", or "rw"
     * @return The matching [LocalExam], or null if not found
     */
    fun loadExamByQuizId(quizId: String, languageCode: String): LocalExam? {
        val wrapper = getOrLoad(languageCode)
        return wrapper.exams.find { it.quizId == quizId }
    }

    /**
     * Load a specific exam by index position in the list.
     * @param examIndex The index (0-based) of the exam in the list
     * @param languageCode Language code: "en", "fr", or "rw"
     * @return The matching [LocalExam], or null if out of bounds
     */
    fun loadExamByIndex(examIndex: Int, languageCode: String): LocalExam? {
        val wrapper = getOrLoad(languageCode)
        return wrapper.exams.getOrNull(examIndex)
    }

    /**
     * Get or load cached JSON for the given language.
     * Caches results so JSON is only parsed once per language.
     */
    private fun getOrLoad(languageCode: String): LocalExamWrapper {
        val normalizedLang = normalizeLanguage(languageCode)
        return cache.getOrPut(normalizedLang) {
            val fileName = LANGUAGE_FILES[normalizedLang]
                ?: LANGUAGE_FILES["en"]!! // Fallback to English
            loadJsonFromAssets(fileName)
        }
    }

    /**
     * Load and parse a JSON file from assets.
     */
    private fun loadJsonFromAssets(fileName: String): LocalExamWrapper {
        return try {
            val jsonString = context.assets.open("$EXAMS_PATH/$fileName")
                .bufferedReader()
                .use { it.readText() }
            gson.fromJson(jsonString, LocalExamWrapper::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load exams from assets/$EXAMS_PATH/$fileName: ${e.message}", e)
            // If file not found or parse error, return empty wrapper
            LocalExamWrapper(exams = emptyList())
        }
    }

    /**
     * Normalize language codes to our supported set.
     * Maps codes like "en" -> "en", "fr" -> "fr", "rw" -> "rw".
     * Reads numeric languageIds from AppPreferences would need separate handling.
     */
    private fun normalizeLanguage(code: String): String {
        return when {
            code.startsWith("en", ignoreCase = true) -> "en"
            code.startsWith("fr", ignoreCase = true) -> "fr"
            code.startsWith("rw", ignoreCase = true) -> "rw"
            code.startsWith("kin", ignoreCase = true) -> "rw"
            else -> "en" // Default to English
        }
    }

    /** Clear the cache to force re-load from assets */
    fun clearCache() {
        cache.clear()
    }
}
