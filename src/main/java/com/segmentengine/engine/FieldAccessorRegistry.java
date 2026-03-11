package com.segmentengine.engine;

import com.segmentengine.model.Profile;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class FieldAccessorRegistry {
    private final Map<String, Function<Profile, Object>> accessors;

    public FieldAccessorRegistry() {
        Map<String, Function<Profile, Object>> map = new HashMap<>();
        map.put("age", profile -> profile.getAge());
        map.put("total_spent", profile -> profile.getTotalSpent());
        map.put("last_login_days", profile -> profile.getLastLoginDays());
        this.accessors = Collections.unmodifiableMap(map);
    }

    public Map<String, Function<Profile, Object>> accessors() {
        return accessors;
    }

    public Object read(String fieldName, Profile profile) {
        Function<Profile, Object> accessor = accessors.get(fieldName);
        if (accessor == null) {
            throw new IllegalArgumentException("Unsupported field: " + fieldName);
        }
        return accessor.apply(profile);
    }
}
