package com.segmentengine.cli;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoCliBenchmarkExportTest {
    @Test
    void benchmarkSupportsPresetAndOverrides() {
        List<String> out = new ArrayList<>();
        List<String> err = new ArrayList<>();

        int exit = DemoCli.run(new String[]{
                "demo",
                "--mode", "benchmark",
                "--preset", "50k",
                "--profile-count", "200",
                "--segment-count", "20",
                "--seed", "7"
        }, out::add, err::add);

        assertEquals(0, exit);
        assertTrue(out.stream().anyMatch(line -> line.equals("preset=50k")));
        assertTrue(out.stream().anyMatch(line -> line.equals("profiles=200")));
        assertTrue(out.stream().anyMatch(line -> line.equals("segments=20")));
    }

    @Test
    void benchmarkExportsCsvReport() throws Exception {
        Path output = Files.createTempFile("seg-cli-bench-", ".csv");
        List<String> out = new ArrayList<>();
        List<String> err = new ArrayList<>();

        int exit = DemoCli.run(new String[]{
                "demo",
                "--mode", "benchmark",
                "--profile-count", "200",
                "--segment-count", "20",
                "--seed", "7",
                "--output", output.toString()
        }, out::add, err::add);

        assertEquals(0, exit);
        assertTrue(Files.exists(output));
        String csv = Files.readString(output);
        assertTrue(csv.contains("profile_count"));
        assertTrue(out.stream().anyMatch(line -> line.startsWith("report_path=")));
    }
}
