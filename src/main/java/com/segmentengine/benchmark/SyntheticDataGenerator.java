package com.segmentengine.benchmark;

import com.segmentengine.model.Profile;
import com.segmentengine.model.SegmentDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SyntheticDataGenerator {
    public SyntheticDataSet generate(int profileCount, int segmentCount, long seed) {
        Random random = new Random(seed);
        List<Profile> profiles = new ArrayList<>(profileCount);
        for (int i = 0; i < profileCount; i++) {
            profiles.add(new Profile(
                    i + 1,
                    18 + random.nextInt(53),
                    random.nextDouble() * 5000.0,
                    random.nextInt(366)
            ));
        }

        List<SegmentDefinition> segments = new ArrayList<>(segmentCount);
        for (int i = 0; i < segmentCount; i++) {
            int ageThreshold = 18 + random.nextInt(40);
            int loginThreshold = 1 + random.nextInt(365);
            int spendThreshold = 50 + random.nextInt(4500);
            String rule = "age > " + ageThreshold
                    + " AND total_spent > " + spendThreshold
                    + " AND last_login_days < " + loginThreshold;
            segments.add(new SegmentDefinition("segment_" + (i + 1), rule));
        }

        return new SyntheticDataSet(segments, profiles);
    }
}
