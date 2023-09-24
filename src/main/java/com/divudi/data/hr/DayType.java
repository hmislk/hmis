package com.divudi.data.hr;

public enum DayType {
    Normal("Normal Day"),
    DayOff("Day Off"),
    SleepingDay("Sleeping Day"),
    Poya("Poya Day"),
    PublicHoliday("Public Holiday"),
    MurchantileHoliday("Merchantile Holiday"),
    Weekday("Weekday", true),
    Saturday("Saturday", true),
    Sunday("Sunday", true),
    Holiday("Holiday", true),
    All("All", true),
    Extra("Extra Day");

    private final String label;
    private final boolean deprecated;

    DayType(String label) {
        this(label, false);
    }

    DayType(String label, boolean deprecated) {
        this.label = label;
        this.deprecated = deprecated;
    }

    public String getLabel() {
        return label;
    }

    public boolean isDeprecated() {
        return deprecated;
    }
}
