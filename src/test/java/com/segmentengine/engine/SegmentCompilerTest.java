package com.segmentengine.engine;

import com.segmentengine.dsl.Parser;
import com.segmentengine.model.SegmentDefinition;
import com.segmentengine.optimizer.OptimizerFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SegmentCompilerTest {
    @Test
    void rejectsUnsupportedFieldsAtCompileTime() {
        SegmentCompiler compiler = new SegmentCompiler(
                new Parser(),
                OptimizerFactory.defaultOptimizer(),
                new FieldAccessorRegistry()
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> compiler.compile(List.of(new SegmentDefinition("bad", "age > 20 AND country > 1")), false)
        );

        assertTrue(exception.getMessage().contains("unsupported fields"));
        assertTrue(exception.getMessage().contains("country"));
    }
}
