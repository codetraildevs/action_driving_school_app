package com.drivingschoolrwandaapp.models.mappers;

import com.drivingschoolrwandaapp.database.entities.TestResultEntity;
import com.drivingschoolrwandaapp.models.entities.TestResult;

/**
 * Maps between the UI {@link TestResult} model and the persisted
 * {@link TestResultEntity}. Kept as plain static methods because the two
 * types are field-for-field identical.
 */
public final class TestResultMapper {

    private TestResultMapper() {
    }

    public static TestResultEntity toEntity(TestResult result) {
        if (result == null) return null;
        TestResultEntity entity = new TestResultEntity();
        entity.setDate(result.getDate());
        entity.setScore(result.getScore());
        entity.setTotalMarks(result.getTotalMarks());
        entity.setPassed(result.isPassed());
        entity.setTestNumber(result.getTestNumber());
        entity.setTestName(result.getTestName());
        entity.setTestId(result.getTestId());
        entity.setDuration(result.getDuration());
        entity.setElapsedSeconds(result.getElapsedSeconds());
        entity.setCorrectCount(result.getCorrectCount());
        entity.setWrongCount(result.getWrongCount());
        entity.setSkippedCount(result.getSkippedCount());
        return entity;
    }

    public static TestResult toModel(TestResultEntity entity) {
        if (entity == null) return null;
        return new TestResult(
                entity.getScore(),
                entity.getTotalMarks(),
                entity.getPassed(),
                entity.getTestNumber(),
                entity.getTestName() != null ? entity.getTestName() : "",
                entity.getTestId(),
                entity.getDate(),
                entity.getDuration(),
                entity.getElapsedSeconds(),
                entity.getCorrectCount(),
                entity.getWrongCount(),
                entity.getSkippedCount()
        );
    }
}
