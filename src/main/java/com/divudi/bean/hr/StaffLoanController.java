/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.hr;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.data.hr.PaysheetComponentType;
import com.divudi.data.hr.ReportKeyWord;
import com.divudi.ejb.HumanResourceBean;
import com.divudi.entity.hr.PaysheetComponent;
import com.divudi.entity.hr.StaffPaysheetComponent;
import com.divudi.facade.PaysheetComponentFacade;
import com.divudi.facade.StaffPaysheetComponentFacade;
import java.io.Serializable;
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
    List<StaffPaysheetComponent> selectedList;
    /////////////////
    @EJB
    private StaffPaysheetComponentFacade staffPaysheetComponentFacade;
    @EJB
    private PaysheetComponentFacade paysheetComponentFacade;
    ////////
    @Inject
    private SessionController sessionController;

    ReportKeyWord reportKeyWord;
    Date fromDate;
    PaysheetComponent paysheetComponent;
    List<StaffPaysheetComponent> paysheetComponents;
    @EJB
    HumanResourceBean humanResourceBean;
    boolean chequeDetails = false;

    private boolean errorCheck() {

        if (getCurrent().getPaysheetComponent() == null) {
            UtilityController.addErrorMessage("Check Loan Name");
            return true;
        }
//        if (getCurrent().getDa() == null) {
//            UtilityController.addErrorMessage("Check Date");
//            return true;
//        }

        if (getCurrent().getStaff() == null) {
            UtilityController.addErrorMessage("Check Staff");
            return true;
        }

        if (humanResourceBean.checkStaff(getCurrent(), getCurrent().getPaysheetComponent(), getCurrent().getStaff(), getCurrent().getFromDate(), getCurrent().getToDate())) {
            UtilityController.addErrorMessage("There is Some component in Same Date Range");
            return true;
        }

        return false;
    }

    public boolean errorCheckSelected() {
        for (StaffPaysheetComponent s : selectedList) {
            //System.out.println("getChequeNumber() = " + s.getChequeNumber());
            if (s.getChequeNumber()==null || s.getChequeNumber().equals("")) {
                return true;
            }
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

    public void saveSelected() {
        if (errorCheckSelected()) {
            UtilityController.addErrorMessage("Please Check Cheque Date And Cheque No:");
            return;
        }
        
        for (StaffPaysheetComponent s : selectedList) {
            s.setChequePaidBy(getSessionController().getLoggedUser());
            s.setChequePaidDate(new Date());
            getStaffPaysheetComponentFacade().edit(s);
        }
        UtilityController.addSuccessMessage("Sucessffully Updated Selected");

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
        hm.put("tp", Arrays.asList(new PaysheetComponentType[]{PaysheetComponentType.LoanInstallemant, PaysheetComponentType.LoanNetSalary, PaysheetComponentType.Advance_Payment_Deduction}));

        return getPaysheetComponentFacade().findBySQL(sql, hm);

    }

    public void createLones() {
        String sql;
        HashMap hm = new HashMap();

        sql = "Select ss from StaffPaysheetComponent ss "
                + " where ss.retired=false "
                + " and ss.fromDate <=:fd ";

        if (paysheetComponent != null) {
            sql += " and ss.paysheetComponent=:tp ";
            hm.put("tp", getPaysheetComponent());
        } else {
            sql += " and ss.paysheetComponent.componentType in :tp ";
            hm.put("tp", Arrays.asList(new PaysheetComponentType[]{PaysheetComponentType.LoanInstallemant,
                PaysheetComponentType.LoanNetSalary,
                PaysheetComponentType.Advance_Payment_Deduction}));
        }

        if (getReportKeyWord().getStaff() != null) {
            sql += " and ss.staff=:stf ";
            hm.put("stf", getReportKeyWord().getStaff());
        }

        if (getReportKeyWord().getDepartment() != null) {
            sql += " and ss.staff.workingDepartment=:dep ";
            hm.put("dep", getReportKeyWord().getDepartment());
        }

        if (getReportKeyWord().getInstitution() != null) {
            sql += " and ss.staff.institution=:ins ";
            hm.put("ins", getReportKeyWord().getInstitution());
        }

        if (getReportKeyWord().getStaffCategory() != null) {
            sql += " and ss.staff.staffCategory=:stfCat ";
            hm.put("stfCat", getReportKeyWord().getStaffCategory());
        }

        if (getReportKeyWord().getDesignation() != null) {
            sql += " and ss.staff.designation=:des ";
            hm.put("des", getReportKeyWord().getDesignation());
        }

        if (getReportKeyWord().getRoster() != null) {
            sql += " and ss.staff.roster=:rs ";
            hm.put("rs", getReportKeyWord().getRoster());
        }

        hm.put("fd", getFromDate());

//        hm.put("tp", Arrays.asList(new PaysheetComponentType[]{PaysheetComponentType.LoanInstallemant,
//            PaysheetComponentType.LoanNetSalary,
//            PaysheetComponentType.Advance_Payment_Deduction}));
        paysheetComponents = getStaffPaysheetComponentFacade().findBySQL(sql, hm, TemporalType.DATE);
        
        chequeDetails = false;
    }

    public void createsheduleForPaidLones() {
        String sql;
        HashMap hm = new HashMap();

        sql = "Select ss from StaffPaysheetComponent ss "
                + " where ss.retired=false "
                + " and ss.fromDate <=:fd "
                + " and ss.sheduleForPaid=true ";

        if (paysheetComponent != null) {
            sql += " and ss.paysheetComponent=:tp ";
            hm.put("tp", getPaysheetComponent());
        } else {
            sql += " and ss.paysheetComponent.componentType in :tp ";
            hm.put("tp", Arrays.asList(new PaysheetComponentType[]{PaysheetComponentType.LoanInstallemant,
                PaysheetComponentType.LoanNetSalary,
                PaysheetComponentType.Advance_Payment_Deduction}));
        }

        if (getReportKeyWord().getStaff() != null) {
            sql += " and ss.staff=:stf ";
            hm.put("stf", getReportKeyWord().getStaff());
        }

        if (getReportKeyWord().getDepartment() != null) {
            sql += " and ss.staff.workingDepartment=:dep ";
            hm.put("dep", getReportKeyWord().getDepartment());
        }

        if (getReportKeyWord().getInstitution() != null) {
            sql += " and ss.staff.workingDepartment.institution=:ins ";
            hm.put("ins", getReportKeyWord().getInstitution());
        }

        if (getReportKeyWord().getStaffCategory() != null) {
            sql += " and ss.staff.staffCategory=:stfCat ";
            hm.put("stfCat", getReportKeyWord().getStaffCategory());
        }

        if (getReportKeyWord().getDesignation() != null) {
            sql += " and ss.staff.designation=:des ";
            hm.put("des", getReportKeyWord().getDesignation());
        }

        if (getReportKeyWord().getRoster() != null) {
            sql += " and ss.staff.roster=:rs ";
            hm.put("rs", getReportKeyWord().getRoster());
        }

        hm.put("fd", getFromDate());

//        hm.put("tp", Arrays.asList(new PaysheetComponentType[]{PaysheetComponentType.LoanInstallemant,
//            PaysheetComponentType.LoanNetSalary,
//            PaysheetComponentType.Advance_Payment_Deduction}));
        paysheetComponents = getStaffPaysheetComponentFacade().findBySQL(sql, hm, TemporalType.DATE);

        if (paysheetComponents.size() > 0) {
            chequeDetails = true;
        }
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

    public ReportKeyWord getReportKeyWord() {
        if (reportKeyWord == null) {
            reportKeyWord = new ReportKeyWord();
        }
        return reportKeyWord;
    }

    public void setReportKeyWord(ReportKeyWord reportKeyWord) {
        this.reportKeyWord = reportKeyWord;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public PaysheetComponent getPaysheetComponent() {
        return paysheetComponent;
    }

    public void setPaysheetComponent(PaysheetComponent paysheetComponent) {
        this.paysheetComponent = paysheetComponent;
    }

    public List<StaffPaysheetComponent> getPaysheetComponents() {
        return paysheetComponents;
    }

    public void setPaysheetComponents(List<StaffPaysheetComponent> paysheetComponents) {
        this.paysheetComponents = paysheetComponents;
    }

    public HumanResourceBean getHumanResourceBean() {
        return humanResourceBean;
    }

    public void setHumanResourceBean(HumanResourceBean humanResourceBean) {
        this.humanResourceBean = humanResourceBean;
    }

    public boolean isChequeDetails() {
        return chequeDetails;
    }

    public void setChequeDetails(boolean chequeDetails) {
        this.chequeDetails = chequeDetails;
    }

    public List<StaffPaysheetComponent> getSelectedList() {
        return selectedList;
    }

    public void setSelectedList(List<StaffPaysheetComponent> selectedList) {
        this.selectedList = selectedList;
    }
}
