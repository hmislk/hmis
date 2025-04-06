/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service;

import com.divudi.core.util.JsfUtil;
import com.divudi.core.entity.Staff;
import com.divudi.core.facade.StaffFacade;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 *
 * @author Dr M H B Ariyaratne, buddhika.ari@gmail.com
 *
 */
@Stateless
public class StaffService {

    @EJB
    StaffFacade facade;

    public void updateStaffCredit(Staff staff, double value) {
        if (staff == null || staff.getId() == null) {
            return;
        }
        if (staff.getCreditLimitQualified() >= staff.getCurrentCreditValue() + value) {
            staff.setCurrentCreditValue(staff.getCurrentCreditValue() + value);
            getFacade().edit(staff);

        } else {
            JsfUtil.addErrorMessage("Staff credit limit exceeded. The current credit value cannot exceed the credit limit qualified by the staff member.");
        }
    }

    public void updateStaffWelfare(Staff staff, double value) {
        if (staff == null || staff.getId() == null) {
            return;
        }
        staff.setAnnualWelfareUtilized(staff.getAnnualWelfareUtilized() + value);
        getFacade().edit(staff);
    }

    public StaffFacade getFacade() {
        return facade;
    }

    public void setFacade(StaffFacade facade) {
        this.facade = facade;
    }

}
