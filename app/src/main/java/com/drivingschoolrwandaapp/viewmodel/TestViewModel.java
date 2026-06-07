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

    private final MutableLiveData<Map<Integer, Integer>> selectedAnswers = new MutableLiveData<>(new HashMap<>());
    private final Set<Integer> feedbackHiddenQuestions = new HashSet<>();

    @Inject
    public TestViewModel(@NonNull Application application, TestRepository testRepository) {
        super(application);
        this.testRepository = testRepository;
        
        // Initialize refreshTrigger
        refreshTrigger.setValue(false); // Default: no force refresh

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

    public void loadQuestionsForTest(int testId) {
        this.testId.setValue(testId);
    }

    public LiveData<Resource<TestWithQuestions>> downloadQuestions(int testId) {
        return testRepository.getTestWithQuestions(testId);
    }

    public Integer getTestId() {
        return testId.getValue();
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
        Resource<TestWithQuestions> resource = questionsForTest.getValue();
        if (resource != null && resource.data != null && resource.data.questions != null && !resource.data.questions.isEmpty()) {
            int correctAnswers = 0;
            int totalMarks = resource.data.test.getTotalMarks();
            int passMarks = resource.data.test.getPassMarks();
            int numberOfQuestions = resource.data.questions.size();

            Map<Integer, Integer> userAnswers = selectedAnswers.getValue();
            if (userAnswers == null) return;

            for (com.drivingschoolrwandaapp.database.entities.QuestionWithOptions questionWithOptions : resource.data.questions) {
                Integer userAnswerId = userAnswers.get(questionWithOptions.question.getId());
                if (userAnswerId != null) {
                    for (com.drivingschoolrwandaapp.database.entities.QuestionOptionEntity option : questionWithOptions.options) {
                        if (option.getId() == userAnswerId && option.isCorrect()) {
                            correctAnswers++;
                        }
                    }
                }
            }
            double marksPerQuestion = (double) totalMarks / numberOfQuestions;
            int finalScore = (int) Math.round(correctAnswers * marksPerQuestion);

            boolean isPassed = finalScore >= passMarks;
            testResult.setValue(new TestResult(finalScore, totalMarks, isPassed));
        }
    }
}
