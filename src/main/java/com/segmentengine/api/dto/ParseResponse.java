package com.segmentengine.api.dto;

import java.util.List;

public record ParseResponse(String mode, boolean optimize, List<ParseSegmentResponse> segments) {
}
