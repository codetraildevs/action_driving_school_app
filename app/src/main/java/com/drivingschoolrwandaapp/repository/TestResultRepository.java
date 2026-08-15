package com.drivingschoolrwandaapp.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.drivingschoolrwandaapp.database.dao.TestResultDao;
import com.drivingschoolrwandaapp.database.entities.TestResultEntity;
import com.drivingschoolrwandaapp.models.entities.TestResult;
import com.drivingschoolrwandaapp.models.mappers.TestResultMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Persists completed exam attempts so the user's score history is available
 * offline and survives app restarts. Writes run on a dedicated background
 * executor; reads are exposed as a Room-backed LiveData (newest first).
 */
@Singleton
public class TestResultRepository {

    private static final String TAG = "TestResultRepository";

    private final TestResultDao testResultDao;
    private final ExecutorService executorService;

    @Inject
    public TestResultRepository(TestResultDao testResultDao) {
        this.testResultDao = testResultDao;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * All stored results, newest first, as UI models.
     */
    public LiveData<List<TestResult>> getHistory() {
        return Transformations.map(testResultDao.getAll(), entities -> {
            List<TestResult> results = new ArrayList<>(entities.size());
            for (TestResultEntity entity : entities) {
                TestResult model = TestResultMapper.toModel(entity);
                if (model != null) {
                    results.add(model);
                }
            }
            return results;
        });
    }

    /**
     * Persist a completed exam result off the main thread. Duplicate
     * completions at the same timestamp are ignored by the DAO.
     */
    public void saveResult(TestResult result) {
        if (result == null) return;
        executeSafely(() -> {
            try {
                testResultDao.insert(TestResultMapper.toEntity(result));
            } catch (Exception e) {
                Log.e(TAG, "Failed to persist test result", e);
                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e);
            }
        });
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
