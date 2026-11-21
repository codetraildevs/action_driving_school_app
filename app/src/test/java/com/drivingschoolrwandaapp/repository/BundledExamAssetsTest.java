package com.drivingschoolrwandaapp.repository;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Test;
import static org.junit.Assert.*;

public class BundledExamAssetsTest {
    @Test public void everyBundledExamAndQuestionImageExists() throws Exception {
        File assets = new File("src/main/assets");
        if (!assets.isDirectory()) assets = new File("app/src/main/assets");
        File[] files = new File(assets, "json_exams").listFiles((dir, name) -> name.endsWith(".json"));
        assertNotNull("Exam assets directory must exist", files);
        assertTrue("Must validate all three languages", files.length >= 3);
        for (File file : files) {
            JsonElement data = JsonParser.parseString(Files.readString(file.toPath(), StandardCharsets.UTF_8));
            checkImages(data, assets, file.getName());
        }
    }

    private void checkImages(JsonElement value, File assets, String source) {
        if (value.isJsonArray()) {
            for (JsonElement child : value.getAsJsonArray()) checkImages(child, assets, source);
        } else if (value.isJsonObject()) {
            value.getAsJsonObject().entrySet().forEach(entry -> {
                if ((entry.getKey().equals("examImgUrl") || entry.getKey().equals("questionImgUrl"))
                        && !entry.getValue().isJsonNull()) {
                    String path = entry.getValue().getAsString();
                    if (!path.isEmpty() && !path.startsWith("https://") && !path.startsWith("http://")) {
                        String relative = path.startsWith("assets/") ? path.substring(7) : path;
                        assertTrue(source + ": missing " + path, new File(assets, relative).isFile());
                    }
                } else checkImages(entry.getValue(), assets, source);
            });
        }
    }
}
