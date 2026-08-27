package com.drivingschoolrwandaapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.drivingschoolrwandaapp.database.entities.QuestionOptionEntity
import com.drivingschoolrwandaapp.database.entities.QuestionWithOptions
import com.drivingschoolrwandaapp.database.entities.TestEntity
import com.drivingschoolrwandaapp.database.entities.TestQuestionEntity
import com.drivingschoolrwandaapp.database.entities.TestWithQuestions
import com.drivingschoolrwandaapp.repository.Resource
import com.drivingschoolrwandaapp.repository.TestRepository
import com.drivingschoolrwandaapp.repository.TestResultRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anyBoolean
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.verify

/**
 * Unit tests for [TestViewModel].
 *
 * IMPORTANT: [Transformations.switchMap] lazily activates source observers only
 * when the result [LiveData] itself has active observers. Therefore any test
 * that calls [TestViewModel.loadQuestionsForTest] must first activate the
 * observer chain by calling [LiveData.observeForever] on the result LiveData.
 * Same applies to [TestViewModel.refreshTests] via [TestViewModel.getTests].
 */
class TestViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var testRepository: TestRepository
    private lateinit var testResultRepository: TestResultRepository
    private lateinit var application: Application
    private lateinit var viewModel: TestViewModel
    private var logMock: MockedStatic<Log>? = null

    @Before
    fun setUp() {
        try { logMock?.close() } catch (_: Exception) { }
        try { logMock = mockStatic(Log::class.java) } catch (_: Exception) { }

        testRepository = mock(TestRepository::class.java)
        testResultRepository = mock(TestResultRepository::class.java)
        application = mock(Application::class.java)

        // Stub getTests to return a LiveData (required by constructor's switchMap)
        `when`(testRepository.getTests(anyBoolean())).thenReturn(MutableLiveData())
        // Stub persisted history (required by the constructor's observeForever)
        `when`(testResultRepository.getHistory()).thenReturn(MutableLiveData())

        viewModel = TestViewModel(application, testRepository, testResultRepository)
    }

    @After
    fun tearDown() {
        try { logMock?.close() } catch (_: Exception) { }
    }

    // ---------------------------------------------------------------------------
    // Helper: activate the switchMap observer for questionsForTest
    // ---------------------------------------------------------------------------

    /**
     * Activate the [Transformations.switchMap] observer chain on
     * [TestViewModel.getQuestionsForTest] so that [TestViewModel.loadQuestionsForTest]
     * triggers [TestRepository.getTestWithQuestions].
     */
    private fun activateQuestionsObserver() {
        viewModel.getQuestionsForTest().observeForever { }
    }

    /**
     * Activate the [Transformations.switchMap] observer chain on
     * [TestViewModel.getTests] so that [TestViewModel.refreshTests]
     * triggers [TestRepository.getTests].
     */
    private fun activateTestsObserver() {
        viewModel.getTests().observeForever { }
    }

    // ---------------------------------------------------------------------------
    // Helper: create a TestWithQuestions with 1 question and 4 options
    // ---------------------------------------------------------------------------

    /**
     * Build a [TestWithQuestions] with the specified scoring parameters.
     *
     * @param totalMarks  Total marks for the test
     * @param passMarks   Minimum marks needed to pass
     * @param correctOptionIndicesPerQuestion For each question, the index (0-3) of the correct option
     */
    private fun createMultiQuestionTest(
        totalMarks: Int = 10,
        passMarks: Int = 5,
        correctOptionIndicesPerQuestion: List<Int> = emptyList()
    ): TestWithQuestions {
        val questionCount = correctOptionIndicesPerQuestion.size
        val testEntity = TestEntity(
            id = 1, title = "Practice Test",
            totalMarks = totalMarks, passMarks = passMarks,
            questionCount = questionCount
        )
        val questionWithOptionsList = correctOptionIndicesPerQuestion.mapIndexed { qIdx, correctIdx ->
            val options = (0..3).map { oIdx ->
                QuestionOptionEntity(
                    id = qIdx * 4 + oIdx + 1,
                    questionId = qIdx + 1,
                    text = "Option ${oIdx + 1}",
                    isCorrect = oIdx == correctIdx
                )
            }
            QuestionWithOptions(
                question = TestQuestionEntity(
                    id = qIdx + 1, testId = 1,
                    questionText = "Question ${qIdx + 1}?",
                    questionType = "multiple_choice"
                ),
                options = options
            )
        }
        val twq = TestWithQuestions()
        twq.test = testEntity
        twq.questions = questionWithOptionsList
        return twq
    }

    private fun createOneQuestionTest(
        totalMarks: Int = 10,
        passMarks: Int = 5,
        correctOptionIndex: Int = 0
    ): TestWithQuestions {
        return createMultiQuestionTest(totalMarks, passMarks, listOf(correctOptionIndex))
    }

    // ---------------------------------------------------------------------------
    // calculateResult
    // ---------------------------------------------------------------------------

    /**
     * Helper: load questions into the ViewModel and return the backing LiveData
     * so the test can control when the data arrives.
     */
    private fun loadQuestionsIntoView(testId: Int = 1): MutableLiveData<Resource<TestWithQuestions>> {
        activateQuestionsObserver()
        val liveData = MutableLiveData<Resource<TestWithQuestions>>()
        `when`(testRepository.getTestWithQuestions(testId)).thenReturn(liveData)
        viewModel.loadQuestionsForTest(testId)
        return liveData
    }

    @Test
    fun `calculateResult with correct answer returns full score and passed`() {
        val twq = createMultiQuestionTest(
            totalMarks = 20, passMarks = 10,
            correctOptionIndicesPerQuestion = listOf(0, 1, 2, 3)
        )
        val liveData = loadQuestionsIntoView(1)
        liveData.setValue(Resource.success(twq))

        viewModel.setAnswer(1, 1)   // q1, option 0 (id=1) ✓
        viewModel.setAnswer(2, 6)   // q2, option 1 (id=6) ✓
        viewModel.setAnswer(3, 11)  // q3, option 2 (id=11) ✓
        viewModel.setAnswer(4, 16)  // q4, option 3 (id=16) ✓
        viewModel.calculateResult()

        val result = viewModel.getTestResult().value!!
        assertEquals(20, result.score)
        assertEquals(20, result.totalMarks)
        assertTrue("Expected passed for full score", result.passed)
        assertEquals(4, result.correctCount)
        assertEquals(0, result.wrongCount)
        assertEquals(0, result.skippedCount)
    }

    @Test
    fun `calculateResult with all wrong answers returns zero and not passed`() {
        val twq = createMultiQuestionTest(
            totalMarks = 10, passMarks = 5,
            correctOptionIndicesPerQuestion = listOf(0, 0)
        )
        val liveData = loadQuestionsIntoView(1)
        liveData.setValue(Resource.success(twq))

        viewModel.setAnswer(1, 2)   // q1 wrong ✗
        viewModel.setAnswer(2, 7)   // q2 wrong ✗
        viewModel.calculateResult()

        val result = viewModel.getTestResult().value!!
        assertEquals(0, result.score)
        assertFalse("Expected not passed for zero score", result.passed)
        assertEquals(0, result.correctCount)
        assertEquals(2, result.wrongCount)
        assertEquals(0, result.skippedCount)
    }

    @Test
    fun `calculateResult with partial correct answers scores proportionally`() {
        val twq = createMultiQuestionTest(
            totalMarks = 20, passMarks = 8,
            correctOptionIndicesPerQuestion = listOf(0, 1, 2, 3)
        )
        val liveData = loadQuestionsIntoView(1)
        liveData.setValue(Resource.success(twq))

        // 3 correct out of 4 → score = round(3 * 20/4) = round(15) = 15
        viewModel.setAnswer(1, 1)   // correct ✓
        viewModel.setAnswer(2, 5)   // wrong ✗
        viewModel.setAnswer(3, 11)  // correct ✓
        viewModel.setAnswer(4, 16)  // correct ✓
        viewModel.calculateResult()

        val result = viewModel.getTestResult().value!!
        assertEquals(15, result.score)
        assertTrue(result.passed)
    }

    @Test
    fun `calculateResult at exact pass threshold is passed`() {
        val twq = createMultiQuestionTest(
            totalMarks = 20, passMarks = 10,
            correctOptionIndicesPerQuestion = listOf(0, 1, 2, 3)
        )
        val liveData = loadQuestionsIntoView(1)
        liveData.setValue(Resource.success(twq))

        // 2 correct out of 4 → score = round(2 * 20/4) = 10 = passMarks → passed
        viewModel.setAnswer(1, 1)   // correct ✓
        viewModel.setAnswer(2, 6)   // correct ✓
        viewModel.setAnswer(3, 9)   // wrong ✗
        viewModel.setAnswer(4, 13)  // wrong ✗
        viewModel.calculateResult()

        val result = viewModel.getTestResult().value!!
        assertEquals(10, result.score)
        assertTrue("Passed when score equals passMarks", result.passed)
    }

    @Test
    fun `calculateResult with uneven marks per question rounds correctly`() {
        // 3 questions, totalMarks=10 → marksPerQuestion = 3.333...
        // 1 correct: round(1 * 3.333) = 3, passMarks = 5 → not passed
        val twq = createMultiQuestionTest(
            totalMarks = 10, passMarks = 5,
            correctOptionIndicesPerQuestion = listOf(0, 1, 2)
        )
        val liveData = loadQuestionsIntoView(1)
        liveData.setValue(Resource.success(twq))

        viewModel.setAnswer(1, 1)   // q1 correct ✓
        viewModel.setAnswer(2, 5)   // q2 wrong ✗
        viewModel.setAnswer(3, 9)   // q3 wrong ✗
        viewModel.calculateResult()

        val result = viewModel.getTestResult().value!!
        assertEquals(3, result.score)
        assertFalse(result.passed)
    }

    @Test
    fun `calculateResult with two thirds correct rounds up`() {
        // 3 questions, totalMarks=10 → marksPerQuestion = 3.333...
        // 2 correct: round(2 * 3.333) = round(6.666) = 7, passMarks = 5 → passed
        val twq = createMultiQuestionTest(
            totalMarks = 10, passMarks = 5,
            correctOptionIndicesPerQuestion = listOf(0, 1, 2)
        )
        val liveData = loadQuestionsIntoView(1)
        liveData.setValue(Resource.success(twq))

        viewModel.setAnswer(1, 1)   // q1 correct ✓
        viewModel.setAnswer(2, 6)   // q2 correct ✓
        viewModel.setAnswer(3, 9)   // q3 wrong ✗
        viewModel.calculateResult()

        val result = viewModel.getTestResult().value!!
        assertEquals(7, result.score)
        assertTrue(result.passed)
    }

    @Test
    fun `calculateResult with no user answers scores zero and not passed`() {
        val liveData = loadQuestionsIntoView(1)
        liveData.setValue(Resource.success(createOneQuestionTest(correctOptionIndex = 0)))

        // No setAnswer calls — userAnswers map exists but is empty
        viewModel.calculateResult()

        val result = viewModel.getTestResult().value
        assertNotNull("Expected a result even with no answers selected", result)
        assertEquals(0, result!!.score)
        assertFalse("Expected not passed with zero correct", result.passed)
        assertEquals(0, result.correctCount)
        assertEquals(0, result.wrongCount)
        assertEquals("Expected the unanswered question to count as skipped", 1, result.skippedCount)
    }

    @Test
    fun `calculateResult records correct wrong and skipped breakdown`() {
        val twq = createMultiQuestionTest(
            totalMarks = 20, passMarks = 10,
            correctOptionIndicesPerQuestion = listOf(0, 1, 2, 3)
        )
        val liveData = loadQuestionsIntoView(1)
        liveData.setValue(Resource.success(twq))

        viewModel.setAnswer(1, 1)   // q1 correct ✓
        viewModel.setAnswer(2, 5)   // q2 wrong ✗
        viewModel.setAnswer(3, 11)  // q3 correct ✓
        // q4 left unanswered
        viewModel.calculateResult()

        val result = viewModel.getTestResult().value!!
        assertEquals(2, result.correctCount)
        assertEquals(1, result.wrongCount)
        assertEquals(1, result.skippedCount)
        // correct + wrong + skipped must add up to the question count
        assertEquals(4, result.correctCount + result.wrongCount + result.skippedCount)
    }

    @Test
    fun `calculateResult records completion date and test duration`() {
        val twq = createMultiQuestionTest(
            totalMarks = 20, passMarks = 10,
            correctOptionIndicesPerQuestion = listOf(0, 1, 2, 3)
        )
        twq.test?.duration = 15
        val before = System.currentTimeMillis()
        val liveData = loadQuestionsIntoView(1)
        liveData.setValue(Resource.success(twq))
        // Clock starts with the countdown timer, mirroring TestQuestionsFragment
        viewModel.markTestStarted()

        viewModel.setAnswer(1, 1)   // q1 correct ✓
        viewModel.calculateResult()
        val after = System.currentTimeMillis()

        val result = viewModel.getTestResult().value!!
        assertTrue("Expected a completion date to be recorded", result.date in before..after)
        assertEquals(15, result.duration)
        // Elapsed time runs from markTestStarted() to calculateResult(), so it must be
        // non-negative and no larger than the wall-clock span of the test run.
        assertTrue("Expected non-negative elapsed time", result.elapsedSeconds >= 0)
        assertTrue(
            "Expected elapsed time within test run bounds, was ${result.elapsedSeconds}",
            result.elapsedSeconds * 1000L <= (after - before)
        )
    }

    @Test
    fun `calculateResult reports zero elapsed time when test never started`() {
        val twq = createOneQuestionTest(correctOptionIndex = 0)
        val liveData = loadQuestionsIntoView(1)
        liveData.setValue(Resource.success(twq))
        // No markTestStarted() call — clock stays disarmed

        viewModel.setAnswer(1, 1)
        viewModel.calculateResult()

        val result = viewModel.getTestResult().value!!
        assertEquals("Expected zero elapsed time when test never started", 0, result.elapsedSeconds)
    }

    @Test
    fun `loadQuestionsForTest clears a previously started clock`() {
        val twq = createOneQuestionTest(correctOptionIndex = 0)
        val liveData = loadQuestionsIntoView(1)
        liveData.setValue(Resource.success(twq))

        viewModel.markTestStarted()          // a previous test's clock is running...
        viewModel.loadQuestionsForTest(2)    // ...but loading a new test must re-arm it

        viewModel.setAnswer(1, 1)
        viewModel.calculateResult()

        val result = viewModel.getTestResult().value!!
        assertEquals("Expected stale clock cleared by loadQuestionsForTest", 0, result.elapsedSeconds)
    }

    @Test
    fun `calculateResult with no questions loaded returns null`() {
        viewModel.calculateResult()
        assertNull("Expected null when no questions loaded", viewModel.getTestResult().value)
    }

    @Test
    fun `calculateResult with empty questions list returns null`() {
        val emptyTwq = TestWithQuestions()
        emptyTwq.test = TestEntity(id = 1, totalMarks = 0, passMarks = 0, questionCount = 0)
        emptyTwq.questions = emptyList()

        val liveData = loadQuestionsIntoView(1)
        liveData.setValue(Resource.success(emptyTwq))

        viewModel.calculateResult()

        assertNull("Expected null when questions list empty", viewModel.getTestResult().value)
    }

    @Test
    fun `calculateResult with question missing options counts as skipped`() {
        val twq = createOneQuestionTest(correctOptionIndex = 0)
        // Simulate corrupt data: question exists but options list is null
        twq.questions!![0].options = null
        val liveData = loadQuestionsIntoView(1)
        liveData.setValue(Resource.success(twq))

        viewModel.setAnswer(1, 1)
        viewModel.calculateResult()

        val result = viewModel.getTestResult().value!!
        assertEquals("Question without options cannot be evaluated", 0, result.correctCount)
        assertEquals(0, result.wrongCount)
        assertEquals("Question without options counts as skipped", 1, result.skippedCount)
        assertEquals(0, result.score)
        assertFalse(result.passed)
    }

    @Test
    fun `calculateResult with null question entity counts as skipped`() {
        val twq = createOneQuestionTest(correctOptionIndex = 0)
        twq.questions!![0].question = null
        val liveData = loadQuestionsIntoView(1)
        liveData.setValue(Resource.success(twq))

        viewModel.setAnswer(1, 1)
        viewModel.calculateResult()

        val result = viewModel.getTestResult().value!!
        assertEquals("Null question entity must not crash scoring", 0, result.correctCount)
        assertEquals(1, result.skippedCount)
    }

    @Test
    fun `calculateResult with answer for non existent question counts as skipped`() {
        val twq = createOneQuestionTest(correctOptionIndex = 0)
        val liveData = loadQuestionsIntoView(1)
        liveData.setValue(Resource.success(twq))

        // Answer a question id that is not part of this test
        viewModel.setAnswer(99, 1)
        viewModel.calculateResult()

        val result = viewModel.getTestResult().value!!
        assertEquals(0, result.correctCount)
        assertEquals("Orphan answer must be ignored, question stays skipped", 1, result.skippedCount)
    }

    @Test
    fun `loadQuestionsForTest clears a previously computed result`() {
        val liveData = loadQuestionsIntoView(1)
        liveData.setValue(Resource.success(createOneQuestionTest(correctOptionIndex = 0)))
        viewModel.setAnswer(1, 1)
        viewModel.calculateResult()
        assertNotNull("Expected result before loading a new test", viewModel.getTestResult().value)

        viewModel.loadQuestionsForTest(2)

        assertNull("Loading a new test must clear the previous result", viewModel.getTestResult().value)
    }

    @Test
    fun `calculateResult appends each submission to result history`() {
        val liveData = loadQuestionsIntoView(1)
        liveData.setValue(Resource.success(createOneQuestionTest(correctOptionIndex = 0)))

        viewModel.setAnswer(1, 1)
        viewModel.calculateResult()
        viewModel.calculateResult()

        val history = viewModel.getTestResultHistory().value!!
        assertEquals("Each calculateResult call adds a history entry", 2, history.size)
    }

    @Test
    fun `calculateResult persists each submission to the repository`() {
        val liveData = loadQuestionsIntoView(1)
        liveData.setValue(Resource.success(createOneQuestionTest(correctOptionIndex = 0)))

        viewModel.setAnswer(1, 1)
        viewModel.calculateResult()

        verify(testResultRepository).saveResult(org.mockito.ArgumentMatchers.any())
    }

    @Test
    fun `storeTestData null falls back to switchMap value`() {
        val liveData = loadQuestionsIntoView(1)
        liveData.setValue(Resource.success(createOneQuestionTest(correctOptionIndex = 0)))

        viewModel.storeTestData(null)
        viewModel.setAnswer(1, 1)
        viewModel.calculateResult()

        // storeTestData(null) must not poison scoring — switchMap value is used instead
        val result = viewModel.getTestResult().value
        assertNotNull("Expected fallback to switchMap value", result)
        assertEquals(1, result!!.correctCount)
    }

    // ---------------------------------------------------------------------------
    // Exam Question Loading
    // ---------------------------------------------------------------------------

    @Test
    fun `loadQuestionsForTest triggers getTestWithQuestions with correct id`() {
        activateQuestionsObserver()
        val liveData = MutableLiveData<Resource<TestWithQuestions>>()
        `when`(testRepository.getTestWithQuestions(42)).thenReturn(liveData)

        viewModel.loadQuestionsForTest(42)

        verify(testRepository).getTestWithQuestions(42)
    }

    @Test
    fun `loadQuestionsForTest stores testId`() {
        viewModel.loadQuestionsForTest(99)
        assertEquals(Integer.valueOf(99), viewModel.getTestId())
    }

    // ---------------------------------------------------------------------------
    // getTests & refreshTests
    // ---------------------------------------------------------------------------

    @Test
    fun `getTests returns non-null LiveData`() {
        assertNotNull("getTests should return non-null LiveData", viewModel.getTests())
    }

    @Test
    fun `refreshTests triggers getTests with true`() {
        activateTestsObserver()
        viewModel.refreshTests()
        verify(testRepository).getTests(true)
    }

    // ---------------------------------------------------------------------------
    // Answer Management
    // ---------------------------------------------------------------------------

    @Test
    fun `setAnswer stores answer`() {
        viewModel.setAnswer(5, 3)
        val answers = viewModel.getSelectedAnswers().value!!
        assertEquals(3, answers[5])
    }

    @Test
    fun `setAnswer overwrites previous answer`() {
        viewModel.setAnswer(1, 2)
        viewModel.setAnswer(1, 4)
        val answers = viewModel.getSelectedAnswers().value!!
        assertEquals(4, answers[1])
    }

    @Test
    fun `setAnswer stores multiple distinct answers`() {
        viewModel.setAnswer(1, 2)
        viewModel.setAnswer(2, 6)
        viewModel.setAnswer(3, 10)
        val answers = viewModel.getSelectedAnswers().value!!
        assertEquals(3, answers.size.toLong())
    }

    // ---------------------------------------------------------------------------
    // Feedback Hidden
    // ---------------------------------------------------------------------------

    @Test
    fun `question feedback is not hidden by default`() {
        assertFalse(viewModel.isQuestionFeedbackHidden(1))
    }

    @Test
    fun `markQuestionFeedbackHidden hides feedback for specific question`() {
        viewModel.markQuestionFeedbackHidden(3)
        assertTrue(viewModel.isQuestionFeedbackHidden(3))
        assertFalse(viewModel.isQuestionFeedbackHidden(1))
    }

    @Test
    fun `markQuestionFeedbackHidden is idempotent`() {
        viewModel.markQuestionFeedbackHidden(5)
        viewModel.markQuestionFeedbackHidden(5)
        assertTrue(viewModel.isQuestionFeedbackHidden(5))
    }
}
