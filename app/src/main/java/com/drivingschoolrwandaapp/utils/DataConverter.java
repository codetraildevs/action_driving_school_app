package com.drivingschoolrwandaapp.utils;

import androidx.room.TypeConverter;

import com.drivingschoolrwandaapp.models.entities.QuestionOptionTranslation;
import com.drivingschoolrwandaapp.models.entities.QuestionTranslation;
import com.drivingschoolrwandaapp.models.entities.TestTranslation;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public class DataConverter {

    private static final Gson gson = new Gson();

    @TypeConverter
    public static List<TestTranslation> fromTestTranslationString(String value) {
        if (value == null) {
            return Collections.emptyList();
        }
        Type listType = new TypeToken<List<TestTranslation>>() {}.getType();
        return gson.fromJson(value, listType);
    }

    @TypeConverter
    public static String toTestTranslationString(List<TestTranslation> list) {
        if (list == null) {
            return null;
        }
        return gson.toJson(list);
    }

    @TypeConverter
    public static List<QuestionTranslation> fromQuestionTranslationString(String value) {
        if (value == null) {
            return Collections.emptyList();
        }
        Type listType = new TypeToken<List<QuestionTranslation>>() {}.getType();
        return gson.fromJson(value, listType);
    }

    @TypeConverter
    public static String toQuestionTranslationString(List<QuestionTranslation> list) {
        if (list == null) {
            return null;
        }
        return gson.toJson(list);
    }

    @TypeConverter
    public static List<QuestionOptionTranslation> fromQuestionOptionTranslationString(String value) {
        if (value == null) {
            return Collections.emptyList();
        }
        Type listType = new TypeToken<List<QuestionOptionTranslation>>() {}.getType();
        return gson.fromJson(value, listType);
    }

    @TypeConverter
    public static String toQuestionOptionTranslationString(List<QuestionOptionTranslation> list) {
        if (list == null) {
            return null;
        }
        return gson.toJson(list);
    }
}
