package com.segmentengine.api.dto;

import com.segmentengine.model.SegmentDefinition;

import java.util.List;

public record ParseRequest(List<SegmentDefinition> segments, Boolean optimize) {
}
