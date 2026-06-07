package com.drivingschoolrwandaapp.repository;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.drivingschoolrwandaapp.api.ApiService;
import com.drivingschoolrwandaapp.database.AppDatabase;
import com.drivingschoolrwandaapp.database.dao.QuestionOptionDao;
import com.drivingschoolrwandaapp.database.dao.TestDao;
import com.drivingschoolrwandaapp.database.dao.TestQuestionDao;
import com.drivingschoolrwandaapp.database.dao.UserDao;
import com.drivingschoolrwandaapp.database.entities.TestEntity;
import com.drivingschoolrwandaapp.database.entities.TestQuestionEntity;
import com.drivingschoolrwandaapp.database.entities.TestWithQuestions;
import com.drivingschoolrwandaapp.database.entities.User;
import com.drivingschoolrwandaapp.models.mappers.TestMapper;
import com.drivingschoolrwandaapp.models.response.TestQuestionsResponse;
import com.drivingschoolrwandaapp.models.response.TestsResponse;
import com.drivingschoolrwandaapp.utils.NetworkUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import retrofit2.Call;

public class TestRepository {

    private static final long CACHE_EXPIRATION_TIME = TimeUnit.DAYS.toMillis(7);
    private static final String TAG = "TestRepository";

    private final ApiService apiService;
    private final TestDao testDao;
    private final TestQuestionDao testQuestionDao;
    private final QuestionOptionDao questionOptionDao;
    private final UserDao userDao;
    private final ExecutorService executorService;
    private final Application application;

