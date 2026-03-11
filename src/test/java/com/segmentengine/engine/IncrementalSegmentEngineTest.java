package com.segmentengine.engine;

import com.segmentengine.dsl.Parser;
import com.segmentengine.incremental.IncrementalSegmentEngine;
import com.segmentengine.incremental.IncrementalUpdateResult;
import com.segmentengine.incremental.SegmentDependencyIndex;
import com.segmentengine.model.Profile;
import com.segmentengine.model.ProfileUpdate;
import com.segmentengine.model.SegmentDefinition;
import com.segmentengine.optimizer.OptimizerFactory;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IncrementalSegmentEngineTest {
    @Test
    void updatesOnlyImpactedSegmentsForProfileChange() {
        List<SegmentDefinition> segments = List.of(
                new SegmentDefinition("high_value", "age > 25 AND total_spent >= 1000 AND last_login_days < 30"),
                new SegmentDefinition("young_active", "age <= 30 AND last_login_days < 10")
        );
        List<Profile> profiles = new ArrayList<>(List.of(
                new Profile(1, 26, 1500, 20),
                new Profile(2, 22, 3000, 2)
        ));

        SegmentCompiler compiler = new SegmentCompiler(new Parser(), OptimizerFactory.defaultOptimizer());
        SegmentEngine engine = new SegmentEngine(new FieldAccessorRegistry());
        List<CompiledSegment> compiled = compiler.compile(segments, false);

        Map<String, Set<Long>> membership = engine.evaluateAllSegments(compiled, profiles);
        IncrementalSegmentEngine incremental = new IncrementalSegmentEngine(engine, new SegmentDependencyIndex());

        IncrementalUpdateResult result = incremental.applyUpdate(
                compiled,
                profiles,
                membership,
                new ProfileUpdate(1, "total_spent", 500)
        );

        assertEquals(Set.of("high_value"), result.getImpactedSegments());
        assertTrue(result.getBeforeMatchBySegment().get("high_value"));
        assertEquals(false, result.getAfterMatchBySegment().get("high_value"));
        assertEquals(Set.of(), membership.get("high_value"));
        assertEquals(Set.of(2L), membership.get("young_active"));
    }
}
