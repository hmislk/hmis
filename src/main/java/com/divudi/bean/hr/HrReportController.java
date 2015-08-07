/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.hr;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.data.MonthEndRecord;
import com.divudi.data.Sex;
import com.divudi.data.dataStructure.WeekDayWork;
import com.divudi.data.hr.DayType;
import com.divudi.data.hr.DepartmentAttendance;
import com.divudi.data.hr.FingerPrintRecordType;
import com.divudi.data.hr.LeaveType;
import com.divudi.data.hr.ReportKeyWord;
import com.divudi.data.hr.StaffLeaveBallance;
import com.divudi.data.hr.StaffShiftAggrgation;
import com.divudi.ejb.CommonFunctions;
import com.divudi.ejb.FinalVariables;
import com.divudi.ejb.HumanResourceBean;
import com.divudi.entity.Department;
import com.divudi.entity.Form;
import com.divudi.entity.Institution;
import com.divudi.entity.Staff;
import com.divudi.entity.hr.FingerPrintRecord;
import com.divudi.entity.hr.FingerPrintRecordHistory;
import com.divudi.entity.hr.SalaryCycle;
import com.divudi.entity.hr.Shift;
import com.divudi.entity.hr.StaffLeave;
import com.divudi.entity.hr.StaffPaysheetComponent;
import com.divudi.entity.hr.StaffSalary;
import com.divudi.entity.hr.StaffSalaryComponant;
import com.divudi.entity.hr.StaffShift;
import com.divudi.entity.hr.StaffShiftExtra;
import com.divudi.entity.hr.StaffShiftHistory;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.FingerPrintRecordFacade;
import com.divudi.facade.FingerPrintRecordHistoryFacade;
import com.divudi.facade.FormFacade;
import com.divudi.facade.ShiftFacade;
import com.divudi.facade.StaffFacade;
import com.divudi.facade.StaffLeaveFacade;
import com.divudi.facade.StaffPaysheetComponentFacade;
import com.divudi.facade.StaffSalaryComponantFacade;
import com.divudi.facade.StaffSalaryFacade;
import com.divudi.facade.StaffShiftFacade;
import com.divudi.facade.StaffShiftHistoryFacade;
import com.divudi.facade.util.JsfUtil;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import org.primefaces.model.UploadedFile;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class HrReportController implements Serializable {

    /**
     *
     * JBS
     *
     */
    @EJB
    CommonFunctions commonFunctions;
    @EJB
    StaffFacade staffFacade;
    @EJB
    StaffShiftFacade staffShiftFacade;
    @EJB
    ShiftFacade shiftFacade;
    @EJB
    FingerPrintRecordFacade fingerPrintRecordFacade;
    @EJB
    DepartmentFacade departmentFacade;
    @EJB
    FormFacade formFacade;

    /**
     *
     * Managed Beans
     *
     */
    @Inject
    SessionController sessionController;
    @Inject
    ShiftFingerPrintAnalysisController shiftFingerPrintAnalysisController;

    /**
     *
     * Properties
     *
     */
    ReportKeyWord reportKeyWord;
    Date fromDate;
    Date toDate;
    Institution institution;
    double totalWorkedTime;
    DayType[] dayTypesSelected;

    List<StaffShift> staffShifts;
    List<StaffShift> staffShiftsHoliday;
    List<Staff> staffs;
    List<Shift> shiftLists;
    List<FingerPrintRecord> fingerPrintRecords;
    List<WeekDayWork> weekDayWorks;
    List<Department> selectDepartments;
    List<FingerPrintRecord> selectedFingerPrintRecords;
    List<OverTimeAllMonth> overTimeAllMonths;
    List<SummeryForMonth> summeryForMonths;

    String backButtonPage;

    public String fromStaffFingerprintAnalysisToStaffLeave(Date date, Staff staff) {
        fromDate = CommonFunctions.getStartOfDay(date);
        toDate = CommonFunctions.getEndOfDay(date);
        if (reportKeyWord == null) {
            reportKeyWord = new ReportKeyWord();
        }
        reportKeyWord.resetKeyWord();
        reportKeyWord.setStaff(staff);
        createStaffLeaveDetail();
        backButtonPage = "/hr/hr_report_leave_summery_by_staff";
        return "/hr/hr_report_leave_summery_by_staff";
    }

    public void onEditBlockedUpdate(StaffSalary staffSalary) {
        if (staffSalary == null) {
            return;
        }
        staffSalaryFacade.edit(staffSalary);
    }

    public DayType[] getDayTypesSelected() {
        return dayTypesSelected;
    }

    public void setDayTypesSelected(DayType[] dayTypesSelected) {
        this.dayTypesSelected = dayTypesSelected;
    }

    private void calculateWorkedTime() {
        totalWorkedTime = 0;

        for (StaffShift staffShift : staffShiftsNormal) {
            totalWorkedTime += staffShift.getWorkedWithinTimeFrameVarified();

        }

    }

    public double getTotalWorkedTime() {
        return totalWorkedTime;
    }

    public void setTotalWorkedTime(double totalWorkedTime) {
        this.totalWorkedTime = totalWorkedTime;
    }

    public PhDateController getPhDateController() {
        return phDateController;
    }

    public void setPhDateController(PhDateController phDateController) {
        this.phDateController = phDateController;
    }

    private UploadedFile file;

    public UploadedFile getFile() {
        return file;
    }

    public void setFile(UploadedFile file) {
        this.file = file;
    }

    public String importToExcel() throws IOException {

        String zoneCode;
        String employerNumber;
        String epfNumber;
        String empNo;
        String nicNo;
        String fullname;
        String initials;
        String surnames;
        String name;
        String address;
        Sex sex;
        Date dob;
        Date doj;
        String department;
        String designation;
        String acNo;

        int zoneCodeCol = 0;
        int employerNumberCol = 1;
        int epfNumberCol;
        int empNoCol;
        int nicNoCol;
        int fullnameCol;
        int initialsCol;
        int surnamesCol;
        int nameCol;
        int addressCol;
        int sexCol;
        int dobCol;
        int dojCol;
        int departmentCol;
        int designationCol;
        int acNoCol;

        int startRow = 2;

        File inputWorkbook;
        Workbook w;
        Cell cell;
        InputStream in;
        UtilityController.addSuccessMessage(file.getFileName());
        try {
            UtilityController.addSuccessMessage(file.getFileName());
            in = file.getInputstream();
            File f;
            f = new File(Calendar.getInstance().getTimeInMillis() + file.getFileName());
            FileOutputStream out = new FileOutputStream(f);
            int read = 0;
            byte[] bytes = new byte[1024];
            while ((read = in.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            in.close();
            out.flush();
            out.close();

            inputWorkbook = new File(f.getAbsolutePath());

            UtilityController.addSuccessMessage("Excel File Opened");

            w = Workbook.getWorkbook(inputWorkbook);
            Sheet sheet = w.getSheet(0);

            for (int i = startRow; i < sheet.getRows(); i++) {

                Map m = new HashMap();

                //Category
                cell = sheet.getCell(employerNumberCol, i);
                acNo = cell.getContents();

            }

            UtilityController.addSuccessMessage("Succesful. All the data in Excel File Impoted to the database");
            return "";
        } catch (IOException | BiffException ex) {
            UtilityController.addErrorMessage(ex.getMessage());
            return "";
        }
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public List<FingerPrintRecord> getSelectedFingerPrintRecords() {
        return selectedFingerPrintRecords;
    }

    public void setSelectedFingerPrintRecords(List<FingerPrintRecord> selectedFingerPrintRecords) {
        this.selectedFingerPrintRecords = selectedFingerPrintRecords;
    }

    public StaffSalaryFacade getStaffSalaryFacade() {
        return staffSalaryFacade;
    }

    public void setStaffSalaryFacade(StaffSalaryFacade staffSalaryFacade) {
        this.staffSalaryFacade = staffSalaryFacade;
    }

    public void delete(StaffLeave ss) {
        if (ss == null) {
            return;
        }

        Form form = ss.getForm();
        if (form != null) {
            form.setRetired(true);
            form.setRetiredAt(new Date());
            form.setRetireComments(ss.getRetireComments());
            form.setRetirer(sessionController.getLoggedUser());
            formFacade.edit(form);
        }

        ss.setRetired(true);
        ss.setRetiredAt(new Date());
        ss.setRetirer(sessionController.getLoggedUser());
        staffLeaveFacade.edit(ss);

        StaffShift staffShift = ss.getStaffShift();

        if (staffShift != null) {
            staffShift.setAutoLeave(false);
            staffShift.setConsiderForEarlyOut(false);
            staffShift.setConsiderForLateIn(false);
            staffShift.resetLeaveData(ss.getLeaveType());
            staffShift.calLeaveTime();
            staffShiftFacade.edit(staffShift);

            String sql = "Select s from StaffShift s "
                    + " where s.retired=false "
                    + " and s.referenceStaffShiftLateIn=:ref "
                    + " and s.considerForLateIn=true ";
            HashMap hm = new HashMap();
            hm.put("ref", staffShift);
            List<StaffShift> list = staffShiftFacade.findBySQL(sql, hm);
            if (list != null) {
                for (StaffShift s : list) {
                    s.setConsiderForLateIn(false);
                    staffShiftFacade.edit(s);
                }
            }

            sql = "Select s from StaffShift s "
                    + " where s.retired=false "
                    + " and s.referenceStaffShiftEarlyOut=:ref "
                    + " and s.considerForEarlyOut=true ";
            hm = new HashMap();
            hm.put("ref", staffShift);
            list = staffShiftFacade.findBySQL(sql, hm);
            if (list != null) {
                for (StaffShift s : list) {
                    s.setConsiderForEarlyOut(false);
                    staffShiftFacade.edit(s);
                }
            }

        }

    }

    public List<StaffShift> getStaffShiftsHoliday() {
        return staffShiftsHoliday;
    }

    public void setStaffShiftsHoliday(List<StaffShift> staffShiftsHoliday) {
        this.staffShiftsHoliday = staffShiftsHoliday;
    }

    public StaffPaysheetComponentFacade getStaffPaysheetComponentFacade() {
        return staffPaysheetComponentFacade;
    }

    public void setStaffPaysheetComponentFacade(StaffPaysheetComponentFacade staffPaysheetComponentFacade) {
        this.staffPaysheetComponentFacade = staffPaysheetComponentFacade;
    }

    public FormFacade getFormFacade() {
        return formFacade;
    }

    public void setFormFacade(FormFacade formFacade) {
        this.formFacade = formFacade;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public List<Department> getSelectDepartments() {
        return selectDepartments;
    }

    public void setSelectDepartments(List<Department> selectDepartments) {
        this.selectDepartments = selectDepartments;
    }

    public void crateDepartmentList() {
        String sql = " select i from Department i "
                + " where i.retired=false "
                + " and i.institution=:ins "
                + " order by i.name ";
        HashMap hm = new HashMap();
        hm.put("ins", institution);
        selectDepartments = departmentFacade.findBySQL(sql, hm);
    }

    public List<WeekDayWork> getWeekDayWorks() {
        return weekDayWorks;
    }

    public void setWeekDayWorks(List<WeekDayWork> weekDayWorks) {
        this.weekDayWorks = weekDayWorks;
    }

    public HumanResourceBean getHumanResourceBean() {
        return humanResourceBean;
    }

    public void setHumanResourceBean(HumanResourceBean humanResourceBean) {
        this.humanResourceBean = humanResourceBean;
    }

    public ShiftFacade getShiftFacade() {
        return shiftFacade;
    }

    public void setShiftFacade(ShiftFacade shiftFacade) {
        this.shiftFacade = shiftFacade;
    }

    public void createFingerPrintRecordLogged() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = createFingerPrintQuary(hm);
        sql += " and ss.fingerPrintRecordType=:ftp "
                + " and ss.verifiedRecord.staffShift is not null ";
        hm.put("ftp", FingerPrintRecordType.Logged);
//        sql += " order by ss.staff,ss.recordTimeStamp";
        sql += " order by ss.staff.codeInterger,ss.recordTimeStamp ";
        fingerPrintRecords = fingerPrintRecordFacade.findBySQL(sql, hm, TemporalType.DATE);
    }

    public void createFingerPrintRecordAll() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = createFingerPrintQuary(hm);
        sql += " and ss.fingerPrintRecordType=:ftp ";
//                + " and ss.verifiedRecord.staffShift is not null ";
        hm.put("ftp", FingerPrintRecordType.Logged);
//        sql += " order by ss.staff,ss.recordTimeStamp";
        sql += " order by ss.staff.codeInterger,ss.recordTimeStamp ";
        fingerPrintRecords = fingerPrintRecordFacade.findBySQL(sql, hm, TemporalType.DATE);
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

    public void createFingerPrintRecordVarifiedAll() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = createFingerPrintQuary(hm);
        sql += " and ss.fingerPrintRecordType=:ftp "
                + " and ss.staffShift is not null ";
        hm.put("ftp", FingerPrintRecordType.Varified);
//        sql += " order by ss.staff,ss.recordTimeStamp";
        sql += " order by ss.staff.codeInterger,ss.recordTimeStamp ";
        fingerPrintRecords = fingerPrintRecordFacade.findBySQL(sql, hm, TemporalType.DATE);
    }

    public void approve() {
        if (selectedFingerPrintRecords == null) {
            UtilityController.addErrorMessage("Select Fingerprint");
            return;
        }

        for (FingerPrintRecord fpr : selectedFingerPrintRecords) {
            fpr.setApproved(true);
            fpr.setApprovedAt(new Date());
            fpr.setApprover(sessionController.getLoggedUser());
            fingerPrintRecordFacade.edit(fpr);
        }
    }

    public void createFingerPrintNotApproved() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = createFingerPrintQuary(hm);
        sql += " and ss.fingerPrintRecordType=:ftp "
                + " and ss.approved=false "
                + " and ss.staffShift is not null "
                + " and (ss.loggedRecord is null "
                + " or (ss.loggedRecord.recordTimeStamp!=ss.recordTimeStamp))";
        hm.put("ftp", FingerPrintRecordType.Varified);
//        sql += " order by ss.staff,ss.recordTimeStamp";
        sql += " order by ss.staff.codeInterger,ss.recordTimeStamp ";
        fingerPrintRecords = fingerPrintRecordFacade.findBySQL(sql, hm, TemporalType.DATE);

        //////////////////////////
//        sql = "";
//        hm = new HashMap();
//        sql = createFingerPrintQuary(hm);
//        sql += " and ss.fingerPrintRecordType=:ftp  "
//                + " and ss.staffShift is not null "
//                + " and ss.loggedRecord.recordTimeStamp!=ss.recordTimeStamp ";
//        hm.put("ftp", FingerPrintRecordType.Varified);
//        sql += " order by ss.staff.codeInterger,ss.recordTimeStamp ";
//        List<FingerPrintRecord> list2 = fingerPrintRecordFacade.findBySQL(sql, hm, TemporalType.DATE);
    }

    public void createFingerPrintApproved() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = createFingerPrintQuary(hm);
        sql += " and ss.fingerPrintRecordType=:ftp "
                + " and ss.approved=true "
                + " and ss.staffShift is not null "
                + " and (ss.loggedRecord is null "
                + " or (ss.loggedRecord.recordTimeStamp!=ss.recordTimeStamp))";
        hm.put("ftp", FingerPrintRecordType.Varified);
//        sql += " order by ss.staff,ss.recordTimeStamp";
        sql += " order by ss.staff.codeInterger,ss.recordTimeStamp ";
        fingerPrintRecords = fingerPrintRecordFacade.findBySQL(sql, hm, TemporalType.DATE);

        //////////////////////////
//        sql = "";
//        hm = new HashMap();
//        sql = createFingerPrintQuary(hm);
//        sql += " and ss.fingerPrintRecordType=:ftp  "
//                + " and ss.staffShift is not null "
//                + " and ss.loggedRecord.recordTimeStamp!=ss.recordTimeStamp ";
//        hm.put("ftp", FingerPrintRecordType.Varified);
//        sql += " order by ss.staff.codeInterger,ss.recordTimeStamp ";
//        List<FingerPrintRecord> list2 = fingerPrintRecordFacade.findBySQL(sql, hm, TemporalType.DATE);
    }

    public void createFingerPrintRecordVarifiedWithLogged() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = createFingerPrintQuary(hm);
        sql += " and ss.fingerPrintRecordType=:ftp "
                + " and ss.staffShift is not null "
                + " and ss.loggedRecord is not null ";
        hm.put("ftp", FingerPrintRecordType.Varified);
//        sql += " order by ss.staff,ss.recordTimeStamp";
        sql += " order by ss.staff.codeInterger,ss.recordTimeStamp ";
        fingerPrintRecords = fingerPrintRecordFacade.findBySQL(sql, hm, TemporalType.DATE);
    }

    public void createFingerPrintRecordVarifiedWithOutLogged() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = createFingerPrintQuary(hm);
        sql += " and ss.fingerPrintRecordType=:ftp  "
                + " and ss.staffShift is not null "
                + " and ss.loggedRecord is null "
                + " and (ss.comments!=:nx"
                + " and ss.comments!=:pr)";
        hm.put("ftp", FingerPrintRecordType.Varified);
        hm.put("nx", "(NEW PREV)");
        hm.put("pr", "(NEW NEXT)");
