/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.hr;

/**
 *
 * @author Buddhika
 */
public enum LeaveType {

    Casual, //7 working Days
    CasualHalf,
    Annual, //14
    AnnualHalf,
    Medical, //14   
    Maternity1st, // 84 working days 
    Maternity2nd, // 42 working Days
    Maternity1stHalf, //For Saturday
    Maternity2ndHalf, //For Saturday    
    Lieu(true),
    LieuHalf(true),
    No_Pay(true),
    No_Pay_Half(true),
    @Deprecated
    Sick, //    
    @Deprecated
    Maternity,
    @Deprecated
    Absent,;
//    Other,;

  private  boolean exceptionalLeave = false;

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

}
