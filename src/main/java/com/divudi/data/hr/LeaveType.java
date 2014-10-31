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
    Lieu,
    LieuHalf,
    No_Pay,
    No_Pay_Half,
    @Deprecated
    Sick, //    
    @Deprecated
    Maternity,
    @Deprecated
    Absent,;
//    Other,;

    public double getLeaveUtilization() {
        switch (this) {
            case Annual:
            case AnnualHalf:
                return 14;
            case Casual:
            case CasualHalf:
                return 7;
            case Medical:
                return 14;
            case Maternity1st:
            case Maternity1stHalf:
                return 84;
            case Maternity2nd:
            case Maternity2ndHalf:
                return 42;
            default:
                return 0;
        }
    }
}
