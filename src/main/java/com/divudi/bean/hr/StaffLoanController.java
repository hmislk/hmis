/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.hr;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.data.hr.PaysheetComponentType;
import com.divudi.entity.Staff;
import com.divudi.entity.hr.PaysheetComponent;
import com.divudi.entity.hr.StaffPaysheetComponent;
import com.divudi.facade.PaysheetComponentFacade;
import com.divudi.facade.StaffPaysheetComponentFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class StaffLoanController implements Serializable {

    private StaffPaysheetComponent current;
    ////////////////
    private List<StaffPaysheetComponent> filteredStaff;
    private List<StaffPaysheetComponent> items;
    /////////////////
    @EJB
    private StaffPaysheetComponentFacade staffPaysheetComponentFacade;
    @EJB
    private PaysheetComponentFacade paysheetComponentFacade;
    ////////
    @Inject
    private SessionController sessionController;

    private boolean errorCheck() {

        if (getCurrent().getPaysheetComponent() == null) {
            UtilityController.addErrorMessage("Check Loan Name");
            return true;
        }
        if (getCurrent().getDateAffectFrom() == null) {
            UtilityController.addErrorMessage("Check Date");
            return true;
        }

        if (getCurrent().getStaff() == null) {
            UtilityController.addErrorMessage("Check Staff");
            return true;
        }

        return false;
    }

    public void remove() {
        getCurrent().setRetired(true);
        getCurrent().setRetiredAt(new Date());
        getCurrent().setRetirer(getSessionController().getLoggedUser());
        getStaffPaysheetComponentFacade().edit(getCurrent());

       makeNull();
    }

    public void save() {
        if (errorCheck()) {
            return;
        }


        if (getCurrent().getId() == null) {
            getStaffPaysheetComponentFacade().create(getCurrent());
        } else {
            getStaffPaysheetComponentFacade().edit(getCurrent());
        }

        makeNull();
    }

    public void makeNull() {
        current = null;
        items = null;
        filteredStaff = null;
    }

    public List<StaffPaysheetComponent> getItems() {
        if (items == null) {
            String sql = "Select s from StaffPaysheetComponent s"
                    + " where s.retired=false "
                    + " and s.paysheetComponent.componentType in :tp ";
            HashMap hm = new HashMap();
//            hm.put("current", new Date());
            hm.put("tp", Arrays.asList(new PaysheetComponentType[]{PaysheetComponentType.LoanInstallemant,
                PaysheetComponentType.LoanNetSalary,
                PaysheetComponentType.Advance_Payment_Deduction}));

            items = getStaffPaysheetComponentFacade().findBySQL(sql, hm, TemporalType.DATE);
        }

        return items;
    }

    public List<PaysheetComponent> getCompnent() {
        String sql = "Select pc From PaysheetComponent pc "
                + " where pc.retired=false "
                + " and pc.componentType in :tp";
        HashMap hm = new HashMap();
        hm.put("tp", Arrays.asList(new PaysheetComponentType[]{PaysheetComponentType.LoanInstallemant,PaysheetComponentType.LoanNetSalary,PaysheetComponentType.Advance_Payment_Deduction}));

        return getPaysheetComponentFacade().findBySQL(sql, hm);

    }

    public StaffLoanController() {
    }

    public StaffPaysheetComponent getCurrent() {
        if (current == null) {
            current = new StaffPaysheetComponent();
        }
        return current;
    }

    public void setCurrent(StaffPaysheetComponent current) {
        this.current = current;
    }

    public StaffPaysheetComponentFacade getStaffPaysheetComponentFacade() {
        return staffPaysheetComponentFacade;
    }

    public void setStaffPaysheetComponentFacade(StaffPaysheetComponentFacade staffPaysheetComponentFacade) {
        this.staffPaysheetComponentFacade = staffPaysheetComponentFacade;
    }

    public PaysheetComponentFacade getPaysheetComponentFacade() {
        return paysheetComponentFacade;
    }

    public void setPaysheetComponentFacade(PaysheetComponentFacade paysheetComponentFacade) {
        this.paysheetComponentFacade = paysheetComponentFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public List<StaffPaysheetComponent> getFilteredStaff() {
        return filteredStaff;
    }

    public void setFilteredStaff(List<StaffPaysheetComponent> filteredStaff) {
        this.filteredStaff = filteredStaff;
    }
}
