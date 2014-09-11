/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.hr;

import com.divudi.data.hr.ReportKeyWord;
import com.divudi.ejb.CommonFunctions;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Staff;
import com.divudi.entity.hr.StaffShift;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.StaffFacade;
import com.divudi.facade.StaffShiftFacade;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.persistence.TemporalType;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class HrReportController implements Serializable {

    ReportKeyWord reportKeyWord;
    Date fromDate;
    Date toDate;
    Institution insitution;
    @EJB
    CommonFunctions commonFunctions;
    List<StaffShift> staffShifts;
    @EJB
    StaffShiftFacade staffShiftFacade;
    
    @EJB
    DepartmentFacade departmentFacade;
    @EJB
    private StaffFacade staffFacade;
    
    List<Department> selectDepartments;
    private List<Staff> staffs;

    public void createStaffShift() {
        HashMap hm = new HashMap();
        String sql = "select ss from StaffShift ss"
                + " where ss.retired=false "
                + " and ss.shiftDate between :frm  and :to ";
        hm.put("frm", fromDate);
        hm.put("to", toDate);

        if (getReportKeyWord().getStaff() != null) {
            sql += " and ss.staff=:stf";
            hm.put("stf", getReportKeyWord().getStaff());
        }

        if (getReportKeyWord().getDepartment() != null) {
            sql += " and ss.staff.department=:dep";
            hm.put("dep", getReportKeyWord().getDepartment());
        }

        if (getReportKeyWord().getStaffCategory() != null) {
            sql += " and ss.staff.staffCategory=:stfCat";
            hm.put("stfCat", getReportKeyWord().getStaffCategory());
        }

        if (getReportKeyWord().getDesignation() != null) {
            sql += " and ss.staff.designation=:des";
            hm.put("des", getReportKeyWord().getDesignation());
        }

        if (getReportKeyWord().getRoster()!= null) {
            sql += " and ss.staff.roster=:rs";
            hm.put("rs", getReportKeyWord().getRoster());
        }

        staffShifts = staffShiftFacade.findBySQL(sql, hm, TemporalType.DATE);
    }
    
   public void createDepatmentList(){
       String sql;
       HashMap m=new HashMap();
       
       sql=" select d from Department d where "
               + " d.retired=false ";
       
       if(getInsitution()!=null){
           sql+=" and d.institution=:ins ";
           m.put("ins", insitution);
       }
       
       selectDepartments=getDepartmentFacade().findBySQL(sql, m);
       
   }
   
   public void createStaffList(){
       String sql;
       HashMap m=new HashMap();
       
       sql=" select s from Staff s where "
               + " s.retired=false and type(s)=:dtp";
       
       m.put("dtp", Staff.class);
       
       staffs=getStaffFacade().findBySQL(sql,m);
       
   }
    /**
     * Creates a new instance of HrReport
     */
    public HrReportController() {
    }

    public void makeNull() {
        reportKeyWord = null;
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
        if (fromDate == null) {
            fromDate = commonFunctions.getStartOfMonth(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = commonFunctions.getEndOfMonth(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public List<StaffShift> getStaffShifts() {
        return staffShifts;
    }

    public void setStaffShifts(List<StaffShift> staffShifts) {
        this.staffShifts = staffShifts;
    }

    public DepartmentFacade getDepartmentFacade() {
        return departmentFacade;
    }

    public void setDepartmentFacade(DepartmentFacade departmentFacade) {
        this.departmentFacade = departmentFacade;
    }

    public List<Department> getSelectDepartments() {
        return selectDepartments;
    }

    public void setSelectDepartments(List<Department> selectDepartments) {
        this.selectDepartments = selectDepartments;
    }

    public Institution getInsitution() {
        return insitution;
    }

    public void setInsitution(Institution insitution) {
        this.insitution = insitution;
    }

    public StaffFacade getStaffFacade() {
        return staffFacade;
    }

    public void setStaffFacade(StaffFacade staffFacade) {
        this.staffFacade = staffFacade;
    }

    public List<Staff> getStaffs() {
        return staffs;
    }

    public void setStaffs(List<Staff> staffs) {
        this.staffs = staffs;
    }

}
