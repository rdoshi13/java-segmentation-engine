package com.segmentengine.api.dto;

import java.util.List;
import java.util.Map;

public record IncrementalUpdateResponse(
        long profileId,
        String fieldName,
        Map<String, Boolean> before,
        Map<String, Boolean> after,
        List<String> impactedSegments
) {
}
