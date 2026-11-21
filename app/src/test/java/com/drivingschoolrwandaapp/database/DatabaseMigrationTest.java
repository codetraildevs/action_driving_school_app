package com.drivingschoolrwandaapp.database;

import androidx.sqlite.db.SupportSQLiteDatabase;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** Executes the production migration SQL with SQLite, not a mocked SQL engine. */
public class DatabaseMigrationTest {
    @Test public void additiveMigrationPreservesExistingRowsAndMatchesNewTable() throws Exception {
        Path path = Path.of("schemas/com.drivingschoolrwandaapp.database.AppDatabase/14.json");
        if (!Files.exists(path)) path = Path.of("app").resolve(path);
        JsonArray tables = JsonParser.parseString(Files.readString(path)).getAsJsonObject()
                .getAsJsonObject("database").getAsJsonArray("entities");
        try (Connection old = DriverManager.getConnection("jdbc:sqlite::memory:");
             Connection fresh = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            // v13's tables are unchanged by the additive 13->14 migration.
            for (JsonElement element : tables) {
                String table = element.getAsJsonObject().get("tableName").getAsString();
                String sql = element.getAsJsonObject().get("createSql").getAsString()
                        .replace("${TABLE_NAME}", table);
                fresh.createStatement().execute(sql);
                if (!table.equals("test_results")) old.createStatement().execute(sql);
            }
            old.createStatement().execute("INSERT INTO bookmarks(id,pdfId,page,name) VALUES(1,42,19,'Saved page')");
            old.createStatement().execute("INSERT INTO user_subscription(id,userId,subscriptionPlanId,status) VALUES(1,7,2,'ACTIVE')");
            old.createStatement().execute("PRAGMA user_version=13");
            SupportSQLiteDatabase db = mock(SupportSQLiteDatabase.class);
            doAnswer(call -> { old.createStatement().execute(call.getArgument(0)); return null; })
                    .when(db).execSQL(anyString());
            AppDatabase.MIGRATION_13_14.migrate(db);
            assertEquals(columns(fresh, "test_results"), columns(old, "test_results"));
            try (ResultSet rows = old.createStatement().executeQuery("SELECT page,name FROM bookmarks WHERE id=1")) {
                assertTrue(rows.next()); assertEquals(19, rows.getInt(1)); assertEquals("Saved page", rows.getString(2));
            }
            try (ResultSet rows = old.createStatement().executeQuery("SELECT status FROM user_subscription WHERE id=1")) {
                assertTrue(rows.next()); assertEquals("ACTIVE", rows.getString(1));
            }
            // Migration remains safe if Room retries after an interrupted attempt.
            AppDatabase.MIGRATION_13_14.migrate(db);
            assertEquals(columns(fresh, "test_results"), columns(old, "test_results"));
        }
    }

    private List<String> columns(Connection connection, String table) throws Exception {
        List<String> result = new ArrayList<>();
        try (ResultSet rows = connection.createStatement().executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rows.next()) result.add(rows.getString("name") + ":" + rows.getString("type")
                    + ":" + rows.getInt("notnull") + ":" + rows.getInt("pk"));
        }
        return result;
    }
}
