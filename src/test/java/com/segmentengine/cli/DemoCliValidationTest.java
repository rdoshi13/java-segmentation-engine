package com.segmentengine.cli;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoCliValidationTest {
    @Test
    void reportsValidationErrorForDuplicateSegmentNames() throws Exception {
        Path segments = writeTempJson("""
                [
                  {"name":"dup","rule":"age > 20"},
                  {"name":"dup","rule":"age > 30"}
                ]
                """);
        Path profiles = writeTempJson("""
                [
                  {"id":1,"age":30,"totalSpent":1000,"lastLoginDays":2}
                ]
                """);

        List<String> out = new ArrayList<>();
        List<String> err = new ArrayList<>();
        int exit = DemoCli.run(new String[]{
                "demo",
                "--mode", "evaluate",
                "--segments", segments.toString(),
                "--profiles", profiles.toString()
        }, out::add, err::add);

        assertEquals(3, exit);
        assertTrue(err.get(0).startsWith("Validation error:"));
    }

    @Test
    void reportsParseErrorForInvalidRule() throws Exception {
        Path segments = writeTempJson("""
                [
                  {"name":"bad_rule","rule":"age >"}
                ]
                """);

        List<String> out = new ArrayList<>();
        List<String> err = new ArrayList<>();
        int exit = DemoCli.run(new String[]{
                "demo",
                "--mode", "parse",
                "--segments", segments.toString()
        }, out::add, err::add);

        assertEquals(3, exit);
        assertTrue(err.get(0).startsWith("Parse error:"));
    }

    @Test
    void reportsValidationErrorForUnsupportedUpdateField() throws Exception {
        String segments = resourcePath("demo/segments.json");
        String profiles = resourcePath("demo/profiles.json");
        Path updates = writeTempJson("""
                [
                  {"profileId":1,"fieldName":"country","newValue":2}
                ]
                """);

        List<String> out = new ArrayList<>();
        List<String> err = new ArrayList<>();
        int exit = DemoCli.run(new String[]{
                "demo",
                "--mode", "incremental",
                "--segments", segments,
                "--profiles", profiles,
                "--updates", updates.toString()
        }, out::add, err::add);

        assertEquals(3, exit);
        assertTrue(err.get(0).startsWith("Validation error:"));
        assertTrue(err.get(0).contains("unsupported fieldName"));
    }

    @Test
    void reportsValidationErrorForUnsupportedSegmentField() throws Exception {
        Path segments = writeTempJson("""
                [
                  {"name":"bad_field","rule":"age > 20 AND country > 1"}
                ]
                """);
        String profiles = resourcePath("demo/profiles.json");

        List<String> out = new ArrayList<>();
        List<String> err = new ArrayList<>();
        int exit = DemoCli.run(new String[]{
                "demo",
                "--mode", "evaluate",
                "--segments", segments.toString(),
                "--profiles", profiles
        }, out::add, err::add);

        assertEquals(3, exit);
        assertTrue(err.get(0).startsWith("Validation error:"));
        assertTrue(err.get(0).contains("unsupported fields"));
        assertTrue(err.get(0).contains("country"));
    }

    private Path writeTempJson(String content) throws Exception {
        Path file = Files.createTempFile("seg-cli-", ".json");
        Files.writeString(file, content);
        return file;
    }

    private String resourcePath(String name) throws Exception {
        return Path.of(getClass().getClassLoader().getResource(name).toURI()).toString();
    }
}
