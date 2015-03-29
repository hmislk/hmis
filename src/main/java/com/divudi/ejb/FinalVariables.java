/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.ejb;

import com.divudi.bean.channel.SheduleController;
import javax.ejb.Stateless;
import javax.inject.Inject;

/**
 *
 * @author safrin
 */
@Stateless
public class FinalVariables {

    @Inject
    SheduleController sheduleController;
//    public double getMaximumWorkingHourPerWeek() {
//        return 45;
//    }
    public double getWorkingDaysPerMonth() {
        return 26;
    }

    public double getMinimumWorkingHourPerWeek() {
        return 28;
    }

    public Integer getSessionSessionDayCounter() {
        int maxRowNumber=0;
        System.out.println("maxRowNumber = " + maxRowNumber);
        maxRowNumber = getSheduleController().getCurrent().getMaxTableRows();
        System.out.println("maxRowNumber = " + maxRowNumber);
        if(getSheduleController().getCurrent().getMaxTableRows() == 0){
           return 7;
        }
        return maxRowNumber;
    }

    public double getCahnnelingDurationMinute() {
        return 10.0;
    }

    public double getOtTime() {
        return 45;
    }

    public double getOverTimeMultiply() {
        return 1.5;
    }

    public double getHoliDayAllowanceMultiply() {
        return 1.5;
    }
    
      public double getDayOffAllowanceMultiply() {
        return 1.5;
    }

      
    // Add business logic below. (Right-click in editor and choose
    // "Insert Code > Add Business Method")

    public SheduleController getSheduleController() {
        return sheduleController;
    }

    public void setSheduleController(SheduleController sheduleController) {
        this.sheduleController = sheduleController;
    }
}
