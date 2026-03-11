package com.segmentengine.cli;

import com.segmentengine.model.Profile;
import com.segmentengine.model.ProfileUpdate;
import com.segmentengine.model.SegmentDefinition;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class CliInputValidator {
    private static final Set<String> SUPPORTED_FIELDS = Set.of("age", "total_spent", "last_login_days");

    private CliInputValidator() {
    }

    static void validateSegments(List<SegmentDefinition> segments) {
        if (segments == null || segments.isEmpty()) {
            throw new IllegalArgumentException("Segments input is empty.");
        }

        Set<String> seenNames = new HashSet<>();
        for (int i = 0; i < segments.size(); i++) {
            SegmentDefinition segment = segments.get(i);
            if (segment.getName() == null || segment.getName().isBlank()) {
                throw new IllegalArgumentException("Segment at index " + i + " has empty name.");
            }
            if (segment.getRule() == null || segment.getRule().isBlank()) {
                throw new IllegalArgumentException("Segment '" + segment.getName() + "' has empty rule.");
            }
            if (!seenNames.add(segment.getName())) {
                throw new IllegalArgumentException("Duplicate segment name: " + segment.getName());
            }
        }
    }

    static void validateProfiles(List<Profile> profiles) {
        if (profiles == null || profiles.isEmpty()) {
            throw new IllegalArgumentException("Profiles input is empty.");
        }

        Set<Long> seenIds = new HashSet<>();
        for (Profile profile : profiles) {
            if (!seenIds.add(profile.getId())) {
                throw new IllegalArgumentException("Duplicate profile id: " + profile.getId());
            }
        }
    }

    static void validateUpdates(List<ProfileUpdate> updates) {
        if (updates == null || updates.isEmpty()) {
            throw new IllegalArgumentException("Updates input is empty.");
        }

        for (int i = 0; i < updates.size(); i++) {
            ProfileUpdate update = updates.get(i);
            if (update.getProfileId() <= 0) {
                throw new IllegalArgumentException("Update at index " + i + " has invalid profileId.");
            }
            if (update.getFieldName() == null || update.getFieldName().isBlank()) {
                throw new IllegalArgumentException("Update at index " + i + " has empty fieldName.");
            }
            if (!SUPPORTED_FIELDS.contains(update.getFieldName())) {
                throw new IllegalArgumentException(
                        "Update at index " + i + " uses unsupported fieldName: " + update.getFieldName()
                );
            }
        }
    }
}