//        sql += " order by ss.staff,ss.recordTimeStamp";
        sql += " order by ss.staff.codeInterger,ss.recordTimeStamp ";
        fingerPrintRecords = fingerPrintRecordFacade.findBySQL(sql, hm, TemporalType.DATE);
    }

    public void createFingerPrintRecordNoShiftSetted() {
        HashMap hm = new HashMap();
        String sql = "";
        sql = "select ss from FingerPrintRecord ss "
                + " where ss.retired=false "
                + " and ss.verifiedRecord.staffShift is null "
                + " and ss.recordTimeStamp between :frm  and :to "
                + " and ss.fingerPrintRecordType=:ftp";

        hm.put("ftp", FingerPrintRecordType.Logged);
        hm.put("frm", fromDate);
        hm.put("to", toDate);

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
            sql += " and ss.staff.staffCategory=:stfCat";
            hm.put("stfCat", getReportKeyWord().getStaffCategory());
        }

        if (getReportKeyWord().getDesignation() != null) {
            sql += " and ss.staff.designation=:des";
            hm.put("des", getReportKeyWord().getDesignation());
        }

        if (getReportKeyWord().getRoster() != null) {
            sql += " and ss.roster=:rs ";
            hm.put("rs", getReportKeyWord().getRoster());
        }

        sql += " order by ss.staff.codeInterger,ss.recordTimeStamp";
        fingerPrintRecords = fingerPrintRecordFacade.findBySQL(sql, hm, TemporalType.TIMESTAMP);
    }

    public void createFingerPrintQuary(String sql, HashMap hm) {
        sql = "select ss from FingerPrintRecord ss"
                + " where ss.retired=false"
                + " and ss.recordTimeStamp between :frm  and :to ";
        hm.put("frm", fromDate);
        hm.put("to", toDate);

        if (getReportKeyWord().getStaff() != null) {
            sql += " and ss.staff=:stf";
            hm.put("stf", getReportKeyWord().getStaff());
        }

        if (getReportKeyWord().getDepartment() != null) {
            sql += " and ss.staff.workingDepartment=:dep";
            hm.put("dep", getReportKeyWord().getDepartment());
        }
        if (getReportKeyWord().getInstitution() != null) {
            sql += " and ss.staff.institution=:ins ";
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
            sql += " and ss.roster=:rs";
            hm.put("rs", getReportKeyWord().getRoster());
        }

        if (getReportKeyWord().getShift() != null) {
            sql += " and ss.staffShift.shift=:sh";
            hm.put("sh", getReportKeyWord().getShift());
        }

    }

    public String createFingerPrintQuary(HashMap hm) {
        String sql = "";
        sql = "select ss from FingerPrintRecord ss"
                + " where ss.retired=false"
                + " and ss.recordTimeStamp between :frm  and :to ";
        hm.put("frm", fromDate);
        hm.put("to", toDate);

        if (getReportKeyWord().getStaff() != null) {
            sql += " and ss.staff=:stf";
            hm.put("stf", getReportKeyWord().getStaff());
        }

        if (getReportKeyWord().getDepartment() != null) {
            sql += " and ss.staff.workingDepartment=:dep";
            hm.put("dep", getReportKeyWord().getDepartment());
        }

        if (getReportKeyWord().getInstitution() != null) {
            sql += " and ss.staff.institution=:ins ";
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
            sql += " and ss.roster=:rs";
            hm.put("rs", getReportKeyWord().getRoster());
        }

        if (getReportKeyWord().getShift() != null) {
            sql += " and ss.staffShift.shift=:sh";
            hm.put("sh", getReportKeyWord().getShift());
        }

        return sql;

    }

    public void createStaffListAll() {
        String sql;
        HashMap hm = new HashMap();
        sql = "select ss from Staff ss "
                + " where ss.retired=false "
                + " and ss.codeInterger!=0 "
                + " order by ss.codeInterger ";
        staffs = getStaffFacade().findBySQL(sql);
    }

    public void createStaffList() {
        String sql;
        HashMap hm = new HashMap();

        sql = "select ss from Staff ss "
                + " where ss.retired=false "
                + " and ss.codeInterger!=0 ";

        if (getReportKeyWord().getDepartment() != null) {
            sql += " and ss.department=:dep ";
            hm.put("dep", getReportKeyWord().getDepartment());
        }

        if (getReportKeyWord().getInstitution() != null) {
            sql += " and ss.institution=:ins ";
            hm.put("ins", getReportKeyWord().getInstitution());
        }

        if (getReportKeyWord().getStaffCategory() != null) {
            sql += " and ss.staffCategory=:stfCat ";
            hm.put("stfCat", getReportKeyWord().getStaffCategory());
        }

        if (getReportKeyWord().getSpeciality() != null) {
            sql += " and ss.speciality=:spc ";
            hm.put("spc", getReportKeyWord().getSpeciality());
        }

        if (getReportKeyWord().getDesignation() != null) {
            sql += " and ss.designation=:des ";
            hm.put("des", getReportKeyWord().getDesignation());
        }

        if (getReportKeyWord().getRoster() != null) {
            sql += " and ss.roster=:rs ";
            hm.put("rs", getReportKeyWord().getRoster());
        }

        sql += " order by ss.codeInterger ";
        staffs = getStaffFacade().findBySQL(sql, hm);
    }

    public String createStaffShiftQuary(HashMap hm) {

        String sql = "";
        sql = "select ss from StaffShift ss "
                + " where ss.retired=false "
                + " and ss.shift is not null "
                + " and ss.workedWithinTimeFrameVarified!=0"
                + " and ss.shiftDate between :frm  and :to ";
        hm.put("frm", fromDate);
        hm.put("to", toDate);

        if (getReportKeyWord().getStaff() != null) {
            sql += " and ss.staff=:stf ";
            hm.put("stf", getReportKeyWord().getStaff());
        }

        if (getReportKeyWord().getEmployeeStatus() != null) {
            sql += " and ss.staff.employeeStatus=:empStat ";
            hm.put("empStat", getReportKeyWord().getEmployeeStatus());
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
            sql += " and ss.roster=:rs ";
            hm.put("rs", getReportKeyWord().getRoster());
        }

        if (getReportKeyWord().getShift() != null) {
            sql += " and ss.shift=:sh ";
            hm.put("sh", getReportKeyWord().getShift());
        }

        return sql;
    }

    private boolean blocked;
    private boolean hold;

    public String createStaffSalaryQuary(HashMap hm) {
        String sql = "";
        sql = "select ss from StaffSalary ss "
                + " where ss.retired=false "
                + " and ss.salaryCycle=:scl ";
        if (blocked == true) {
            sql += " and ss.blocked=true";
        }
        if (hold == true) {
            sql += " and ss.hold=true";
        }
        hm.put("scl", getReportKeyWord().getSalaryCycle());

        if (getReportKeyWord().getStaff() != null) {
            sql += " and ss.staff=:stf ";
            hm.put("stf", getReportKeyWord().getStaff());
        }

        if (getReportKeyWord().getInstitution() != null) {
            sql += " and ss.institution=:ins ";
            hm.put("ins", getReportKeyWord().getInstitution());
        }

        if (getReportKeyWord().getBank() != null) {
            sql += " and ss.bankBranch=:bk ";
            hm.put("bk", getReportKeyWord().getBank());
        }

        if (getReportKeyWord().getDepartment() != null) {
            sql += " and ss.department=:dep ";
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

        if (getReportKeyWord().getRoster() != null) {
            sql += " and ss.staff.roster=:rs ";
            hm.put("rs", getReportKeyWord().getRoster());
        }

        return sql;
    }

    public String createStaffSalaryComponentQuary(HashMap hm) {
        String sql = "";
        sql = "select ss from StaffSalaryComponant ss "
                + " where ss.retired=false "
                + " and ss.salaryCycle=:scl "
                + " and ss.staffSalary.blocked=false ";
        hm.put("scl", getReportKeyWord().getSalaryCycle());

        if (getReportKeyWord().getStaff() != null) {
            sql += " and ss.staffSalary.staff=:stf ";
            hm.put("stf", getReportKeyWord().getStaff());
        }

        if (getReportKeyWord().getPaysheetComponent() != null) {
            sql += " and ss.staffPaysheetComponent.paysheetComponent=:pt ";
            hm.put("pt", getReportKeyWord().getPaysheetComponent());
        }

        if (getReportKeyWord().getInstitution() != null) {
            sql += " and ss.staffSalary.institution=:ins ";
            hm.put("ins", getReportKeyWord().getInstitution());
        }

        if (getReportKeyWord().getBank() != null) {
            sql += " and ss.staffSalary.staff.bankBranch=:bk ";
            hm.put("bk", getReportKeyWord().getBank());
        }

        if (getReportKeyWord().getDepartment() != null) {
            sql += " and ss.staffSalary.department=:dep ";
            hm.put("dep", getReportKeyWord().getDepartment());
        }

        if (getReportKeyWord().getStaffCategory() != null) {
            sql += " and ss.staffSalary.staff.staffCategory=:stfCat";
            hm.put("stfCat", getReportKeyWord().getStaffCategory());
        }

        if (getReportKeyWord().getDesignation() != null) {
            sql += " and ss.staffSalary.staff.designation=:des";
            hm.put("des", getReportKeyWord().getDesignation());
        }

        if (getReportKeyWord().getRoster() != null) {
            sql += " and ss.staffSalary.staff.roster=:rs ";
            hm.put("rs", getReportKeyWord().getRoster());
        }

        return sql;
    }

    public String createStaffShiftExtraQuary(HashMap hm) {
        String sql = "";
        sql = "select ss from StaffShiftExtra ss "
                + " where ss.retired=false "
                + " and ss.shiftDate between :frm  and :to ";
        hm.put("frm", fromDate);
        hm.put("to", toDate);

        if (getReportKeyWord().getStaff() != null) {
            sql += " and ss.staff=:stf ";
            hm.put("stf", getReportKeyWord().getStaff());
        }

        if (getReportKeyWord().getDepartment() != null) {
            sql += " and ss.staff.workingDepartment=:dep ";
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

        if (getReportKeyWord().getRoster() != null) {
            sql += " and ss.roster=:rs ";
            hm.put("rs", getReportKeyWord().getRoster());
        }

        if (getReportKeyWord().getShift() != null) {
            sql += " and ss.shift=:sh ";
            hm.put("sh", getReportKeyWord().getShift());
        }

        return sql;
    }

    List<StaffLeave> staffLeaves;
    @EJB
    StaffLeaveFacade staffLeaveFacade;

    public FingerPrintRecordFacade getFingerPrintRecordFacade() {
        return fingerPrintRecordFacade;
    }

    public void setFingerPrintRecordFacade(FingerPrintRecordFacade fingerPrintRecordFacade) {
        this.fingerPrintRecordFacade = fingerPrintRecordFacade;
    }

    public List<StaffLeave> getStaffLeaves() {
        return staffLeaves;
    }

    public void setStaffLeaves(List<StaffLeave> staffLeaves) {
        this.staffLeaves = staffLeaves;
    }

    public StaffLeaveFacade getStaffLeaveFacade() {
        return staffLeaveFacade;
    }

    public void setStaffLeaveFacade(StaffLeaveFacade staffLeaveFacade) {
        this.staffLeaveFacade = staffLeaveFacade;
    }

    public void createStaffLeave() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = "select ss from StaffLeave ss "
                + " where ss.retired=false "
                + " and ss.leaveDate between :frm  and :to ";
        hm.put("frm", fromDate);
        hm.put("to", toDate);

        if (getReportKeyWord().getStaff() != null) {
            sql += " and ss.staff=:stf ";
            hm.put("stf", getReportKeyWord().getStaff());
        }

        if (getReportKeyWord().getDepartment() != null) {
            sql += " and ss.staff.workingDepartment=:dep ";
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

        if (getReportKeyWord().getRoster() != null) {
            sql += " and ss.roster=:rs ";
            hm.put("rs", getReportKeyWord().getRoster());
        }

        if (getReportKeyWord().getLeaveType() != null) {
            List<LeaveType> list = getReportKeyWord().getLeaveType().getLeaveTypes();

            sql += " and ss.leaveType in :ltp ";
            hm.put("ltp", list);

        }

        sql += " order by ss.form.code,ss.staff.codeInterger ";
        staffLeaves = staffLeaveFacade.findBySQL(sql, hm, TemporalType.DATE);
    }

    public void createStaffLeaveSystem() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = "select ss from StaffLeaveSystem ss "
                + " where ss.retired=false "
                + " and ss.leaveDate between :frm  and :to ";
        hm.put("frm", fromDate);
        hm.put("to", toDate);

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
            hm.put("ins", getReportKeyWord().getDepartment());
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
            sql += " and ss.roster=:rs ";
            hm.put("rs", getReportKeyWord().getRoster());
        }

        if (getReportKeyWord().getLeaveType() != null) {
            List<LeaveType> list = getReportKeyWord().getLeaveType().getLeaveTypes();

            sql += " and ss.leaveType in :ltp ";
            hm.put("ltp", list);

        }

        sql += " order by ss.staff.codeInterger";
        staffLeaves = staffLeaveFacade.findBySQL(sql, hm, TemporalType.DATE);
    }

    private List<StaffLeave> staffLeavesAnnual;
    private List<StaffLeave> staffLeavesCashual;
    private List<StaffLeave> staffLeavesNoPay;
    private List<StaffLeave> staffLeavesDutyLeave;
    private List<StaffLeave> staffLeavesMedical;
    private List<StaffLeave> staffLeavesMaternity1st;
    private List<StaffLeave> staffLeavesMaternity2nd;
    private List<StaffLeave> staffLeavesLieu;

    double annualEntitle;
    double annualUtilized;
    double casualEntitle;
    double casualUtilized;
    double nopayEntitle;
    double nopayUtilized;
    double dutyLeaveEntitle;
    double dutyLeaveUtilized;
    private double medicalEntitle;
    private double medicalUtilized;
    private double maternity1Entitle;
    private double maternity1Utilized;
    private double maternity2Entitle;
    private double maternity2Utilized;
    private double lieuEntitle;
    private double lieuUtilized;

    public double getAnnualEntitle() {
        return annualEntitle;
    }

    public void setAnnualEntitle(double annualEntitle) {
        this.annualEntitle = annualEntitle;
    }

    public double getAnnualUtilized() {
        return annualUtilized;
    }

    public void setAnnualUtilized(double annualUtilized) {
        this.annualUtilized = annualUtilized;
    }

    public List<Shift> getShiftLists() {
        return shiftLists;
    }

    public void setShiftLists(List<Shift> shiftLists) {
        this.shiftLists = shiftLists;
    }

    List<StaffShift> staffShiftExtraDuties;

    public List<StaffShift> getStaffShiftExtraDuties() {
        return staffShiftExtraDuties;
    }

    public void setStaffShiftExtraDuties(List<StaffShift> staffShiftExtraDuties) {
        this.staffShiftExtraDuties = staffShiftExtraDuties;
    }

    private List<StaffShift> staffShiftsNoPay;
    SalaryCycle salaryCycle;

    public SalaryCycle getSalaryCycle() {
        return salaryCycle;
    }

    public void setSalaryCycle(SalaryCycle salaryCycle) {
        this.salaryCycle = salaryCycle;
    }

    List<StaffShift> staffShiftsNormal;

    public List<StaffShift> getStaffShiftsNormal() {
        return staffShiftsNormal;
    }

    public void setStaffShiftsNormal(List<StaffShift> staffShiftsNormal) {
        this.staffShiftsNormal = staffShiftsNormal;
    }

    List<StaffShift> staffShiftsDayOff;

    public List<StaffShift> getStaffShiftsDayOff() {
        return staffShiftsDayOff;
    }

    public void setStaffShiftsDayOff(List<StaffShift> staffShiftsDayOff) {
        this.staffShiftsDayOff = staffShiftsDayOff;
    }

    public void reset() {
        if (staffShiftsNormal != null) {
            for (StaffShift s : staffShiftsNormal) {
                if (s.isConsiderForEarlyOut()
                        || s.isConsiderForLateIn()
                        || s.isAutoLeave()) {
                    s.setConsiderForEarlyOut(false);
                    s.setConsiderForLateIn(false);
                    s.setAutoLeave(false);

                }
                s.setLeaveType(null);
                staffShiftFacade.edit(s);
            }
        }

        if (staffLeaveSystem != null) {
            for (StaffLeave s : staffLeaveSystem) {
                s.setRetired(true);
                s.setRetiredAt(new Date());
                s.setRetirer(sessionController.getLoggedUser());
                staffLeaveFacade.edit(s);
            }
        }

    }

    public void resetSystemLeave() {

        String sql = "select s from StaffLeaveSystem s "
                + " where s.retired=false";
        List<StaffLeave> list = staffLeaveFacade.findBySQL(sql);

        for (StaffLeave s : list) {
            if (s.getStaffShift() != null) {
                if (s.getStaffShift().isConsiderForEarlyOut()
                        || s.getStaffShift().isConsiderForLateIn()
                        || s.getStaffShift().isAutoLeave()) {
                    s.getStaffShift().setConsiderForEarlyOut(false);
                    s.getStaffShift().setConsiderForLateIn(false);
                    s.getStaffShift().setAutoLeave(false);

                }
                s.getStaffShift().setLeaveType(null);
                staffShiftFacade.edit(s.getStaffShift());
            }

            s.setRetired(true);
            s.setRetiredAt(new Date());
            s.setRetirer(sessionController.getLoggedUser());
            staffLeaveFacade.edit(s);

            if (s.getForm() != null) {
                s.getForm().setRetired(true);
                s.getForm().setRetiredAt(new Date());
                s.getForm().setRetirer(sessionController.getLoggedUser());
                formFacade.edit(s.getForm());
            }
        }

    }

    public void createStaffWrokedDetail() {
        if (getReportKeyWord().getStaff() == null) {
            UtilityController.addErrorMessage("Please Select  Staff");
            return;
        }
        staffShiftsNormal = humanResourceBean.fetchStaffShiftNormal(getSalaryCycle().getWorkedFromDate(), getSalaryCycle().getWorkedToDate(), getReportKeyWord().getStaff());
        System.err.println("Sh Normal " + staffShiftsNormal);
        staffShiftsHoliday = humanResourceBean.fetchStaffShiftAllowance(getSalaryCycle().getSalaryFromDate(),
                getSalaryCycle().getSalaryToDate(),
                getReportKeyWord().getStaff(),
                Arrays.asList(new DayType[]{DayType.MurchantileHoliday, DayType.Poya}));
        System.err.println("Sh Holiday " + staffShiftsHoliday);
        staffShiftsDayOff = humanResourceBean.fetchStaffShiftAllowance(getSalaryCycle().getSalaryFromDate(),
                getSalaryCycle().getSalaryToDate(),
                getReportKeyWord().getStaff(),
                Arrays.asList(new DayType[]{DayType.DayOff, DayType.SleepingDay}));
        System.err.println("Sh Day Off " + staffShiftsDayOff);
        staffShiftExtraDuties = humanResourceBean.fetchStaffShiftExtraDuty(getSalaryCycle().getWorkedFromDate(), getSalaryCycle().getWorkedToDate(), getReportKeyWord().getStaff());
        System.err.println("Sh Extra Duty " + staffShiftExtraDuties);
        staffLeavesNoPay = humanResourceBean.fetchStaffLeaveAddedLeaveList(getReportKeyWord().getStaff(), LeaveType.No_Pay, getSalaryCycle().getSalaryFromDate(), getSalaryCycle().getSalaryToDate());
        System.err.println("User Leave " + staffLeavesNoPay);
        staffLeaveSystem = humanResourceBean.fetchStaffLeaveSystemList(getReportKeyWord().getStaff(), getSalaryCycle().getSalaryFromDate(), getSalaryCycle().getSalaryToDate());
        System.err.println("System Leave " + staffLeaveSystem);

        calculateWorkedTime();
    }

    List<StaffLeave> staffLeaveSystem;

    public List<StaffLeave> getStaffLeaveSystem() {
        return staffLeaveSystem;
    }

    public void setStaffLeaveSystem(List<StaffLeave> staffLeaveSystem) {
        this.staffLeaveSystem = staffLeaveSystem;
    }

    public void createStaffLeaveDetail() {
        if (getReportKeyWord().getStaff() == null) {
            return;
        }

        annualEntitle = humanResourceBean.fetchStaffLeaveEntitle(getReportKeyWord().getStaff(), LeaveType.Annual, fromDate, toDate);
        //System.out.println("annualEntitle = " + annualEntitle);
        annualUtilized = humanResourceBean.fetchStaffLeave(getReportKeyWord().getStaff(), LeaveType.Annual, fromDate, toDate);
        staffLeavesAnnual = createStaffLeave(LeaveType.Annual, getReportKeyWord().getStaff(), getFromDate(), getToDate());

        casualEntitle = humanResourceBean.fetchStaffLeaveEntitle(getReportKeyWord().getStaff(), LeaveType.Casual, fromDate, toDate);
        casualUtilized = humanResourceBean.fetchStaffLeave(getReportKeyWord().getStaff(), LeaveType.Casual, fromDate, toDate);
        staffLeavesCashual = createStaffLeave(LeaveType.Casual, getReportKeyWord().getStaff(), getFromDate(), getToDate());

        nopayEntitle = humanResourceBean.fetchStaffLeaveEntitle(getReportKeyWord().getStaff(), LeaveType.No_Pay, fromDate, toDate);
        nopayUtilized = humanResourceBean.fetchStaffLeave(getReportKeyWord().getStaff(), LeaveType.No_Pay, fromDate, toDate);
        staffLeavesNoPay = createStaffLeave(LeaveType.No_Pay, getReportKeyWord().getStaff(), getFromDate(), getToDate());

        dutyLeaveEntitle = humanResourceBean.fetchStaffLeaveEntitle(getReportKeyWord().getStaff(), LeaveType.DutyLeave, fromDate, toDate);
        dutyLeaveUtilized = humanResourceBean.fetchStaffLeave(getReportKeyWord().getStaff(), LeaveType.DutyLeave, fromDate, toDate);
        staffLeavesDutyLeave = createStaffLeave(LeaveType.DutyLeave, getReportKeyWord().getStaff(), getFromDate(), getToDate());

        medicalEntitle = humanResourceBean.fetchStaffLeaveEntitle(getReportKeyWord().getStaff(), LeaveType.Medical, fromDate, toDate);
        medicalUtilized = humanResourceBean.fetchStaffLeave(getReportKeyWord().getStaff(), LeaveType.Medical, fromDate, toDate);
        staffLeavesMedical = createStaffLeave(LeaveType.Medical, getReportKeyWord().getStaff(), getFromDate(), getToDate());

        maternity1Entitle = humanResourceBean.fetchStaffLeaveEntitle(getReportKeyWord().getStaff(), LeaveType.Maternity1st, fromDate, toDate);
        maternity1Utilized = humanResourceBean.fetchStaffLeave(getReportKeyWord().getStaff(), LeaveType.Maternity1st, fromDate, toDate);
        staffLeavesMaternity1st = createStaffLeave(LeaveType.Maternity1st, getReportKeyWord().getStaff(), getFromDate(), getToDate());

        maternity2Entitle = humanResourceBean.fetchStaffLeaveEntitle(getReportKeyWord().getStaff(), LeaveType.Maternity2nd, fromDate, toDate);
        maternity2Utilized = humanResourceBean.fetchStaffLeave(getReportKeyWord().getStaff(), LeaveType.Maternity2nd, fromDate, toDate);
        staffLeavesMaternity2nd = createStaffLeave(LeaveType.Maternity2nd, getReportKeyWord().getStaff(), getFromDate(), getToDate());

        lieuEntitle = humanResourceBean.fetchStaffLeaveEntitle(getReportKeyWord().getStaff(), LeaveType.Lieu, fromDate, toDate);
        lieuUtilized = humanResourceBean.fetchStaffLeave(getReportKeyWord().getStaff(), LeaveType.Lieu, fromDate, toDate);
        staffLeavesLieu = createStaffLeave(LeaveType.Lieu, getReportKeyWord().getStaff(), getFromDate(), getToDate());
    }

    public List<StaffLeave> createStaffLeave(LeaveType leaveType, Staff staff, Date fromDate, Date toDate) {
        if (leaveType == null || staff == null) {
            return null;
        }

        String sql = "";
        HashMap hm = new HashMap();
        sql = "select ss from StaffLeave ss "
                + " where ss.retired=false "
                + " and ss.form.retired=false "
                + " and ss.leaveDate between :frm  and :to "
                + " and ss.staff=:stf"
                + " and ss.leaveType in :ltp ";
        hm.put("frm", fromDate);
        hm.put("to", toDate);
        hm.put("stf", staff);
        hm.put("ltp", leaveType.getLeaveTypes());

        return staffLeaveFacade.findBySQL(sql, hm, TemporalType.DATE);
    }

