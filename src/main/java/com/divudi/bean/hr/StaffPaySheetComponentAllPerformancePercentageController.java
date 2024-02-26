/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.hr;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.hr.PaysheetComponentType;
import com.divudi.data.hr.ReportKeyWord;
import com.divudi.ejb.HumanResourceBean;
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
import org.primefaces.event.RowEditEvent;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class StaffPaySheetComponentAllPerformancePercentageController implements Serializable {

    private StaffPaysheetComponent current;
    private PaysheetComponent paysheetComponent;
    private Date fromDate;
    private Date toDate;
    ReportKeyWord reportKeyWord;
    ////////////////
    private List<StaffPaysheetComponent> filteredStaffPaysheet;
    private List<StaffPaysheetComponent> items;
    private List<StaffPaysheetComponent> repeatedComponent;
    private List<StaffPaysheetComponent> selectedStaffComponent;
    /////////////////
    @EJB
    private StaffPaysheetComponentFacade staffPaysheetComponentFacade;
    @EJB
    private PaysheetComponentFacade paysheetComponentFacade;
    ////////
    @Inject
    private SessionController sessionController;
    @Inject
    private StaffController staffController;

    public ReportKeyWord getReportKeyWord() {
        if (reportKeyWord == null) {
            reportKeyWord = new ReportKeyWord();
        }
        return reportKeyWord;
    }

    public void setReportKeyWord(ReportKeyWord reportKeyWord) {
        this.reportKeyWord = reportKeyWord;
    }

    public void makeItemNull() {
        items = null;
        filteredStaffPaysheet = null;
        selectedStaffComponent = null;
        reportKeyWord = null;
    }

    public void removeAll() {
        for (StaffPaysheetComponent spc : getSelectedStaffComponent()) {
            spc.setRetired(true);
            spc.setRetiredAt(new Date());
            spc.setRetirer(getSessionController().getLoggedUser());
            getStaffPaysheetComponentFacade().edit(spc);
        }

        makeNull();
    }

//    private boolean checkStaff() {
//        repeatedComponent = null;
//        for (Staff s : getStaffController().getSelectedList()) {
//            String sql = "Select s "
//                    + " From StaffPaysheetComponent s "
//                    + " where s.retired=false"
//                    + " and s.paysheetComponent=:tp "
//                    + " and s.staff=:st "
//                    + " and s.fromDate<=:cu  "
//                    + " and s.toDate>=:cu ";
//            HashMap hm = new HashMap();
//            hm.put("tp", getPaysheetComponent());
//            hm.put("st", s);
//            hm.put("cu", getToDate());
//
//            List<StaffPaysheetComponent> tmp = getStaffPaysheetComponentFacade().findByJpql(sql, hm, TemporalType.DATE);
//
//            if (!tmp.isEmpty()) {
//                getRepeatedComponent().addAll(tmp);
//            }
//
//        }
//
//        if (!getRepeatedComponent().isEmpty()) {
//            JsfUtil.addErrorMessage("There is already " + getPaysheetComponent().getName() + " defined please finalize his ending date range && add new one or remove");
//            items = null;
//            return true;
//        }
//
//        return false;
//    }
    public void onEdit(RowEditEvent event) {
        StaffPaysheetComponent tmp = (StaffPaysheetComponent) event.getObject();
        tmp.setLastEditedAt(new Date());
        tmp.setLastEditor(getSessionController().getLoggedUser());
        getStaffPaysheetComponentFacade().edit(tmp);
    }

    @EJB
    HumanResourceBean humanResourceBean;

    private boolean errorCheck() {

        if (getStaffController().getSelectedList() == null) {
            JsfUtil.addErrorMessage("Please select staff");
            return true;
        }

        if (getFromDate() == null) {
            JsfUtil.addErrorMessage("Please select From Date");
            return true;
        }

        if (getToDate() == null) {
            JsfUtil.addErrorMessage("Please select To Date");
            return true;
        }

        for (Staff staff : staffController.getSelectedList()) {
            if (staff.getTransDblValue() == 0) {
                JsfUtil.addErrorMessage("Some Staff Has No Percentage Value");
                return true;
            }

            if (humanResourceBean.checkStaff(getPaysheetComponent(), staff, getFromDate(), getToDate())) {
                JsfUtil.addErrorMessage("There is Some component in Same Date Range");
                return true;
            }
        }

//        if (checkStaff()) {
//            return true;
//        }
        return false;
    }

    public void remove() {
        getCurrent().setRetired(true);
        getCurrent().setRetiredAt(new Date());
        getCurrent().setRetirer(getSessionController().getLoggedUser());
        getStaffPaysheetComponentFacade().edit(getCurrent());

        current = null;
        items = null;
    }

    public void save() {

        if (errorCheck()) {
            return;
        }

        for (Staff s : getStaffController().getSelectedList()) {
            StaffPaysheetComponent spc = new StaffPaysheetComponent();
            spc.setCreatedAt(new Date());
            spc.setCreater(getSessionController().getLoggedUser());
            spc.setFromDate(getFromDate());
            spc.setToDate(getToDate());
            spc.setPaysheetComponent(getPaysheetComponent());
            spc.setStaff(s);
            spc.setStaffPaySheetComponentValue(s.getTransDblValue());
            getStaffPaysheetComponentFacade().create(spc);
        }

        JsfUtil.addSuccessMessage("Succesfully Saved");
//        updateExistingComponent();
        makeNullWithout();
    }

    public void makeNull() {
        current = null;
        paysheetComponent = null;
        fromDate = null;
        toDate = null;
        filteredStaffPaysheet = null;
        items = null;
        selectedStaffComponent = null;
        repeatedComponent = null;
        getStaffController().setSelectedList(null);
        getStaffController().setFilteredStaff(null);
    }

    public void makeNullWithout() {
        current = null;
        filteredStaffPaysheet = null;
        items = null;
        selectedStaffComponent = null;
        repeatedComponent = null;
        getStaffController().setSelectedList(null);
        getStaffController().setFilteredStaff(null);
    }

    public void createStaffPaysheetComponent() {
        HashMap hm = new HashMap();
        String sql = "Select ss "
                + " from StaffPaysheetComponent ss"
                + " where ss.retired=false "
                + " and ss.paysheetComponent.componentType=:pct ";
                
        if (getFromDate() != null) {
            sql += " and ((ss.fromDate <=:fd "
                    + " and ss.toDate >=:fd) or ss.fromDate >=:fd) ";
            hm.put("fd", getFromDate());
        }
        
        hm.put("pct", PaysheetComponentType.PerformanceAllowancePercentage);
        
//        if (paysheetComponent != null) {
//            sql += " and ss.paysheetComponent=:tp ";
//            hm.put("tp", getPaysheetComponent());
//        }

        if (paysheetComponent != null) {
            sql += " and ss.paysheetComponent=:tp ";
            hm.put("tp", getPaysheetComponent());
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
            sql += " and ss.staff.staffCategory=:stfCat";
            hm.put("stfCat", getReportKeyWord().getStaffCategory());
        }

        if (getReportKeyWord().getDesignation() != null) {
            sql += " and ss.staff.designation=:des";
            hm.put("des", getReportKeyWord().getDesignation());
        }

        if (getReportKeyWord().getRoster() != null) {
            sql += " and ss.staff.roster=:rs ";
            hm.put("rs", getReportKeyWord().getRoster());
        }

        items = getStaffPaysheetComponentFacade().findByJpql(sql, hm, TemporalType.DATE);

        if (!getRepeatedComponent().isEmpty()) {
            for (StaffPaysheetComponent sp : items) {
                for (StaffPaysheetComponent err : getRepeatedComponent()) {
                    if (sp.getId().equals(err.getId())) {
                        sp.setExist(true);
                        //////// // System.out.println("settin");
                    }
                }
            }
        }
    }

    public List<StaffPaysheetComponent> getItems() {

        return items;
    }

    public List<PaysheetComponent> getCompnent() {
        String sql = "Select pc From PaysheetComponent pc "
                + " where pc.retired=false"
                + " and pc.componentType not in :tp1"
                + " and pc.componentType not in :tp2";
        HashMap hm = new HashMap();
        hm.put("tp1", PaysheetComponentType.addition.getSystemDefinedComponents());
        hm.put("tp2", Arrays.asList(new PaysheetComponentType[]{PaysheetComponentType.LoanInstallemant, PaysheetComponentType.LoanNetSalary, PaysheetComponentType.Advance_Payment_Deduction}));

        return getPaysheetComponentFacade().findByJpql(sql, hm);

    }
    
    public StaffPaySheetComponentAllPerformancePercentageController() {
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

    public List<StaffPaysheetComponent> getFilteredStaffPaysheet() {
        return filteredStaffPaysheet;
    }

    public void setFilteredStaffPaysheet(List<StaffPaysheetComponent> filteredStaffPaysheet) {
        this.filteredStaffPaysheet = filteredStaffPaysheet;
    }

    public PaysheetComponent getPaysheetComponent() {
        if (paysheetComponent == null) {
            paysheetComponent = humanResourceBean.getComponent(getSessionController().getLoggedUser(), PaysheetComponentType.PerformanceAllowancePercentage);
        }
        return paysheetComponent;
    }

    public void setPaysheetComponent(PaysheetComponent paysheetComponent) {
        this.paysheetComponent = paysheetComponent;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public List<StaffPaysheetComponent> getRepeatedComponent() {
        if (repeatedComponent == null) {
            repeatedComponent = new ArrayList<>();
        }
        return repeatedComponent;
    }

    public void setRepeatedComponent(List<StaffPaysheetComponent> repeatedComponent) {
        this.repeatedComponent = repeatedComponent;
    }

    public StaffController getStaffController() {
        return staffController;
    }

    public void setStaffController(StaffController staffController) {
        this.staffController = staffController;
    }

    public List<StaffPaysheetComponent> getSelectedStaffComponent() {
        if (selectedStaffComponent == null) {
            selectedStaffComponent = new ArrayList<>();
        }
        return selectedStaffComponent;
    }

    public void setSelectedStaffComponent(List<StaffPaysheetComponent> selectedStaffComponent) {
        this.selectedStaffComponent = selectedStaffComponent;
    }
}
