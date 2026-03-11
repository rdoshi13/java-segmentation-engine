package com.segmentengine.engine;

import com.segmentengine.model.Profile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FieldAccessorRegistryTest {
    @Test
    void readsKnownFields() {
        FieldAccessorRegistry registry = new FieldAccessorRegistry();
        Profile profile = new Profile(10, 33, 200.5, 4);

        assertEquals(33, registry.read("age", profile));
        assertEquals(200.5, registry.read("total_spent", profile));
        assertEquals(4, registry.read("last_login_days", profile));
    }

    @Test
    void rejectsUnknownField() {
        FieldAccessorRegistry registry = new FieldAccessorRegistry();
        Profile profile = new Profile(10, 33, 200.5, 4);
        assertThrows(IllegalArgumentException.class, () -> registry.read("country", profile));
    }
}
