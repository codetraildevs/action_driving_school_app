package com.drivingschoolrwandaapp.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.drivingschoolrwandaapp.database.entities.TestEntity;
import com.drivingschoolrwandaapp.database.entities.TestQuestionEntity;
import com.drivingschoolrwandaapp.database.entities.QuestionOptionEntity;
import com.drivingschoolrwandaapp.database.entities.TestWithQuestions;
import com.drivingschoolrwandaapp.database.entities.QuestionWithOptions;
import com.drivingschoolrwandaapp.data.local.preferences.AppPreferences;
import com.drivingschoolrwandaapp.models.LocalExam;
import com.drivingschoolrwandaapp.utils.ErrorUtils;
import com.drivingschoolrwandaapp.models.LocalQuestion;
import com.drivingschoolrwandaapp.repository.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Repository that loads exam data from local JSON assets
 * instead of making network API calls.
 * Exam data is loaded using [LocalExamDataSource] and mapped to
 * existing database entities for UI compatibility.
 * Supports multilingual exams following the app's language preference.
 *
 * The JSON assets (200-250 KB per language) are read and parsed on a
 * dedicated background executor and the result is posted back to the
 * main thread via LiveData, so opening the Exams screen never blocks
 * the UI thread with a large synchronous file parse.
 */
@Singleton
public class TestRepository {

    private static final String TAG = "TestRepository";
    private static final String ASSETS_PREFIX = "assets/";
    private static final String FILE_ASSET_PREFIX = "file:///android_asset/";

    private final LocalExamDataSource localExamDataSource;
    private final AppPreferences appPreferences;
    private final Context context;
    private final ExecutorService executorService;

