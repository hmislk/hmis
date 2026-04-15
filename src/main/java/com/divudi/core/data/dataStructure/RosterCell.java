/*
 * Open Hospital Management Information System
 *
 * One cell in the roster table = one shift on one date.
 * Contains the list of staff assigned to this shift on this date.
 *
 * Package: com.divudi.core.data.dataStructure
 * File: RosterCell.java
 */
package com.divudi.core.data.dataStructure;

import com.divudi.core.entity.Staff;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * One cell = one shift on one date.
 * Contains assigned staff list and metadata.
 */
public class RosterCell implements Serializable {

    private Date date;
    private int requiredCount;
    private List<Staff> assignedStaff;
    private boolean understaffed;
    private String dayType;

    public RosterCell() {
        this.assignedStaff = new ArrayList<>();
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public int getRequiredCount() {
        return requiredCount;
    }

    public void setRequiredCount(int requiredCount) {
        this.requiredCount = requiredCount;
    }

    public List<Staff> getAssignedStaff() {
        return assignedStaff;
    }

    public void setAssignedStaff(List<Staff> assignedStaff) {
        this.assignedStaff = assignedStaff;
    }

    public boolean isUnderstaffed() {
        return assignedStaff.size() < requiredCount;
    }

    public void setUnderstaffed(boolean understaffed) {
        this.understaffed = understaffed;
    }

    public String getDayType() {
        return dayType;
    }

    public void setDayType(String dayType) {
        this.dayType = dayType;
    }

    /**
     * Returns assigned staff names as a comma-separated string.
     * Convenient for simple rendering in XHTML.
     */
    public String getStaffNames() {
        if (assignedStaff == null || assignedStaff.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < assignedStaff.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            Staff s = assignedStaff.get(i);
            if (s.getPerson() != null && s.getPerson().getName() != null) {
                sb.append(s.getPerson().getName());
            }
        }
        return sb.toString();
    }
}