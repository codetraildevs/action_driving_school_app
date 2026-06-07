package com.drivingschoolrwandaapp.database.entities;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

public class TestWithQuestions {
    @Embedded
    public TestEntity test;

    @Relation(
        entity = TestQuestionEntity.class,
        parentColumn = "id",
        entityColumn = "testId"
    )
    public List<QuestionWithOptions> questions;
}
