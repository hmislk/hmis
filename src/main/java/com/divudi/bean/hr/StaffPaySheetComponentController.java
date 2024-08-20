/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.hr;

import com.divudi.bean.common.SessionController;

import com.divudi.data.hr.PaysheetComponentType;
import com.divudi.data.hr.ReportKeyWord;
import com.divudi.ejb.HumanResourceBean;
import com.divudi.entity.Staff;
import com.divudi.entity.hr.PaysheetComponent;
import com.divudi.entity.hr.StaffPaysheetComponent;
import com.divudi.facade.PaysheetComponentFacade;
import com.divudi.facade.StaffPaysheetComponentFacade;
import com.divudi.bean.common.util.JsfUtil;
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
import org.primefaces.event.RowEditEvent;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class StaffPaySheetComponentController implements Serializable {

    private StaffPaysheetComponent current;
    private StaffPaysheetComponent removingEntry;
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
    Date fromDate;
    Date toDate;
    ReportKeyWord reportKeyWord;

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

    public ReportKeyWord getReportKeyWord() {
        if (reportKeyWord == null) {
            reportKeyWord = new ReportKeyWord();
        }
        return reportKeyWord;
    }

    public void setReportKeyWord(ReportKeyWord reportKeyWord) {
        this.reportKeyWord = reportKeyWord;
    }

    public void onEdit(RowEditEvent event) {
        StaffPaysheetComponent tmp = (StaffPaysheetComponent) event.getObject();
        tmp.setLastEditedAt(new Date());
        tmp.setLastEditor(getSessionController().getLoggedUser());
        getStaffPaysheetComponentFacade().edit(tmp);

    }

//    private boolean checkStaff() {
//
//        String sql = "Select s From StaffPaysheetComponent s "
//                + " where s.retired=false"
//                + " and s.paysheetComponent=:tp "
//                + " and s.staff=:st "
//                + " and s.fromDate<=:cu  "
//                + " and s.toDate>=:cu ";
//        HashMap hm = new HashMap();
//        hm.put("tp", getCurrent().getPaysheetComponent());
//        hm.put("st", getCurrent().getStaff());
//        hm.put("cu", getCurrent().getToDate());
//        List<StaffPaysheetComponent> tmp = getStaffPaysheetComponentFacade().findByJpql(sql, hm, TemporalType.DATE);
//
//        if (!tmp.isEmpty()) {
//            JsfUtil.addErrorMessage("There is already Component define for " + getCurrent().getStaff().getPerson().getNameWithTitle() + " for this date range u can edit or remove add new one ");
//            return true;
//        }
//
//        return false;
//    }
    private boolean errorCheck() {

        if (getCurrent().getPaysheetComponent() == null) {
            JsfUtil.addErrorMessage("Check Component Name");
            return true;
        }
        if (getCurrent().getToDate() == null || getCurrent().getFromDate() == null) {
            JsfUtil.addErrorMessage("Check Date");
            return true;
        }

        if (getCurrent().getStaff() == null) {
            JsfUtil.addErrorMessage("Check Staff");
            return true;
        }

//        if (checkStaff()) {
//            return true;
//        }
        
       

        if (humanResourceBean.checkStaff(getCurrent(), getCurrent().getPaysheetComponent(), getCurrent().getStaff(), getCurrent().getFromDate(), getCurrent().getToDate())) {
            JsfUtil.addErrorMessage("There is Some component in Same Date Range");
            return true;
        }

        return false;
    }

    @EJB
    HumanResourceBean humanResourceBean;
    
    public void remove() {
        getRemovingEntry().setRetired(true);
        getRemovingEntry().setRetiredAt(new Date());
        getRemovingEntry().setRetirer(getSessionController().getLoggedUser());
        getStaffPaysheetComponentFacade().edit(getRemovingEntry());

    }

    public void save() {
        if (errorCheck()) {
            return;
        }

        if (getCurrent().getId() == null) {
            getCurrent().setCreatedAt(new Date());
            getCurrent().setCreater(getSessionController().getLoggedUser());
            getStaffPaysheetComponentFacade().create(getCurrent());
        } else {
            getStaffPaysheetComponentFacade().edit(getCurrent());
        }
        
        Staff s = getCurrent().getStaff();
        Date fd = getCurrent().getFromDate();
        Date td = getCurrent().getToDate();

        JsfUtil.addSuccessMessage("Succesfully Saved");
        makeItemNull();
        getCurrent(s, fd, td);
    }

    public void makeNull() {
        current = null;
        items = null;
        filteredStaff = null;
        reportKeyWord = null;
    }

    PaysheetComponent paysheetComponent;

    public PaysheetComponent getPaysheetComponent() {
        return paysheetComponent;
    }

    public void setPaysheetComponent(PaysheetComponent paysheetComponent) {
        this.paysheetComponent = paysheetComponent;
    }

    public void createTable() {

        if (getPaysheetComponent() == null) {
            JsfUtil.addErrorMessage("Set Pay Sheet Component");
        }
        HashMap hm = new HashMap();
        String sql = "Select ss from "
                + " StaffPaysheetComponent ss"
                + " where ss.retired=false ";
        
        if (getFromDate() != null) {
            sql += " and ((ss.fromDate <=:fd "
                    + " and ss.toDate >=:fd) or ss.fromDate >=:fd) ";
            hm.put("fd", getFromDate());
        }

        if (getPaysheetComponent() != null) {
            sql += " and ss.paysheetComponent=:pt ";
            hm.put("pt", getPaysheetComponent());
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

        sql += " order by ss.staff.codeInterger";
        items = getStaffPaysheetComponentFacade().findByJpql(sql, hm, TemporalType.DATE);
    }

    public List<StaffPaysheetComponent> getItems() {
        //   //////// // System.out.println("getting Lst ");
        //   //////// // System.out.println("Staff  : " + getCurrent().getStaff());
        if (items == null) {

        }

        return items;
    }

    public void makeItemNull() {
        items = null;
        current = null;
    }

    public List<PaysheetComponent> getCompnent() {
        String sql = "Select pc From PaysheetComponent pc "
                + " where pc.retired=false "
                + " and pc.componentType not in :tp1"
                + " and pc.componentType not in :tp2";
        HashMap hm = new HashMap();
        hm.put("tp1", PaysheetComponentType.addition.getSystemDefinedComponents());
        hm.put("tp2", Arrays.asList(new PaysheetComponentType[]{PaysheetComponentType.LoanInstallemant, PaysheetComponentType.LoanNetSalary, PaysheetComponentType.Advance_Payment_Deduction}));
        return getPaysheetComponentFacade().findByJpql(sql, hm);

    }

    public StaffPaySheetComponentController() {
    }

    public StaffPaysheetComponent getCurrent() {
        if (current == null) {
            current = new StaffPaysheetComponent();
        }
        return current;
    }
    public StaffPaysheetComponent getCurrent(Staff s,Date fd,Date td) {
        if (current == null) {
            current = new StaffPaysheetComponent();
            current.setStaff(s);
            current.setFromDate(fd);
            current.setToDate(td);
        }
        return current;
    }

    public void setCurrent(StaffPaysheetComponent current) {
        items = null;
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

    public StaffPaysheetComponent getRemovingEntry() {
        return removingEntry;
    }

    public void setRemovingEntry(StaffPaysheetComponent removingEntry) {
        this.removingEntry = removingEntry;
    }
}