//    public List<StaffLeave> createStaffLeave(LeaveType leaveType, Staff staff, Date fromDate, Date toDate) {
//        if (leaveType == null || staff == null) {
//            return null;
//        }
//
//        String sql = "";
//        HashMap hm = new HashMap();
//        sql = "select ss from StaffLeave ss "
//                + " where ss.retired=false "
//                + " and ss.leaveDate between :frm  and :to "
//                + " and ss.staff=:stf"
//                + " and ss.leaveType in :ltp ";
//        hm.put("frm", fromDate);
//        hm.put("to", toDate);
//        hm.put("stf", staff);
//        hm.put("ltp", leaveType.getLeaveTypes());
//
//        return staffLeaveFacade.findBySQL(sql, hm, TemporalType.DATE);
//    }
    List<StaffLeaveBallance> staffLeaveBallances;

    public List<StaffLeaveBallance> getStaffLeaveBallances() {
        return staffLeaveBallances;
    }

    public void setStaffLeaveBallances(List<StaffLeaveBallance> staffLeaveBallances) {
        this.staffLeaveBallances = staffLeaveBallances;
    }

    public void createStaffLeaveAggregate() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = "select new com.divudi.data.hr.StaffLeaveBallance(ss.staff,ss.leaveType,sum(ss.qty)) "
                + " from StaffLeave ss "
                + " where ss.retired=false "
                + " and ss.leaveDate between :frm  and :to ";
        hm.put("frm", fromDate);
        hm.put("to", toDate);

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
            sql += " and ss.staff.staffCategory=:stfCat";
            hm.put("stfCat", getReportKeyWord().getStaffCategory());
        }

        if (getReportKeyWord().getDesignation() != null) {
            sql += " and ss.staff.designation=:des";
            hm.put("des", getReportKeyWord().getDesignation());
        }

        if (getReportKeyWord().getRoster() != null) {
            sql += " and ss.roster=:rs ";
            hm.put("rs", getReportKeyWord().getRoster());
        }

        if (getReportKeyWord().getLeaveType() != null) {
            List<LeaveType> list = getReportKeyWord().getLeaveType().getLeaveTypes();
            sql += " and ss.leaveType in :ltp ";
            hm.put("ltp", list);
        }

        sql += " group by ss.staff,ss.leaveType"
                + " order by ss.staff.codeInterger,ss.leaveType ";

        staffLeaveBallances = (List<StaffLeaveBallance>) (Object) staffLeaveFacade.findAggregates(sql, hm, TemporalType.DATE);
    }

    public void createStaffAttendanceAggregate() {
        String sql = "";

        HashMap hm = new HashMap();
        sql = "select new com.divudi.data.hr.DepartmentAttendance(ss.staff.workingDepartment,"
                + "FUNC('Date',ss.shiftDate),count(distinct(ss.staff))) "
                + " from StaffShift ss "
                + " where ss.retired=false "
                + " and (ss.startRecord.recordTimeStamp is not null "
                + " and ss.endRecord.recordTimeStamp is not null ) "
                + " and ss.shiftDate between :frm  and :to ";
        hm.put("frm", fromDate);
        hm.put("to", toDate);

        if (getReportKeyWord().getStaff() != null) {
            sql += " and ss.staff=:stf ";
            hm.put("stf", getReportKeyWord().getStaff());
        }

        if (getReportKeyWord().getSex() != null) {
            sql += " and ss.staff.person.sex=:sx ";
            hm.put("sx", getReportKeyWord().getStaff());
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
            sql += " and ss.roster=:rs ";
            hm.put("rs", getReportKeyWord().getRoster());
        }

        sql += " group by FUNC('Date',ss.shiftDate),ss.staff.workingDepartment"
                + " order by ss.shiftDate,ss.staff.workingDepartment.name";

        departmentAttendances = (List<DepartmentAttendance>) (Object) staffLeaveFacade.findAggregates(sql, hm, TemporalType.DATE);
        calTotal();
    }

    double totalAttendance;

    public double getTotalAttendance() {
        return totalAttendance;
    }

    public void setTotalAttendance(double totalAttendance) {
        this.totalAttendance = totalAttendance;
    }

    private void calTotal() {
        totalAttendance = 0;

        if (departmentAttendances == null) {
            return;
        }
        for (DepartmentAttendance d : departmentAttendances) {
            totalAttendance += d.getPresent();
        }

    }

    private List<Staff> fetchStaff() {
        String sql = "";

        HashMap hm = new HashMap();
        sql = "select distinct(ss.staff)"
                + " from StaffShift ss "
                + " where ss.retired=false "
                //                + " and ((ss.startRecord.recordTimeStamp is not null "
                //                + " and ss.endRecord.recordTimeStamp is not null) "
                //                + " or (ss.leaveType is not null) ) "
                + " and ss.shiftStartTime between :frm  and :to ";
        hm.put("frm", fromDate);
        hm.put("to", toDate);

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
            sql += " and ss.roster=:rs ";
            hm.put("rs", getReportKeyWord().getRoster());
        }

        sql += " order by ss.staff.codeInterger";

        return staffFacade.findBySQL(sql, hm, TemporalType.TIMESTAMP);
    }

    private List<Staff> fetchStaff(Date f, Date t) {
        String sql = "";

        HashMap hm = new HashMap();
        sql = "select distinct(ss.staff)"
                + " from StaffShift ss "
                + " where ss.retired=false "
                //                + " and ((ss.startRecord.recordTimeStamp is not null "
                //                + " and ss.endRecord.recordTimeStamp is not null) "
                //                + " or (ss.leaveType is not null) ) "
                + " and ss.shiftStartTime between :frm  and :to ";
        hm.put("frm", f);
        hm.put("to", t);

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
            sql += " and ss.roster=:rs ";
            hm.put("rs", getReportKeyWord().getRoster());
        }

        sql += " order by ss.staff.codeInterger";

        return staffFacade.findBySQL(sql, hm, TemporalType.TIMESTAMP);
    }

    private long fetchWorkedDays(Staff staff) {
        String sql = "";

        HashMap hm = new HashMap();
        sql = "select count(distinct(FUNC('Date',ss.shiftDate)))"
                + " from StaffShift ss "
                + " where ss.retired=false"
                + " and ss.staff=:stf "
                + " and ((ss.startRecord.recordTimeStamp is not null "
                + " and ss.endRecord.recordTimeStamp is not null)) "
                //                + " or (ss.leaveType is not null) ) "
                + " and ss.shiftDate between :frm  and :to ";
        hm.put("frm", fromDate);
        hm.put("to", toDate);
        hm.put("stf", staff);

//        if (getReportKeyWord().getStaff() != null) {
//            sql += " and ss.staff=:stf ";
//            hm.put("stf", getReportKeyWord().getStaff());
//        }
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
            sql += " and ss.roster=:rs ";
            hm.put("rs", getReportKeyWord().getRoster());
        }