    @Inject
    public TestRepository(Application application, ApiService apiService) {
        this.application = application;
        this.apiService = apiService;
        AppDatabase database = AppDatabase.getDatabase(application);
        this.testDao = database.testDao();
        this.testQuestionDao = database.testQuestionDao();
        this.questionOptionDao = database.questionOptionDao();
        this.userDao = database.userDao();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<Resource<List<TestEntity>>> getTests(boolean forceRefresh) {
        return new NetworkBoundResource<List<TestEntity>, TestsResponse>() {
            @Override
            protected void saveCallResult(TestsResponse item) {
                if (item != null && item.getData() != null) {
                    // Do NOT deleteAllTests() because it cascades and deletes questions for all tests.

                    List<TestEntity> testEntities = TestMapper.INSTANCE.toEntity(item.getData());
                    List<Integer> newIds = new ArrayList<>();

                    for (TestEntity entity : testEntities) {
                        entity.setLastRefreshed(System.currentTimeMillis());
                        newIds.add(entity.getId());
                    }

                    // Strategy to avoid CASCADE DELETE of questions:
                    // 1. Insert with IGNORE (adds new tests, keeps existing ones)
                    testDao.insertTestsIgnore(testEntities);
                    // 2. Update existing tests (updates details of tests that were already there)
                    testDao.updateTests(testEntities);
                    // 3. Delete tests that are no longer in the response
                    if (!newIds.isEmpty()) {
                        testDao.deleteTestsNotIn(newIds);
                    }
                }
            }

            @Override
            protected boolean shouldFetch(List<TestEntity> data) {
                if (forceRefresh) return true;

                if (data == null || data.isEmpty()) {
                    return true;
                }
                if (!NetworkUtils.isNetworkAvailable(application)) {
                    return false;
                }

                long expirationTime = getCacheExpirationTime();
                if (expirationTime == 0) return true; // access expired

                return System.currentTimeMillis() - data.get(0).getLastRefreshed() > expirationTime;
            }

            @Override
            protected LiveData<List<TestEntity>> loadFromDb() {
                return testDao.getAllTests();
            }

            @Override
            protected Call<TestsResponse> createCall() {
                return apiService.getTests();
            }
        }.getAsLiveData();
    }

    public LiveData<Resource<TestWithQuestions>> getTestWithQuestions(int testId) {
        return new NetworkBoundResource<TestWithQuestions, TestQuestionsResponse>() {
            @Override
            protected void saveCallResult(TestQuestionsResponse item) {
                Log.d(TAG, "saveCallResult called for testId: " + testId);
                if (item != null && item.getData() != null && item.getData().getTest() != null && item.getData().getQuestions() != null) {
                    try {
                        AppDatabase.getDatabase(application).runInTransaction(() -> {
                            Log.d(TAG, "Starting transaction to save questions. Count: " + item.getData().getQuestions().size());

                            // Clear old data first
                            questionOptionDao.deleteOptionsForTest(testId);
                            testQuestionDao.deleteQuestionsForTest(testId);

                            // Insert the test
                            TestEntity testEntity = TestMapper.INSTANCE.toEntity(item.getData().getTest());
                            testEntity.setLastRefreshed(System.currentTimeMillis());

                            long rowId = testDao.insertTest(testEntity);
                            Log.d(TAG, "Inserted/Updated TestEntity. ID: " + testEntity.getId() + ", Insert Result: " + rowId);

                            // Insert questions and their options
                            List<TestQuestionEntity> questionEntities = TestMapper.INSTANCE.toQuestionEntities(item.getData().getQuestions());
                            Log.d(TAG, "Mapped " + questionEntities.size() + " question entities");

                            for (int i = 0; i < questionEntities.size(); i++) {
                                TestQuestionEntity questionEntity = questionEntities.get(i);
                                questionEntity.setTestId(testId);
                                long questionId = testQuestionDao.insertQuestion(questionEntity);
                                Log.d(TAG, "Inserted Question " + i + ", ID: " + questionId + ", TestID: " + testId);

                                if (item.getData().getQuestions().get(i).getOptions() != null) {
                                    questionOptionDao.insertOptions(TestMapper.INSTANCE.toOptionEntities(item.getData().getQuestions().get(i).getOptions(), (int) questionId));
                                }
                            }
                            Log.d(TAG, "Transaction completed successfully");
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Error saving test questions", e);
                    }
                } else {
                    Log.e(TAG, "saveCallResult: Data is null or incomplete");
                }
            }

            @Override
            protected boolean shouldFetch(TestWithQuestions data) {
                // If no data, we must fetch
                if (data == null || data.test == null || data.questions == null || data.questions.isEmpty()) {
                    Log.d(TAG, "shouldFetch: Data is null or empty, fetching...");
                    return true;
                }

                if (!NetworkUtils.isNetworkAvailable(application)) {
                    Log.d(TAG, "shouldFetch: Offline, using cache.");
                    return false;
                }

                long lastRefreshed = data.test.getLastRefreshed();
                long expirationTime = getCacheExpirationTime();

                if (expirationTime == 0) { // access expired
                    Log.d(TAG, "shouldFetch: User test access expired, fetching...");
                    return true;
                }

                boolean isExpired = System.currentTimeMillis() - lastRefreshed > expirationTime;
                Log.d(TAG, "shouldFetch: Online. Expired: " + isExpired);
                return isExpired;
            }

            @Override
            protected LiveData<TestWithQuestions> loadFromDb() {
                return testDao.getTestWithQuestions(testId);
            }

            @Override
            protected Call<TestQuestionsResponse> createCall() {
                return apiService.getTestQuestions(testId);
            }
        }.getAsLiveData();
    }
    
    private long getCacheExpirationTime() {
        User user = userDao.getUserSync();
        if (user != null && user.getTestAccessExpiresAt() != null) {
            try {
                // Try parsing ISO 8601 format (e.g., 2025-12-27T16:20:29.814Z)
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date expirationDate = sdf.parse(user.getTestAccessExpiresAt());
                
                if (expirationDate != null) {
                    long remainingTime = expirationDate.getTime() - System.currentTimeMillis();
                    if (remainingTime <= 0) {
                        return 0; // Access expired
                    }
                    return Math.min(CACHE_EXPIRATION_TIME, remainingTime);
                }
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing date ISO8601: " + user.getTestAccessExpiresAt(), e);
                // Fallback to simpler format
                try {
                     SimpleDateFormat sdfFallback = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                     Date expirationDate = sdfFallback.parse(user.getTestAccessExpiresAt());
                     if (expirationDate != null) {
                         long remainingTime = expirationDate.getTime() - System.currentTimeMillis();
                         if (remainingTime <= 0) {
                             return 0; // Access expired
                         }
                         return Math.min(CACHE_EXPIRATION_TIME, remainingTime);
                     }
                } catch (ParseException e2) {
                     Log.e(TAG, "Error parsing date fallback: " + user.getTestAccessExpiresAt(), e2);
                }
            }
        }
        return CACHE_EXPIRATION_TIME;
    }
}
