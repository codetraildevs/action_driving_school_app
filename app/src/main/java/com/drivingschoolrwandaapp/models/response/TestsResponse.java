package com.drivingschoolrwandaapp.models.response;

import com.drivingschoolrwandaapp.models.entities.Test;

import java.util.List;

public class TestsResponse extends ApiResponse<List<Test>> {
    public TestsResponse(boolean success, String message, List<Test> data, String error) {
        super(success, message, data, error);
    }
}
