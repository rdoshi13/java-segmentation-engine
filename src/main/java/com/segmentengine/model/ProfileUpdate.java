package com.segmentengine.model;

import com.fasterxml.jackson.annotation.JsonAlias;

public class ProfileUpdate {
    private long profileId;
    private String fieldName;
    @JsonAlias("new_value")
    private double newValue;

    public ProfileUpdate() {
    }

    public ProfileUpdate(long profileId, String fieldName, double newValue) {
        this.profileId = profileId;
        this.fieldName = fieldName;
        this.newValue = newValue;
    }

    public long getProfileId() {
        return profileId;
    }

    public void setProfileId(long profileId) {
        this.profileId = profileId;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public double getNewValue() {
        return newValue;
    }

    public void setNewValue(double newValue) {
        this.newValue = newValue;
    }
}
