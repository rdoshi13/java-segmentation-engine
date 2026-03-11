package com.segmentengine.engine;

import com.segmentengine.dsl.Parser;
import com.segmentengine.model.Profile;
import com.segmentengine.model.SegmentDefinition;
import com.segmentengine.optimizer.OptimizerFactory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SegmentEngineTest {
    @Test
    void evaluatesSegmentMembershipAcrossProfiles() {
        List<SegmentDefinition> segments = List.of(
                new SegmentDefinition("high_value", "age > 25 AND total_spent >= 1000 AND last_login_days < 30"),
                new SegmentDefinition("young_active", "age <= 30 AND last_login_days < 10")
        );
        List<Profile> profiles = List.of(
                new Profile(1, 26, 1500, 20),
                new Profile(2, 22, 3000, 2),
                new Profile(3, 40, 800, 50),
                new Profile(4, 29, 1200, 3)
        );

        SegmentCompiler compiler = new SegmentCompiler(new Parser(), OptimizerFactory.defaultOptimizer(), new FieldAccessorRegistry());
        SegmentEngine engine = new SegmentEngine(new FieldAccessorRegistry());

        Map<String, Set<Long>> membership = engine.evaluateAllSegments(compiler.compile(segments, false), profiles);

        assertEquals(Set.of(1L, 4L), membership.get("high_value"));
        assertEquals(Set.of(2L, 4L), membership.get("young_active"));
    }
}