//        sql += " group by FUNC('Date',ss.shiftDate)";
        return staffFacade.findLongByJpql(sql, hm, TemporalType.DATE);
    }

    private void fetchWorkedTimeTemporary(Staff staff) {
        String sql = "";

//                + " sum(ss.workedWithinTimeFrameVarified+ss.leavedTime),"
//                + " sum(ss.extraTimeFromStartRecordVarified+ss.extraTimeFromEndRecordVarified),"
//                + " sum((ss.extraTimeFromStartRecordVarified+ss.extraTimeFromEndRecordVarified)*ss.multiplyingFactorOverTime*ss.overTimeValuePerSecond)
        HashMap hm = new HashMap();
        sql = "select ss from StaffShift ss "
                + " where ss.retired=false "
                + " and type(ss)!=:tp"
                + " and ss.staff=:stf"
                + " and ss.dayType not in :dtp "
                + " and ss.shiftDate between :frm  and :to ";
        System.out.println("hm = " + hm);
        hm.put("frm", fromDate);
        hm.put("to", toDate);
        hm.put("tp", StaffShiftExtra.class);
        hm.put("stf", staff);
        hm.put("dtp", Arrays.asList(new DayType[]{DayType.DayOff, DayType.MurchantileHoliday, DayType.SleepingDay, DayType.Poya}));

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
            sql += " and ss.roster=:rs ";
            hm.put("rs", getReportKeyWord().getRoster());
        }

        sql += " order by ss.id ";
        System.out.println("sql = " + sql);
        System.out.println("hm = " + hm);

        List<StaffShift> sss = staffShiftFacade.findBySQL(sql, hm, TemporalType.DATE);

        for (StaffShift ss : sss) {
            System.out.println("ss = " + ss.getId());
            System.out.println("ss.workedWithinTimeFrameVarified() = " + ss.getWorkedWithinTimeFrameVarified());
            System.out.println("ss.leavedTime = " + ss.getLeavedTime());
            System.out.println("ss.extraTimeFromStartRecordVarified = " + ss.getExtraTimeFromStartRecordVarified());
            System.out.println("ss.extraTimeFromEndRecordVarified = " + ss.getExtraTimeFromEndRecordVarified());
            System.out.println("ss.multiplyingFactorOverTime = " + ss.getMultiplyingFactorOverTime());
            System.out.println("ss.overTimeValuePerSecond = " + ss.getOverTimeValuePerSecond());

            //                + " sum(ss.workedWithinTimeFrameVarified+ss.leavedTime),"
//                + " sum(ss.extraTimeFromStartRecordVarified+ss.extraTimeFromEndRecordVarified),"
//                + " sum((ss.extraTimeFromStartRecordVarified+ss.extraTimeFromEndRecordVarified)*ss.multiplyingFactorOverTime*ss.overTimeValuePerSecond)
        }

    }

    private List<Object[]> fetchWorkedTime(Staff staff) {
        String sql = "";

        HashMap hm = new HashMap();
        sql = "select ss.dayOfWeek,"
                + " sum(ss.workedWithinTimeFrameVarified+ss.leavedTime),"
                + " sum(ss.extraTimeFromStartRecordVarified+ss.extraTimeFromEndRecordVarified),"
                + " sum((ss.extraTimeFromStartRecordVarified+ss.extraTimeFromEndRecordVarified)*ss.multiplyingFactorOverTime*ss.overTimeValuePerSecond)"
                + " from StaffShift ss "
                + " where ss.retired=false "
                + " and type(ss)!=:tp"
                + " and ss.staff=:stf"
                //                + " and ss.leavedTime=0 "
                + " and ss.dayType not in :dtp "
                //                + " and ((ss.startRecord.recordTimeStamp is not null "
                //                + " and ss.endRecord.recordTimeStamp is not null) "
                //                + " or (ss.leaveType is not null) ) "
                + " and ss.shiftDate between :frm  and :to ";
        hm.put("frm", fromDate);
        hm.put("to", toDate);
        hm.put("tp", StaffShiftExtra.class);
        hm.put("stf", staff);
        hm.put("dtp", Arrays.asList(new DayType[]{DayType.DayOff, DayType.MurchantileHoliday, DayType.SleepingDay, DayType.Poya}));

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
            sql += " and ss.roster=:rs ";
            hm.put("rs", getReportKeyWord().getRoster());
        }

        sql += " group by ss.dayOfWeek "
                + " order by ss.dayOfWeek,ss.staff.codeInterger ";
        return staffShiftFacade.findAggregates(sql, hm, TemporalType.TIMESTAMP);
    }

    private List<Object[]> fetchWorkedTimeByDateOnly(Staff staff) {
        String sql = "";

        HashMap hm = new HashMap();
        sql = "select ss.dayOfWeek,"
                + " sum(ss.workedWithinTimeFrameVarified+ss.leavedTime),"
                + " sum(ss.extraTimeFromStartRecordVarified+ss.extraTimeFromEndRecordVarified),"
                + " sum((ss.extraTimeFromStartRecordVarified+ss.extraTimeFromEndRecordVarified)*ss.multiplyingFactorOverTime*ss.overTimeValuePerSecond), "
                + " ss, "
                + " sum(ss.leavedTime)"
                + " from StaffShift ss "
                + " where ss.retired=false "
                + " and type(ss)!=:tp"
                + " and ss.staff=:stf"
                //                + " and ss.leavedTime=0 "
                + " and ss.dayType not in :dtp "
                //                + " and ((ss.startRecord.recordTimeStamp is not null "
                //                + " and ss.endRecord.recordTimeStamp is not null) "
                //                + " or (ss.leaveType is not null) ) "
                + " and ss.shiftDate between :frm  and :to ";
        hm.put("frm", fromDate);
        hm.put("to", toDate);
        hm.put("tp", StaffShiftExtra.class);
        hm.put("stf", staff);
        hm.put("dtp", Arrays.asList(new DayType[]{DayType.DayOff, DayType.MurchantileHoliday, DayType.SleepingDay, DayType.Poya}));

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
            sql += " and ss.roster=:rs ";
            hm.put("rs", getReportKeyWord().getRoster());
        }

        sql += " group by ss.dayOfWeek "
                + " order by ss.dayOfWeek,ss.staff.codeInterger ";
        return staffShiftFacade.findAggregates(sql, hm, TemporalType.DATE);
    }
    
    private List<Object[]> fetchWorkedTimeByDateOnly(Staff staff,Date f,Date t) {
        String sql = "";

        HashMap hm = new HashMap();
        sql = "select ss.dayOfWeek,"
                + " sum(ss.workedWithinTimeFrameVarified+ss.leavedTime),"
                + " sum(ss.extraTimeFromStartRecordVarified+ss.extraTimeFromEndRecordVarified),"
                + " sum((ss.extraTimeFromStartRecordVarified+ss.extraTimeFromEndRecordVarified)*ss.multiplyingFactorOverTime*ss.overTimeValuePerSecond), "
                + " ss, "
                + " sum(ss.leavedTime)"
                + " from StaffShift ss "
                + " where ss.retired=false "
                + " and type(ss)!=:tp"
                + " and ss.staff=:stf"
                //                + " and ss.leavedTime=0 "
                + " and ss.dayType not in :dtp "
                //                + " and ((ss.startRecord.recordTimeStamp is not null "
                //                + " and ss.endRecord.recordTimeStamp is not null) "
                //                + " or (ss.leaveType is not null) ) "
                + " and ss.shiftDate between :frm  and :to ";
        hm.put("frm", f);
        hm.put("to", t);
        hm.put("tp", StaffShiftExtra.class);
        hm.put("stf", staff);
        hm.put("dtp", Arrays.asList(new DayType[]{DayType.DayOff, DayType.MurchantileHoliday, DayType.SleepingDay, DayType.Poya}));

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
            sql += " and ss.roster=:rs ";
            hm.put("rs", getReportKeyWord().getRoster());
        }

        sql += " group by ss.dayOfWeek "
                + " order by ss.dayOfWeek,ss.staff.codeInterger ";
        return staffShiftFacade.findAggregates(sql, hm, TemporalType.DATE);
    }

    private List<Object[]> fetchStaffShiftData() {
        String sql = "";

        HashMap hm = new HashMap();
        sql = "select ss.staff,"
                + " sum(ss.earlyInVarified),"
                + " sum(ss.earlyOutVarified),"
                + " sum(ss.workedWithinTimeFrameVarified),"
                + " sum(ss.workedOutSideTimeFrameVarified),"
                + " sum(ss.workedTimeVarified),"
                + " sum(ss.lateInVarified),"
                + " sum(ss.lateOutVarified),"
                + " sum(ss.extraTimeFromStartRecordVarified),"
                + " sum(ss.extraTimeFromEndRecordVarified),"
                + " sum(ss.extraTimeCompleteRecordVarified),"
                + "sum(ss.leavedTime),"
                + " sum(ss.leavedTimeNoPay),"
                + " sum(ss.leavedTimeOther)"
                + " from StaffShift ss "
                + " where ss.retired=false "
                //                + " and ((ss.startRecord.recordTimeStamp is not null "
                //                + " and ss.endRecord.recordTimeStamp is not null) "
                //                + " or (ss.leaveType is not null)) "
                + " and ss.shiftDate between :frm  and :to ";
        hm.put("frm", fromDate);
        hm.put("to", toDate);

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
            sql += " and ss.roster=:rs ";
            hm.put("rs", getReportKeyWord().getRoster());
        }

        sql += " group by ss.staff "
                + " order by ss.staff.codeInterger";
        return staffShiftFacade.findAggregates(sql, hm, TemporalType.DATE);
    }

    private long fetchExtraDutyDays(Staff staff) {
        String sql = "";

        HashMap hm = new HashMap();
        sql = "select count(distinct(FUNC('Date',ss.shiftDate)))"
                + " from StaffShift ss "
                + " where ss.retired=false"
                + " and ss.staff=:stf "
                + " and (ss.extraTimeFromStartRecordVarified+ss.extraTimeFromEndRecordVarified)>0"
                //                + " and ((ss.startRecord.recordTimeStamp is not null "
                //                + " and ss.endRecord.recordTimeStamp is not null) "
                //                + " or (ss.leaveType is not null) ) "
                + " and ss.shiftDate between :frm  and :to ";
        hm.put("frm", fromDate);
        hm.put("to", toDate);
        hm.put("stf", staff);

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
            sql += " and ss.roster=:rs ";
            hm.put("rs", getReportKeyWord().getRoster());
        }

//        sql += " group by FUNC('Date',ss.shiftDate)";
        return staffFacade.findLongByJpql(sql, hm, TemporalType.DATE);
    }

    private long fetchLateDays(Staff staff) {
        String sql = "";

        HashMap hm = new HashMap();
        sql = "select count(distinct(FUNC('Date',ss.shiftDate)))"
                + " from StaffShift ss "
                + " where ss.retired=false"
                + " and ss.staff=:stf "
                + " and (ss.lateInVarified)>0 "
                //                + " and ((ss.startRecord.recordTimeStamp is not null "
                //                + " and ss.endRecord.recordTimeStamp is not null) "
                //                + " or (ss.leaveType is not null) ) "
                + " and ss.shiftDate between :frm  and :to ";
        hm.put("frm", fromDate);
        hm.put("to", toDate);
        hm.put("stf", staff);

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
            sql += " and ss.roster=:rs ";
            hm.put("rs", getReportKeyWord().getRoster());
        }

