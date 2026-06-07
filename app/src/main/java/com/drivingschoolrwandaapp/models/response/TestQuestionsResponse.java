package com.drivingschoolrwandaapp.models.response;

import com.drivingschoolrwandaapp.models.entities.TestQuestion;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TestQuestionsResponse extends ApiResponse<TestQuestionsResponse.TestQuestionsData> {

    public TestQuestionsResponse(boolean success, String message, TestQuestionsData data, String error) {
        super(success, message, data, error);
    }

    public static class TestQuestionsData {
        @SerializedName("test")
        private com.drivingschoolrwandaapp.models.entities.Test test;

        @SerializedName("questions")
        private List<TestQuestion> questions;

        public com.drivingschoolrwandaapp.models.entities.Test getTest() {
            return test;
        }

        public void setTest(com.drivingschoolrwandaapp.models.entities.Test test) {
            this.test = test;
        }

        public List<TestQuestion> getQuestions() {
            return questions;
        }

        public void setQuestions(List<TestQuestion> questions) {
            this.questions = questions;
        }
    }
}
