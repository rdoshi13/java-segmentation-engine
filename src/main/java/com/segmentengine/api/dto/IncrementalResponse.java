package com.segmentengine.api.dto;

import java.util.List;
import java.util.Map;

public record IncrementalResponse(
        String mode,
        boolean optimize,
        List<IncrementalUpdateResponse> updates,
        Map<String, List<Long>> finalMembership
) {
}
