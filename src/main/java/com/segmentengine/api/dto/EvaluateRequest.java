package com.segmentengine.api.dto;

import com.segmentengine.model.Profile;
import com.segmentengine.model.SegmentDefinition;

import java.util.List;

public record EvaluateRequest(List<SegmentDefinition> segments, List<Profile> profiles, Boolean optimize) {
}
