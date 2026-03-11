package com.segmentengine.api.dto;

public record BenchmarkRequest(
        String preset,
        Integer profileCount,
        Integer segmentCount,
        Long seed,
        Boolean optimize
) {
}
