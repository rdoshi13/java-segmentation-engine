package com.segmentengine.api.dto;

public record BenchmarkResponse(
        String mode,
        String preset,
        boolean optimize,
        long seed,
        long profileCount,
        long segmentCount,
        long totalEvaluationMillis,
        double profilesPerSecond,
        double predicateEvaluationsPerSecond,
        double avgIncrementalLatencyMicros
) {
}
