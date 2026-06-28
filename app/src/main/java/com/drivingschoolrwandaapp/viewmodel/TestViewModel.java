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
    private final Set<Integer> feedbackHiddenQuestions = new HashSet<>();
    
    // History of test results for Previous Tests page
    private final MutableLiveData<List<TestResult>> testResultHistory = new MutableLiveData<>(new ArrayList<>());

    @Inject
    public TestViewModel(@NonNull Application application, TestRepository testRepository) {
        super(application);
        this.testRepository = testRepository;
        
        // Initialize refreshTrigger
        refreshTrigger.setValue(false);

        this.tests = Transformations.switchMap(refreshTrigger, force -> testRepository.getTests(force));
        
        this.questionsForTest = Transformations.switchMap(testId, testRepository::getTestWithQuestions);
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

    public void calculateResult() {
        // Use stored test data instead of relying on switchMap getValue()
        if (loadedTestData != null && loadedTestData.questions != null && !loadedTestData.questions.isEmpty()) {
            int correctAnswers = 0;
            int totalMarks = loadedTestData.test != null ? loadedTestData.test.getTotalMarks() : loadedTestData.questions.size();
            int passMarks = loadedTestData.test != null ? loadedTestData.test.getPassMarks() : (int) Math.ceil(loadedTestData.questions.size() * 0.5);
            int numberOfQuestions = loadedTestData.questions.size();

            Map<Integer, Integer> userAnswers = selectedAnswers.getValue();
            if (userAnswers == null) return;

            for (com.drivingschoolrwandaapp.database.entities.QuestionWithOptions questionWithOptions : loadedTestData.questions) {
                Integer userAnswerId = userAnswers.get(questionWithOptions.question != null ? questionWithOptions.question.getId() : 0);
                if (userAnswerId != null && questionWithOptions.options != null) {
                    for (com.drivingschoolrwandaapp.database.entities.QuestionOptionEntity option : questionWithOptions.options) {
                        if (option.getId() == userAnswerId && option.isCorrect()) {
                            correctAnswers++;
                        }
                    }
                }
            }
            
            double marksPerQuestion = numberOfQuestions > 0 ? (double) totalMarks / numberOfQuestions : 0;
            int finalScore = (int) Math.round(correctAnswers * marksPerQuestion);

            boolean isPassed = finalScore >= passMarks;
            
            int testNumber = loadedTestData.test != null ? loadedTestData.test.getTestNumber() : 0;
            String testName = loadedTestData.test != null && loadedTestData.test.getTitle() != null 
                ? loadedTestData.test.getTitle() : "Exam " + testNumber;
            
            TestResult result = new TestResult(finalScore, totalMarks, isPassed, testNumber, testName);
            testResult.setValue(result);
            
            // Add to history
            List<TestResult> history = testResultHistory.getValue();
            if (history != null) {
                history.add(0, result); // Add at beginning (newest first)
                testResultHistory.setValue(history);
            }
        }
    }
}
