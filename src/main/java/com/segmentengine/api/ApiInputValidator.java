package com.segmentengine.api;

import com.segmentengine.model.Profile;
import com.segmentengine.model.ProfileUpdate;
import com.segmentengine.model.SegmentDefinition;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class ApiInputValidator {
    private static final Set<String> SUPPORTED_FIELDS = Set.of("age", "total_spent", "last_login_days");

    private ApiInputValidator() {
    }

    static void validateSegments(List<SegmentDefinition> segments) {
        if (segments == null || segments.isEmpty()) {
            throw new IllegalArgumentException("segments must not be empty.");
        }

        Set<String> seenNames = new HashSet<>();
        for (int i = 0; i < segments.size(); i++) {
            SegmentDefinition segment = segments.get(i);
            if (segment.getName() == null || segment.getName().isBlank()) {
                throw new IllegalArgumentException("segments[" + i + "].name must not be empty.");
            }
            if (segment.getRule() == null || segment.getRule().isBlank()) {
                throw new IllegalArgumentException("segments[" + i + "].rule must not be empty.");
            }
            if (!seenNames.add(segment.getName())) {
                throw new IllegalArgumentException("duplicate segment name: " + segment.getName());
            }
        }
    }

    static void validateProfiles(List<Profile> profiles) {
        if (profiles == null || profiles.isEmpty()) {
            throw new IllegalArgumentException("profiles must not be empty.");
        }

        Set<Long> seenIds = new HashSet<>();
        for (Profile profile : profiles) {
            if (!seenIds.add(profile.getId())) {
                throw new IllegalArgumentException("duplicate profile id: " + profile.getId());
            }
        }
    }

    static void validateUpdates(List<ProfileUpdate> updates) {
        if (updates == null || updates.isEmpty()) {
            throw new IllegalArgumentException("updates must not be empty.");
        }
        for (int i = 0; i < updates.size(); i++) {
            ProfileUpdate update = updates.get(i);
            if (update.getProfileId() <= 0) {
                throw new IllegalArgumentException("updates[" + i + "].profileId must be positive.");
            }
            if (update.getFieldName() == null || update.getFieldName().isBlank()) {
                throw new IllegalArgumentException("updates[" + i + "].fieldName must not be empty.");
            }
            if (!SUPPORTED_FIELDS.contains(update.getFieldName())) {
                throw new IllegalArgumentException(
                        "updates[" + i + "].fieldName is unsupported: " + update.getFieldName()
                );
            }
        }
    }
}
