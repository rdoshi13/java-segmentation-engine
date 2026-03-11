package com.segmentengine.model;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.Objects;

public class Profile {
    private long id;
    private int age;
    @JsonAlias("total_spent")
    private double totalSpent;
    @JsonAlias("last_login_days")
    private int lastLoginDays;

    public Profile() {
    }

    public Profile(long id, int age, double totalSpent, int lastLoginDays) {
        this.id = id;
        this.age = age;
        this.totalSpent = totalSpent;
        this.lastLoginDays = lastLoginDays;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public double getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(double totalSpent) {
        this.totalSpent = totalSpent;
    }

    public int getLastLoginDays() {
        return lastLoginDays;
    }

    public void setLastLoginDays(int lastLoginDays) {
        this.lastLoginDays = lastLoginDays;
    }

    public void applyFieldUpdate(String fieldName, double newValue) {
        switch (fieldName) {
            case "age" -> setAge((int) newValue);
            case "total_spent" -> setTotalSpent(newValue);
            case "last_login_days" -> setLastLoginDays((int) newValue);
            default -> throw new IllegalArgumentException("Unsupported field: " + fieldName);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Profile profile)) {
            return false;
        }
        return id == profile.id
                && age == profile.age
                && Double.compare(totalSpent, profile.totalSpent) == 0
                && lastLoginDays == profile.lastLoginDays;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, age, totalSpent, lastLoginDays);
    }
}
