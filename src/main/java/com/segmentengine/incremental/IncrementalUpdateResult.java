package com.segmentengine.incremental;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class IncrementalUpdateResult {
    private final long profileId;
    private final Map<String, Boolean> beforeMatchBySegment;
    private final Map<String, Boolean> afterMatchBySegment;
    private final Set<String> impactedSegments;

    public IncrementalUpdateResult(
            long profileId,
            Map<String, Boolean> beforeMatchBySegment,
            Map<String, Boolean> afterMatchBySegment,
            Set<String> impactedSegments
    ) {
        this.profileId = profileId;
        this.beforeMatchBySegment = new LinkedHashMap<>(beforeMatchBySegment);
        this.afterMatchBySegment = new LinkedHashMap<>(afterMatchBySegment);
        this.impactedSegments = Set.copyOf(impactedSegments);
    }

    public long getProfileId() {
        return profileId;
    }

    public Map<String, Boolean> getBeforeMatchBySegment() {
        return beforeMatchBySegment;
    }

    public Map<String, Boolean> getAfterMatchBySegment() {
        return afterMatchBySegment;
    }

    public Set<String> getImpactedSegments() {
        return impactedSegments;
    }
}
