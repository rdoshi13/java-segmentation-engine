package com.segmentengine.incremental;

import com.segmentengine.engine.CompiledSegment;
import com.segmentengine.engine.DependencyExtractionVisitor;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SegmentDependencyIndex {
    public Map<String, Set<String>> dependenciesBySegment(List<CompiledSegment> segments) {
        DependencyExtractionVisitor visitor = new DependencyExtractionVisitor();
        Map<String, Set<String>> out = new LinkedHashMap<>();
        for (CompiledSegment segment : segments) {
            out.put(segment.name(), segment.executableExpression().accept(visitor));
        }
        return out;
    }

    public Set<String> impactedSegments(Map<String, Set<String>> dependenciesBySegment, String fieldName) {
        Set<String> impacted = new LinkedHashSet<>();
        for (Map.Entry<String, Set<String>> entry : dependenciesBySegment.entrySet()) {
            if (entry.getValue().contains(fieldName)) {
                impacted.add(entry.getKey());
            }
        }
        return impacted;
    }
}
