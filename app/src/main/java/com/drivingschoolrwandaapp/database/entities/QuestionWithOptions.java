package com.drivingschoolrwandaapp.database.entities;

import androidx.room.Embedded;
import androidx.room.Relation;
import java.util.List;

public class QuestionWithOptions {
    @Embedded
    public TestQuestionEntity question;

    @Relation(
        parentColumn = "id",
        entityColumn = "questionId"
    )
    public List<QuestionOptionEntity> options;
}
