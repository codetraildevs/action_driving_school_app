package com.drivingschoolrwandaapp.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.drivingschoolrwandaapp.database.entities.TestEntity
import com.drivingschoolrwandaapp.database.entities.TestWithQuestions
import com.drivingschoolrwandaapp.models.LocalExam
import com.drivingschoolrwandaapp.models.LocalQuestion
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic

/**
 * Unit tests for [TestRepository].
 *
 * Verifies the mapping from [LocalExam] JSON models to Room entities
 * ([TestEntity], [TestWithQuestions], etc.) and tests error handling
 * for the local exam data flow.
 *
 * Uses [InstantTaskExecutorRule] so [androidx.lifecycle.LiveData.setValue]
 * executes synchronously on the test thread without needing a device.
 */
class TestRepositoryTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var localExamDataSource: LocalExamDataSource
    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var repository: TestRepository
    private var logMock: MockedStatic<Log>? = null
    private var crashlyticsMock: MockedStatic<FirebaseCrashlytics>? = null

    @Before
    fun setUp() {
        logMock = mockStatic(Log::class.java)
        crashlyticsMock = mockStatic(FirebaseCrashlytics::class.java)
        `when`(FirebaseCrashlytics.getInstance()).thenReturn(mock(FirebaseCrashlytics::class.java))

        localExamDataSource = mock(LocalExamDataSource::class.java)
        context = mock(Context::class.java)
        sharedPreferences = mock(SharedPreferences::class.java)

        // Stub SharedPreferences used by AppPreferences internally
        `when`(context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE))
            .thenReturn(sharedPreferences)
        // Return English as the current language
        `when`(sharedPreferences.getString("language_code", "rw")).thenReturn("en")

        repository = TestRepository(localExamDataSource, context)
    }

    @After
    fun tearDown() {
        logMock?.close()
        crashlyticsMock?.close()
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    /** Create a [LocalQuestion] with the given options. Option at [correctOptionIdx] is correct. */
    private fun localQuestion(
        text: String = "Sample question?",
        options: List<String> = listOf("Option A", "Option B", "Option C", "Option D"),
        correctOptionIdx: Int = 0,
        imageUrl: String = "assets/json_questions_images/q_img.png"
    ): LocalQuestion {
        return LocalQuestion(
            question = text,
            option1 = options.getOrElse(0) { "" },
            option2 = options.getOrElse(1) { "" },
            option3 = options.getOrElse(2) { "" },
            option4 = options.getOrElse(3) { "" },
            correctAnswer = options.getOrElse(correctOptionIdx) { "" },
            questionImgUrl = imageUrl
        )
    }

    private fun sampleExam(): LocalExam {
        return LocalExam(
            quizId = "177",
            examType = "RANDOM",
            title = "Practice Test A",
            examImgUrl = "assets/json_questions_images/exam.png",
            questions = listOf(
                localQuestion("Q1?", correctOptionIdx = 0),
                localQuestion("Q2?", correctOptionIdx = 2)
            )
        )
    }

    // ---------------------------------------------------------------------------
    // getTests — Success Paths
    // ---------------------------------------------------------------------------

    @Test
    fun `getTests maps local exam fields to TestEntity correctly`() {
        val exam = sampleExam()
        `when`(localExamDataSource.loadExams("en")).thenReturn(listOf(exam))

        val resource = repository.getTests(false).value!!

        assertEquals(Resource.Status.SUCCESS, resource.status)
        val entities = resource.data!!
        assertEquals(1, entities.size)

        val entity = entities[0]
        assertEquals("Practice Test A", entity.title)
        assertEquals("RANDOM", entity.description)
        assertEquals(177, entity.id)
        assertEquals(1, entity.testNumber)
        assertEquals("file:///android_asset/json_questions_images/exam.png", entity.imageUrl)
        assertEquals(2, entity.totalMarks)
        assertEquals(1, entity.passMarks)  // ceil(2 * 0.5) = 1
        assertEquals(30, entity.duration)
        assertEquals(false, entity.isFree)
    }

    @Test
    fun `getTests maps multiple exams preserving order`() {
        val exam1 = LocalExam("1", "CLASS", "Exam One", "", emptyList())
        val exam2 = LocalExam("2", "RANDOM", "Exam Two", "", emptyList())
        `when`(localExamDataSource.loadExams("en")).thenReturn(listOf(exam1, exam2))

        val resource = repository.getTests(false).value!!

        assertEquals(2, resource.data!!.size)
        assertEquals("Exam One", resource.data[0].title)
        assertEquals(1, resource.data[0].testNumber)
        assertEquals("Exam Two", resource.data[1].title)
        assertEquals(2, resource.data[1].testNumber)
    }

    @Test
    fun `getTests sets isFree true for Free exam type`() {
        val freeExam = LocalExam("1", "Free", "Free Exam", "", emptyList())
        `when`(localExamDataSource.loadExams("en")).thenReturn(listOf(freeExam))

        val resource = repository.getTests(false).value!!

        assertTrue("Expected isFree=true for exam type 'Free'", resource.data!![0].isFree)
    }

    @Test
    fun `getTests parses quizId to int for entity id`() {
        val exam = LocalExam("200", "RANDOM", "Test", "", emptyList())
        `when`(localExamDataSource.loadExams("en")).thenReturn(listOf(exam))

        val resource = repository.getTests(false).value!!

        assertEquals(200, resource.data!![0].id)
    }

    @Test
    fun `getTests handles null imageUrl gracefully`() {
        val exam = LocalExam("1", "RANDOM", "Test", "", emptyList())
        `when`(localExamDataSource.loadExams("en")).thenReturn(listOf(exam))

        val resource = repository.getTests(false).value!!

        assertNull("Expected null imageUrl for empty input", resource.data!![0].imageUrl)
    }

    @Test
    fun `getTests strips assets prefix and prepends file URI`() {
        val exam = LocalExam("1", "RANDOM", "Test", "assets/img.png", emptyList())
        `when`(localExamDataSource.loadExams("en")).thenReturn(listOf(exam))

        val resource = repository.getTests(false).value!!

        assertEquals("file:///android_asset/img.png", resource.data!![0].imageUrl)
    }

    @Test
    fun `getTests without assets prefix still prepends file URI`() {
        val exam = LocalExam("1", "RANDOM", "Test", "json_questions_images/img.png", emptyList())
        `when`(localExamDataSource.loadExams("en")).thenReturn(listOf(exam))

        val resource = repository.getTests(false).value!!

        assertEquals("file:///android_asset/json_questions_images/img.png", resource.data!![0].imageUrl)
    }

    // ---------------------------------------------------------------------------
    // getTests — Language & Preferences
    // ---------------------------------------------------------------------------

    @Test
    fun `getTests reads language from AppPreferences`() {
        `when`(sharedPreferences.getString("language_code", "rw")).thenReturn("fr")
        val exam = LocalExam("1", "RANDOM", "Examen", "", emptyList())
        `when`(localExamDataSource.loadExams("fr")).thenReturn(listOf(exam))

        val resource = repository.getTests(false).value!!

        assertEquals(Resource.Status.SUCCESS, resource.status)
        assertEquals("Examen", resource.data!![0].title)
    }

    // ---------------------------------------------------------------------------
    // getTests — Empty & Error Paths
    // ---------------------------------------------------------------------------

    @Test
    fun `getTests handles empty exam list`() {
        `when`(localExamDataSource.loadExams("en")).thenReturn(emptyList())

        val resource = repository.getTests(false).value!!

        assertEquals(Resource.Status.SUCCESS, resource.status)
        assertTrue("Expected empty entity list", resource.data!!.isEmpty())
    }

    @Test
    fun `getTests returns error when data source throws exception`() {
        `when`(localExamDataSource.loadExams("en")).thenThrow(RuntimeException("DB error"))

        val resource = repository.getTests(false).value!!

        assertEquals(Resource.Status.ERROR, resource.status)
        assertNotNull("Expected error message", resource.message)
        // Verify ErrorUtils replaced the raw exception text with a user-friendly message
        assertTrue("Error should not contain raw exception text",
            !resource.message!!.contains("DB error") && !resource.message!!.contains("RuntimeException"))
    }

    @Test
    fun `getTests returns error when data source returns null`() {
        // loadExams returns non-null in Kotlin, but the Java catch block handles any exception
        `when`(localExamDataSource.loadExams("en")).thenThrow(NullPointerException("Unexpected null"))

        val resource = repository.getTests(false).value!!

        assertEquals(Resource.Status.ERROR, resource.status)
    }

    // ---------------------------------------------------------------------------
    // getTestWithQuestions — Success Paths
    // ---------------------------------------------------------------------------

    @Test
    fun `getTestWithQuestions finds exam by quizId and returns entity with questions`() {
        val exam = sampleExam()
        `when`(localExamDataSource.loadExams("en")).thenReturn(listOf(exam))
        `when`(localExamDataSource.loadExamByQuizId("177", "en")).thenReturn(exam)

        val resource = repository.getTestWithQuestions(177).value!!

        assertEquals(Resource.Status.SUCCESS, resource.status)
        val twq = resource.data!!
        assertNotNull("Expected TestEntity", twq.test)
        assertNotNull("Expected questions list", twq.questions)

        // Verify exam entity fields
        assertEquals("Practice Test A", twq.test!!.title)
        assertEquals("RANDOM", twq.test!!.description)
        assertEquals(2, twq.test!!.totalMarks)

        // Verify questions were mapped
        assertEquals(2, twq.questions!!.size)
    }

    @Test
    fun `getTestWithQuestions maps question fields correctly`() {
        val questions = listOf(
            localQuestion("What is 2+2?", correctOptionIdx = 1, imageUrl = "assets/img/q1.png")
        )
        val exam = LocalExam("1", "RANDOM", "Math Test", "", questions)
        `when`(localExamDataSource.loadExams("en")).thenReturn(listOf(exam))
        `when`(localExamDataSource.loadExamByQuizId("1", "en")).thenReturn(exam)

        val resource = repository.getTestWithQuestions(1).value!!
        val twq = resource.data!!
        val qwo = twq.questions!![0]

        assertEquals("What is 2+2?", qwo.question!!.questionText)
        assertEquals("multiple_choice", qwo.question!!.questionType)
        assertEquals(1, qwo.question!!.testId)
        assertEquals("file:///android_asset/img/q1.png", qwo.question!!.imageUrl)
    }

    @Test
    fun `getTestWithQuestions maps options and identifies correct answer`() {
        val questions = listOf(
            localQuestion(
                "Which is correct?",
                options = listOf("Wrong1", "Right Answer", "Wrong2", "Wrong3"),
                correctOptionIdx = 1
            )
        )
        val exam = LocalExam("1", "RANDOM", "Quiz", "", questions)
        `when`(localExamDataSource.loadExams("en")).thenReturn(listOf(exam))
        `when`(localExamDataSource.loadExamByQuizId("1", "en")).thenReturn(exam)

        val resource = repository.getTestWithQuestions(1).value!!
        val options = resource.data!!.questions!![0].options!!

        assertEquals(4, options.size)
        assertEquals("Wrong1", options[0].text)
        assertEquals(false, options[0].isCorrect)
        assertEquals("Right Answer", options[1].text)
        assertEquals(true, options[1].isCorrect)
        assertEquals("Wrong2", options[2].text)
        assertEquals(false, options[2].isCorrect)
        assertEquals("Wrong3", options[3].text)
        assertEquals(false, options[3].isCorrect)
    }

    @Test
    fun `getTestWithQuestions falls back to index when quizId not found`() {
        val exam = sampleExam()
        // testId=1 maps to index 0 for a 1-item list (testId must be <= list.size)
        `when`(localExamDataSource.loadExams("en")).thenReturn(listOf(exam))
        `when`(localExamDataSource.loadExamByQuizId("1", "en")).thenReturn(null)
        `when`(localExamDataSource.loadExamByIndex(0, "en")).thenReturn(exam)

        val resource = repository.getTestWithQuestions(1).value!!

        assertEquals(Resource.Status.SUCCESS, resource.status)
        assertEquals("Practice Test A", resource.data!!.test!!.title)
    }

    @Test
    fun `getTestWithQuestions uses loadExamByIndex with zero-based index`() {
        val exam1 = LocalExam("1", "CLASS", "First", "", emptyList())
        val exam2 = LocalExam("2", "RANDOM", "Second", "", emptyList())
        `when`(localExamDataSource.loadExams("en")).thenReturn(listOf(exam1, exam2))
        `when`(localExamDataSource.loadExamByQuizId("2", "en")).thenReturn(null)
        `when`(localExamDataSource.loadExamByIndex(1, "en")).thenReturn(exam2)

        val resource = repository.getTestWithQuestions(2).value!!

        assertEquals(Resource.Status.SUCCESS, resource.status)
        assertEquals("Second", resource.data!!.test!!.title)
    }

    // ---------------------------------------------------------------------------
    // getTestWithQuestions — Error Paths
    // ---------------------------------------------------------------------------

    @Test
    fun `getTestWithQuestions returns error for non existent testId`() {
        `when`(localExamDataSource.loadExams("en")).thenReturn(emptyList())
        `when`(localExamDataSource.loadExamByQuizId("999", "en")).thenReturn(null)

        val resource = repository.getTestWithQuestions(999).value!!

        assertEquals(Resource.Status.ERROR, resource.status)
        assertNotNull("Expected error message", resource.message)
        assertTrue("Error should mention exam not found", resource.message!!.contains("not found"))
    }

    @Test
    fun `getTestWithQuestions returns error when data source throws`() {
        `when`(localExamDataSource.loadExams("en")).thenThrow(RuntimeException("Load failed"))

        val resource = repository.getTestWithQuestions(1).value!!

        assertEquals(Resource.Status.ERROR, resource.status)
        assertNotNull("Expected error message", resource.message)
        // Verify ErrorUtils replaced the raw exception text with a user-friendly message
        assertTrue("Error should not contain raw exception text",
            !resource.message!!.contains("Load failed") && !resource.message!!.contains("RuntimeException"))
    }
}
