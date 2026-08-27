package com.drivingschoolrwandaapp.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.drivingschoolrwandaapp.database.entities.TestEntity;
import com.drivingschoolrwandaapp.database.entities.TestWithQuestions;
import com.drivingschoolrwandaapp.models.entities.TestResult;
import com.drivingschoolrwandaapp.repository.Resource;
import com.drivingschoolrwandaapp.repository.TestRepository;
import com.drivingschoolrwandaapp.repository.TestResultRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class TestViewModel extends AndroidViewModel {

    private final TestRepository testRepository;
    private final TestResultRepository testResultRepository;
    private final LiveData<Resource<List<TestEntity>>> tests;
    private final MutableLiveData<Boolean> refreshTrigger = new MutableLiveData<>();
    private final MutableLiveData<Integer> testId = new MutableLiveData<>();
    private final LiveData<Resource<TestWithQuestions>> questionsForTest;
    private final MutableLiveData<TestResult> testResult = new MutableLiveData<>();
    
    // Store loaded test data directly for reliable calculateResult()
    private TestWithQuestions loadedTestData = null;
    private int currentTestId = -1;
    private String currentTestName = "";
    private int currentTestNumber = 0;

    private final MutableLiveData<Map<Integer, Integer>> selectedAnswers = new MutableLiveData<>(new HashMap<>());
    // Snapshot of answers saved at submit time for review mode
    private Map<Integer, Integer> savedAnswersForReview = new HashMap<>();
    private final Set<Integer> feedbackHiddenQuestions = new HashSet<>();
    
    // History of test results for Previous Tests page
    private final MutableLiveData<List<TestResult>> testResultHistory = new MutableLiveData<>(new ArrayList<>());

    // Persisted history observer: seeds the in-memory list once from Room so
    // Previous Tests survives restarts and works fully offline.
    private final LiveData<List<TestResult>> persistedHistory;
    private final androidx.lifecycle.Observer<List<TestResult>> persistedHistoryObserver;
    private boolean historySeededFromDb = false;

    // Timestamp captured when a test begins, used to report actual time taken
    private long testStartTime = 0L;

    @Inject
    public TestViewModel(@NonNull Application application, TestRepository testRepository,
                         TestResultRepository testResultRepository) {
        super(application);
        this.testRepository = testRepository;
        this.testResultRepository = testResultRepository;
        
        // Initialize refreshTrigger
        refreshTrigger.setValue(false);

        this.tests = Transformations.switchMap(refreshTrigger, force -> testRepository.getTests(force));
        
        this.questionsForTest = Transformations.switchMap(testId, testRepository::getTestWithQuestions);

        // Seed the Previous Tests list from persisted results. The observer
        // deliberately ignores later emissions (which originate from our own
        // inserts — those entries are already in the in-memory list), so it can
        // never overwrite or duplicate live-session history.
        this.persistedHistory = testResultRepository.getHistory();
        this.persistedHistoryObserver = persisted -> {
            if (!historySeededFromDb) {
                historySeededFromDb = true;
                List<TestResult> current = testResultHistory.getValue();
                if (current == null || current.isEmpty()) {
                    testResultHistory.setValue(persisted != null ? new ArrayList<>(persisted) : new ArrayList<>());
                }
            }
        };
        this.persistedHistory.observeForever(persistedHistoryObserver);
    }

    @Override
    protected void onCleared() {
        // Only detach the observer. The repository is a process-wide singleton
        // (its executor must stay alive for future ViewModels), so it is never
        // shut down here.
        if (persistedHistory != null && persistedHistoryObserver != null) {
            persistedHistory.removeObserver(persistedHistoryObserver);
        }
        super.onCleared();
    }

    public LiveData<Resource<List<TestEntity>>> getTests() {
        return tests;
    }

    public void refreshTests() {
       refreshTrigger.setValue(true);
    }

    public LiveData<Resource<TestWithQuestions>> getQuestionsForTest() {
        return questionsForTest;
    }

    public LiveData<TestResult> getTestResult() {
        return testResult;
    }
    
    public LiveData<List<TestResult>> getTestResultHistory() {
        return testResultHistory;
    }

    public void loadQuestionsForTest(int testId) {
        this.currentTestId = testId;
        this.testId.setValue(testId);
        // Reset answers for new test
        selectedAnswers.setValue(new HashMap<>());
        feedbackHiddenQuestions.clear();
        testResult.setValue(null);
        // Clock is armed (not started) until markTestStarted() fires with the countdown timer
        this.testStartTime = 0L;
    }

    /**
     * Marks the moment the countdown timer actually starts for a test,
     * so elapsed time excludes question-loading time.
     */
    public void markTestStarted() {
        this.testStartTime = System.currentTimeMillis();
    }
    
    /**
     * Store loaded test data for reliable calculateResult().
     * Called from the fragment observer when questions load.
     */
    public void storeTestData(TestWithQuestions data) {
        this.loadedTestData = data;
        if (data != null && data.test != null) {
            currentTestName = data.test.getTitle() != null ? data.test.getTitle() : "";
            currentTestNumber = data.test.getTestNumber();
        }
    }

    public LiveData<Resource<TestWithQuestions>> downloadQuestions(int testId) {
        return testRepository.getTestWithQuestions(testId);
    }

    public Integer getTestId() {
        return testId.getValue();
    }
    
    public int getCurrentTestNumber() {
        return currentTestNumber;
    }
    
    public String getCurrentTestName() {
        return currentTestName;
    }

    public void setAnswer(int questionId, int optionId) {
        Map<Integer, Integer> answers = selectedAnswers.getValue();
        if (answers != null) {
            answers.put(questionId, optionId);
            selectedAnswers.setValue(answers);
        }
    }

    public LiveData<Map<Integer, Integer>> getSelectedAnswers() {
        return selectedAnswers;
    }

    public void markQuestionFeedbackHidden(int questionId) {
        feedbackHiddenQuestions.add(questionId);
    }

    public boolean isQuestionFeedbackHidden(int questionId) {
        return feedbackHiddenQuestions.contains(questionId);
    }

    /**
     * Restore the saved answers from the last test submission.
     * Used by review mode to show which answers the user selected.
     */
    public void restoreReviewAnswers() {
        Map<Integer, Integer> current = selectedAnswers.getValue();
        if (current != null) {
            current.clear();
            current.putAll(savedAnswersForReview);
            selectedAnswers.setValue(current);
        }
    }

    public void calculateResult() {
        // First try stored test data (set via storeTestData when observed from fragments)
        TestWithQuestions data = loadedTestData;

        // Fall back to switchMap value if storeTestData was not called (e.g., in tests)
        if (data == null || data.questions == null || data.questions.isEmpty()) {
            Resource<TestWithQuestions> resource = questionsForTest.getValue();
            if (resource != null && resource.data != null
                    && resource.data.questions != null && !resource.data.questions.isEmpty()) {
                data = resource.data;
            }
        }

        if (data != null && data.questions != null && !data.questions.isEmpty()) {
            // Save a snapshot of the user's answers for review mode before they get reset
            Map<Integer, Integer> currentAnswers = selectedAnswers.getValue();
            if (currentAnswers != null) {
                savedAnswersForReview = new HashMap<>(currentAnswers);
            }

            int correctAnswers = 0;
            int wrongAnswers = 0;
            int totalMarks = data.test != null ? data.test.getTotalMarks() : data.questions.size();
            int passMarks = data.test != null ? data.test.getPassMarks() : (int) Math.ceil(data.questions.size() * 0.5);
            int numberOfQuestions = data.questions.size();

            Map<Integer, Integer> userAnswers = currentAnswers;
            if (userAnswers == null) return;

            for (com.drivingschoolrwandaapp.database.entities.QuestionWithOptions questionWithOptions : data.questions) {
                Integer userAnswerId = userAnswers.get(questionWithOptions.question != null ? questionWithOptions.question.getId() : 0);
                if (userAnswerId != null && questionWithOptions.options != null) {
                    for (com.drivingschoolrwandaapp.database.entities.QuestionOptionEntity option : questionWithOptions.options) {
                        if (option.getId() == userAnswerId) {
                            if (option.isCorrect()) {
                                correctAnswers++;
                            } else {
                                wrongAnswers++;
                            }
                            break;
                        }
                    }
                }
            }
            int skippedAnswers = Math.max(0, numberOfQuestions - correctAnswers - wrongAnswers);

            double marksPerQuestion = numberOfQuestions > 0 ? (double) totalMarks / numberOfQuestions : 0;
            int finalScore = (int) Math.round(correctAnswers * marksPerQuestion);

            boolean isPassed = finalScore >= passMarks;

            int testNumber = data.test != null ? data.test.getTestNumber() : 0;
            int resultTestId = data.test != null ? data.test.getId() : currentTestId;
            String testName = data.test != null && data.test.getTitle() != null
                ? data.test.getTitle() : "Exam " + testNumber;

            long completedAt = System.currentTimeMillis();
            int durationLimitMinutes = data.test != null ? data.test.getDuration() : 0;
            int elapsedSeconds = testStartTime > 0 ? (int) ((completedAt - testStartTime) / 1000) : 0;

            TestResult result = new TestResult(finalScore, totalMarks, isPassed, testNumber, testName,
                    resultTestId, completedAt, durationLimitMinutes, elapsedSeconds,
                    correctAnswers, wrongAnswers, skippedAnswers);
            testResult.setValue(result);

            // Add to history (in-memory, for the live session) and persist so
            // the result survives restarts and is visible offline.
            List<TestResult> history = testResultHistory.getValue();
            if (history != null) {
                history.add(0, result);
                testResultHistory.setValue(history);
            }
            if (testResultRepository != null) {
                testResultRepository.saveResult(result);
            }
        }
    }
}
