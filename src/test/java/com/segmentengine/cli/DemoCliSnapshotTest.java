package com.segmentengine.cli;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoCliSnapshotTest {
    @Test
    void parseModeMatchesSnapshot() throws Exception {
        List<String> out = new ArrayList<>();
        List<String> err = new ArrayList<>();

        int exit = DemoCli.run(new String[]{
                "demo",
                "--mode", "parse",
                "--segments", resourcePath("demo/segments.json"),
                "--optimize"
        }, out::add, err::add);

        assertEquals(0, exit);
        assertEquals(readSnapshot("snapshots/parse_optimized.txt"), toSnapshotText(out));
    }

    @Test
    void evaluateModeMatchesSnapshot() throws Exception {
        List<String> out = new ArrayList<>();
        List<String> err = new ArrayList<>();

        int exit = DemoCli.run(new String[]{
                "demo",
                "--mode", "evaluate",
                "--segments", resourcePath("demo/segments.json"),
                "--profiles", resourcePath("demo/profiles.json")
        }, out::add, err::add);

        assertEquals(0, exit);
        assertEquals(readSnapshot("snapshots/evaluate.txt"), toSnapshotText(out));
    }

    @Test
    void incrementalModeMatchesSnapshot() throws Exception {
        List<String> out = new ArrayList<>();
        List<String> err = new ArrayList<>();

        int exit = DemoCli.run(new String[]{
                "demo",
                "--mode", "incremental",
                "--segments", resourcePath("demo/segments.json"),
                "--profiles", resourcePath("demo/profiles.json"),
                "--updates", resourcePath("demo/updates.json")
        }, out::add, err::add);

        assertEquals(0, exit);
        assertEquals(readSnapshot("snapshots/incremental.txt"), toSnapshotText(out));
    }

    @Test
    void benchmarkModeReturnsMetrics() {
        List<String> out = new ArrayList<>();
        List<String> err = new ArrayList<>();

        int exit = DemoCli.run(new String[]{
                "demo",
                "--mode", "benchmark",
                "--profile-count", "200",
                "--segment-count", "20",
                "--seed", "7"
        }, out::add, err::add);

        assertEquals(0, exit);
        assertTrue(out.get(0).equals("MODE=benchmark"));
        assertTrue(out.stream().anyMatch(line -> line.startsWith("profiles_per_sec=")));
        assertTrue(out.stream().anyMatch(line -> line.startsWith("avg_incremental_latency_micros=")));
    }

    private String resourcePath(String name) throws URISyntaxException {
        return Path.of(getClass().getClassLoader().getResource(name).toURI()).toString();
    }

    private String readSnapshot(String name) throws IOException, URISyntaxException {
        Path path = Path.of(getClass().getClassLoader().getResource(name).toURI());
        return Files.readString(path);
    }

    private String toSnapshotText(List<String> lines) {
        return String.join("\n", lines) + "\n";
    }
}