    @Inject
    public TestRepository(LocalExamDataSource localExamDataSource, @ApplicationContext Context context) {
        this.localExamDataSource = localExamDataSource;
        this.appPreferences = new AppPreferences(context);
        this.context = context.getApplicationContext();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Get all tests from local JSON assets.
     * @param forceRefresh ignored for local data, kept for API compatibility
     * @return LiveData with Resource containing list of TestEntity
     */
    public LiveData<Resource<List<TestEntity>>> getTests(boolean forceRefresh) {
        MutableLiveData<Resource<List<TestEntity>>> result = new MutableLiveData<>();

        executeSafely(() -> {
            try {
                // Get current language from app preferences
                String languageCode = getCurrentLanguage();

                // Load exams from local JSON
                List<LocalExam> localExams = localExamDataSource.loadExams(languageCode);
                List<TestEntity> testEntities = new ArrayList<>();

                for (int i = 0; i < localExams.size(); i++) {
                    LocalExam localExam = localExams.get(i);
                    TestEntity entity = new TestEntity();
                    entity.setId(parseQuizId(localExam.getQuizId(), i));
                    entity.setTitle(localExam.getTitle());
                    entity.setDescription(localExam.getExamType());
                    entity.setTestNumber(i + 1);
                    entity.setImageUrl(convertToAssetUri(localExam.getExamImgUrl()));
                    entity.setTotalMarks(localExam.getQuestions().size());
                    entity.setPassMarks((int) Math.ceil(localExam.getQuestions().size() * 0.5)); // 50% to pass
                    entity.setDuration(localExam.getQuestions().size()); // 1 minute per question
                    entity.setFree("Free".equalsIgnoreCase(localExam.getExamType()));
                    entity.setQuestionCount(localExam.getQuestions().size());
                    entity.setLastRefreshed(System.currentTimeMillis());
                    testEntities.add(entity);
                }

                result.postValue(Resource.success(testEntities));
            } catch (Exception e) {
                reportQuietly(TAG, "Error loading local exams", e);
                result.postValue(Resource.<List<TestEntity>>error(ErrorUtils.getUserFriendlyMessage(context, e), null));
            }
        });

        return result;
    }

    /**
     * Get test with questions from local JSON assets.
     * @param testId The test ID (position-based, 1-indexed, or quizId as int)
     * @return LiveData with Resource containing TestWithQuestions
     */
    public LiveData<Resource<TestWithQuestions>> getTestWithQuestions(int testId) {
        MutableLiveData<Resource<TestWithQuestions>> result = new MutableLiveData<>();

        executeSafely(() -> {
            try {
                String languageCode = getCurrentLanguage();

                // Load the specific exam (testId maps to quiz sequential index)
                List<LocalExam> localExams = localExamDataSource.loadExams(languageCode);

                // Find the exam by its position (1-indexed) or by quizId
                LocalExam localExam;
                String quizIdStr = String.valueOf(testId);

                // First try by quizId
                localExam = localExamDataSource.loadExamByQuizId(quizIdStr, languageCode);

                // If not found, try by position (1-indexed)
                if (localExam == null && testId > 0 && testId <= localExams.size()) {
                    localExam = localExamDataSource.loadExamByIndex(testId - 1, languageCode);
                }

                if (localExam == null) {
                    result.postValue(Resource.<TestWithQuestions>error(
                            context.getString(com.drivingschoolrwandaapp.R.string.exam_not_found, testId), null));
                    return;
                }

                // Find sequential position (consistent with getTests() i+1 numbering)
                int sequentialNumber = 1;
                for (int i = 0; i < localExams.size(); i++) {
                    if (localExams.get(i).getQuizId().equals(localExam.getQuizId())) {
                        sequentialNumber = i + 1;
                        break;
                    }
                }

                // Build TestWithQuestions from local data
                TestWithQuestions testWithQuestions = buildTestWithQuestions(localExam, testId, languageCode, sequentialNumber);
                result.postValue(Resource.success(testWithQuestions));
            } catch (Exception e) {
                reportQuietly(TAG, "Error loading local exam questions", e);
                result.postValue(Resource.<TestWithQuestions>error(ErrorUtils.getUserFriendlyMessage(context, e), null));
            }
        });

        return result;
    }

    /**
     * Build a TestWithQuestions entity from a LocalExam.
     */
    private TestWithQuestions buildTestWithQuestions(LocalExam localExam, int testId, String languageCode, int sequentialNumber) {
        TestEntity testEntity = new TestEntity();
        testEntity.setId(testId);
        testEntity.setTitle(localExam.getTitle());
        testEntity.setDescription(localExam.getExamType());
        testEntity.setTestNumber(sequentialNumber);
        testEntity.setImageUrl(convertToAssetUri(localExam.getExamImgUrl()));
        testEntity.setTotalMarks(localExam.getQuestions().size());
        testEntity.setPassMarks((int) Math.ceil(localExam.getQuestions().size() * 0.5));
        testEntity.setDuration(localExam.getQuestions().size()); // 1 minute per question
        testEntity.setFree("Free".equalsIgnoreCase(localExam.getExamType()));
        testEntity.setQuestionCount(localExam.getQuestions().size());
        testEntity.setLastRefreshed(System.currentTimeMillis());

        List<QuestionWithOptions> questionsWithOptions = new ArrayList<>();
        List<LocalQuestion> localQuestions = localExam.getQuestions();

        for (int qIdx = 0; qIdx < localQuestions.size(); qIdx++) {
            LocalQuestion localQ = localQuestions.get(qIdx);

            TestQuestionEntity questionEntity = new TestQuestionEntity();
            questionEntity.setId(qIdx + 1);
            questionEntity.setTestId(testId);
            questionEntity.setQuestionText(localQ.getQuestion());
            questionEntity.setQuestionType("multiple_choice");
            questionEntity.setImageUrl(convertToAssetUri(localQ.getQuestionImgUrl()));

            List<QuestionOptionEntity> options = new ArrayList<>();
            String[] optionTexts = {
                localQ.getOption1(),
                localQ.getOption2(),
                localQ.getOption3(),
                localQ.getOption4()
            };

            int correctIndex = localQ.getCorrectOptionIndex();

            for (int oIdx = 0; oIdx < optionTexts.length; oIdx++) {
                QuestionOptionEntity option = new QuestionOptionEntity();
                option.setId(qIdx * 4 + oIdx + 1);
                option.setQuestionId(qIdx + 1);
                option.setText(optionTexts[oIdx]);
                option.setCorrect(oIdx == correctIndex);
                options.add(option);
            }

            QuestionWithOptions questionWithOptions = new QuestionWithOptions();
            questionWithOptions.question = questionEntity;
            questionWithOptions.options = options;
            questionsWithOptions.add(questionWithOptions);
        }

        TestWithQuestions testWithQuestions = new TestWithQuestions();
        testWithQuestions.test = testEntity;
        testWithQuestions.questions = questionsWithOptions;
        return testWithQuestions;
    }

    /**
     * Get the current language code from app preferences.
     * Returns the user's selected language: "en", "fr", or "rw".
     */
    private String getCurrentLanguage() {
        String language = appPreferences.getLanguage();
        if (language == null || language.isEmpty()) {
            return "en"; // Default to English
        }
        return language;
    }

    /**
     * Parse quizId from string to int. Falls back to index if parsing fails.
     */
    private int parseQuizId(String quizIdStr, int fallback) {
        try {
            return Integer.parseInt(quizIdStr);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /**
     * Convert an asset path from the JSON file to a file:///android_asset/ URI
     * that Glide can load from Android's assets directory.
     *
     * JSON stores paths like: "json_questions_images/exam1.webp"
     * Glide expects:          "file:///android_asset/json_questions_images/exam1.webp"
     *
     * @param imagePath The image path from JSON (may be null or empty)
     * @return The converted file:// URI, or null if the input is null/empty
     */
    private String convertToAssetUri(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return null;
        }
        // Strip the "assets/" prefix if present
        String cleaned = imagePath;
        if (cleaned.startsWith(ASSETS_PREFIX)) {
            cleaned = cleaned.substring(ASSETS_PREFIX.length());
        }
        return FILE_ASSET_PREFIX + cleaned;
    }

    /**
     * Logs and reports an exception to Crashlytics without ever throwing.
     * This code runs on the background executor, and a failure inside logging
     * or crash reporting must never prevent the error Resource from being
     * delivered to the UI (otherwise the screen would hang on its spinner).
     */
    private static void reportQuietly(String tag, String message, Throwable e) {
        try {
            Log.e(tag, message, e);
        } catch (Throwable ignored) {
            // Logging must never break the flow.
        }
        try {
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e);
        } catch (Throwable ignored) {
            // Crash reporting must never break the flow.
        }
    }

    private void executeSafely(Runnable runnable) {
        try {
            if (!executorService.isShutdown() && !executorService.isTerminated()) {
                executorService.execute(runnable);
            }
        } catch (RejectedExecutionException e) {
            Log.w(TAG, "Task rejected, executor is shutting down", e);
        }
    }

    /**
     * Cleanly shut down the internal executor to release the background thread.
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
