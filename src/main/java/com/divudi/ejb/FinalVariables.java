/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.ejb;

import javax.ejb.Stateless;

/**
 *
 * @author safrin
 */
@Stateless
public class FinalVariables {

    public double getMaximumWorkingHourPerWeek() {
        return 45;
    }

    public double getMinimumWorkingHourPerWeek() {
        return 28;
    }

    public Integer getSessionSessionDayCounter() {
        return 5;
    }

    public double getCahnnelingDurationMinute() {
        return 10.0;
    }

    public double getOtTime() {
        return 45;
    }
    // Add business logic below. (Right-click in editor and choose
    // "Insert Code > Add Business Method")
}
