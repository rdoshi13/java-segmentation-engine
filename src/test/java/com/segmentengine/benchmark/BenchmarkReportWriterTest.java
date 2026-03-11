package com.segmentengine.benchmark;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.segmentengine.metrics.BenchmarkResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BenchmarkReportWriterTest {
    @Test
    void writesJsonReport() throws Exception {
        Path output = Files.createTempFile("seg-bench-", ".json");
        BenchmarkResult result = new BenchmarkResult(200, 20, 10, 2000.0, 8000.0, 12.5);

        new BenchmarkReportWriter().write(output, "json", result, 7L, true, "50k");

        Map<?, ?> payload = new ObjectMapper().readValue(output.toFile(), Map.class);
        assertEquals("50k", payload.get("preset"));
        assertEquals(200, ((Number) payload.get("profileCount")).intValue());
        assertEquals(7, ((Number) payload.get("seed")).intValue());
        assertEquals(true, payload.get("optimize"));
    }

    @Test
    void writesCsvReportAndAppendsRows() throws Exception {
        Path output = Files.createTempFile("seg-bench-", ".csv");
        BenchmarkResult result = new BenchmarkResult(100, 10, 5, 1000.0, 3000.0, 10.0);
        BenchmarkReportWriter writer = new BenchmarkReportWriter();

        writer.write(output, "csv", result, 42L, false, "100k");
        writer.write(output, "csv", result, 43L, true, "100k");

        String csv = Files.readString(output);
        assertTrue(csv.startsWith("preset,profile_count,segment_count"));
        assertEquals(3, csv.lines().count());
    }
}
