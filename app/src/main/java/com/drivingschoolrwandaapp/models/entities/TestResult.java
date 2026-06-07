package com.drivingschoolrwandaapp.models.entities;

public class TestResult {

    private final int score;
    private final int totalMarks;
    private final boolean passed;

    public TestResult(int score, int totalMarks, boolean passed) {
        this.score = score;
        this.totalMarks = totalMarks;
        this.passed = passed;
    }

    public int getScore() {
        return score;
    }

    public int getTotalMarks() {
        return totalMarks;
    }

    public boolean isPassed() {
        return passed;
    }
}
