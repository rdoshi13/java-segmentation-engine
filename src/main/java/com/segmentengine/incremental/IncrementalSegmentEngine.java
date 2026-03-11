package com.segmentengine.incremental;

import com.segmentengine.engine.CompiledSegment;
import com.segmentengine.engine.SegmentEngine;
import com.segmentengine.model.Profile;
import com.segmentengine.model.ProfileUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class IncrementalSegmentEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(IncrementalSegmentEngine.class);

    private final SegmentEngine segmentEngine;
    private final SegmentDependencyIndex dependencyIndex;

    public IncrementalSegmentEngine(SegmentEngine segmentEngine, SegmentDependencyIndex dependencyIndex) {
        this.segmentEngine = segmentEngine;
        this.dependencyIndex = dependencyIndex;
    }

    public IncrementalUpdateResult applyUpdate(
            List<CompiledSegment> compiledSegments,
            List<Profile> profiles,
            Map<String, Set<Long>> membership,
            ProfileUpdate update
    ) {
        Map<Long, Profile> profilesById = profiles.stream().collect(Collectors.toMap(Profile::getId, Function.identity()));
        Profile profile = profilesById.get(update.getProfileId());
        if (profile == null) {
            throw new IllegalArgumentException("Unknown profile ID: " + update.getProfileId());
        }

        Map<String, Set<String>> dependencies = dependencyIndex.dependenciesBySegment(compiledSegments);
        Set<String> impacted = dependencyIndex.impactedSegments(dependencies, update.getFieldName());

        Map<String, CompiledSegment> byName = compiledSegments.stream()
                .collect(Collectors.toMap(CompiledSegment::name, Function.identity()));

        Map<String, Boolean> before = new LinkedHashMap<>();
        Map<String, Boolean> after = new LinkedHashMap<>();

        for (String segment : impacted) {
            Set<Long> segmentMembership = membership.get(segment);
            before.put(segment, segmentMembership.contains(profile.getId()));
        }

        profile.applyFieldUpdate(update.getFieldName(), update.getNewValue());

        for (String segment : impacted) {
            CompiledSegment compiled = byName.get(segment);
            boolean matches = segmentEngine.evaluate(compiled.executableExpression(), profile);
            Set<Long> segmentMembership = membership.get(segment);
            if (matches) {
                segmentMembership.add(profile.getId());
            } else {
                segmentMembership.remove(profile.getId());
            }
            after.put(segment, matches);
        }

        LOGGER.debug(
                "Applied update profileId={} field={} impactedSegments={}",
                update.getProfileId(),
                update.getFieldName(),
                impacted.size()
        );

        return new IncrementalUpdateResult(update.getProfileId(), before, after, impacted);
    }
}
