/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data.dataStructure;

import com.divudi.entity.hr.StaffShift;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 * @author safrin
 */
public class ShiftTable {

    private List<StaffShift> staffShift;
    private Date date;   
    boolean flag;
    private boolean ph;
    private boolean merch;

    public boolean isFlag() {
        return flag;
    }
    
     public boolean getFlag() {
        return flag;
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
    }
   
    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public List<StaffShift> getStaffShift() {
        if (staffShift == null) {
            staffShift = new ArrayList<>();
        }
        return staffShift;
    }

    public void setStaffShift(List<StaffShift> staffShift) {
        this.staffShift = staffShift;
    }

    public boolean isPh() {
        return ph;
    }

    public void setPh(boolean ph) {
        this.ph = ph;
    }

    public boolean isMerch() {
        return merch;
    }

    public void setMerch(boolean merch) {
        this.merch = merch;
    }

}
