package com.segmentengine.benchmark;

import com.segmentengine.model.Profile;
import com.segmentengine.model.SegmentDefinition;

import java.util.List;

public record SyntheticDataSet(List<SegmentDefinition> segments, List<Profile> profiles) {
}
