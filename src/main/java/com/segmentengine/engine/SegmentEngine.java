package com.segmentengine.engine;

import com.segmentengine.model.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class SegmentEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(SegmentEngine.class);

    private final FieldAccessorRegistry fieldAccessorRegistry;

    public SegmentEngine(FieldAccessorRegistry fieldAccessorRegistry) {
        this.fieldAccessorRegistry = fieldAccessorRegistry;
    }

    public Map<String, Set<Long>> evaluateAllSegments(List<CompiledSegment> segments, List<Profile> profiles) {
        Map<String, Set<Long>> membership = new LinkedHashMap<>();
        for (CompiledSegment segment : segments) {
            Set<Long> matches = new TreeSet<>();
            for (Profile profile : profiles) {
                if (evaluate(segment.executableExpression(), profile)) {
                    matches.add(profile.getId());
                }
            }
            membership.put(segment.name(), matches);
        }
        LOGGER.debug("Evaluated {} segments across {} profiles", segments.size(), profiles.size());
        return membership;
    }

    public boolean evaluate(com.segmentengine.dsl.ast.Expression expression, Profile profile) {
        return expression.accept(new EvaluationVisitor(profile, fieldAccessorRegistry));
    }
}
