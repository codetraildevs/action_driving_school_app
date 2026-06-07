package com.drivingschoolrwandaapp.models.mappers;

import com.drivingschoolrwandaapp.database.entities.QuestionOptionEntity;
import com.drivingschoolrwandaapp.database.entities.QuestionWithOptions;
import com.drivingschoolrwandaapp.database.entities.TestEntity;
import com.drivingschoolrwandaapp.database.entities.TestQuestionEntity;
import com.drivingschoolrwandaapp.database.entities.TestWithQuestions;
import com.drivingschoolrwandaapp.models.entities.QuestionOption;
import com.drivingschoolrwandaapp.models.entities.Test;
import com.drivingschoolrwandaapp.models.entities.TestQuestion;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface TestMapper {

    TestMapper INSTANCE = Mappers.getMapper(TestMapper.class);

    // --- To Entity (for saving to DB) ---

    @Mapping(source = "id", target = "id")
    @Mapping(source = "testNumber", target = "testNumber")
    @Mapping(source = "imageUrl", target = "imageUrl")
    @Mapping(source = "free", target = "free") // Changed target to "free"
    @Mapping(source = "count.testQuestions", target = "questionCount")
    @Mapping(source = "testTranslations", target = "testTranslations")
    TestEntity toEntity(Test test);

    List<TestEntity> toEntity(List<Test> tests);

    @Mapping(source = "testId", target = "testId") // Direct mapping
    @Mapping(source = "questionTranslations", target = "questionTranslations")
    TestQuestionEntity toQuestionEntity(TestQuestion question);

    List<TestQuestionEntity> toQuestionEntities(List<TestQuestion> questions);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "text", target = "text")
    @Mapping(source = "imageUrl", target = "imageUrl")
    @Mapping(source = "correct", target = "correct")
    @Mapping(source = "questionOptionTranslations", target = "questionOptionTranslations")
    QuestionOptionEntity toOptionEntity(QuestionOption option, @Context int questionId);

    List<QuestionOptionEntity> toOptionEntities(List<QuestionOption> options, @Context int questionId);

    @AfterMapping
    default void afterMappingOption(@MappingTarget QuestionOptionEntity entity, @Context int questionId) {
        entity.setQuestionId(questionId);
    }

    // --- To Model (for displaying in UI) ---

    @Mapping(source = "test.id", target = "id")
    @Mapping(source = "test.title", target = "title")
    @Mapping(source = "test.description", target = "description")
    @Mapping(source = "test.testNumber", target = "testNumber")
    @Mapping(source = "test.imageUrl", target = "imageUrl")
    @Mapping(source = "test.totalMarks", target = "totalMarks")
    @Mapping(source = "test.passMarks", target = "passMarks")
    @Mapping(source = "test.duration", target = "duration")
    @Mapping(source = "test.free", target = "free")
    @Mapping(source = "questions", target = "questions")
    @Mapping(source = "test.testTranslations", target = "testTranslations")
    Test toModel(TestWithQuestions testWithQuestions);

    @Mapping(source = "question.id", target = "id")
    @Mapping(source = "question.questionText", target = "questionText")
    @Mapping(source = "question.questionType", target = "questionType")
    @Mapping(source = "question.imageUrl", target = "imageUrl")
    @Mapping(source = "options", target = "options")
    @Mapping(source = "question.questionTranslations", target = "questionTranslations")
    TestQuestion toModel(QuestionWithOptions questionWithOptions);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "text", target = "text")
    @Mapping(source = "imageUrl", target = "imageUrl")
    @Mapping(source = "correct", target = "correct")
    @Mapping(source = "questionOptionTranslations", target = "questionOptionTranslations")
    QuestionOption toModel(QuestionOptionEntity questionOptionEntity);

}