//        sql += " group by FUNC('Date',ss.shiftDate)";
        return staffFacade.findLongByJpql(sql, hm, TemporalType.DATE);
    }

    private long fetchDayOff(Staff staff) {
        String sql = "";

        HashMap hm = new HashMap();
        sql = "select count(distinct(FUNC('Date',ss.shiftDate)))"
                + " from StaffShift ss "
                + " where ss.retired=false"
                + " and ss.staff=:stf "
                + " and ss.shift.dayType=:dtp"
                //                + " and ((ss.startRecord.recordTimeStamp is not null "
                //                + " and ss.endRecord.recordTimeStamp is not null) "
                //                + " or (ss.leaveType is not null) ) "
                + " and ss.shiftDate between :frm  and :to ";
        hm.put("frm", fromDate);
        hm.put("to", toDate);
        hm.put("stf", staff);
        hm.put("dtp", DayType.DayOff);

        if (getReportKeyWord().getStaff() != null) {
            sql += " and ss.staff=:stf ";
            hm.put("stf", getReportKeyWord().getStaff());
        }

        if (getReportKeyWord().getDepartment() != null) {
            sql += " and ss.staff.workingDepartment=:dep ";
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

        if (getReportKeyWord().getRoster() != null) {
            sql += " and ss.roster=:rs ";
            hm.put("rs", getReportKeyWord().getRoster());
        }

//        sql += " group by FUNC('Date',ss.shiftDate)";
        return staffFacade.findLongByJpql(sql, hm, TemporalType.DATE);
    }

    private long fetchWorkedDays(Staff staff, DayType dayType, boolean fullShifts, boolean halfShift) {
        String sql = "";

        HashMap hm = new HashMap();
        sql = "select count(distinct(FUNC('Date',ss.shiftDate)))"
                + " from StaffShift ss "
                + " where ss.retired=false"
                + " and ss.staff=:stf "
                + " and ss.dayType=:dtp"
                + " and ( ss.startRecord.recordTimeStamp is not null "
                + " and ss.endRecord.recordTimeStamp is not null ) "
                //                + " or (ss.leaveType is not null) ) "
                + " and ss.shiftDate between :frm  and :to ";
        hm.put("frm", fromDate);
        hm.put("to", toDate);
        hm.put("stf", staff);
        hm.put("dtp", dayType);
//2246740
        if (fullShifts && halfShift) {

        } else if (fullShifts) {
            sql += " and ss.shift.halfShift = false ";
        } else if (halfShift) {
            sql += " and ss.shift.halfShift = true ";
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
            sql += " and ss.roster=:rs ";
            hm.put("rs", getReportKeyWord().getRoster());
        }

//        StaffShift ss;
//        sql += " group by FUNC('Date',ss.shiftDate)";
        return staffFacade.findLongByJpql(sql, hm, TemporalType.DATE);
    }

    private double fetchWorkedDays(Staff staff, DayType dayType) {
        long fs = fetchWorkedDays(staff, dayType, true, false);
        //System.out.println("fs = " + fs);
        long hs = fetchWorkedDays(staff, dayType, false, true);
        //System.out.println("hs = " + hs);

        double fullAndHald = fs + (hs * .5);
        //System.out.println("fullAndHald = " + fullAndHald);

        return fullAndHald;
    }

//    private long fetchWorkedDays(Staff staff, DayType dayType) {
//        long fs = fetchWorkedDays(staff, dayType, true, false);
//        long hs = fetchWorkedDays(staff, dayType, false, true);
//        
//        double fullAndHald = fs + (hs*.5);
//        
//        return fullAndHald;
//        
//        String sql = "";
//
//        HashMap hm = new HashMap();
//        sql = "select count(distinct(FUNC('Date',ss.shiftDate)))"
//                + " from StaffShift ss "
//                + " where ss.retired=false"
//                + " and ss.staff=:stf "
//                + " and ss.dayType=:dtp"
//                + " and ( ss.startRecord.recordTimeStamp is not null "
//                + " and ss.endRecord.recordTimeStamp is not null ) "
//                //                + " or (ss.leaveType is not null) ) "
//                + " and ss.shiftDate between :frm  and :to ";
//        hm.put("frm", fromDate);
//        hm.put("to", toDate);
//        hm.put("stf", staff);
//        hm.put("dtp", dayType);
//
//        if (getReportKeyWord().getStaff() != null) {
//            sql += " and ss.staff=:stf ";
//            hm.put("stf", getReportKeyWord().getStaff());
//        }
//
//        if (getReportKeyWord().getDepartment() != null) {
//            sql += " and ss.staff.workingDepartment=:dep ";
//            hm.put("dep", getReportKeyWord().getDepartment());
//        }
//
//        if (getReportKeyWord().getInstitution() != null) {
//            sql += " and ss.staff.workingDepartment.institution=:ins ";
//            hm.put("ins", getReportKeyWord().getInstitution());
//        }
//
//        if (getReportKeyWord().getStaffCategory() != null) {
//            sql += " and ss.staff.staffCategory=:stfCat";
//            hm.put("stfCat", getReportKeyWord().getStaffCategory());
//        }
//
//        if (getReportKeyWord().getDesignation() != null) {
//            sql += " and ss.staff.designation=:des";
//            hm.put("des", getReportKeyWord().getDesignation());
//        }
//
//        if (getReportKeyWord().getRoster() != null) {
//            sql += " and ss.roster=:rs ";
//            hm.put("rs", getReportKeyWord().getRoster());
//        }
//
//        StaffShift ss;
//
////        sql += " group by FUNC('Date',ss.shiftDate)";
//        return staffFacade.findLongByJpql(sql, hm, TemporalType.DATE);
//    }
//
    private List<MonthEndRecord> monthEndRecords;

    public FinalVariables getFinalVariables() {
        return finalVariables;
    }

    public void setFinalVariables(FinalVariables finalVariables) {
        this.finalVariables = finalVariables;
    }

    @EJB
    HumanResourceBean humanResourceBean;

    public void createMonthEndReport() {
        List<Staff> staffList = fetchStaff();
        monthEndRecords = new ArrayList<>();
        for (Staff stf : staffList) {
            MonthEndRecord monthEnd = new MonthEndRecord();
            monthEnd.setStaff(stf);
            monthEnd.setWorkedDays(fetchWorkedDays(stf));
            monthEnd.setLeave_annual(humanResourceBean.calStaffLeave(stf, LeaveType.Annual, getFromDate(), getToDate()));
            monthEnd.setLeave_casual(humanResourceBean.calStaffLeave(stf, LeaveType.Casual, getFromDate(), getToDate()));
            monthEnd.setLeave_medical(humanResourceBean.calStaffLeave(stf, LeaveType.Medical, getFromDate(), getToDate()));
            monthEnd.setLeave_nopay(humanResourceBean.calStaffLeave(stf, LeaveType.No_Pay, getFromDate(), getToDate()));
            monthEnd.setLeave_dutyLeave(humanResourceBean.calStaffLeave(stf, LeaveType.DutyLeave, getFromDate(), getToDate()));
            monthEnd.setExtraDutyDays(fetchExtraDutyDays(stf));
            monthEnd.setLatedays(fetchLateDays(stf));
            monthEnd.setLateNoPays(humanResourceBean.calStaffLeaveSystem(stf, LeaveType.No_Pay, getFromDate(), getToDate()));
            monthEnd.setDayoff(fetchWorkedDays(stf, DayType.DayOff));
            monthEnd.setSleepingDays(fetchWorkedDays(stf, DayType.SleepingDay));
            monthEnd.setPoyaDays(fetchWorkedDays(stf, DayType.Poya));
            monthEnd.setMerhchantileDays(fetchWorkedDays(stf, DayType.MurchantileHoliday));
            monthEndRecords.add(monthEnd);
        }
    }

    public String fromWeekelyOverTimeReportToStaffFingerPrintAnalysis(Staff staff) {
        shiftFingerPrintAnalysisController.setFromDate(fromDate);
        shiftFingerPrintAnalysisController.setToDate(toDate);
        shiftFingerPrintAnalysisController.setStaff(staff);
        shiftFingerPrintAnalysisController.makeTableNull();
        shiftFingerPrintAnalysisController.createShiftTableByStaff();
        shiftFingerPrintAnalysisController.setBackButtonIsActive(true);
        shiftFingerPrintAnalysisController.setBackButtonPage("/hr/hr_report_month_end_work_time_miniuts");
        return "/hr/hr_shift_table_finger_print_by_staff";
    }

    public void createMonthEndWorkTimeReport() {
        Long dateCount = commonFunctions.getDayCount(getFromDate(), getToDate());
        if (dateCount > 8) {
            UtilityController.addErrorMessage("Check Date Range .Date range should be within 8 days");
            return;
        }
        System.err.println(dateCount);
//        double numOfWeeks = dateCount / 7.0;

        List<Staff> staffList = fetchStaff();
        weekDayWorks = new ArrayList<>();
        for (Staff stf : staffList) {
            WeekDayWork weekDayWork = new WeekDayWork();
            weekDayWork.setStaff(stf);
//            List<Object[]> list = fetchWorkedTime(stf);

            List<Object[]> list = fetchWorkedTimeByDateOnly(stf); // Added by Buddhika

            System.err.println("list = " + list);
            System.out.println("list size " + list.size());

//            fetchWorkedTimeTemporary(stf); // For Testing
            int i = 0;
            for (Object[] obj : list) {
                i++;
                System.out.println("i" + i);
                Integer dayOfWeek = (Integer) obj[0] != null ? (Integer) obj[0] : -1;
                System.out.println("dayOfWeek = " + dayOfWeek);
                System.out.println("obj[0] = " + obj[0]);
                Double value = (Double) obj[1] != null ? (Double) obj[1] : 0;
                Double valueExtra = (Double) obj[2] != null ? (Double) obj[2] : 0;
                Double totalExtraDuty = (Double) obj[3] != null ? (Double) obj[3] : 0;
                StaffShift ss = (StaffShift) obj[4] != null ? (StaffShift) obj[4] : new StaffShift();
                Double leavedTimeValue = (Double) obj[5] != null ? (Double) obj[5] : 0;

                System.err.println("Staff " + stf.getCodeInterger() + " :Value : " + value);
                if (ss.getShift() != null && ss.getShift().getLeaveHourHalf() != 0 && leavedTimeValue > 0) {
                    System.out.println("value = " + value);
                    System.out.println("leavedTimeValue = " + leavedTimeValue);
                    System.out.println("ss.getShift().getDurationMin()*60 = " + ss.getShift().getDurationMin() * 60);
                    if ((ss.getShift().getDurationMin() * 60) < value) {
                        value = ss.getShift().getDurationMin() * 60;
                        System.out.println("4.b dbl(else) = " + value);
                    }
                }
                switch (dayOfWeek) {
                    case Calendar.SUNDAY:
                        weekDayWork.setSunDay(value);
                        weekDayWork.setSunDayExtra(valueExtra);
                        System.out.println("sunday = ");
                        break;
                    case Calendar.MONDAY:
                        weekDayWork.setMonDay(value);
                        weekDayWork.setMonDayExtra(valueExtra);
                        break;
                    case Calendar.TUESDAY:
                        weekDayWork.setTuesDay(value);
                        weekDayWork.setTuesDayExtra(valueExtra);
                        break;
                    case Calendar.WEDNESDAY:
                        weekDayWork.setWednesDay(value);
                        weekDayWork.setWednesDayExtra(valueExtra);
                        break;
                    case Calendar.THURSDAY:
                        weekDayWork.setThursDay(value);
                        weekDayWork.setThursDayExtra(valueExtra);
                        break;
                    case Calendar.FRIDAY:
                        weekDayWork.setFriDay(value);
                        weekDayWork.setFriDayExtra(valueExtra);
                        break;
                    case Calendar.SATURDAY:
                        weekDayWork.setSaturDay(value);
                        weekDayWork.setSaturDayExtra(valueExtra);
                        break;
                }

                weekDayWork.setTotal(weekDayWork.getTotal() + value);
                weekDayWork.setExtraDutyValue(weekDayWork.getExtraDutyValue() + totalExtraDuty);
                weekDayWork.setExtraDuty(weekDayWork.getExtraDuty() + valueExtra);

            }

//            if (stf.getWorkingTimeForOverTimePerWeek() != 0 && numOfWeeks != 0) {
//                double normalWorkTime = numOfWeeks * stf.getWorkingTimeForOverTimePerWeek() * 60 * 60;
//                double overTime = weekDayWork.getTotal() - normalWorkTime;
//                if (overTime > 0) {
//                    weekDayWork.setOverTime(overTime);
//                }
//            }
            weekDayWork.setOverTime(humanResourceBean.getOverTimeFromRoster(stf.getWorkingTimeForOverTimePerWeek(), 1, weekDayWork.getTotal()));

            //Fetch Basic
            double value = humanResourceBean.getOverTimeValue(stf, getToDate());

            if (value != 0) {
                weekDayWork.setBasicPerSecond(value / (200 * 60 * 60));
            }

            weekDayWorks.add(weekDayWork);
        }
    }

    public void createMonthEndWorkTimeReportForMonth() {

        if (salaryCycle == null) {
            JsfUtil.addErrorMessage("Select Slary Cycle");
            return;
        }

        System.out.println("calculating over time in minutes");
        Long dateCount = commonFunctions.getDayCount(getSalaryCycle().getWorkedFromDate(), getSalaryCycle().getWorkedToDate());
        System.out.println("dateCount = " + dateCount);
        Long numOfWeeks = dateCount / 7;
        System.out.println("numOfWeeks = " + numOfWeeks);

        Date fDate = getSalaryCycle().getWorkedFromDate();
        System.out.println("fDate = " + fDate);
        Calendar frmCal = Calendar.getInstance();
        frmCal.setTime(fDate);
        frmCal.set(Calendar.HOUR, 0);
        frmCal.set(Calendar.MINUTE, 0);
        frmCal.set(Calendar.SECOND, 0);
        frmCal.set(Calendar.MILLISECOND, 1);

        Calendar toCal = Calendar.getInstance();
        toCal.setTime(fDate);
        toCal.add(Calendar.DATE, 6);
        toCal.set(Calendar.HOUR, 23);
        toCal.set(Calendar.MINUTE, 59);
        toCal.set(Calendar.SECOND, 59);
        toCal.set(Calendar.MILLISECOND, 999);

        System.err.println("FROM " + frmCal.getTime());
        System.err.println("TO " + toCal.getTime());

        overTimeAllMonths = new ArrayList<>();
        summeryForMonths = new ArrayList<>();

        for (int i = 0; i < numOfWeeks; i++) {
            OverTimeAllMonth otam = new OverTimeAllMonth();
            StringBuilder date = new StringBuilder();

            DateFormat df = new SimpleDateFormat(" yyyy MMMM dd hh:mm:ss a ");
            String formattedDate = df.format(frmCal.getTime());
            date.append(formattedDate);
            date.append(" To ");
            formattedDate = df.format(toCal.getTime());
            date.append(formattedDate);
            otam.setDateRange(date.toString());
            otam.setDayWorks(createMonthEndWorkTimeReport(frmCal.getTime(), toCal.getTime(), i));
            System.out.println("otam.getDateRange() = " + otam.getDateRange());
            System.out.println("otam.getDayWorks().size() = " + otam.getDayWorks().size());
            overTimeAllMonths.add(otam);

            setSummeryTableForMonth(otam, overTimeAllMonths.size());

            frmCal.add(Calendar.DATE, 7);
            toCal.add(Calendar.DATE, 7);
            System.err.println("FROM " + frmCal.getTime());
            System.err.println("TO " + toCal.getTime());
        }

    }

    public List<WeekDayWork> createMonthEndWorkTimeReport(Date frDate, Date tDate, int j) {
        Long dateCount = commonFunctions.getDayCount(frDate, tDate);

        System.err.println(dateCount);
//        double numOfWeeks = dateCount / 7.0;

        List<Staff> staffList = fetchStaff(frDate, tDate);
        System.out.println("staffList = " + staffList.size());
        weekDayWorks = new ArrayList<>();
        for (Staff stf : staffList) {
            WeekDayWork weekDayWork = new WeekDayWork();
            weekDayWork.setStaff(stf);
//            List<Object[]> list = fetchWorkedTime(stf);

            List<Object[]> list = fetchWorkedTimeByDateOnly(stf,frDate,tDate); 

            System.err.println("list = " + list);
            System.out.println("list size " + list.size());

//            fetchWorkedTimeTemporary(stf); // For Testing
            int i = 0;
            for (Object[] obj : list) {
                i++;
                System.out.println("i" + i);
                Integer dayOfWeek = (Integer) obj[0] != null ? (Integer) obj[0] : -1;
                System.out.println("dayOfWeek = " + dayOfWeek);
                System.out.println("obj[0] = " + obj[0]);
                Double value = (Double) obj[1] != null ? (Double) obj[1] : 0;
                Double valueExtra = (Double) obj[2] != null ? (Double) obj[2] : 0;
                Double totalExtraDuty = (Double) obj[3] != null ? (Double) obj[3] : 0;
                StaffShift ss = (StaffShift) obj[4] != null ? (StaffShift) obj[4] : new StaffShift();
                Double leavedTimeValue = (Double) obj[5] != null ? (Double) obj[5] : 0;

                System.err.println("Staff " + stf.getCodeInterger() + " :Value : " + value);
                if (ss.getShift() != null && ss.getShift().getLeaveHourHalf() != 0 && leavedTimeValue > 0) {
                    System.out.println("value = " + value);
                    System.out.println("leavedTimeValue = " + leavedTimeValue);
                    System.out.println("ss.getShift().getDurationMin()*60 = " + ss.getShift().getDurationMin() * 60);
                    if ((ss.getShift().getDurationMin() * 60) < value) {
                        value = ss.getShift().getDurationMin() * 60;
                        System.out.println("4.b dbl(else) = " + value);
                    }
                }
                switch (dayOfWeek) {
                    case Calendar.SUNDAY:
                        weekDayWork.setSunDay(value);
                        weekDayWork.setSunDayExtra(valueExtra);
                        System.out.println("sunday = ");
                        break;
                    case Calendar.MONDAY:
                        weekDayWork.setMonDay(value);
                        weekDayWork.setMonDayExtra(valueExtra);
                        break;
                    case Calendar.TUESDAY:
                        weekDayWork.setTuesDay(value);
                        weekDayWork.setTuesDayExtra(valueExtra);
                        break;
                    case Calendar.WEDNESDAY:
                        weekDayWork.setWednesDay(value);
                        weekDayWork.setWednesDayExtra(valueExtra);
                        break;
                    case Calendar.THURSDAY:
                        weekDayWork.setThursDay(value);
                        weekDayWork.setThursDayExtra(valueExtra);
                        break;
                    case Calendar.FRIDAY:
                        weekDayWork.setFriDay(value);
                        weekDayWork.setFriDayExtra(valueExtra);
                        break;
                    case Calendar.SATURDAY:
                        weekDayWork.setSaturDay(value);
                        weekDayWork.setSaturDayExtra(valueExtra);
                        break;
                }

                weekDayWork.setTotal(weekDayWork.getTotal() + value);
                weekDayWork.setExtraDutyValue(weekDayWork.getExtraDutyValue() + totalExtraDuty);
                weekDayWork.setExtraDuty(weekDayWork.getExtraDuty() + valueExtra);

            }

//            if (stf.getWorkingTimeForOverTimePerWeek() != 0 && numOfWeeks != 0) {
//                double normalWorkTime = numOfWeeks * stf.getWorkingTimeForOverTimePerWeek() * 60 * 60;
//                double overTime = weekDayWork.getTotal() - normalWorkTime;
//                if (overTime > 0) {
//                    weekDayWork.setOverTime(overTime);
//                }
//            }
            weekDayWork.setOverTime(humanResourceBean.getOverTimeFromRoster(stf.getWorkingTimeForOverTimePerWeek(), 1, weekDayWork.getTotal()));

            //Fetch Basic
            double value = humanResourceBean.getOverTimeValue(stf, tDate);

            if (value != 0) {
                weekDayWork.setBasicPerSecond(value / (200 * 60 * 60));
            }

            weekDayWorks.add(weekDayWork);

        }

        return weekDayWorks;
    }

    public void setSummeryTableForMonth(OverTimeAllMonth otam, int count) {
        if (count == 1) {
            for (WeekDayWork weekDayWork : otam.getDayWorks()) {
                SummeryForMonth summeryForMonth = new SummeryForMonth();
                summeryForMonth.setStaff(weekDayWork.getStaff());
                summeryForMonth.setWeek1(weekDayWork.getTotal());
                summeryForMonth.setTotal(summeryForMonth.getTotal() + summeryForMonth.getWeek1());
                summeryForMonth.setOverTime(weekDayWork.getOverTime());
                summeryForMonth.setBasicPerSecond(weekDayWork.getBasicPerSecond());
                summeryForMonths.add(summeryForMonth);
            }
        } else {
            switch (count) {
                case 2:
                    int i = 0;
                    for (WeekDayWork weekDayWork : otam.getDayWorks()) {
                        summeryForMonths.get(i).setWeek2(weekDayWork.getTotal());
                        summeryForMonths.get(i).setTotal(summeryForMonths.get(i).getTotal() + summeryForMonths.get(i).getWeek2());
                        summeryForMonths.get(i).setOverTime(summeryForMonths.get(i).getOverTime() + weekDayWork.getOverTime());
                        i++;
                    }
                    break;
                case 3:
                    i = 0;
                    for (WeekDayWork weekDayWork : otam.getDayWorks()) {
                        summeryForMonths.get(i).setWeek3(weekDayWork.getTotal());
                        summeryForMonths.get(i).setTotal(summeryForMonths.get(i).getTotal() + summeryForMonths.get(i).getWeek3());
                        summeryForMonths.get(i).setOverTime(summeryForMonths.get(i).getOverTime() + weekDayWork.getOverTime());
                        i++;
                    }
                    break;
                case 4:
                    i = 0;
                    for (WeekDayWork weekDayWork : otam.getDayWorks()) {
                        summeryForMonths.get(i).setWeek4(weekDayWork.getTotal());
                        summeryForMonths.get(i).setTotal(summeryForMonths.get(i).getTotal() + summeryForMonths.get(i).getWeek4());
                        summeryForMonths.get(i).setOverTime(summeryForMonths.get(i).getOverTime() + weekDayWork.getOverTime());
                        i++;
                    }
                    break;
                case 5:
                    i = 0;
                    for (WeekDayWork weekDayWork : otam.getDayWorks()) {
                        summeryForMonths.get(i).setWeek5(weekDayWork.getTotal());
                        summeryForMonths.get(i).setTotal(summeryForMonths.get(i).getTotal() + summeryForMonths.get(i).getWeek5());
                        summeryForMonths.get(i).setOverTime(summeryForMonths.get(i).getOverTime() + weekDayWork.getOverTime());
                        i++;
                    }
                    break;
            }
        }
    }

    public void createMonthEndWorkTimeReportBySalaryGenerationMethod() {
        System.err.println("Create Month End Work Time Report By Salary Generation Method");
        Long dateCount = commonFunctions.getDayCount(getFromDate(), getToDate());
        System.out.println("From Date() = " + getFromDate());
        System.out.println("To Date() = " + getToDate());
        if (dateCount > 8) {
            UtilityController.addErrorMessage("Check Date Range .Date range should be within 8 days");
            return;
        }
        System.err.println("Date Count is " + dateCount);

        List<Staff> staffList = fetchStaff();
        weekDayWorks = new ArrayList<>();

        for (Staff stf : staffList) {
            System.out.println("Staff Member = " + stf.getPerson().getName() + "(" + stf.getCode() + ")");
            WeekDayWork weekDayWork = new WeekDayWork();
            weekDayWork.setStaff(stf);

            Date fd = this.getFromDate();
            Calendar frmCal = Calendar.getInstance();
            frmCal.setTime(fd);
            frmCal.setTime(fd);
            frmCal.set(Calendar.HOUR, 0);
            frmCal.set(Calendar.MINUTE, 0);
            frmCal.set(Calendar.SECOND, 0);
            frmCal.set(Calendar.MILLISECOND, 1);

            Calendar toCal = Calendar.getInstance();
            toCal.setTime(fromDate);
            toCal.add(Calendar.DATE, 1);
            toCal.add(Calendar.MILLISECOND, -1);

            int numberOfDays = (int) ((toDate.getTime() - fromDate.getTime())
                    / (1000 * 60 * 60 * 24));
//        
            for (int i = 0; i < numberOfDays + 1; i++) {

                Double value;
                Double valueExtra = 0.0;
                Double totalExtraDuty = 0.0;

                double workedWithinTimeFrameVarified = getHumanResourceBean().calculateWorkTimeForOverTimeByDate(frmCal.getTime(), toCal.getTime(), stf);
                value = workedWithinTimeFrameVarified;

                System.err.println("From " + frmCal.getTime() + " to " + toCal.getTime() + " workedtime varified is " + workedWithinTimeFrameVarified / (60 * 60));

                Integer dayOfWeek = frmCal.get(Calendar.DAY_OF_WEEK) + 1;

                switch (dayOfWeek) {
                    case Calendar.SUNDAY:
                        weekDayWork.setSunDay(value);
                        weekDayWork.setSunDayExtra(valueExtra);
                        System.out.println("sunday = ");
                        break;
                    case Calendar.MONDAY:
                        weekDayWork.setMonDay(value);
                        weekDayWork.setMonDayExtra(valueExtra);
                        break;
                    case Calendar.TUESDAY:
                        weekDayWork.setTuesDay(value);
                        weekDayWork.setTuesDayExtra(valueExtra);
                        break;
                    case Calendar.WEDNESDAY:
                        weekDayWork.setWednesDay(value);
                        weekDayWork.setWednesDayExtra(valueExtra);
                        break;
                    case Calendar.THURSDAY:
                        weekDayWork.setThursDay(value);
                        weekDayWork.setThursDayExtra(valueExtra);
                        break;
                    case Calendar.FRIDAY:
                        weekDayWork.setFriDay(value);
                        weekDayWork.setFriDayExtra(valueExtra);
                        break;
                    case Calendar.SATURDAY:
                        weekDayWork.setSaturDay(value);
                        weekDayWork.setSaturDayExtra(valueExtra);
                        break;
                }

                weekDayWork.setTotal(weekDayWork.getTotal() + value);
                weekDayWork.setExtraDutyValue(weekDayWork.getExtraDutyValue() + totalExtraDuty);
                weekDayWork.setExtraDuty(weekDayWork.getExtraDuty() + valueExtra);

                frmCal.add(Calendar.DATE, 1);
                toCal.add(Calendar.DATE, 1);
            }

            weekDayWork.setOverTime(humanResourceBean.getOverTimeFromRoster(stf.getWorkingTimeForOverTimePerWeek(), 1, weekDayWork.getTotal()));

            //Fetch Basic
            double value = humanResourceBean.getOverTimeValue(stf, getToDate());

            if (value != 0) {
                weekDayWork.setBasicPerSecond(value / (200 * 60 * 60));
            }

            weekDayWorks.add(weekDayWork);
        }
    }

    public void createMonthEndStaffShiftReport() {
        List<Object[]> list = fetchStaffShiftData();
        staffShifts = new ArrayList<>();
        for (Object[] obj : list) {
            StaffShift s = new StaffShift();
            Staff staff = (Staff) obj[0];
            Double earlyInVarified = (Double) obj[1];
            Double earlyOutVarified = (Double) obj[2];
            Double workedWithinTimeFrameVarified = (Double) obj[3];
            Double workedOutSideTimeFrameVarified = (Double) obj[4];
            Double workedTimeVarified = (Double) obj[5];
            Double lateInVarified = (Double) obj[6];
            Double lateOutVarified = (Double) obj[7];
            Double extraTimeFromStartRecordVarified = (Double) obj[8];
            Double extraTimeFromEndRecordVarified = (Double) obj[9];
            Double extraTimeCompleteRecordVarified = (Double) obj[10];
            Double leavedTime = (Double) obj[11];
            Double leavedTimeNoPay = (Double) obj[12];
            Double leavedTimeOther = (Double) obj[13];

            s.setStaff(staff);
            s.setEarlyInVarified(earlyInVarified == null ? 0 : earlyInVarified);
            s.setEarlyOutVarified(earlyOutVarified == null ? 0 : earlyOutVarified);
            s.setWorkedWithinTimeFrameVarified(workedWithinTimeFrameVarified == null ? 0 : workedWithinTimeFrameVarified);
            s.setWorkedOutSideTimeFrameVarified(workedOutSideTimeFrameVarified == null ? 0 : workedOutSideTimeFrameVarified);
            s.setWorkedTimeVarified(workedTimeVarified == null ? 0 : workedTimeVarified);
            s.setLateInVarified(lateInVarified == null ? 0 : lateInVarified);
            s.setLateOutVarified(lateOutVarified == null ? 0 : lateOutVarified);
            s.setExtraTimeFromStartRecordVarified(extraTimeFromStartRecordVarified == null ? 0 : extraTimeFromStartRecordVarified);
            s.setExtraTimeFromEndRecordVarified(extraTimeFromEndRecordVarified == null ? 0 : extraTimeFromEndRecordVarified);
            s.setExtraTimeCompleteRecordVarified(extraTimeCompleteRecordVarified == null ? 0 : extraTimeCompleteRecordVarified);
            s.setLeavedTime(leavedTime == null ? 0 : leavedTime);
            s.setLeavedTimeNoPay(leavedTimeNoPay == null ? 0 : leavedTimeNoPay);
            s.setLeavedTimeOther(leavedTimeOther == null ? 0 : leavedTimeOther);

            staffShifts.add(s);
        }
    }

    public void createMonthEndWorkTimeReportNoPay() {
        Long dateCount = commonFunctions.getDayCount(getFromDate(), getToDate());
        Long numOfWeeks = dateCount / 7;
        List<Staff> staffList = fetchStaff();
        weekDayWorks = new ArrayList<>();
        for (Staff stf : staffList) {
            WeekDayWork weekDayWork = new WeekDayWork();
            weekDayWork.setStaff(stf);
            List<Object[]> list = fetchWorkedTime(stf);

            for (Object[] obj : list) {
                Integer dayOfWeek = (Integer) obj[0];
                Double value = (Double) obj[1];

                if (value == null) {
                    value = 0.0;
                }

                if (dayOfWeek == null) {
                    dayOfWeek = -1;
                }
                switch (dayOfWeek) {
                    case Calendar.SUNDAY:
                        weekDayWork.setSunDay(value);
                        break;
                    case Calendar.MONDAY:
                        weekDayWork.setMonDay(value);
                        break;
                    case Calendar.TUESDAY:
                        weekDayWork.setTuesDay(value);
                        break;
                    case Calendar.WEDNESDAY:
                        weekDayWork.setWednesDay(value);
                        break;
                    case Calendar.THURSDAY:
                        weekDayWork.setThursDay(value);
                        break;
                    case Calendar.FRIDAY:
                        weekDayWork.setFriDay(value);
                        break;
                    case Calendar.SATURDAY:
                        weekDayWork.setSaturDay(value);
                        break;
                }

                weekDayWork.setTotal(weekDayWork.getTotal() + value);
            }

            double normalWorkTime = numOfWeeks * stf.getWorkingTimeForNoPayPerWeek() * 60 * 60;
            double noPays = weekDayWork.getTotal() - normalWorkTime;

            if (noPays < 0) {
                weekDayWork.setNoPay(noPays);
                weekDayWorks.add(weekDayWork);
            }

        }
    }

    List<StaffShiftAggrgation> staffShiftAggrgations;

    public List<StaffShiftAggrgation> getStaffShiftAggrgations() {
        return staffShiftAggrgations;
    }

    public void setStaffShiftAggrgations(List<StaffShiftAggrgation> staffShiftAggrgations) {
        this.staffShiftAggrgations = staffShiftAggrgations;
    }

    @EJB
    FinalVariables finalVariables;

    public void createStaffWorkingTime() {
        String sql = "";

        HashMap hm = new HashMap();
        sql = "select new com.divudi.data.hr.StaffShiftAggrgation(ss.staff,"
                + "sum(ss.workedWithinTimeFrameVarified),sum(ss.leavedTime+ss.leavedTimeOther)) "
                + " from StaffShift ss "
                + " where ss.retired=false "
                //  + " and ((ss.startRecord.recordTimeStamp is not null "
                //   + " and ss.endRecord.recordTimeStamp is not null) "
                // + " or (ss.leaveType is not null) ) "
                + " and ss.shiftDate between :frm  and :to ";
        hm.put("frm", fromDate);
        hm.put("to", toDate);

        if (getReportKeyWord().getStaff() != null) {
            sql += " and ss.staff=:stf ";
            hm.put("stf", getReportKeyWord().getStaff());
        }

        if (getReportKeyWord().getDepartment() != null) {
            sql += " and ss.staff.workingDepartment=:dep ";
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

        if (getReportKeyWord().getRoster() != null) {
            sql += " and ss.roster=:rs ";
            hm.put("rs", getReportKeyWord().getRoster());
        }

        sql += " group by ss.staff "
                //                + "  having (sum(ss.workedWithinTimeFrameVarified)+sum(ss.leavedTime))> 0 "
                + " order by ss.staff.codeInterger";

        staffShiftAggrgations = (List<StaffShiftAggrgation>) (Object) staffLeaveFacade.findAggregates(sql, hm, TemporalType.DATE);
    }

    public void createStaffWorkingTimeBelow28() {
        createStaffWorkingTime();
        if (staffShiftAggrgations == null) {
            return;
        }

        Long datRange = commonFunctions.getDayCount(getFromDate(), getToDate());
        Long mul = datRange / 7;
        double maxLimitInSec = mul * finalVariables.getMinimumWorkingHourPerWeek() * 60 * 60;
        System.err.println("Max Limit " + maxLimitInSec);
        List<StaffShiftAggrgation> list = new ArrayList<>();

        for (StaffShiftAggrgation ssa : staffShiftAggrgations) {
            double sum = 0;

            if (ssa.getWorkingTime() != null) {
                sum += ssa.getWorkingTime();
            }

            if (ssa.getLeavedTime() != null) {
                sum += ssa.getLeavedTime();
            }

            if (sum < maxLimitInSec) {
                list.add(ssa);
            }
        }

        staffShiftAggrgations = list;
    }

    List<DepartmentAttendance> departmentAttendances;

    public List<DepartmentAttendance> getDepartmentAttendances() {
        return departmentAttendances;
    }

    public void setDepartmentAttendances(List<DepartmentAttendance> departmentAttendances) {
        this.departmentAttendances = departmentAttendances;
    }
    @EJB
    StaffPaysheetComponentFacade staffPaysheetComponentFacade;

    public void updateStaffPaySheetComponent() {
        String sql = " Select s"
                + "  From StaffPaysheetComponent s"
                + " where s.staffPaySheetComponentValue=0 ";

        List<StaffPaysheetComponent> list = staffPaysheetComponentFacade.findBySQL(sql);

        if (list == null) {
            return;
        }
        for (StaffPaysheetComponent spc : list) {
            spc.setStaffPaySheetComponentValue(spc.getStaffPaySheetComponentValue());
            staffPaysheetComponentFacade.edit(spc);
        }
    }

    public void updateOverTimeValuePerSecond() {
        String sql = "select s from StaffShift s "
                + " where s.overTimeValuePerSecond=0"
                + " and s.additionalForm is not null ";
        List<StaffShift> list = staffShiftFacade.findBySQL(sql);
        if (list == null) {
            return;
        }

        for (StaffShift ss : list) {
            System.err.println(ss.getId());
            double valueForOverTime = humanResourceBean.getOverTimeValue(ss.getStaff(), ss.getShiftDate());
            ss.setOverTimeValuePerSecond(valueForOverTime / (200 * 60 * 60));
            staffShiftFacade.edit(ss);
        }

    }

    @Inject
    PhDateController phDateController;

    public void updateHoliday() {
        String sql = "select s from StaffShift s"
                + " where s.startRecord.recordTimeStamp is not null "
                + " and s.endRecord.recordTimeStamp is not null";
        List<StaffShift> list = staffShiftFacade.findBySQL(sql);
        if (list == null) {
            return;
        }

        int i = 0;
        for (StaffShift ss : list) {
            DayType dtp = phDateController.getHolidayType(ss.getShiftDate());
            ss.setDayType(dtp);
            if (dtp == null) {
                if (ss.getShift() != null) {
                    ss.setDayType(ss.getShift().getDayType());
                }
            } else {
                System.err.println("$$$$ " + dtp + "  " + ++i);
            }
            staffShiftFacade.edit(ss);
        }

    }

    public void updateAutomaticData() {
        String sql = "select s from StaffLeaveSystem s ";

        List<StaffLeave> list = staffLeaveFacade.findBySQL(sql);
        if (list == null) {
            return;
        }

        for (StaffLeave ss : list) {
            ss.setRetired(true);
            ss.setRetireComments("Deleted By System");
            staffLeaveFacade.edit(ss);

            if (ss.getForm() != null) {
                ss.getForm().setRetired(true);
                ss.getForm().setRetireComments("Deleted By System");
                formFacade.edit(ss.getForm());
            }
        }

        sql = "select s from StaffShift s"
                + " where  (s.considerForEarlyOut=true "
                + " or s.considerForLateIn=true "
                + " or s.referenceStaffShiftLateIn is not null "
                + " or s.referenceStaffShiftEarlyOut is not null "
                + " or s.referenceStaffShift is not null )";

        List<StaffShift> list2 = staffShiftFacade.findBySQL(sql);
        if (list2 == null) {
            return;
        }

        for (StaffShift s : list2) {
            s.setConsiderForEarlyOut(false);
            s.setConsiderForLateIn(false);
            s.setAutoLeave(false);
            s.setLeaveType(null);
            staffShiftFacade.edit(s);
        }

    }

    @Inject
    StaffSalaryController staffSalaryController;

    public void updateStaffPaysheetComponent() {
        String sql = "select s from StaffPaysheetComponent s "
                + " where s.retired=false ";

        List<StaffPaysheetComponent> staffPaysheetComponents = staffPaysheetComponentFacade.findBySQL(sql);

        if (staffPaysheetComponents == null) {
            return;
        }

        for (StaffPaysheetComponent spc : staffPaysheetComponents) {
            if (spc.getStaffPaySheetComponentValue() != 0) {
                continue;
            }

            if (spc.getModifiedValue() != 0) {
                spc.setStaffPaySheetComponentValue(spc.getModifiedValue());
            } else {
                spc.setStaffPaySheetComponentValue(spc.getCreatedValue());
            }

            staffPaysheetComponentFacade.edit(spc);
        }

    }

    public void updateLateLeaveData() {
        String sql = "select s from StaffSalary s "
                + " where s.retired=false"
                + " and s.salaryCycle.retired=false "
                + " and s.blocked=false";

        List<StaffSalary> list = staffSalaryFacade.findBySQL(sql);
        if (list == null) {
            return;
        }

        for (StaffSalary ss : list) {
//            double noPayCount = getHumanResourceBean().fetchStaffLeaveAddedLeave(ss.getStaff(), LeaveType.No_Pay, ss.getSalaryCycle().getWorkedFromDate(), ss.getSalaryCycle().getWorkedToDate());
//            double all = staffSalaryController.calAllowanceValueForNoPay(ss.getStaffSalaryComponants());
//            if (all != 0) {
//                ss.setNoPayValueAllowance(0 - noPayCount * (all / finalVariables.getWorkingDaysPerMonth()));
//            } else {
//                ss.setNoPayValueAllowance(0);
//            }
//
//            ////////
//            double noPayCountLate = getHumanResourceBean().fetchStaffLeaveSystem(ss.getStaff(), LeaveType.No_Pay, ss.getSalaryCycle().getWorkedFromDate(), ss.getSalaryCycle().getWorkedToDate());
//            ss.setLateNoPayCount(noPayCountLate);
//            ss.setLateNoPayBasicValue(0 - (ss.getBasicValue() / finalVariables.getWorkingDaysPerMonth()) * noPayCountLate);
//            ss.setLateNoPayAllovanceValue(0.0);

            staffSalaryController.setSalaryCycle(ss.getSalaryCycle());
            staffSalaryController.setCurrent(ss);
            staffSalaryController.setOT();
            ss.calculateComponentTotal();
            ss.calcualteEpfAndEtf();

            staffSalaryFacade.edit(ss);

        }

    }

    public void createStaffShift() {
//        if(Arrays.asList(dayTypesSelected).isEmpty() ){
//            JsfUtil.addErrorMessage("Select Day Type");
//            return;
//        }
        String sql = "";
        HashMap hm = new HashMap();
        sql = createStaffShiftQuary(hm);
//        StaffShift ss = new StaffShift();
//        ss.getDayType();
        if (dayTypesSelected != null && !Arrays.asList(dayTypesSelected).isEmpty()) {
            sql += " and ss.dayType in :dts ";
            hm.put("dts", Arrays.asList(dayTypesSelected));
        }

        sql += " order by ss.staff.codeInterger,ss.shiftDate ";
        staffShifts = staffShiftFacade.findBySQL(sql, hm, TemporalType.TIMESTAMP);
        calWorkedTimeTotal(staffShifts);
    }

    private void calWorkedTimeTotal(List<StaffShift> list) {
        if (list == null) {
            return;
        }
        totalWorkedTime = 0;
        for (StaffShift s : list) {
            totalWorkedTime += s.getWorkedWithinTimeFrameVarified();
        }
    }

    public void createStaffShiftWorked() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = createStaffShiftQuary(hm);
        sql += " and ss.startRecord.recordTimeStamp is not null "
                + " and ss.endRecord.recordTimeStamp is not null ";
        sql += " order by ss.staff.codeInterger ";
        staffShifts = staffShiftFacade.findBySQL(sql, hm, TemporalType.DATE);
    }

    public void createStaffShiftLieAllowed() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = createStaffShiftQuary(hm);
        sql += " and ss.lieuAllowed=true ";
        sql += " order by ss.staff.codeInterger ";
        staffShifts = staffShiftFacade.findBySQL(sql, hm, TemporalType.DATE);
    }

    public void createStaffShiftLieAllowedWorked() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = createStaffShiftQuary(hm);
        sql += " and ss.lieuAllowed=true "
                + "  and ss.startRecord.recordTimeStamp is not null "
                + " and ss.endRecord.recordTimeStamp is not null ";
        sql += " order by ss.staff.codeInterger ";
        staffShifts = staffShiftFacade.findBySQL(sql, hm, TemporalType.DATE);
    }

    List<StaffSalary> staffSalarys;

    public List<StaffSalary> getStaffSalarys() {
        return staffSalarys;
    }

    public void setStaffSalarys(List<StaffSalary> staffSalarys) {
        this.staffSalarys = staffSalarys;
    }

    @EJB
    StaffSalaryFacade staffSalaryFacade;
    private boolean netSalary;
    private boolean otPayment;

    public void createStaffSalaryNetSalary() {
        netSalary = true;
        otPayment = false;
        createStaffSalary();
    }

    public void createStaffSalaryOtPayment() {
        netSalary = false;
        otPayment = true;
        createStaffSalary();
    }

    public void createStaffSalaryNetAndOtPayment() {
        netSalary = true;
        otPayment = true;
        createStaffSalary();
    }

    public void createStaffSalary() {
        System.out.println("Creating Staff Salary");
        String sql = "";
        HashMap hm = new HashMap();
        sql = createStaffSalaryQuary(hm);
        sql += " order by ss.staff.codeInterger ";
        System.out.println("sql = " + sql);
        System.out.println("hm = " + hm);
        staffSalarys = staffSalaryFacade.findBySQL(sql, hm, TemporalType.DATE);
        calTotalNoPay();
        calTableTotal(staffSalarys);
    }

    String chequeNo;

    public String getChequeNo() {
        return chequeNo;
    }

    public void setChequeNo(String chequeNo) {
        this.chequeNo = chequeNo;
    }

    public void update() {
        if (staffSalarys == null) {
            return;
        }

        for (StaffSalary staffSalary : staffSalarys) {
            if (netSalary && otPayment) {
                staffSalary.setChequeNumberSalaryAndOverTime(chequeNo);
            } else if (netSalary && !otPayment) {
                staffSalary.setChequeNumberOverTime(chequeNo);
            } else if (!netSalary && otPayment) {
                staffSalary.setChequeNumberOverTime(chequeNo);
            }

            staffSalaryFacade.edit(staffSalary);
        }
    }

    List<StaffSalaryComponant> staffSalaryComponants;
    @EJB
    StaffSalaryComponantFacade staffSalaryComponantFacade;

    public void createStaffSalaryComponent() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = createStaffSalaryComponentQuary(hm);
        sql += " order by ss.staffSalary.staff.codeInterger ";
        staffSalaryComponants = staffSalaryComponantFacade.findBySQL(sql, hm, TemporalType.DATE);

    }

    public List<StaffSalaryComponant> getStaffSalaryComponants() {
        return staffSalaryComponants;
    }

    public void setStaffSalaryComponants(List<StaffSalaryComponant> staffSalaryComponants) {
        this.staffSalaryComponants = staffSalaryComponants;
    }

    public StaffSalaryComponantFacade getStaffSalaryComponantFacade() {
        return staffSalaryComponantFacade;
    }

    public void setStaffSalaryComponantFacade(StaffSalaryComponantFacade staffSalaryComponantFacade) {
        this.staffSalaryComponantFacade = staffSalaryComponantFacade;
    }

