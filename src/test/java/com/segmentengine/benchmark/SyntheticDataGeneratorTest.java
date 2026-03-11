package com.segmentengine.benchmark;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SyntheticDataGeneratorTest {
    @Test
    void sameSeedProducesSameDataset() {
        SyntheticDataGenerator generator = new SyntheticDataGenerator();

        SyntheticDataSet first = generator.generate(50, 10, 1234);
        SyntheticDataSet second = generator.generate(50, 10, 1234);

        assertEquals(first.profiles(), second.profiles());
        assertEquals(first.segments(), second.segments());
    }
}
