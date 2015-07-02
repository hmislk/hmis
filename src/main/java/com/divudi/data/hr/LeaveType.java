/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.hr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 *
 * @author Buddhika
 */
public enum LeaveType {

    Casual, //7 working Days
    CasualHalf(Casual),
    Annual, //14 AnnualHalf
    AnnualHalf(Annual),
    Medical, //14   
    Maternity1st, // 84 working days 
    Maternity2nd, // 42 working Days
    Maternity1stHalf(Maternity1st), //For Saturday
    Maternity2ndHalf(Maternity2nd), //For Saturday    
    DutyLeave(true),
    DutyLeaveHalf(DutyLeave, true),
    Lieu(true),
    LieuHalf(Lieu, true),
    No_Pay(true),
    No_Pay_Half(No_Pay, true),
    @Deprecated
    Sick, //    
    @Deprecated
    Maternity,
    @Deprecated
    Absent,;
//    Other,;

    private boolean exceptionalLeave = false;
    boolean autoLeave = false;

    public boolean isAutoLeave() {
        return autoLeave;
    }

    public void setAutoLeave(boolean autoLeave) {
        this.autoLeave = autoLeave;
    }
    
    

    public boolean isExceptionalLeave() {
        return exceptionalLeave;
    }

//    public double getLeaveEntitle() {
//        return leaveEntitle;
//    }
    private LeaveType(boolean exB) {
        exceptionalLeave = exB;
    }

    private LeaveType() {

    }

    private LeaveType parent = null;
    
    

    private LeaveType(LeaveType parent) {
        this.parent = parent;
        if (this.parent != null) {
            this.parent.addChild(this);
        }
    }

    private LeaveType(LeaveType parent, boolean exB) {
        this.parent = parent;
        exceptionalLeave = exB;
        if (this.parent != null) {
            this.parent.addChild(this);
        }
    }

    private final List<LeaveType> children = new ArrayList<>();

    public LeaveType[] children() {
        return children.toArray(new LeaveType[children.size()]);
    }

    public LeaveType[] allChildren() {
        List<LeaveType> list = new ArrayList<>();
        addChildren(this, list);
        return list.toArray(new LeaveType[list.size()]);
    }

    private static void addChildren(LeaveType root, List<LeaveType> list) {
        list.addAll(root.children);
        for (LeaveType child : root.children) {
            addChildren(child, list);
        }
    }

    private void addChild(LeaveType child) {
        this.children.add(child);
    }

    public LeaveType getParent() {
        return this.parent;
    }

    public boolean is(LeaveType other) {
        if (other == null) {
            return false;
        }

        for (LeaveType t = this; t != null; t = t.parent) {
            if (other == t) {
                return true;
            }
        }
        return false;
    }

    public List<LeaveType> getFullDayLeavs() {
        List<LeaveType> list = new ArrayList<>();
        list.add(LeaveType.Annual);
        list.add(LeaveType.Casual);
        list.add(LeaveType.DutyLeave);
        list.add(LeaveType.Lieu);
        list.add(LeaveType.Maternity1st);
        list.add(LeaveType.Maternity2nd);
        list.add(LeaveType.Medical);
        list.add(LeaveType.No_Pay);
        return list;
    }

    public boolean isFullDayLeave() {
        switch (this) {
            case Annual:
                return true;
            case Casual:
                return true;
            case DutyLeave:
                return true;
            case Lieu:
                return true;
            case Medical:
                return true;
            case Maternity1st:
                return true;
            case Maternity2nd:
                return true;
            case No_Pay:
                return true;
        }
        return false;
    }

    public List<LeaveType> getLeaveTypes() {
        HashSet<LeaveType> set = new HashSet<>();
        set.add(this);

        if (this.getParent() != null) {
            set.add(this.getParent());
        }

        if (this.children() != null) {
            set.addAll(Arrays.asList(this.children()));
        }

        List<LeaveType> list = new ArrayList<>();

        list.addAll(set);

        return list;
    }
}
