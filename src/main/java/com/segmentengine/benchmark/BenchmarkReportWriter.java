package com.segmentengine.benchmark;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.segmentengine.metrics.BenchmarkResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class BenchmarkReportWriter {
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public void write(Path outputPath, String format, BenchmarkResult result, long seed, boolean optimize, String preset)
            throws IOException {
        createParentDirectory(outputPath);
        if ("json".equals(format)) {
            writeJson(outputPath, result, seed, optimize, preset);
            return;
        }
        if ("csv".equals(format)) {
            writeCsv(outputPath, result, seed, optimize, preset);
            return;
        }
        throw new IllegalArgumentException("Unsupported report format: " + format + ". Use json or csv.");
    }

    private void writeJson(Path outputPath, BenchmarkResult result, long seed, boolean optimize, String preset)
            throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("preset", preset);
        payload.put("profileCount", result.profileCount());
        payload.put("segmentCount", result.segmentCount());
        payload.put("totalEvaluationMillis", result.totalEvaluationMillis());
        payload.put("profilesPerSecond", result.profilesPerSecond());
        payload.put("predicateEvaluationsPerSecond", result.predicateEvaluationsPerSecond());
        payload.put("avgIncrementalLatencyMicros", result.avgIncrementalLatencyMicros());
        payload.put("seed", seed);
        payload.put("optimize", optimize);
        MAPPER.writeValue(outputPath.toFile(), payload);
    }

    private void writeCsv(Path outputPath, BenchmarkResult result, long seed, boolean optimize, String preset)
            throws IOException {
        boolean hasFile = Files.exists(outputPath) && Files.size(outputPath) > 0;
        if (!hasFile) {
            Files.writeString(
                    outputPath,
                    "preset,profile_count,segment_count,total_eval_ms,profiles_per_sec,predicate_evals_per_sec,avg_incremental_latency_micros,seed,optimize\n",
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        }

        String line = String.format(
                Locale.US,
                "%s,%d,%d,%d,%.2f,%.2f,%.2f,%d,%s%n",
                preset,
                result.profileCount(),
                result.segmentCount(),
                result.totalEvaluationMillis(),
                result.profilesPerSecond(),
                result.predicateEvaluationsPerSecond(),
                result.avgIncrementalLatencyMicros(),
                seed,
                optimize
        );
        Files.writeString(outputPath, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private void createParentDirectory(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }
}
