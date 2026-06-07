package com.drivingschoolrwandaapp.models.request;

import java.util.Date;
import java.util.List;

public class TestSubmissionRequest {
    private List<TestAnswerRequest> answers;
    private Date endTime;

    public TestSubmissionRequest(List<TestAnswerRequest> answers, Date endTime) {
        this.answers = answers;
        this.endTime = endTime;
    }

    public List<TestAnswerRequest> getAnswers() { return answers; }
    public void setAnswers(List<TestAnswerRequest> answers) { this.answers = answers; }

    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }
}