//    public void createStaffSalaryForBanking() {
//        String sql = "";
//        HashMap hm = new HashMap();
//        sql = createStaffSalaryQuary(hm);
//        sql += " order by ss.staff.codeInterger";
//        staffSalarys = staffSalaryFacade.findBySQL(sql, hm, TemporalType.DATE);
//        calTotalNoPay();
//        calTableTotal(staffSalarys);
//
//    }
    double totalOvertimeMinit = 0.0; //overTimeMinute
    double totalExtraDutyNormalMinute = 0.0; //extraDutyNormalMinute
    double totalRatePerMinut = 0.0;  //overTimeRatePerMinute*1.5
    double totalOtValue = 0.0; //overTimeValue+ss.extraDutyNormalValue
    double totalPhOtMin = 0.0; //extraDutyPoyaMinute+ss.extraDutyMerchantileMinute
    double totalRatePerMinutPhOt = 0.0;  //ss.overTimeRatePerMinute*1.5
    double totalPhOtValue = 0.0;//extraDutyMerchantileValue+ss.extraDutyPoyaValue
    double totalOffDayOtMin = 0.0; //extraDutySleepingDayMinute+ss.extraDutyDayOffMinute
    double totalRatePerMinuts = 0.0;  //overTimeRatePerMinute*2.5
    double totalOffdyOtValue = 0.0;  //extraDutyDayOffValue+ss.extraDutySleepingDayValue
    double totalValue = 0.0; //ss.overTimeValue+ss.extraDutyNormalValue+ss.extraDutyMerchantileValue+ss.extraDutyPoyaValue+ss.extraDutyDayOffValue+ss.extraDutySleepingDayValue
    double totalTransNetSalary = 0.0; //total of the transNetSalary;
    double totalOverTime = 0.0; //ss.transExtraDutyValue+ss.overTimeValue
    double totalofTotals = 0.0;//ss.transExtraDutyValue+ss.overTimeValue+ss.transNetSalry
    double totaldayOffAllowance = 0.0;
    double totaldayOffCount = 0.0;

    public void calTableTotal(List<StaffSalary> stfSal) {

        totalOvertimeMinit = 0.0; //overTimeMinute
        totalExtraDutyNormalMinute = 0.0; //extraDutyNormalMinute
        totalRatePerMinut = 0.0;  //overTimeRatePerMinute*1.5
        totalOtValue = 0.0; //overTimeValue+ss.extraDutyNormalValue
        totalPhOtMin = 0.0; //extraDutyPoyaMinute+ss.extraDutyMerchantileMinute
        totalRatePerMinutPhOt = 0.0;  //ss.overTimeRatePerMinute*1.5
        totalPhOtValue = 0.0;//extraDutyMerchantileValue+ss.extraDutyPoyaValue
        totalOffDayOtMin = 0.0; //extraDutySleepingDayMinute+ss.extraDutyDayOffMinute
        totalRatePerMinuts = 0.0;  //overTimeRatePerMinute*2.5
        totalOffdyOtValue = 0.0;  //extraDutyDayOffValue+ss.extraDutySleepingDayValue
        totalValue = 0.0; //ss.overTimeValue+ss.extraDutyNormalValue+ss.extraDutyMerchantileValue+ss.extraDutyPoyaValue+ss.extraDutyDayOffValue+ss.extraDutySleepingDayValue
        totalTransNetSalary = 0.0;//total of transNetSalary
        totalOverTime = 0.0;//ss.transExtraDutyValue+ss.overTimeValue
        totalofTotals = 0.0;//ss.transExtraDutyValue+ss.overTimeValue+ss.transNetSalry
        totaldayOffAllowance = 0.0;
        totaldayOffCount = 0.0;

        for (StaffSalary totStaffSalary : stfSal) {
            totalOvertimeMinit += totStaffSalary.getOverTimeMinute();
            totalExtraDutyNormalMinute += totStaffSalary.getExtraDutyNormalMinute();
            totalRatePerMinut += totStaffSalary.getOverTimeRatePerMinute() * 1.5;
            totalOtValue += totStaffSalary.getOverTimeValue() + totStaffSalary.getExtraDutyNormalValue();
            totalPhOtMin += totStaffSalary.getExtraDutyPoyaMinute() + totStaffSalary.getExtraDutyMerchantileMinute();
            totalRatePerMinutPhOt += totStaffSalary.getOverTimeMinute() * 1.5;
            totalPhOtValue += totStaffSalary.getExtraDutyMerchantileValue() + totStaffSalary.getExtraDutyPoyaValue();
            totalOffDayOtMin += totStaffSalary.getExtraDutySleepingDayMinute() + totStaffSalary.getExtraDutyDayOffMinute();
            totalRatePerMinuts += totStaffSalary.getOverTimeRatePerMinute() * 2.5;
            totalOffdyOtValue += totStaffSalary.getExtraDutyDayOffValue() + totStaffSalary.getExtraDutySleepingDayValue();
            totalValue += totStaffSalary.getOverTimeValue() + totStaffSalary.getExtraDutyNormalValue() + totStaffSalary.getExtraDutyMerchantileValue() + totStaffSalary.getExtraDutyPoyaValue() + totStaffSalary.getExtraDutyDayOffValue() + totStaffSalary.getExtraDutySleepingDayValue();
            totalTransNetSalary += totStaffSalary.getTransNetSalry();
            totalOverTime += totStaffSalary.getTransExtraDutyValue() + totStaffSalary.getOverTimeValue();
            totalofTotals += totStaffSalary.getTransExtraDutyValue() + totStaffSalary.getOverTimeValue() + totStaffSalary.getTransNetSalry();
            totaldayOffAllowance += totStaffSalary.getDayOffAllowance();
            totaldayOffCount += totStaffSalary.getDayOffCount();

        }

    }

    double merchantileAllowanceValueTotal = 0;
    double merchantileCountTotal = 0;
    double poyaAllowanceValueTotal = 0;
    double poyaCountTotal = 0;
    double lateNoPayAllovanceValueTotal = 0;
    double lateNoPayBasicValueTotal = 0;
    double noPayValueAllowanceTotal = 0;
    double noPayValueBasicTotal = 0;
    double lateNoPayCountTotal = 0;
    double noPayCountTotal = 0;

    private void calTotalNoPay() {
        if (staffSalarys == null) {
            return;
        }

        merchantileAllowanceValueTotal = 0;
        merchantileCountTotal = 0;
        poyaAllowanceValueTotal = 0;
        poyaCountTotal = 0;
        lateNoPayAllovanceValueTotal = 0;
        lateNoPayBasicValueTotal = 0;
        noPayValueAllowanceTotal = 0;
        noPayValueBasicTotal = 0;
        lateNoPayCountTotal = 0;
        noPayCountTotal = 0;

        for (StaffSalary s : staffSalarys) {
            merchantileAllowanceValueTotal += s.getMerchantileAllowanceValue();
            merchantileCountTotal += s.getMerchantileCount();
            poyaAllowanceValueTotal += s.getPoyaAllowanceValue();
            poyaCountTotal += s.getPoyaCount();
            lateNoPayAllovanceValueTotal += s.getLateNoPayAllovanceValue();
            lateNoPayBasicValueTotal += s.getLateNoPayBasicValue();
            noPayValueAllowanceTotal += s.getNoPayValueAllowance();
            noPayValueBasicTotal += s.getNoPayValueBasic();
            lateNoPayCountTotal += s.getLateNoPayCount();
            noPayCountTotal += s.getNoPayCount();

        }

    }

    public StaffSalaryController getStaffSalaryController() {
        return staffSalaryController;
    }

    public void setStaffSalaryController(StaffSalaryController staffSalaryController) {
        this.staffSalaryController = staffSalaryController;
    }

    public double getMerchantileAllowanceValueTotal() {
        return merchantileAllowanceValueTotal;
    }

    public void setMerchantileAllowanceValueTotal(double merchantileAllowanceValueTotal) {
        this.merchantileAllowanceValueTotal = merchantileAllowanceValueTotal;
    }

    public double getMerchantileCountTotal() {
        return merchantileCountTotal;
    }

    public void setMerchantileCountTotal(double merchantileCountTotal) {
        this.merchantileCountTotal = merchantileCountTotal;
    }

    public double getPoyaAllowanceValueTotal() {
        return poyaAllowanceValueTotal;
    }

    public void setPoyaAllowanceValueTotal(double poyaAllowanceValueTotal) {
        this.poyaAllowanceValueTotal = poyaAllowanceValueTotal;
    }

    public double getPoyaCountTotal() {
        return poyaCountTotal;
    }

    public void setPoyaCountTotal(double poyaCountTotal) {
        this.poyaCountTotal = poyaCountTotal;
    }

    public double getLateNoPayAllovanceValueTotal() {
        return lateNoPayAllovanceValueTotal;
    }

    public void setLateNoPayAllovanceValueTotal(double lateNoPayAllovanceValueTotal) {
        this.lateNoPayAllovanceValueTotal = lateNoPayAllovanceValueTotal;
    }

    public double getLateNoPayBasicValueTotal() {
        return lateNoPayBasicValueTotal;
    }

    public void setLateNoPayBasicValueTotal(double lateNoPayBasicValueTotal) {
        this.lateNoPayBasicValueTotal = lateNoPayBasicValueTotal;
    }

    public double getNoPayValueAllowanceTotal() {
        return noPayValueAllowanceTotal;
    }

    public void setNoPayValueAllowanceTotal(double noPayValueAllowanceTotal) {
        this.noPayValueAllowanceTotal = noPayValueAllowanceTotal;
    }

    public double getNoPayValueBasicTotal() {
        return noPayValueBasicTotal;
    }

    public void setNoPayValueBasicTotal(double noPayValueBasicTotal) {
        this.noPayValueBasicTotal = noPayValueBasicTotal;
    }

    public double getLateNoPayCountTotal() {
        return lateNoPayCountTotal;
    }

    public void setLateNoPayCountTotal(double lateNoPayCountTotal) {
        this.lateNoPayCountTotal = lateNoPayCountTotal;
    }

    public double getNoPayCountTotal() {
        return noPayCountTotal;
    }

    public void setNoPayCountTotal(double noPayCountTotal) {
        this.noPayCountTotal = noPayCountTotal;
    }

    public void createShiftTable() {
        String sql = "Select s From Shift s "
                + " where s.retired=false ";
        //   + " order by s.shiftOrder ";
        //System.out.println("sql = " + sql);
        HashMap hm = new HashMap();

        if (getReportKeyWord().getRoster() != null) {
            sql += " and s.roster=:rs ";
            hm.put("rs", getReportKeyWord().getRoster());
        }

        sql += " order by s.roster.id";

        shiftLists = getShiftFacade().findBySQL(sql, hm);
    }

    public void createStaffShiftExtra() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = createStaffShiftExtraQuary(hm);
        sql += " order by ss.staff.codeInterger ";
        staffShifts = staffShiftFacade.findBySQL(sql, hm, TemporalType.DATE);
    }

    public void createStaffShiftLateIn() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = createStaffShiftQuary(hm);
        sql += " and ss.lateInLogged>0 "
                + " order by ss.staff.codeInterger ";
        staffShifts = staffShiftFacade.findBySQL(sql, hm, TemporalType.DATE);
    }

    public void createStaffShiftOnlyOt() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = createStaffShiftQuary(hm);

        if (dayTypesSelected != null && !Arrays.asList(dayTypesSelected).isEmpty()) {
            sql += " and ss.dayType in :dts ";
            hm.put("dts", Arrays.asList(dayTypesSelected));
        }

        sql += " and (ss.startRecord.allowedExtraDuty=true or "
                + " ss.endRecord.allowedExtraDuty=true )";
        sql += " and ss.startRecord.recordTimeStamp is not null "
                + " and ss.endRecord.recordTimeStamp is not null ";
        sql += " order by ss.staff.codeInterger ";
        staffShifts = staffShiftFacade.findBySQL(sql, hm, TemporalType.DATE);

    }

    public void createStaffShiftEarlyIn() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = createStaffShiftQuary(hm);
        sql += " and ss.shiftStartTime  > ss.startRecord.recordTimeStamp";
        sql += " order by ss.staff.codeInterger ";
