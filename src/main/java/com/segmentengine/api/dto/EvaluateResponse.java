package com.segmentengine.api.dto;

import java.util.List;
import java.util.Map;

public record EvaluateResponse(String mode, boolean optimize, Map<String, List<Long>> membership) {
}
