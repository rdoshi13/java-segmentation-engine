package com.segmentengine.model;

import java.util.Objects;

public class SegmentDefinition {
    private String name;
    private String rule;

    public SegmentDefinition() {
    }

    public SegmentDefinition(String name, String rule) {
        this.name = name;
        this.rule = rule;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SegmentDefinition that)) {
            return false;
        }
        return Objects.equals(name, that.name) && Objects.equals(rule, that.rule);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, rule);
    }
}
