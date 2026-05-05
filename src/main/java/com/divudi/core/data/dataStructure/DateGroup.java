/*
 * Open Hospital Management Information System
 *
 * View model: groups RosterCells by date for the accordion-based editing UI.
 * This does NOT own the cells - it references the same RosterCell objects
 * that live inside the RosterTable, so edits here flow through to the
 * underlying table automatically.
 *
 * Package: com.divudi.core.data.dataStructure
 * File: DateGroup.java
 */
package com.divudi.core.data.dataStructure;

import com.divudi.core.entity.hr.Shift;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DateGroup implements Serializable {

    private Date date;
    private List<ShiftGroup> shiftGroups = new ArrayList<>();

    public DateGroup() {
    }

    public DateGroup(Date date) {
        this.date = date;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public List<ShiftGroup> getShiftGroups() {
        return shiftGroups;
    }

    public void setShiftGroups(List<ShiftGroup> shiftGroups) {
        this.shiftGroups = shiftGroups;
    }

    /**
     * Holds one shift's worth of data for a single date.
     * References the actual RosterCell from the underlying RosterTable
     * so edits propagate automatically.
     */
    public static class ShiftGroup implements Serializable {

        private Shift shift;
        private String shiftName;
        private RosterCell cell; // live reference

        public ShiftGroup() {
        }

        public ShiftGroup(Shift shift, String shiftName, RosterCell cell) {
            this.shift = shift;
            this.shiftName = shiftName;
            this.cell = cell;
        }

        public Shift getShift() {
            return shift;
        }

        public void setShift(Shift shift) {
            this.shift = shift;
        }

        public String getShiftName() {
            return shiftName;
        }

        public void setShiftName(String shiftName) {
            this.shiftName = shiftName;
        }

        public RosterCell getCell() {
            return cell;
        }

        public void setCell(RosterCell cell) {
            this.cell = cell;
        }
    }
}