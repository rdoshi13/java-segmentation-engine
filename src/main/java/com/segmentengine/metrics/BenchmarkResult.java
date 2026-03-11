package com.segmentengine.metrics;

public record BenchmarkResult(
        long profileCount,
        long segmentCount,
        long totalEvaluationMillis,
        double profilesPerSecond,
        double predicateEvaluationsPerSecond,
        double avgIncrementalLatencyMicros
) {
}
