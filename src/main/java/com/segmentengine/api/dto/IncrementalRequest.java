package com.segmentengine.api.dto;

import com.segmentengine.model.Profile;
import com.segmentengine.model.ProfileUpdate;
import com.segmentengine.model.SegmentDefinition;

import java.util.List;

public record IncrementalRequest(
        List<SegmentDefinition> segments,
        List<Profile> profiles,
        List<ProfileUpdate> updates,
        Boolean optimize
) {
}
