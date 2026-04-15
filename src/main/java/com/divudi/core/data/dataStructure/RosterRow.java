/*
 * Open Hospital Management Information System
 *
 * One row in the roster table = one shift.
 *
 * Package: com.divudi.core.data.dataStructure
 * File: RosterRow.java
 */
package com.divudi.core.data.dataStructure;

import com.divudi.core.entity.hr.Shift;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * One row = one shift across all dates.
 */
public class RosterRow implements Serializable {

    private Shift shift;
    private String shiftName;
    private int shiftOrder;
    private boolean dayOff;
    private List<RosterCell> cells;  // one per date, same order as RosterTable.dates

    public RosterRow() {
        this.cells = new ArrayList<>();
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

    public int getShiftOrder() {
        return shiftOrder;
    }

    public void setShiftOrder(int shiftOrder) {
        this.shiftOrder = shiftOrder;
    }

    public boolean isDayOff() {
        return dayOff;
    }

    public void setDayOff(boolean dayOff) {
        this.dayOff = dayOff;
    }

    public List<RosterCell> getCells() {
        return cells;
    }

    public void setCells(List<RosterCell> cells) {
        this.cells = cells;
    }
}