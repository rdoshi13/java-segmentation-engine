package com.segmentengine.benchmark;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BenchmarkPresetTest {
    @Test
    void resolvesSupportedPresetValues() {
        assertEquals(50_000, BenchmarkPreset.fromValue("50k").profileCount());
        assertEquals(100_000, BenchmarkPreset.fromValue("100K").profileCount());
        assertEquals(500_000, BenchmarkPreset.fromValue("500k").profileCount());
    }

    @Test
    void rejectsUnsupportedPreset() {
        assertThrows(IllegalArgumentException.class, () -> BenchmarkPreset.fromValue("1m"));
    }
}
