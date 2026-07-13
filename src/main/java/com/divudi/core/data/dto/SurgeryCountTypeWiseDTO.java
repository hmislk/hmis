package com.divudi.core.data.dto;

import java.util.HashMap;
import java.util.Map;

public class SurgeryCountTypeWiseDTO {

    private String monthString;
    private int monthIndex;
    private Map<String, Integer> categoryCounts = new HashMap<>();
    private int totalCount = 0;

    public SurgeryCountTypeWiseDTO(String monthString, int monthIndex) {
        this.monthString = monthString;
        this.monthIndex = monthIndex;
    }

    public int getCount(String category) {
        return categoryCounts.getOrDefault(category, 0);
    }

    public void addCount(String category, int count) {
        categoryCounts.merge(category, count, Integer::sum);
        totalCount += count;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public String getMonthString() {
        return monthString;
    }

    public void setMonthString(String monthString) {
        this.monthString = monthString;
    }

    public int getMonthIndex() {
        return monthIndex;
    }

    public void setMonthIndex(int monthIndex) {
        this.monthIndex = monthIndex;
    }

    public Map<String, Integer> getCategoryCounts() {
        return categoryCounts;
    }

    public void setCategoryCounts(Map<String, Integer> categoryCounts) {
        this.categoryCounts = categoryCounts;
    }
}
