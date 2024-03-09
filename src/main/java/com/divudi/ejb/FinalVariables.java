/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ejb;

import com.divudi.bean.channel.ChannelScheduleController;
import com.divudi.bean.common.SessionController;
import com.divudi.data.ApplicationInstitution;
import com.divudi.entity.ServiceSession;
import com.divudi.facade.ServiceSessionFacade;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;

/**
 *
 * @author safrin
 */
@Stateless
public class FinalVariables {

    @Inject
    ChannelScheduleController sheduleController;
    @Inject
    SessionController sessionController;
    @EJB
    ServiceSessionFacade serviceSessionFacade;
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
        int maxRowNumber = 0;
        ////// // System.out.println("maxRowNumber = " + maxRowNumber);
        maxRowNumber = getSheduleController().getCurrent().getMaxTableRows();
        ////// // System.out.println("maxRowNumber = " + maxRowNumber);
        if (maxRowNumber == 0) {
            return 14;
        }
        return maxRowNumber;
    }

    public Integer getSessionSessionDayCounter(List<ServiceSession> inputSessions) {
        int maxRowNumber = 0;
        ////// // System.out.println("maxRowNumber = " + maxRowNumber);
        for (ServiceSession ss : inputSessions) {
            maxRowNumber = ss.getMaxTableRows();
            ////// // System.out.println("maxRowNumber = " + maxRowNumber);
            if (maxRowNumber == 0) {
                return 14;
            }
            return maxRowNumber;
        }

        return maxRowNumber;
    }

    public Integer getSessionSessionDayCounterLargest(List<ServiceSession> inputSessions) {
        int maxRowNumber = 0;
        for (ServiceSession ss : inputSessions) {
            if (maxRowNumber < ss.getMaxTableRows()) {
                maxRowNumber = ss.getMaxTableRows();
            }
        }
        if (maxRowNumber == 0) {
            maxRowNumber = 28;
        }
        return maxRowNumber;
    }

//    public Integer getSessionSessionDayCounterLargestById(List<Long> inputSessions) {
//        int maxRowNumber = 0;
//        for (Long s : inputSessions) {
//            ServiceSession ss = serviceSessionFacade.find(s);
////            //// // System.out.println("maxRowNumber = " + maxRowNumber);
////            //// // System.out.println("ss.getMaxTableRows() = " + ss.getMaxTableRows());
//            if (maxRowNumber < ss.getMaxTableRows()) {
//                maxRowNumber = ss.getMaxTableRows();
////                //// // System.out.println("maxRowNumber = " + maxRowNumber);
//            }
//        }
//        if (maxRowNumber != 0) {
//            return maxRowNumber;
//        } else {
//            maxRowNumber = 14;
//            return maxRowNumber;
//        }
//    }

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

    public double getVATPercentage() {
        //VAT 15% Only Vat Value
        if (sessionController.getLoggedPreference().getApplicationInstitution() == ApplicationInstitution.Ruhuna) {
//            return 0.152;
            return 0.15;
//            return 0;
        } else if (sessionController.getLoggedPreference().getApplicationInstitution() == ApplicationInstitution.Cooperative
                || sessionController.getLoggedPreference().getApplicationInstitution() == ApplicationInstitution.Arogya) {
            return 0.15;
//            return 0;
        } else {
            return 0;
        }
    }

    public double getVATPercentageWithAmount() {
        //VAT 15% With Total
        if (sessionController.getLoggedPreference().getApplicationInstitution() == ApplicationInstitution.Ruhuna) {
//            return 1.152;
            return 1;
//            return 1;
        } else if (sessionController.getLoggedPreference().getApplicationInstitution() == ApplicationInstitution.Cooperative
                || sessionController.getLoggedPreference().getApplicationInstitution() == ApplicationInstitution.Arogya) {
            return 1;
//            return 1;
        } else {
            return 1;
        }
    }

    // Add business logic below. (Right-click in editor and choose
    // "Insert Code > Add Business Method")
    public ChannelScheduleController getSheduleController() {
        return sheduleController;
    }

    public void setSheduleController(ChannelScheduleController sheduleController) {
        this.sheduleController = sheduleController;
    }
}
