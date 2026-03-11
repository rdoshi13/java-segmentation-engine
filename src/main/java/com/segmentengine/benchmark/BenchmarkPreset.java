package com.segmentengine.benchmark;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public enum BenchmarkPreset {
    P50K("50k", 50_000, 100),
    P100K("100k", 100_000, 100),
    P500K("500k", 500_000, 100);

    private final String label;
    private final int profileCount;
    private final int segmentCount;

    BenchmarkPreset(String label, int profileCount, int segmentCount) {
        this.label = label;
        this.profileCount = profileCount;
        this.segmentCount = segmentCount;
    }

    public String label() {
        return label;
    }

    public int profileCount() {
        return profileCount;
    }

    public int segmentCount() {
        return segmentCount;
    }

    public static BenchmarkPreset fromValue(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        for (BenchmarkPreset preset : values()) {
            if (preset.label.equals(normalized)) {
                return preset;
            }
        }
        throw new IllegalArgumentException(
                "Unsupported benchmark preset: " + value + ". Supported: "
                        + Arrays.stream(values()).map(BenchmarkPreset::label).collect(Collectors.joining(", "))
        );
    }
}