//        sql += " order by ss.shift,ss.shiftDate";
        staffShifts = staffShiftFacade.findBySQL(sql, hm, TemporalType.DATE);

    }

//    public void createStaffShiftLateIn() {
//        String sql = "";
//        HashMap hm = new HashMap();
//        sql = createStaffShiftQuary(hm);
//        sql += " and ss.shiftStartTime  < ss.startRecord.recordTimeStamp";
//        sql += " order by ss.staff.codeInterger ";
////        sql += " order by ss.shift,ss.shiftDate";
//        staffShifts = staffShiftFacade.findBySQL(sql, hm, TemporalType.DATE);
//
//    }
    List<StaffShiftHistory> staffShiftHistorys;

    public DepartmentFacade getDepartmentFacade() {
        return departmentFacade;
    }

    public void setDepartmentFacade(DepartmentFacade departmentFacade) {
        this.departmentFacade = departmentFacade;
    }

    public List<StaffShiftHistory> getStaffShiftHistorys() {
        return staffShiftHistorys;
    }

    public void setStaffShiftHistorys(List<StaffShiftHistory> staffShiftHistorys) {
        this.staffShiftHistorys = staffShiftHistorys;
    }

    @EJB
    StaffShiftHistoryFacade staffShiftHistoryFacade;

    public void createStaffShiftHistory() {
        String sql = "";
        HashMap hm = new HashMap();

        sql = "select ss from StaffShiftHistory ss "
                + " where ss.retired=false "
                + " and ss.staffShift.shiftDate between :frm  and :to ";
        hm.put("frm", fromDate);
        hm.put("to", toDate);

        if (getReportKeyWord().getStaff() != null) {
            sql += " and ss.staff=:stf ";
            hm.put("stf", getReportKeyWord().getStaff());
        }

        if (getReportKeyWord().getDepartment() != null) {
            sql += " and ss.staff.workingDepartment=:dep ";
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

        if (getReportKeyWord().getRoster() != null) {
            sql += " and ss.roster=:rs ";
            hm.put("rs", getReportKeyWord().getRoster());
        }

        if (getReportKeyWord().getShift() != null) {
            sql += " and ss.shift=:sh ";
            hm.put("sh", getReportKeyWord().getShift());
        }
//        sql += " and ss.shiftStartTime  < ss.startRecord.recordTimeStamp";
        sql += " order by ss.staffShift.id,ss.staffShift.shiftDate";
        staffShiftHistorys = staffShiftHistoryFacade.findBySQL(sql, hm, TemporalType.TIMESTAMP);

    }

    List<FingerPrintRecordHistory> fingerPrintRecordHistorys;
    @EJB
    FingerPrintRecordHistoryFacade fingerPrintRecordHistoryFacade;

    public StaffShiftHistoryFacade getStaffShiftHistoryFacade() {
        return staffShiftHistoryFacade;
    }

    public void setStaffShiftHistoryFacade(StaffShiftHistoryFacade staffShiftHistoryFacade) {
        this.staffShiftHistoryFacade = staffShiftHistoryFacade;
    }

    public List<FingerPrintRecordHistory> getFingerPrintRecordHistorys() {
        return fingerPrintRecordHistorys;
    }

    public void setFingerPrintRecordHistorys(List<FingerPrintRecordHistory> fingerPrintRecordHistorys) {
        this.fingerPrintRecordHistorys = fingerPrintRecordHistorys;
    }

    public FingerPrintRecordHistoryFacade getFingerPrintRecordHistoryFacade() {
        return fingerPrintRecordHistoryFacade;
    }

    public void setFingerPrintRecordHistoryFacade(FingerPrintRecordHistoryFacade fingerPrintRecordHistoryFacade) {
        this.fingerPrintRecordHistoryFacade = fingerPrintRecordHistoryFacade;
    }

    public void createFingerPrintHistory() {
        String sql = "";
        HashMap hm = new HashMap();

        sql = "select ss from FingerPrintRecordHistory ss "
                + " where ss.retired=false "
                + " and ss.beforeChange between :frm  and :to ";
        hm.put("frm", fromDate);
        hm.put("to", toDate);

        if (getReportKeyWord().getStaff() != null) {
            sql += " and ss.staff=:stf ";
            hm.put("stf", getReportKeyWord().getStaff());
        }

        if (getReportKeyWord().getDepartment() != null) {
            sql += " and ss.staff.workingDepartment=:dep ";
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

        if (getReportKeyWord().getRoster() != null) {
            sql += " and ss.roster=:rs ";
            hm.put("rs", getReportKeyWord().getRoster());
        }

        if (getReportKeyWord().getShift() != null) {
            sql += " and ss.shift=:sh ";
            hm.put("sh", getReportKeyWord().getShift());
        }

//        sql += " and ss.shiftStartTime  < ss.startRecord.recordTimeStamp";
        sql += " order by ss.fingerPrintRecord.id,ss.id";
        fingerPrintRecordHistorys = fingerPrintRecordHistoryFacade.findBySQL(sql, hm, TemporalType.TIMESTAMP);

    }

    public void createStaffShiftLateInEarlyOut() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = createStaffShiftQuary(hm);

        sql += " and (ss.lateInVarified!=0"
                + " or ss.earlyOutVarified!=0)";

        if (getReportKeyWord().getFrom() != 0 && getReportKeyWord().getTo() != 0) {
            sql += " and ((ss.lateInVarified>= :frmTime  and ss.lateInVarified<= :toTime) "
                    + " or (ss.earlyOutVarified>= :frmTime and ss.earlyOutVarified<= :toTime )) ";
            hm.put("frmTime", getReportKeyWord().getFrom() * 60);
            hm.put("toTime", getReportKeyWord().getTo() * 60);
        } else if (getReportKeyWord().getFrom() != 0 && getReportKeyWord().getTo() == 0) {
            sql += " and ((ss.lateInVarified>= :frmTime) "
                    + " or (ss.earlyOutVarified>= :frmTime)) ";
            hm.put("frmTime", getReportKeyWord().getFrom() * 60);
        } else if (getReportKeyWord().getFrom() == 0 && getReportKeyWord().getTo() != 0) {
            sql += " and ((ss.lateInVarified<= :toTime) "
                    + " or (ss.earlyOutVarified<= :toTime )) ";
            hm.put("toTime", getReportKeyWord().getTo() * 60);
        }
        sql += " order by ss.staff.codeInterger";
        staffShifts = staffShiftFacade.findBySQL(sql, hm, TemporalType.DATE);

    }

    public void createStaffShiftEarlyOut() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = createStaffShiftQuary(hm);
        sql += " and ss.earlyOutLogged>0 ";
        sql += " order by ss.staff.codeInterger";
        staffShifts = staffShiftFacade.findBySQL(sql, hm, TemporalType.DATE);

    }

    public void createStaffShiftLateOut() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = createStaffShiftQuary(hm);
        sql += " and ss.shiftEndTime < ss.endRecord.recordTimeStamp";
        sql += " order by ss.staff.codeInterger";
        staffShifts = staffShiftFacade.findBySQL(sql, hm, TemporalType.DATE);

    }

    public class OverTimeAllMonth {

        String dateRange;
        List<WeekDayWork> dayWorks;

        public String getDateRange() {
            return dateRange;
        }

        public void setDateRange(String dateRange) {
            this.dateRange = dateRange;
        }

        public List<WeekDayWork> getDayWorks() {
            return dayWorks;
        }

        public void setDayWorks(List<WeekDayWork> dayWorks) {
            this.dayWorks = dayWorks;
        }

    }

    public class SummeryForMonth {

        Staff staff;
        double week1;
        double week2;
        double week3;
        double week4;
        double week5;
        double total;
        double overTime;
        double basicPerSecond;

        public Staff getStaff() {
            return staff;
        }

        public void setStaff(Staff staff) {
            this.staff = staff;
        }

        public double getWeek1() {
            return week1;
        }

        public void setWeek1(double week1) {
            this.week1 = week1;
        }

        public double getWeek2() {
            return week2;
        }

        public void setWeek2(double week2) {
            this.week2 = week2;
        }

        public double getWeek3() {
            return week3;
        }

        public void setWeek3(double week3) {
            this.week3 = week3;
        }

        public double getWeek4() {
            return week4;
        }

        public void setWeek4(double week4) {
            this.week4 = week4;
        }

        public double getWeek5() {
            return week5;
        }

        public void setWeek5(double week5) {
            this.week5 = week5;
        }

        public double getTotal() {
            return total;
        }

        public void setTotal(double total) {
            this.total = total;
        }

        public double getOverTime() {
            return overTime;
        }

        public void setOverTime(double overTime) {
            this.overTime = overTime;
        }

        public double getBasicPerSecond() {
            return basicPerSecond;
        }

        public void setBasicPerSecond(double basicPerSecond) {
            this.basicPerSecond = basicPerSecond;
        }

    }

    /**
     * Creates a new instance of HrReport
     */
    public HrReportController() {
    }

    public void makeNull() {
        reportKeyWord = null;
        staffShifts = null;
        fingerPrintRecords = null;
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
            fromDate = com.divudi.java.CommonFunctions.getStartOfMonth(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = com.divudi.java.CommonFunctions.getEndOfMonth(new Date());
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

    public List<FingerPrintRecord> getFingerPrintRecords() {
        return fingerPrintRecords;
    }

    public void setFingerPrintRecords(List<FingerPrintRecord> fingerPrintRecords) {
        this.fingerPrintRecords = fingerPrintRecords;
    }

    public StaffShiftFacade getStaffShiftFacade() {
        return staffShiftFacade;
    }

    public void setStaffShiftFacade(StaffShiftFacade staffShiftFacade) {
        this.staffShiftFacade = staffShiftFacade;
    }

    public List<MonthEndRecord> getMonthEndRecords() {
        return monthEndRecords;
    }

    public void setMonthEndRecords(List<MonthEndRecord> monthEndRecords) {
        this.monthEndRecords = monthEndRecords;
    }

    public List<StaffLeave> getStaffLeavesAnnual() {
        return staffLeavesAnnual;
    }

    public void setStaffLeavesAnnual(List<StaffLeave> staffLeavesAnnual) {
        this.staffLeavesAnnual = staffLeavesAnnual;
    }

    public List<StaffLeave> getStaffLeavesCashual() {
        return staffLeavesCashual;
    }

    public void setStaffLeavesCashual(List<StaffLeave> staffLeavesCashual) {
        this.staffLeavesCashual = staffLeavesCashual;
    }

    public List<StaffLeave> getStaffLeavesNoPay() {
        return staffLeavesNoPay;
    }

    public void setStaffLeavesNoPay(List<StaffLeave> staffLeavesNoPay) {
        this.staffLeavesNoPay = staffLeavesNoPay;
    }

    public List<StaffLeave> getStaffLeavesDutyLeave() {
        return staffLeavesDutyLeave;
    }

    public void setStaffLeavesDutyLeave(List<StaffLeave> staffLeavesDutyLeave) {
        this.staffLeavesDutyLeave = staffLeavesDutyLeave;
    }

    public double getCasualEntitle() {
        return casualEntitle;
    }

    public void setCasualEntitle(double casualEntitle) {
        this.casualEntitle = casualEntitle;
    }

    public double getCasualUtilized() {
        return casualUtilized;
    }

    public void setCasualUtilized(double casualUtilized) {
        this.casualUtilized = casualUtilized;
    }

    public double getNopayEntitle() {
        return nopayEntitle;
    }

    public void setNopayEntitle(double nopayEntitle) {
        this.nopayEntitle = nopayEntitle;
    }

    public double getNopayUtilized() {
        return nopayUtilized;
    }

    public void setNopayUtilized(double nopayUtilized) {
        this.nopayUtilized = nopayUtilized;
    }

    public double getDutyLeaveEntitle() {
        return dutyLeaveEntitle;
    }

    public void setDutyLeaveEntitle(double dutyLeaveEntitle) {
        this.dutyLeaveEntitle = dutyLeaveEntitle;
    }

    public double getDutyLeaveUtilized() {
        return dutyLeaveUtilized;
    }

    public void setDutyLeaveUtilized(double dutyLeaveUtilized) {
        this.dutyLeaveUtilized = dutyLeaveUtilized;
    }

    public List<StaffLeave> getStaffLeavesMedical() {
        return staffLeavesMedical;
    }

    public void setStaffLeavesMedical(List<StaffLeave> staffLeavesMedical) {
        this.staffLeavesMedical = staffLeavesMedical;
    }

    public List<StaffLeave> getStaffLeavesMaternity1st() {
        return staffLeavesMaternity1st;
    }

    public void setStaffLeavesMaternity1st(List<StaffLeave> staffLeavesMaternity1st) {
        this.staffLeavesMaternity1st = staffLeavesMaternity1st;
    }

    public List<StaffLeave> getStaffLeavesMaternity2nd() {
        return staffLeavesMaternity2nd;
    }

    public void setStaffLeavesMaternity2nd(List<StaffLeave> staffLeavesMaternity2nd) {
        this.staffLeavesMaternity2nd = staffLeavesMaternity2nd;
    }

    public List<StaffLeave> getStaffLeavesLieu() {
        return staffLeavesLieu;
    }

    public void setStaffLeavesLieu(List<StaffLeave> staffLeavesLieu) {
        this.staffLeavesLieu = staffLeavesLieu;
    }

    public double getMedicalEntitle() {
        return medicalEntitle;
    }

    public void setMedicalEntitle(double medicalEntitle) {
        this.medicalEntitle = medicalEntitle;
    }

    public double getMedicalUtilized() {
        return medicalUtilized;
    }

    public void setMedicalUtilized(double medicalUtilized) {
        this.medicalUtilized = medicalUtilized;
    }

    public double getMaternity1Entitle() {
        return maternity1Entitle;
    }

    public void setMaternity1Entitle(double maternity1Entitle) {
        this.maternity1Entitle = maternity1Entitle;
    }

    public double getMaternity1Utilized() {
        return maternity1Utilized;
    }

    public void setMaternity1Utilized(double maternity1Utilized) {
        this.maternity1Utilized = maternity1Utilized;
    }

    public double getMaternity2Entitle() {
        return maternity2Entitle;
    }

    public void setMaternity2Entitle(double maternity2Entitle) {
        this.maternity2Entitle = maternity2Entitle;
    }

    public double getMaternity2Utilized() {
        return maternity2Utilized;
    }

    public void setMaternity2Utilized(double maternity2Utilized) {
        this.maternity2Utilized = maternity2Utilized;
    }

    public double getLieuEntitle() {
        return lieuEntitle;
    }

    public void setLieuEntitle(double lieuEntitle) {
        this.lieuEntitle = lieuEntitle;
    }

    public double getLieuUtilized() {
        return lieuUtilized;
    }

    public void setLieuUtilized(double lieuUtilized) {
        this.lieuUtilized = lieuUtilized;
    }

    public List<StaffShift> getStaffShiftsNoPay() {
        return staffShiftsNoPay;
    }

    public void setStaffShiftsNoPay(List<StaffShift> staffShiftsNoPay) {
        this.staffShiftsNoPay = staffShiftsNoPay;
    }

    public double getTotalOvertimeMinit() {
        return totalOvertimeMinit;
    }

    public void setTotalOvertimeMinit(double totalOvertimeMinit) {
        this.totalOvertimeMinit = totalOvertimeMinit;
    }

    public double getTotalExtraDutyNormalMinute() {
        return totalExtraDutyNormalMinute;
    }

    public void setTotalExtraDutyNormalMinute(double totalExtraDutyNormalMinute) {
        this.totalExtraDutyNormalMinute = totalExtraDutyNormalMinute;
    }

    public double getTotalRatePerMinut() {
        return totalRatePerMinut;
    }

    public void setTotalRatePerMinut(double totalRatePerMinut) {
        this.totalRatePerMinut = totalRatePerMinut;
    }

    public double getTotalOtValue() {
        return totalOtValue;
    }

    public void setTotalOtValue(double totalOtValue) {
        this.totalOtValue = totalOtValue;
    }

    public double getTotalPhOtMin() {
        return totalPhOtMin;
    }

    public void setTotalPhOtMin(double totalPhOtMin) {
        this.totalPhOtMin = totalPhOtMin;
    }

    public double getTotalRatePerMinutPhOt() {
        return totalRatePerMinutPhOt;
    }

    public void setTotalRatePerMinutPhOt(double totalRatePerMinutPhOt) {
        this.totalRatePerMinutPhOt = totalRatePerMinutPhOt;
    }

    public double getTotalPhOtValue() {
        return totalPhOtValue;
    }

    public void setTotalPhOtValue(double totalPhOtValue) {
        this.totalPhOtValue = totalPhOtValue;
    }

    public double getTotalOffDayOtMin() {
        return totalOffDayOtMin;
    }

    public void setTotalOffDayOtMin(double totalOffDayOtMin) {
        this.totalOffDayOtMin = totalOffDayOtMin;
    }

    public double getTotalRatePerMinuts() {
        return totalRatePerMinuts;
    }

    public void setTotalRatePerMinuts(double totalRatePerMinuts) {
        this.totalRatePerMinuts = totalRatePerMinuts;
    }

    public double getTotalOffdyOtValue() {
        return totalOffdyOtValue;
    }

    public void setTotalOffdyOtValue(double totalOffdyOtValue) {
        this.totalOffdyOtValue = totalOffdyOtValue;
    }

    public double getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(double totalValue) {
        this.totalValue = totalValue;
    }

    public boolean isNetSalary() {
        return netSalary;
    }

    public void setNetSalary(boolean netSalary) {
        this.netSalary = netSalary;
    }

    public boolean isOtPayment() {
        return otPayment;
    }

    public void setOtPayment(boolean otPayment) {
        this.otPayment = otPayment;
    }

    public double getTotalTransNetSalary() {
        return totalTransNetSalary;
    }

    public void setTotalTransNetSalary(double totalTransNetSalary) {
        this.totalTransNetSalary = totalTransNetSalary;
    }

    public double getTotalOverTime() {
        return totalOverTime;
    }

    public void setTotalOverTime(double totalOverTime) {
        this.totalOverTime = totalOverTime;
    }

    public double getTotalofTotals() {
        return totalofTotals;
    }

    public void setTotalofTotals(double totalofTotals) {
        this.totalofTotals = totalofTotals;
    }

    public double getTotaldayOffAllowance() {
        return totaldayOffAllowance;
    }

    public void setTotaldayOffAllowance(double totaldayOffAllowance) {
        this.totaldayOffAllowance = totaldayOffAllowance;
    }

    public double getTotaldayOffCount() {
        return totaldayOffCount;
    }

    public void setTotaldayOffCount(double totaldayOffCount) {
        this.totaldayOffCount = totaldayOffCount;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public boolean isHold() {
        return hold;
    }

    public void setHold(boolean hold) {
        this.hold = hold;
    }

    public String getBackButtonPage() {
        return backButtonPage;
    }

    public void setBackButtonPage(String backButtonPage) {
        this.backButtonPage = backButtonPage;
    }

    public String back() {
        String s = backButtonPage;
        backButtonPage = null;
        return s;
    }

    public List<OverTimeAllMonth> getOverTimeAllMonths() {
        return overTimeAllMonths;
    }

    public void setOverTimeAllMonths(List<OverTimeAllMonth> overTimeAllMonths) {
        this.overTimeAllMonths = overTimeAllMonths;
    }

    public List<SummeryForMonth> getSummeryForMonths() {
        return summeryForMonths;
    }

    public void setSummeryForMonths(List<SummeryForMonth> summeryForMonths) {
        this.summeryForMonths = summeryForMonths;
    }

}
