/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.hr;

import com.divudi.data.hr.ReportKeyWord;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.data.hr.DayType;
import com.divudi.data.hr.LeaveType;
import com.divudi.data.hr.PaysheetComponentType;
import com.divudi.ejb.CommonFunctions;
import com.divudi.ejb.FinalVariables;
import com.divudi.ejb.HumanResourceBean;
import com.divudi.entity.Staff;
import com.divudi.entity.hr.HrForm;
import com.divudi.entity.hr.LeaveFormSystem;
import com.divudi.entity.hr.PaysheetComponent;
import com.divudi.entity.hr.SalaryCycle;
import com.divudi.entity.hr.StaffLeaveSystem;
import com.divudi.entity.hr.StaffPaysheetComponent;
import com.divudi.entity.hr.StaffSalary;
import com.divudi.entity.hr.StaffSalaryComponant;
import com.divudi.entity.hr.StaffShift;
import com.divudi.facade.LeaveFormFacade;
import com.divudi.facade.StaffEmploymentFacade;
import com.divudi.facade.StaffFacade;
import com.divudi.facade.StaffLeaveFacade;
import com.divudi.facade.StaffPaysheetComponentFacade;
import com.divudi.facade.StaffSalaryComponantFacade;
import com.divudi.facade.StaffSalaryFacade;
import com.divudi.facade.StaffShiftFacade;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import javax.ejb.EJB;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.persistence.TemporalType;
import org.primefaces.event.RowEditEvent;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class StaffSalaryController implements Serializable {

    private StaffSalary current;
    //////////   
    List<StaffSalary> items;
    List<StaffSalary> getSelectedStaffSalaryList;
    ///////
    @EJB
    private StaffSalaryFacade staffSalaryFacade;
    @EJB
    private StaffPaysheetComponentFacade staffPaysheetComponentFacade;
    @EJB
    private StaffSalaryComponantFacade staffSalaryComponantFacade;
    @EJB
    private StaffEmploymentFacade staffEmploymentFacade;
    @EJB
    private StaffFacade staffFacade;
    @EJB
    private HumanResourceBean humanResourceBean;
    @EJB
    private CommonFunctions commonFunctions;
    /////////////
    @Inject
    private SessionController sessionController;
    @Inject
    private HrmVariablesController hrmVariablesController;
    @Inject
    private StaffController staffController;
    SalaryCycle salaryCycle;
    boolean printPreview = false;

    public SalaryCycle getSalaryCycle() {
        return salaryCycle;
    }

    public void setSalaryCycle(SalaryCycle salaryCycle) {
        this.salaryCycle = salaryCycle;
    }

    public FinalVariables getFinalVariables() {
        return finalVariables;
    }

    public void setFinalVariables(FinalVariables finalVariables) {
        this.finalVariables = finalVariables;
    }

    public List<StaffSalary> getGetSelectedStaffSalaryList() {
        return getSelectedStaffSalaryList;
    }

    public void setGetSelectedStaffSalaryList(List<StaffSalary> getSelectedStaffSalaryList) {
        this.getSelectedStaffSalaryList = getSelectedStaffSalaryList;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public void printPreview() {
        printPreview = true;
    }

    public void recreateModel() {
        getSelectedStaffSalaryList = null;
        current = null;
        printPreview = false;
    }

    public void remove() {
        getCurrent().setRetired(true);
        getCurrent().setRetiredAt(new Date());
        getCurrent().setRetirer(getSessionController().getLoggedUser());

        getStaffSalaryFacade().edit(getCurrent());
        current = null;
    }

    public void save() {
//
//        if (errorCheck()) {
//            return;
//        }

        List<StaffSalaryComponant> list = getCurrent().getStaffSalaryComponants();

        if (getCurrent().getId() == null) {
            getCurrent().setInstitution(getCurrent().getStaff().getInstitution());
            getCurrent().setDepartment(getCurrent().getStaff().getWorkingDepartment());
            getCurrent().setSalaryCycle(salaryCycle);
            getCurrent().setCreatedAt(new Date());
            getCurrent().setCreater(getSessionController().getLoggedUser());
            getCurrent().setStaffSalaryComponants(null);
            getStaffSalaryFacade().create(getCurrent());
        } else {
            getStaffSalaryFacade().edit(getCurrent());
        }

        updateComponent(list);
//        updateStaffShifts();

        getCurrent().setStaffSalaryComponants(list);
        getStaffSalaryFacade().edit(getCurrent());

        // makeNull();
    }

    private void updateComponent(List<StaffSalaryComponant> list) {
        for (StaffSalaryComponant ssc : list) {
            ssc.setStaffSalary(getCurrent());
            if (ssc.getId() == null) {
                getStaffSalaryComponantFacade().create(ssc);
            } else {
                getStaffSalaryComponantFacade().edit(ssc);
            }
        }

    }

    @EJB
    StaffShiftFacade staffShiftFacade;

    public StaffShiftFacade getStaffShiftFacade() {
        return staffShiftFacade;
    }

    public void setStaffShiftFacade(StaffShiftFacade staffShiftFacade) {
        this.staffShiftFacade = staffShiftFacade;
    }

//    private void updateStaffShifts() {
//        Set<StaffShift> staffShifts = new HashSet<>();
//
//        staffShifts.addAll(getCurrent().getTransStaffShiftsSalary());
//        staffShifts.addAll(getCurrent().getTransStaffShiftsOverTime());
//        staffShifts.addAll(getCurrent().getTransStaffShiftsExtraDuty());
//
//        for (StaffShift ss : staffShifts) {
//            ss.setStaffSalary(getCurrent());
//            getStaffShiftFacade().edit(ss);
//        }
//
//    }
    public void onEdit(RowEditEvent event) {
        //////System.out.println("Runn");
        StaffSalaryComponant tmp = (StaffSalaryComponant) event.getObject();

        getHumanResourceBean().setEpf(tmp, getHrmVariablesController().getCurrent().getEpfRate(), getHrmVariablesController().getCurrent().getEpfCompanyRate());
        getHumanResourceBean().setEtf(tmp, getHrmVariablesController().getCurrent().getEtfRate(), getHrmVariablesController().getCurrent().getEtfCompanyRate());
        tmp.setLastEditedAt(new Date());
        tmp.setLastEditor(getSessionController().getLoggedUser());

        if (tmp.getId() != null) {
            getStaffSalaryComponantFacade().edit(tmp);
        }

        getCurrent().calculateComponentTotal();
        getCurrent().calcualteEpfAndEtf();
        if (getCurrent().getId() != null) {
            getStaffSalaryFacade().edit(getCurrent());
        }

    }

    public StaffSalaryController() {
    }

    public void makeNull() {
        current = null;
        items = null;
    }

    public void clear() {
        current = null;
        salaryCycle = null;
        items = null;

        getStaffController().makeNull();
    }

    public StaffSalary getCurrent() {
        if (current == null) {
            current = new StaffSalary();

        }
        return current;
    }

    @Inject
    SalaryCycleController salaryCycleController;

    public void onEditListener(StaffSalary staffSalary) {
        current = staffSalary;

        if (current == null) {
            return;
        }

        List<PaysheetComponent> paysheetComponentsAddition = salaryCycleController.fetchPaysheetComponents(PaysheetComponentType.addition.getUserDefinedComponentsAddidtionsWithPerformance(), getSalaryCycle());
        List<PaysheetComponent> paysheetComponentsSubstraction = salaryCycleController.fetchPaysheetComponents(PaysheetComponentType.subtraction.getUserDefinedComponentsDeductionsWithSalaryAdvance(), getSalaryCycle());

//        System.err.println("Add Size " + paysheetComponentsAddition.size());
//        System.err.println("Sub Size " + paysheetComponentsSubstraction.size());
        for (PaysheetComponent psc : paysheetComponentsAddition) {
            StaffSalaryComponant c = salaryCycleController.fetchSalaryComponents(getCurrent(), psc, false, getSalaryCycle());
            getCurrent().getTransStaffSalaryComponantsAddition().add(c);

        }
        for (PaysheetComponent psc : paysheetComponentsSubstraction) {
            StaffSalaryComponant c = salaryCycleController.fetchSalaryComponents(getCurrent(), psc, false, getSalaryCycle());
            getCurrent().getTransStaffSalaryComponantsSubtraction().add(c);

        }

    }

    public void onEditBlockedUpdate(StaffSalary staffSalary) {
//        current = staffSalary;

        if (staffSalary == null) {
            return;
        }

//        if (current.isBlocked()) {
//            current.setBlockedUser(sessionController.getLoggedUser());
//            current.setBlockedDate(new Date());
//        }
        staffSalaryFacade.edit(staffSalary);
    }

    public void setCurrent(StaffSalary current) {
        this.current = current;

//        OtNormalSpecial otNormalSpecial = getHumanResourceBean().calculateOt(getOverTimeFromDate(), getOverTimeToDate(), getCurrent().getStaff());
//
//        current.setTmpOtNormalSpecial(otNormalSpecial);
//        current.setTmpExtraDutyCount(getHumanResourceBean().calExtraDuty(getExtraDutyFromDate(), getExtraDutyToDate(), getCurrent().getStaff()));
    }

    public StaffSalaryFacade getStaffSalaryFacade() {
        return staffSalaryFacade;
    }

    public void setStaffSalaryFacade(StaffSalaryFacade staffSalaryFacade) {
        this.staffSalaryFacade = staffSalaryFacade;
    }

//    private boolean checkBasic() {
//        for (StaffSalaryComponant ss : getStaffSalaryComponants()) {
//            if (ss.getStaffPaysheetComponent().getPaysheetComponent().getComponentType() == PaysheetComponentType.BasicSalary) {
//                return true;
//            }
//        }
//
//        return false;
//    }
    public void cycleSelectListener() {
        staffController.setToDate(salaryCycle.getSalaryToDate());
    }

    private boolean checkDateRange(Date date) {
        if (date == null) {
            return false;
        }

        if ((getSalaryCycle().getSalaryFromDate().getTime() < date.getTime()
                && getSalaryCycle().getSalaryToDate().getTime() > date.getTime())) {

            return true;
        }

        return false;

    }

    private double calValue(double value) {

        if (value == 0) {
            return 0;
        }
//        //System.out.println("calculating value");
        //Check Employee Join Date Come within Salary Cycle
        if (checkDateRange(getCurrent().getStaff().getDateJoined())
                //Check Employee Date Left within Salary Cycle
                || checkDateRange(getCurrent().getStaff().getDateLeft())
                //Check Employee Date Retired within Salary Cycle
                || checkDateRange(getCurrent().getStaff().getDateRetired())) {

            double workedDays = humanResourceBean.calculateWorkedDaysForSalary(salaryCycle.getSalaryFromDate(), salaryCycle.getSalaryToDate(), getCurrent().getStaff());

            if (workedDays >= finalVariables.getWorkingDaysPerMonth()) {
                return value;
            } else {
                return (value / finalVariables.getWorkingDaysPerMonth()) * workedDays;
            }

        } else {
//            //System.out.println("returning full value");

            return value;
        }

    }

    private double calValue(double value, double percentage) {

        if (value == 0 || percentage == 0) {
            return 0;
        }

        //Check Employee Join Date Come within Salary Cycle
        if (checkDateRange(getCurrent().getStaff().getDateJoined())
                //Check Employee Date Left within Salary Cycle
                || checkDateRange(getCurrent().getStaff().getDateLeft())
                //Check Employee Date Retired within Salary Cycle
                || checkDateRange(getCurrent().getStaff().getDateRetired())) {

            double workedDays = humanResourceBean.calculateWorkedDaysForSalary(salaryCycle.getSalaryFromDate(), salaryCycle.getSalaryToDate(), getCurrent().getStaff());

            if (workedDays >= finalVariables.getWorkingDaysPerMonth()) {
                return value * (percentage / 100);
            } else {
                return (value / finalVariables.getWorkingDaysPerMonth()) * workedDays * (percentage / 100);
            }

        } else {

            return value * (percentage / 100);
        }

    }

    private void setBasic() {
//        getHumanResourceBean().calculateBasic(getSalaryFromDate(), getSalaryToDate(), getCurrent().getStaff());

        StaffSalaryComponant ss = new StaffSalaryComponant();
        ss.setCreatedAt(new Date());
        ss.setSalaryCycle(salaryCycle);
        ss.setCreater(getSessionController().getLoggedUser());
        ss.setStaffPaysheetComponent(getHumanResourceBean().getBasic(getCurrent().getStaff(), getSalaryCycle().getSalaryToDate()));
        if (ss.getStaffPaysheetComponent() != null) {

            double basicValue = calValue(ss.getStaffPaysheetComponent().getStaffPaySheetComponentValue());

            ss.setComponantValue(basicValue);
        } else {
            return;
        }

        getHumanResourceBean().setEpf(ss, getHrmVariablesController().getCurrent().getEpfRate(), getHrmVariablesController().getCurrent().getEpfCompanyRate());
        getHumanResourceBean().setEtf(ss, getHrmVariablesController().getCurrent().getEtfRate(), getHrmVariablesController().getCurrent().getEtfCompanyRate());

//        System.err.println("BASIC " + ss.getStaffPaysheetComponent().getPaysheetComponent().getName());
        getCurrent().getStaffSalaryComponants().add(ss);

    }

    private void setPerformaceAllovance() {
        StaffPaysheetComponent percentageComponent = getHumanResourceBean().fetchStaffPaysheetComponent(getCurrent().getStaff(), getSalaryCycle().getSalaryToDate(), PaysheetComponentType.PerformanceAllowancePercentage);
        StaffPaysheetComponent component = getHumanResourceBean().fetchStaffPaysheetComponent(getCurrent().getStaff(), getSalaryCycle().getSalaryToDate(), PaysheetComponentType.PerformanceAllowance);

        if (percentageComponent == null || component == null) {
            return;
        }

        StaffSalaryComponant ss = new StaffSalaryComponant();
        ss.setCreatedAt(new Date());
        ss.setSalaryCycle(salaryCycle);
        ss.setCreater(getSessionController().getLoggedUser());
        ss.setStaffPaysheetComponent(component);
        ss.setStaffPaysheetComponentPercentage(percentageComponent);
        if (ss.getStaffPaysheetComponent() != null) {
            double value = calValue(ss.getStaffPaysheetComponent().getStaffPaySheetComponentValue(), percentageComponent.getStaffPaySheetComponentValue());
            ss.setComponantValue(value);
        } else {
            return;
        }

        getHumanResourceBean().setEpf(ss, getHrmVariablesController().getCurrent().getEpfRate(), getHrmVariablesController().getCurrent().getEpfCompanyRate());
        getHumanResourceBean().setEtf(ss, getHrmVariablesController().getCurrent().getEtfRate(), getHrmVariablesController().getCurrent().getEtfCompanyRate());

//        System.err.println("Performance " + ss.getStaffPaysheetComponent().getPaysheetComponent().getName());
        getCurrent().getStaffSalaryComponants().add(ss);

    }

//    private void setOT() {
//        StaffSalaryComponant ss = new StaffSalaryComponant();
//        ss.setCreatedAt(new Date());
//        ss.setCreater(getSessionController().getLoggedUser());
//        ss.setStaffPaysheetComponent(getHumanResourceBean().getComponent(getCurrent().getStaff(), getSessionController().getLoggedUser(), PaysheetComponentType.OT));
//        if (ss.getStaffPaysheetComponent() != null) {
//            OtNormalSpecial otNormalSpecial = getHumanResourceBean().calculateOt(getOverTimeFromDate(), getOverTimeToDate(), getCurrent().getStaff());
//            ss.setComponantValue(otNormalSpecial.getNormalValue() + otNormalSpecial.getSpecialValue());
//        } else {
//            return;
//        }
//
//        getHumanResourceBean().setEpf(ss, getHrmVariablesController().getEpfRate(), getHrmVariablesController().getEpfCompanyRate());
//        getHumanResourceBean().setEtf(ss, getHrmVariablesController().getEtfRate(), getHrmVariablesController().getEtfCompanyRate());
//
//        System.err.println("OT " + ss.getStaffPaysheetComponent().getPaysheetComponent().getName());
//        getCurrent().getStaffSalaryComponants().add(ss);
//
//    }
    @EJB
    FinalVariables finalVariables;

    public Long calculateOverTimeMinute() {
        System.out.println("calculating over time in minutes");
        Long dateCount = commonFunctions.getDayCount(getSalaryCycle().getWorkedFromDate(), getSalaryCycle().getWorkedToDate());
        System.out.println("dateCount = " + dateCount);
        Long numOfWeeks = dateCount / 7;
        System.out.println("numOfWeeks = " + numOfWeeks);
        if (numOfWeeks == 0l) {
            return 0l;
        }

        Double overTimeSec = 0.0;

        Date fromDate = getSalaryCycle().getWorkedFromDate();
        System.out.println("fromDate = " + fromDate);
        Calendar frmCal = Calendar.getInstance();
        frmCal.setTime(fromDate);
        frmCal.setTime(fromDate);
        frmCal.set(Calendar.HOUR, 7);
        frmCal.set(Calendar.MINUTE, 0);
        frmCal.set(Calendar.SECOND, 0);
        frmCal.set(Calendar.MILLISECOND, 0);

        Calendar toCal = Calendar.getInstance();
        toCal.setTime(fromDate);
        toCal.set(Calendar.HOUR, 7);
        toCal.set(Calendar.MINUTE, 0);
        toCal.set(Calendar.SECOND, 0);
        toCal.set(Calendar.MILLISECOND, 0);
        toCal.add(Calendar.DATE, 7);
        toCal.add((Calendar.MILLISECOND), -1);
//        System.err.println("adding 6 days only. No Milli seconds ");

//        System.err.println("FROM1 " + frmCal.getTime());
//        System.err.println("TO1 " + toCal.getTime());
//        
        for (int i = 0; i < numOfWeeks; i++) {

//            //System.out.println("i = " + i);
            double workedWithinTimeFrameVarified = getHumanResourceBean().calculateWorkTimeForOverTime(frmCal.getTime(), toCal.getTime(), getCurrent().getStaff());
            System.out.println("*** worked Within TimeFrameVarified = " + workedWithinTimeFrameVarified / (60 * 60));

            System.err.println("FROM " + i + frmCal.getTime());
            System.err.println("TO " + i + toCal.getTime());

            //The below line was commented by safrin. Buddhiks uncommented it. Please double check.
//            workedWithinTimeFrameVarified += getHumanResourceBean().calculateLeaveTimeForOverTime(frmCal.getTime(), toCal.getTime(), getCurrent().getStaff());
            double otSec = humanResourceBean.getOverTimeFromRoster(getCurrent().getStaff().getWorkingTimeForOverTimePerWeek(), 1, workedWithinTimeFrameVarified);

//            //System.out.println("otSec = " + otSec);
//            System.err.println("Working Time : " + workedWithinTimeFrameVarified / (60 * 60));
            System.err.println("OT Time : " + otSec / (60 * 60));

            overTimeSec += otSec;
            frmCal.add(Calendar.DATE, 7);
            toCal.add(Calendar.DATE, 7);

        }

        System.err.println("OT Min " + (overTimeSec.longValue() / 60L));
        return (overTimeSec.longValue() / 60L);
    }

    public Long calculateOverTimeMinuteByDate() {
        System.out.println("calculating over time in minutes");
        Long dateCount = commonFunctions.getDayCount(getSalaryCycle().getWorkedFromDate(), getSalaryCycle().getWorkedToDate());
        System.out.println("dateCount = " + dateCount);
        Long numOfWeeks = dateCount / 7;
        System.out.println("numOfWeeks = " + numOfWeeks);
        if (numOfWeeks == 0l) {
            return 0l;
        }

        Double overTimeSec = 0.0;

        Date fromDate = getSalaryCycle().getWorkedFromDate();
        System.out.println("fromDate = " + fromDate);
        Calendar frmCal = Calendar.getInstance();
        frmCal.setTime(fromDate);
        frmCal.setTime(fromDate);
        frmCal.set(Calendar.HOUR, 0);
        frmCal.set(Calendar.MINUTE, 0);
        frmCal.set(Calendar.SECOND, 0);
        frmCal.set(Calendar.MILLISECOND, 1);

        Calendar toCal = Calendar.getInstance();
        toCal.setTime(fromDate);
        toCal.add(Calendar.DATE, 6);
//        toCal.set(Calendar.HOUR, 7);
//        toCal.set(Calendar.MINUTE, 0);
//        toCal.set(Calendar.SECOND, 0);
//        toCal.set(Calendar.MILLISECOND, 0);
//        toCal.add(Calendar.DATE, 7);
//        toCal.add((Calendar.MILLISECOND), -1);
//        System.err.println("adding 6 days only. No Milli seconds ");

//        System.err.println("FROM1 " + frmCal.getTime());
//        System.err.println("TO1 " + toCal.getTime());
//        
        for (int i = 0; i < numOfWeeks; i++) {

//            //System.out.println("i = " + i);
            double workedWithinTimeFrameVarified = getHumanResourceBean().calculateWorkTimeForOverTimeByDate(frmCal.getTime(), toCal.getTime(), getCurrent().getStaff());
            System.out.println("*** worked Within TimeFrameVarified = " + workedWithinTimeFrameVarified / (60 * 60));

            System.err.println("FROM " + i + frmCal.getTime());
            System.err.println("TO " + i + toCal.getTime());

            //The below line was commented by safrin. Buddhiks uncommented it. Please double check.
//            workedWithinTimeFrameVarified += getHumanResourceBean().calculateLeaveTimeForOverTime(frmCal.getTime(), toCal.getTime(), getCurrent().getStaff());
            double otSec = humanResourceBean.getOverTimeFromRoster(getCurrent().getStaff().getWorkingTimeForOverTimePerWeek(), 1, workedWithinTimeFrameVarified);

//            //System.out.println("otSec = " + otSec);
//            System.err.println("Working Time : " + workedWithinTimeFrameVarified / (60 * 60));
            System.err.println("OT Time : " + otSec / (60 * 60));

            overTimeSec += otSec;
            frmCal.add(Calendar.DATE, 7);
            toCal.add(Calendar.DATE, 7);

        }

        System.err.println("OT Min " + (overTimeSec.longValue() / 60L));
        return (overTimeSec.longValue() / 60L);
    }

    public double getBasicPerMinute() {

        for (StaffSalaryComponant staffSalaryComponant : getCurrent().getStaffSalaryComponants()) {
            if (staffSalaryComponant.getStaffPaysheetComponent() == null
                    || staffSalaryComponant.getStaffPaysheetComponent().getPaysheetComponent() == null) {
                continue;
            }

            if (staffSalaryComponant.getStaffPaysheetComponent().getPaysheetComponent().getComponentType() == PaysheetComponentType.BasicSalary
                    && staffSalaryComponant.getComponantValue() != 0) {
                return roundOff(staffSalaryComponant.getComponantValue() / (200 * 60));
            }
        }

        return 0;

    }

    public double getOverTimeValuePerMinute() {
        return roundOff(humanResourceBean.getOverTimeValue(getCurrent().getStaff(), getSalaryCycle().getSalaryToDate()) / (200 * 60));

    }

    public void setOT() {

        StaffSalaryComponant ss = createStaffSalaryComponant(PaysheetComponentType.OT);
        System.out.println("ss = " + ss);
        if (ss.getStaffPaysheetComponent() != null) {

            Long overTimeMinute = calculateOverTimeMinuteByDate();//edited by doctor
            System.out.println("overTimeMinute = " + overTimeMinute);

            double overTimePerMinute = getOverTimeValuePerMinute();
            System.out.println("overTimePerMinute = " + overTimePerMinute);

            ss.setComponantValue(overTimeMinute * overTimePerMinute * finalVariables.getOverTimeMultiply());

            getCurrent().setOverTimeMinute(overTimeMinute);
            System.out.println("overTimeMinute = " + overTimeMinute);
            getCurrent().setBasicRatePerMinute(overTimePerMinute);

            getCurrent().setOverTimeRatePerMinute(overTimePerMinute);

        } else {
            return;
        }

        getHumanResourceBean().setEpf(ss, getHrmVariablesController().getCurrent().getEpfRate(), getHrmVariablesController().getCurrent().getEpfCompanyRate());
        getHumanResourceBean().setEtf(ss, getHrmVariablesController().getCurrent().getEtfRate(), getHrmVariablesController().getCurrent().getEtfCompanyRate());
//        System.err.println("OT " + ss.getStaffPaysheetComponent().getPaysheetComponent().getName());
        getCurrent().getStaffSalaryComponants().add(ss);

    }

    private StaffSalaryComponant createStaffSalaryComponant(PaysheetComponentType paysheetComponentType) {
        StaffSalaryComponant ss = new StaffSalaryComponant();
//        ss.setStaffSalary(getCurrent());
        ss.setCreatedAt(new Date());
        ss.setSalaryCycle(salaryCycle);
        ss.setCreater(getSessionController().getLoggedUser());
        ss.setStaffPaysheetComponent(getHumanResourceBean().getComponent(getCurrent().getStaff(), getSessionController().getLoggedUser(), paysheetComponentType));
        getHumanResourceBean().setEpf(ss, getHrmVariablesController().getCurrent().getEpfRate(), getHrmVariablesController().getCurrent().getEpfCompanyRate());
        getHumanResourceBean().setEtf(ss, getHrmVariablesController().getCurrent().getEtfRate(), getHrmVariablesController().getCurrent().getEtfCompanyRate());

        return ss;
    }

    private void setAdjustments() {

        //
        getCurrent().getStaffSalaryComponants().add(createStaffSalaryComponant(PaysheetComponentType.AdjustmentBasicAdd));
        getCurrent().getStaffSalaryComponants().add(createStaffSalaryComponant(PaysheetComponentType.AdjustmentBasicSub));
        getCurrent().getStaffSalaryComponants().add(createStaffSalaryComponant(PaysheetComponentType.AdjustmentAllowanceAdd));
        getCurrent().getStaffSalaryComponants().add(createStaffSalaryComponant(PaysheetComponentType.AdjustmentAllowanceSub));

        //
    }

//    private void setExtraDuty() {
//        getHumanResourceBean().calculateExtraDuty(getExtraDutyFromDate(), getExtraDutyToDate(), getCurrent().getStaff());
//
//        StaffSalaryComponant ss = new StaffSalaryComponant();
//        ss.setCreatedAt(new Date());
//        ss.setCreater(getSessionController().getLoggedUser());
//        ss.setStaffPaysheetComponent(getHumanResourceBean().getComponent(getCurrent().getStaff(), getSessionController().getLoggedUser(), PaysheetComponentType.ExtraDuty));
//        if (ss.getStaffPaysheetComponent() != null) {
//            List<ExtraDutyCount> extraDutyCounts = getHumanResourceBean().calExtraDuty(getExtraDutyFromDate(), getExtraDutyToDate(), getCurrent().getStaff());
//
//            ss.setComponantValue(0);
//        } else {
//            return;
//        }
//
//        getHumanResourceBean().setEpf(ss, getHrmVariablesController().getCurrent().getEpfRate(), getHrmVariablesController().getCurrent().getEpfCompanyRate());
//        getHumanResourceBean().setEtf(ss, getHrmVariablesController().getCurrent().getEtfRate(), getHrmVariablesController().getCurrent().getEtfCompanyRate());
//
//        System.err.println("EXTRA " + ss.getStaffPaysheetComponent().getPaysheetComponent().getName());
//        getCurrent().getStaffSalaryComponants().add(ss);
//
//    }
    private Long setExtraDuty(PaysheetComponentType paysheetComponentType, DayType dayType) {
        StaffSalaryComponant ss = createStaffSalaryComponant(paysheetComponentType);

//        if (current != null) {
//            ss.setComponantValue(humanResourceBean.calculateExtraWorkTimeValue(getSalaryCycle().getWorkedFromDate(), getSalaryCycle().getWorkedToDate(), getCurrent().getStaff(), dayType, getCurrent().getOverTimeRatePerMinute()));
//        } else {
        ss.setComponantValue(humanResourceBean.calculateExtraWorkTimeValue(getSalaryCycle().getWorkedFromDate(), getSalaryCycle().getWorkedToDate(), getCurrent().getStaff(), dayType));
//        }
        getHumanResourceBean().setEpf(ss, getHrmVariablesController().getCurrent().getEpfRate(), getHrmVariablesController().getCurrent().getEpfCompanyRate());
        getHumanResourceBean().setEtf(ss, getHrmVariablesController().getCurrent().getEtfRate(), getHrmVariablesController().getCurrent().getEtfCompanyRate());
        getCurrent().getStaffSalaryComponants().add(ss);
        return humanResourceBean.calculateExtraWorkMinute(getSalaryCycle().getWorkedFromDate(), getSalaryCycle().getWorkedToDate(), getCurrent().getStaff(), dayType);

    }

    private double roundOff(double d) {
        DecimalFormat newFormat = new DecimalFormat("#.##");
        try {
            return Double.valueOf(newFormat.format(d));
        } catch (Exception e) {
            return 0;
        }
    }

    private Double setHoliDayAllowance(PaysheetComponentType paysheetComponentType, DayType dayType) {
        Double count = 0.0;
        StaffSalaryComponant ss = createStaffSalaryComponant(paysheetComponentType);
        if (ss.getStaffPaysheetComponent() != null) {
            count = getHumanResourceBean().calculateHolidayWork(getSalaryCycle().getSalaryFromDate(), getSalaryCycle().getSalaryToDate(), getCurrent().getStaff(), dayType);

            double salaryValue = 0;

            if (getCurrent().getStaffSalaryComponants() == null) {
                return count;
            }

            for (StaffSalaryComponant staffSalaryComponant : getCurrent().getStaffSalaryComponants()) {
                if (staffSalaryComponant == null
                        || staffSalaryComponant.getStaffPaysheetComponent() == null
                        || staffSalaryComponant.getStaffPaysheetComponent().getPaysheetComponent() == null) {
                    continue;
                }

                if (staffSalaryComponant.getStaffPaysheetComponent().getPaysheetComponent().isIncludeForAllowance()) {
                    salaryValue += calValue(staffSalaryComponant.getComponantValue());
                }
            }

            if (salaryValue != 0) {
                double salaryPerDay = roundOff(salaryValue / finalVariables.getWorkingDaysPerMonth());
                double value = getHumanResourceBean().calculateHolidayWork(getSalaryCycle().getSalaryFromDate(), getSalaryCycle().getSalaryToDate(), getCurrent().getStaff(), dayType, salaryPerDay);
                ss.setComponantValue(value);
            }

        } else {
            return 0.0;
        }

        getHumanResourceBean().setEpf(ss, getHrmVariablesController().getCurrent().getEpfRate(), getHrmVariablesController().getCurrent().getEpfCompanyRate());
        getHumanResourceBean().setEtf(ss, getHrmVariablesController().getCurrent().getEtfRate(), getHrmVariablesController().getCurrent().getEtfCompanyRate());

//        System.err.println("NO " + ss.getStaffPaysheetComponent().getPaysheetComponent().getName());
        getCurrent().getStaffSalaryComponants().add(ss);

        return count;

    }

    private Double setDayOffSleepingDayAllowance(PaysheetComponentType paysheetComponentType, DayType dayType) {
        Double count = 0.0;
        StaffSalaryComponant ss = createStaffSalaryComponant(paysheetComponentType);
        if (ss.getStaffPaysheetComponent() != null) {
            count = getHumanResourceBean().calculateOffDays(getSalaryCycle().getSalaryFromDate(), getSalaryCycle().getSalaryToDate(), getCurrent().getStaff(), dayType);
            double salaryValue = 0;
            for (StaffSalaryComponant staffSalaryComponant : getCurrent().getStaffSalaryComponants()) {
                if (staffSalaryComponant == null
                        || staffSalaryComponant.getStaffPaysheetComponent() == null
                        || staffSalaryComponant.getStaffPaysheetComponent().getPaysheetComponent() == null) {
                    continue;
                }

                if (staffSalaryComponant.getStaffPaysheetComponent().getPaysheetComponent().isIncludeForAllowance()) {
                    salaryValue += calValue(staffSalaryComponant.getComponantValue());
                }
            }

            if (salaryValue != 0) {
                double salaryPerDay = roundOff(salaryValue / finalVariables.getWorkingDaysPerMonth());
                double value = getHumanResourceBean().calculateOffDays(getSalaryCycle().getSalaryFromDate(), getSalaryCycle().getSalaryToDate(), getCurrent().getStaff(), dayType, salaryPerDay);
                //Need Calculation Sum
                ss.setComponantValue(value);
            }
        } else {
            return 0.0;
        }

        getHumanResourceBean().setEpf(ss, getHrmVariablesController().getCurrent().getEpfRate(), getHrmVariablesController().getCurrent().getEpfCompanyRate());
        getHumanResourceBean().setEtf(ss, getHrmVariablesController().getCurrent().getEtfRate(), getHrmVariablesController().getCurrent().getEtfCompanyRate());

//        System.err.println("NO " + ss.getStaffPaysheetComponent().getPaysheetComponent().getName());
        getCurrent().getStaffSalaryComponants().add(ss);
        return count;
    }

    private double setNoPay_Basic(double noPayCount) {
        double noPayValue = 0;
        double salaryValue = 0;
        StaffSalaryComponant ss = createStaffSalaryComponant(PaysheetComponentType.No_Pay_Deduction_Basic);
        if (ss.getStaffPaysheetComponent() != null) {
            noPayCount = getHumanResourceBean().fetchStaffLeave(getCurrent().getStaff(), LeaveType.No_Pay, getSalaryCycle().getSalaryFromDate(), getSalaryCycle().getSalaryToDate());
            salaryValue = 0;

            if (getCurrent().getStaffSalaryComponants() == null) {
                return 0;
            }

            for (StaffSalaryComponant staffSalaryComponant : getCurrent().getStaffSalaryComponants()) {
                if (staffSalaryComponant == null
                        || staffSalaryComponant.getStaffPaysheetComponent() == null
                        || staffSalaryComponant.getStaffPaysheetComponent().getPaysheetComponent() == null) {
                    continue;
                }

                if (staffSalaryComponant.getStaffPaysheetComponent().getPaysheetComponent().getComponentType() == PaysheetComponentType.BasicSalary) {
                    salaryValue += staffSalaryComponant.getComponantValue();
                }
            }

            //Need Calculation Sum
            if (salaryValue != 0) {
                noPayValue = roundOff(salaryValue / finalVariables.getWorkingDaysPerMonth()) * noPayCount;
                ss.setComponantValue(noPayValue);
            }

        } else {
            return 0;
        }

        getHumanResourceBean().setEpf(ss, getHrmVariablesController().getCurrent().getEpfRate(), getHrmVariablesController().getCurrent().getEpfCompanyRate());
        getHumanResourceBean().setEtf(ss, getHrmVariablesController().getCurrent().getEtfRate(), getHrmVariablesController().getCurrent().getEtfCompanyRate());

        getCurrent().getStaffSalaryComponants().add(ss);

        return salaryValue;
    }

    public double calAllowanceValueForNoPay(List<StaffSalaryComponant> staffSalaryComponants) {
        if (staffSalaryComponants == null) {
            return 0;
        }

        double allownaceValue = 0;
        for (StaffSalaryComponant staffSalaryComponant : staffSalaryComponants) {
            if (staffSalaryComponant.getStaffPaysheetComponent() == null
                    || staffSalaryComponant.getStaffPaysheetComponent().getPaysheetComponent() == null) {
                continue;
            }

            if (staffSalaryComponant.getStaffPaysheetComponent().getPaysheetComponent().isIncludedForNoPay()
                    && staffSalaryComponant.getStaffPaysheetComponent().getPaysheetComponent().getComponentType() != PaysheetComponentType.BasicSalary
                    && staffSalaryComponant.getStaffPaysheetComponent().getPaysheetComponent().getComponentType().is(PaysheetComponentType.addition)) {
                allownaceValue += staffSalaryComponant.getComponantValue();
            }
        }

        return allownaceValue;

    }

    private double setNoPay_Allowance() {
        double noPayValue = 0;
        double allownaceValue = 0;
        double noPayCount = 0;
        StaffSalaryComponant ss = createStaffSalaryComponant(PaysheetComponentType.No_Pay_Deduction_Allowance);
        if (ss.getStaffPaysheetComponent() != null) {
            noPayCount = getHumanResourceBean().fetchStaffLeaveAddedLeave(getCurrent().getStaff(), LeaveType.No_Pay, getSalaryCycle().getSalaryFromDate(), getSalaryCycle().getSalaryToDate());
            allownaceValue = calAllowanceValueForNoPay(getCurrent().getStaffSalaryComponants());

            //Need Calculation Sum
            if (allownaceValue != 0) {
                noPayValue = roundOff(allownaceValue / finalVariables.getWorkingDaysPerMonth()) * noPayCount;
                ss.setComponantValue(noPayValue);
            }

        } else {
            return 0;
        }

        getHumanResourceBean().setEpf(ss, getHrmVariablesController().getCurrent().getEpfRate(), getHrmVariablesController().getCurrent().getEpfCompanyRate());
        getHumanResourceBean().setEtf(ss, getHrmVariablesController().getCurrent().getEtfRate(), getHrmVariablesController().getCurrent().getEtfCompanyRate());

        getCurrent().getStaffSalaryComponants().add(ss);

        return allownaceValue;
    }

    private boolean dateCheck() {
        if (getSalaryCycle() == null) {
            UtilityController.addErrorMessage("Please Select Salary Cycle");
            return true;
        }

        if (getSalaryCycle().getSalaryFromDate() == null || getSalaryCycle().getSalaryToDate() == null) {
            UtilityController.addErrorMessage("Please Select Salary Date");
            return true;
        }

        if (getSalaryCycle().getWorkedFromDate() == null || getSalaryCycle().getWorkedToDate() == null) {
            UtilityController.addErrorMessage("Please Select Over time Date");
            return true;
        }

//        if (getHumanResourceBean().checkExistingSalary(getSalaryCycle().getSalaryFromDate(), getSalaryToDate(), getCurrent().getStaff())) {
//            UtilityController.addErrorMessage("There is Already defined Salary for this salary cycle please edit");
//            return true;
//        }
        return false;

    }

    public void addSalaryComponent() {
        System.out.println("Add Salary Component");

        if (getCurrent().getStaff() != null) {
            System.out.println("getCurrent().getStaff() = " + getCurrent().getStaff().getPerson().getName());

            setBasic();
            setPerformaceAllovance();

            List<StaffPaysheetComponent> listAdd = getHumanResourceBean().fetchStaffPaysheetComponent(getCurrent().getStaff(), getSalaryCycle().getSalaryToDate(), PaysheetComponentType.addition.getUserDefinedComponentsAddidtions());
            System.out.println("listAdd = " + listAdd);

            if (listAdd != null) {
                for (StaffPaysheetComponent spc : listAdd) {
                    System.out.println("spc = " + spc.getPaysheetComponent().getName());
                    StaffSalaryComponant ss = new StaffSalaryComponant();
                    ss.setCreatedAt(new Date());
                    ss.setSalaryCycle(salaryCycle);
                    ss.setCreater(getSessionController().getLoggedUser());
                    ss.setStaffPaysheetComponent(spc);
                    ss.setComponantValue(calValue(spc.getStaffPaySheetComponentValue()));
                    System.out.println("getComponantValue() = " + ss.getComponantValue());
                    getHumanResourceBean().setEpf(ss, getHrmVariablesController().getCurrent().getEpfRate(), getHrmVariablesController().getCurrent().getEpfCompanyRate());
                    getHumanResourceBean().setEtf(ss, getHrmVariablesController().getCurrent().getEtfRate(), getHrmVariablesController().getCurrent().getEtfCompanyRate());
                    getCurrent().getStaffSalaryComponants().add(ss);
                }
            }

            List<StaffPaysheetComponent> listSub = getHumanResourceBean().fetchStaffPaysheetComponent(getCurrent().getStaff(), getSalaryCycle().getSalaryToDate(), PaysheetComponentType.subtraction.getUserDefinedComponentsDeductions());

            if (listSub != null) {
                for (StaffPaysheetComponent spc : listSub) {
//                    System.err.println("Loop 1 " + spc.getPaysheetComponent().getName());
                    if ((spc.getPaysheetComponent().getComponentType() == PaysheetComponentType.LoanInstallemant
                            && spc.isCompleted())
                            || spc.getPaysheetComponent().getComponentType() == PaysheetComponentType.LoanNetSalary
                            || spc.getPaysheetComponent().getComponentType() == PaysheetComponentType.Salary_Advance_Deduction) {
//                        System.err.println("Loop 2 " + spc.getPaysheetComponent().getName());
                        continue;
                    }

                    getCurrent().calculateComponentTotal();
                    getCurrent().calcualteEpfAndEtf();

                    double salaryValueForDiduction = getCurrent().getTransGrossSalary() + getCurrent().getTransTotalAllowance() + getCurrent().getTransTotalDeduction();
//                    System.err.println(" Salary Value Diduction " + salaryValueForDiduction);
//                    System.err.println(" Component Value " + spc.getStaffPaySheetComponentValue());
                    if ((salaryValueForDiduction - spc.getStaffPaySheetComponentValue()) < 0) {
                        continue;
                    }

                    StaffSalaryComponant ss = new StaffSalaryComponant();
                    ss.setCreatedAt(new Date());
                    ss.setSalaryCycle(salaryCycle);
                    ss.setCreater(getSessionController().getLoggedUser());
                    ss.setStaffPaysheetComponent(spc);
                    ss.setComponantValue(spc.getStaffPaySheetComponentValue());
                    getHumanResourceBean().setEpf(ss, getHrmVariablesController().getCurrent().getEpfRate(), getHrmVariablesController().getCurrent().getEpfCompanyRate());
                    getHumanResourceBean().setEtf(ss, getHrmVariablesController().getCurrent().getEtfRate(), getHrmVariablesController().getCurrent().getEtfCompanyRate());
                    getCurrent().getStaffSalaryComponants().add(ss);
                }
            }

            fetchAndSetSalaryAdvance();

            setOT();

            //Set Extra Duty Value
            double extraTimeMinute = setExtraDuty(PaysheetComponentType.ExtraDutyNormal, DayType.Normal);
            System.out.println("extraTimeMinute(DayType.Normal) = " + extraTimeMinute);
            
            extraTimeMinute += setExtraDuty(PaysheetComponentType.ExtraDutyNormal, DayType.Extra);            
            System.out.println("extraTimeMinute(DayType.Normal+DayType.Extra) = " + extraTimeMinute);            
            getCurrent().setExtraDutyNormalMinute(extraTimeMinute);
            
            
            extraTimeMinute = setExtraDuty(PaysheetComponentType.ExtraDutyMerchantile, DayType.MurchantileHoliday);            
            System.out.println("extraTimeMinute(DayType.MurchantileHoliday) = " + extraTimeMinute);
            getCurrent().setExtraDutyMerchantileMinute(extraTimeMinute);
            
            extraTimeMinute = setExtraDuty(PaysheetComponentType.ExtraDutyPoya, DayType.Poya);
            System.out.println("extraTimeMinute(DayType.Poya) = " + extraTimeMinute);            
            getCurrent().setExtraDutyPoyaMinute(extraTimeMinute);
            
            extraTimeMinute = setExtraDuty(PaysheetComponentType.ExtraDutyDayOff, DayType.DayOff);
            System.out.println("extraTimeMinute(DayType.DayOff) = " + extraTimeMinute);
            getCurrent().setExtraDutyDayOffMinute(extraTimeMinute);
            
            extraTimeMinute = setExtraDuty(PaysheetComponentType.ExtraDutySleepingDay, DayType.SleepingDay);
            System.out.println("extraTimeMinute(DayType.SleepingDay) = " + extraTimeMinute);
            getCurrent().setExtraDutySleepingDayMinute(extraTimeMinute);

            //Set Holiday Allowance
            Double count = setHoliDayAllowance(PaysheetComponentType.MerchantileAllowance, DayType.MurchantileHoliday);
            System.out.println("extraTimeMinute(DayType.MurchantileHoliday) = " + count);
            getCurrent().setMerchantileCount(count);
            
            count = setHoliDayAllowance(PaysheetComponentType.PoyaAllowance, DayType.Poya);
            System.out.println("count(DayType.Poya) = " + count);
            getCurrent().setPoyaCount(count);
            
            count = setDayOffSleepingDayAllowance(PaysheetComponentType.DayOffAllowance, DayType.DayOff);
            System.out.println("count(DayType.DayOff) = " + count);
            getCurrent().setDayOffCount(count);
            
            count = setDayOffSleepingDayAllowance(PaysheetComponentType.SleepingDayAllowance, DayType.SleepingDay);
            System.out.println("count(DayType.SleepingDay) = " + count);
            getCurrent().setSleepingDayCount(count);

            double noPayCount = getHumanResourceBean().fetchStaffLeave(getCurrent().getStaff(), LeaveType.No_Pay, getSalaryCycle().getSalaryFromDate(), getSalaryCycle().getSalaryToDate());
            System.out.println("noPayCount(LeaveType.No_Pay) = " + noPayCount);
            double basicValue = setNoPay_Basic(noPayCount);
            
            
            setNoPay_Allowance();
            getCurrent().setNoPayCount(noPayCount);
            setAdjustments();

            //Record Late No Pay Leave 
            //Not consider in any calcualtion is alredy with general NO Pay 
            //only for reporting purpose
            double noPayCountLate = getHumanResourceBean().fetchStaffLeaveSystem(getCurrent().getStaff(), LeaveType.No_Pay, getSalaryCycle().getSalaryFromDate(), getSalaryCycle().getSalaryToDate());
            getCurrent().setLateNoPayCount(noPayCountLate);
            getCurrent().setLateNoPayBasicValue(0 - roundOff(basicValue / finalVariables.getWorkingDaysPerMonth()) * noPayCountLate);

        }

    }

    private void fetchAndSetSalaryAdvance() {
        String sql = "select sc from StaffSalaryComponant sc "
                + " where sc.retired=false "
                + " and sc.salaryCycle=:sc "
                + " and sc.staff=:stf";
        HashMap hm = new HashMap();
        hm.put("sc", getSalaryCycle());
        hm.put("stf", getCurrent().getStaff());
        StaffSalaryComponant salaryComponant = staffSalaryComponantFacade.findFirstBySQL(sql, hm);

        if (salaryComponant != null) {
            getCurrent().getStaffSalaryComponants().add(salaryComponant);
        }
    }

    public void deleteAll() {
        if (items == null) {
            return;
        }

        int i = 0;
        for (StaffSalary s : items) {
            System.err.println("Del " + s.getStaff().getCodeInterger() + " " + (++i));
            deleteSalaryComponent(s);
        }
    }

    public void deleteSalaryComponent(StaffSalary staffSalary) {
        if (staffSalary.getId() == null) {
//            System.err.println("RETURE 1");
            return;
        }

        if (staffSalary.getStaff() != null) {

            staffSalary.setRetireComments("deleted");
            staffSalary.setRetired(true);
            staffSalary.setRetiredAt(new Date());
            staffSalary.setRetirer(getSessionController().getLoggedUser());

            staffSalaryFacade.edit(staffSalary);

            for (StaffSalaryComponant spc : getHumanResourceBean().fetchStaffSalaryComponent(staffSalary)) {
                spc.setRetireComments("deleted");
                spc.setRetired(true);
                spc.setRetiredAt(new Date());
                spc.setRetirer(getSessionController().getLoggedUser());

                getStaffSalaryComponantFacade().edit(spc);

            }

//            System.err.println("Return 2");
            updateStaffShiftRedo(staffSalary.getStaff(), staffSalary.getSalaryCycle().getSalaryFromDate(), staffSalary.getSalaryCycle().getSalaryToDate());

//            for (StaffShift ss : getHumanResourceBean().fetchStaffShifts(staffSalary)) {
////                ss.setConsideredForOt(Boolean.FALSE);
////                ss.setConsideredForSalary(Boolean.FALSE);
////                ss.setConsideredForExtraDuty(Boolean.FALSE);
////                ss.setLieuPaid(false);
//                
//                getStaffShiftFacade().edit(ss);
//            }
            getStaffSalaryFacade().edit(staffSalary);

        }

        UtilityController.addSuccessMessage("Record Succesfully Deleted");
    }

    public void calStaffLeaveFromLateIn(StaffShift stfCurrent, double fromTime, double toTime, Double shiftCount) {
        System.out.println("shiftCount = " + shiftCount);
        System.out.println("shiftCount.intValue() = " + shiftCount.intValue());
        System.out.println("fromTime = " + fromTime);
        System.out.println("toTime = " + toTime);
        System.out.println("stfCurrent.getLateInVarified() = " + stfCurrent.getLateInVarified());

        if (!(stfCurrent.getLateInVarified() >= fromTime
                && stfCurrent.getLateInVarified() <= toTime)) {
            return;
        }

//        System.err.println("Late In 10 " + stfCurrent);
        List<StaffShift> staffShiftEarlyIn = staffLeaveFromLateAndEarlyController.fetchStaffShiftLateIn(stfCurrent, fromTime, toTime, shiftCount.intValue());

        if (staffShiftEarlyIn == null) {
            return;
        }
        System.err.println("staffShiftEarlyIn.size()" + staffShiftEarlyIn.size());
        if (staffShiftEarlyIn.size() != 2) {
            return;
        }

        LinkedList<StaffShift> staffShiftLateInTenMinuteLinked = new LinkedList<>();

        for (StaffShift stf : staffShiftEarlyIn) {
            staffShiftLateInTenMinuteLinked.add(stf);
        }

        System.err.println("staffShiftEarlyIn.size()" + staffShiftEarlyIn.size());
        System.err.println("Late in Shift Reference Count " + staffShiftLateInTenMinuteLinked.size());
        System.err.println("shiftCount = " + shiftCount);
        if (staffShiftLateInTenMinuteLinked.size() >= shiftCount) {
            stfCurrent.setReferenceStaffShiftLateIn(stfCurrent);
            stfCurrent.setConsiderForLateIn(true);
            staffShiftFacade.edit(stfCurrent);
            for (int i = 0; i < shiftCount; i++) {

                StaffShift lateShift = staffShiftLateInTenMinuteLinked.pollFirst();
                System.err.println("Late In Shift ID " + lateShift.getId());
                lateShift.setReferenceStaffShiftLateIn(stfCurrent);
                lateShift.setConsiderForLateIn(true);
                staffShiftFacade.edit(lateShift);
//                staffShiftFacade.flush();
            }

            LeaveType leaveType = humanResourceBean.getLeaveType(stfCurrent.getStaff(), stfCurrent.getShiftDate());
            System.err.println("leaveType = " + leaveType);

            HrForm hr = staffLeaveFromLateAndEarlyController.saveLeaveForm(stfCurrent, leaveType, stfCurrent.getShiftDate(), stfCurrent.getShiftDate());

            staffLeaveFromLateAndEarlyController.saveStaffLeaves(stfCurrent, leaveType, hr);
            staffLeaveFromLateAndEarlyController.addLeaveDataToStaffShift(stfCurrent, leaveType, hr);
        }

//        System.err.println("Automatic Late In End " + stfCurrent.getStaff().getCodeInterger());
    }

    public List<StaffShift> fetchStaffShiftForResetLateAndEarly(StaffShift stfCurrent) {
        String sql = " Select s from  StaffShift s "
                + " where s.retired=false "
                + " and s.referenceStaffShiftEarlyOut=:stf ";
//                + " and s.considerForEarlyOut=true";
        HashMap hm = new HashMap();
        hm.put("stf", stfCurrent);
        List<StaffShift> listReturn = new ArrayList<>();
        List<StaffShift> list = staffShiftFacade.findBySQL(sql, hm);

        if (list != null) {
            listReturn.addAll(list);
            System.out.println("list.size(referenceStaffShiftEarlyOut) = " + list.size());
        }

        sql = " Select s from  StaffShift s "
                + " where s.retired=false "
                + " and s.referenceStaffShiftLateIn=:stf ";
//                + " and s.considerForLateIn=true";
        hm = new HashMap();
        hm.put("stf", stfCurrent);

        list = staffShiftFacade.findBySQL(sql, hm);

        if (list != null) {
            listReturn.addAll(list);
            System.out.println("list.size(referenceStaffShiftLateIn) = " + list.size());
        }
        System.out.println("listReturn.size = " + listReturn.size());
        return listReturn;
    }

    @Inject
    StaffLeaveFromLateAndEarlyController staffLeaveFromLateAndEarlyController;

    public void calStaffLeaveFromEarlyOut(StaffShift stfCurrent, double fromTime, double toTime, Double shiftCount) {
        if (!(stfCurrent.getEarlyOutVarified() >= fromTime
                && stfCurrent.getEarlyOutVarified() <= toTime)) {
            return;
        }

//        System.err.println("Early Out 10 " + stfCurrent);
        List<StaffShift> staffShiftEarlyOut = staffLeaveFromLateAndEarlyController.fetchStaffShiftEarlyOut(stfCurrent, fromTime, toTime, shiftCount.intValue());

        if (staffShiftEarlyOut == null) {
            return;
        }
        System.err.println("staffShiftEarlyOut.size()" + staffShiftEarlyOut.size());
        if (staffShiftEarlyOut.size() != 2) {
            return;
        }

        LinkedList<StaffShift> staffShiftEarlyOutThirtyMinuteLinked = new LinkedList<>();

        for (StaffShift stf : staffShiftEarlyOut) {
            staffShiftEarlyOutThirtyMinuteLinked.add(stf);
        }

        System.err.println("staffShiftEarlyOut.size()" + staffShiftEarlyOut.size());
        System.err.println("Late in Shift Reference Count " + staffShiftEarlyOutThirtyMinuteLinked.size());
        System.err.println("shiftCount = " + shiftCount);
        if (staffShiftEarlyOutThirtyMinuteLinked.size() >= shiftCount) {
            stfCurrent.setReferenceStaffShiftEarlyOut(stfCurrent);
            stfCurrent.setConsiderForEarlyOut(true);
            staffShiftFacade.edit(stfCurrent);
            for (int i = 0; i < shiftCount; i++) {
                StaffShift earlyOut = staffShiftEarlyOutThirtyMinuteLinked.pollFirst();
                System.err.println("Early Out  Shift ID " + earlyOut.getId());
                earlyOut.setReferenceStaffShiftEarlyOut(stfCurrent);
                earlyOut.setConsiderForEarlyOut(true);
                staffShiftFacade.edit(earlyOut);
//                staffShiftFacade.flush();
            }

            LeaveType leaveType = humanResourceBean.getLeaveType(stfCurrent.getStaff(), stfCurrent.getShiftDate());
            System.err.println("leaveType = " + leaveType);
            HrForm hr = staffLeaveFromLateAndEarlyController.saveLeaveForm(stfCurrent, leaveType, stfCurrent.getShiftDate(), stfCurrent.getShiftDate());
            staffLeaveFromLateAndEarlyController.saveStaffLeaves(stfCurrent, leaveType, hr);
            staffLeaveFromLateAndEarlyController.addLeaveDataToStaffShift(stfCurrent, leaveType, hr);
        }

//        System.err.println("Automatic Early out END " + stfCurrent.getStaff().getCodeInterger());
    }

    public void calStaffLeaveFromEarlyOut(StaffShift stfCurrent, double fromTime) {
//        System.err.println("Early Out Half " + stfCurrent);
//        if (stfCurrent.getEarlyOutLogged() >= fromTime) {
//        System.err.println("Early Out  Shift ID " + stfCurrent.getId());
        stfCurrent.setReferenceStaffShiftEarlyOut(stfCurrent);
        stfCurrent.setConsiderForEarlyOut(true);
        staffShiftFacade.edit(stfCurrent);
        LeaveType leaveType = humanResourceBean.getLeaveType(stfCurrent.getStaff(), stfCurrent.getShiftDate());
        HrForm hr = staffLeaveFromLateAndEarlyController.saveLeaveForm(stfCurrent, leaveType, stfCurrent.getShiftDate(), stfCurrent.getShiftDate());
        staffLeaveFromLateAndEarlyController.saveStaffLeaves(stfCurrent, leaveType, hr);
        staffLeaveFromLateAndEarlyController.addLeaveDataToStaffShift(stfCurrent, leaveType, hr);

//        }
//        System.err.println("Automatic Early out END " + stfCurrent.getStaff().getCodeInterger());
    }

    @EJB
    LeaveFormFacade leaveFormFacade;
    @EJB
    StaffLeaveFacade staffLeaveFacade;

    public void calStaffAutoLeaveReset(StaffShift stfCurrent) {
        System.err.println("Resetting Shift " + stfCurrent);
        LeaveFormSystem hr = staffLeaveFromLateAndEarlyController.fetchLeaveForm(stfCurrent, stfCurrent.getShiftDate(), stfCurrent.getShiftDate());
        if (hr != null) {

            hr.setRetired(true);
            hr.setRetiredAt(new Date());
            hr.setRetirer(sessionController.getLoggedUser());
            leaveFormFacade.edit(hr);

            StaffLeaveSystem staffLeaveSystem = staffLeaveFromLateAndEarlyController.fetchStaffLeaves(stfCurrent, hr);

            if (staffLeaveSystem != null) {
                staffLeaveSystem.setRetired(true);
                staffLeaveSystem.setRetiredAt(new Date());
                staffLeaveSystem.setRetirer(sessionController.getLoggedUser());
                staffLeaveFacade.edit(staffLeaveSystem);
                stfCurrent.resetLeaveData(staffLeaveSystem.getLeaveType());
            }

        } else {

            StaffLeaveSystem staffLeaveSystem = staffLeaveFromLateAndEarlyController.fetchStaffLeaves(stfCurrent);

            if (staffLeaveSystem != null) {
                staffLeaveSystem.setRetired(true);
                staffLeaveSystem.setRetiredAt(new Date());
                staffLeaveSystem.setRetirer(sessionController.getLoggedUser());
                staffLeaveFacade.edit(staffLeaveSystem);
                stfCurrent.resetLeaveData(staffLeaveSystem.getLeaveType());

                stfCurrent.resetLeaveData();
            }

        }

        stfCurrent.calLeaveTime();
        stfCurrent.setLeaveType(null);
        stfCurrent.setAutoLeave(false);
        stfCurrent.setConsiderForLateIn(false);
        stfCurrent.setConsiderForEarlyOut(false);
        staffShiftFacade.edit(stfCurrent);

        List<StaffShift> list = fetchStaffShiftForResetLateAndEarly(stfCurrent);
        System.out.println("list(fetchStaffShiftForResetLateAndEarly) = " + list.size());
        if (list == null) {
            return;
        }
        System.out.println("stfCurrent = " + stfCurrent);
        for (StaffShift s : list) {
            s.setConsiderForEarlyOut(false);
            s.setConsiderForLateIn(false);
            staffShiftFacade.edit(s);
            System.out.println("s = " + s);
        }

        System.err.println("Automatic Late In END " + stfCurrent.getStaff().getCodeInterger());
    }

    public void calStaffLeaveFromLateIn(StaffShift stfCurrent, double fromTime) {
//        System.err.println("Late In Half " + stfCurrent);
        stfCurrent.setReferenceStaffShiftLateIn(stfCurrent);
//        System.err.println("Late In  Shift ID " + stfCurrent.getId());
        stfCurrent.setConsiderForLateIn(true);
        staffShiftFacade.edit(stfCurrent);

        LeaveType leaveType = humanResourceBean.getLeaveType(stfCurrent.getStaff(), stfCurrent.getShiftDate());
        HrForm hr = staffLeaveFromLateAndEarlyController.saveLeaveForm(stfCurrent, leaveType, stfCurrent.getShiftDate(), stfCurrent.getShiftDate());
        staffLeaveFromLateAndEarlyController.saveStaffLeaves(stfCurrent, leaveType, hr);
        staffLeaveFromLateAndEarlyController.addLeaveDataToStaffShift(stfCurrent, leaveType, hr);

//        System.err.println("Automatic Late In END " + stfCurrent.getStaff().getCodeInterger());
    }

    private void resetAutoLeave(Staff staff, Date fromDate, Date toDate) {
        List<StaffShift> staffShifts = humanResourceBean.fetchStaffShiftForAutoLeaveReset(staff, fromDate, toDate);
        System.out.println("staffShifts = " + staffShifts);
        if (staffShifts == null) {
            return;
        }

//        System.err.println("Rest Count " + staffShifts.size());
        for (StaffShift ss : staffShifts) {
            // Calculate Late in for 1 1\2 h 

            System.err.println("******Automatic Late In Leave " + ss.getStaff().getCodeInterger());
            calStaffAutoLeaveReset(ss);

        }
    }

    private void generateAutoLeave(Staff staff, Date fromDate, Date toDate) {
        List<StaffShift> staffShifts = humanResourceBean.fetchStaffShiftForAutoLeave(staff, fromDate, toDate);

        if (staffShifts == null) {
            return;
        }
        System.err.println("StaffShifts for Auto Leave Half " + staffShifts.size());
        for (StaffShift ss : staffShifts) {
            //Automatic No Pay Diduction
            double fromMinute = 90 * 60;

            // Calculate Late in for 1 1\2 h 
            if (ss.getStaff().isAllowedLateInLeave()
                    && !ss.isConsiderForLateIn()) {
                System.err.println("******Automatic Late In Leave " + ss.getStaff().getCodeInterger());

                if (ss.getLateInVarified() > fromMinute) {
                    calStaffLeaveFromLateIn(ss, 90 * 60);
                }
            }

            if (ss.getStaff().isAllowedEarlyOutLeave()
                    && !ss.isConsiderForEarlyOut()) {
                System.err.println("******Automatic Early Out Leave " + ss.getStaff().getCodeInterger());
//                        calStaffLeaveFromEarlyOut(ss, 30 * 60, 90 * 60, 3);
                if (ss.getEarlyOutVarified() > fromMinute) {
                    calStaffLeaveFromEarlyOut(ss, 90 * 60);
                }
            }

        }

        staffShifts = humanResourceBean.fetchStaffShiftForAutoLeave(staff, fromDate, toDate);
        System.err.println("StaffShifts for Auto Leave System " + staffShifts.size());
        for (StaffShift ss : staffShifts) {
            System.out.println("ss.getLateInVarified() = " + ss.getLateInVarified());
            System.out.println("ss.getStaff().isAllowedLateInLeave() = " + ss.getStaff().isAllowedLateInLeave());
            System.out.println("ss.isConsiderForLateIn() = " + ss.isConsiderForLateIn());
            if (ss.getStaff().isAllowedLateInLeave()
                    && !ss.isConsiderForLateIn()
                    && ss.getLateInVarified() > 0) {
                System.err.println("******Automatic Late In Leave (Out) half " + ss.getStaff().getCodeInterger());
                calStaffLeaveFromLateIn(ss, 10 * 60, 90 * 60, 2.0);

            }

            if (ss.getStaff().isAllowedEarlyOutLeave()
                    && !ss.isConsiderForEarlyOut()
                    && ss.getEarlyInVarified() > 0) {
                System.err.println("******Automatic Early Out Leave (Out) half " + ss.getStaff().getCodeInterger());
                calStaffLeaveFromEarlyOut(ss, 30 * 60, 90 * 60, 2.0);

            }

        }
    }

    public void generate() {
        if (getStaffController().getSelectedList() == null) {
            return;
        }

        if (dateCheck()) {
            return;
        }

        items = null;
        int i = 0;
        for (Staff s : getStaffController().getSelectedList()) {
            System.err.println("Staff Code " + s.getCodeInterger() + " : " + (++i) + " start at " + new Date());
            setCurrent(getHumanResourceBean().getStaffSalary(s, getSalaryCycle()));
            if (getCurrent().getId() == null) {
                resetAutoLeave(s, getSalaryCycle().getSalaryFromDate(), getSalaryCycle().getSalaryToDate());
                generateAutoLeave(s, getSalaryCycle().getSalaryFromDate(), getSalaryCycle().getSalaryToDate());
                fetchAndSetBankData();
                addSalaryComponent();
//                save();
            } else {
                // Allready in database

            }

            getCurrent().calculateComponentTotal();
            getCurrent().calcualteEpfAndEtf();
            getItems().add(current);
            current = null;

        }

        //   createStaffSalaryTable();
    }

    private void fetchAndSetBankData() {
        String sql = "Select s from StaffPaysheetComponent s"
                + " where s.retired=false "
                + " and s.paysheetComponent.componentType= :tp"
                + " and s.completed=false "
                + " and s.staff=:stf "
                + " and s.bankBranch is not null"
                + " and s.accountNo is not null ";
        HashMap hm = new HashMap();
        hm.put("stf", getCurrent().getStaff());
        hm.put("tp", PaysheetComponentType.LoanNetSalary);

        StaffPaysheetComponent staffPaysheetComponent = getStaffPaysheetComponentFacade().findFirstBySQL(sql, hm, TemporalType.DATE);
        if (staffPaysheetComponent != null) {
            getCurrent().setBankBranch(staffPaysheetComponent.getBankBranch());
            getCurrent().setAccountNo(staffPaysheetComponent.getAccountNo());

        } else {
            getCurrent().setBankBranch(getCurrent().getStaff().getBankBranch());
            getCurrent().setAccountNo(getCurrent().getStaff().getAccountNo());
        }

        getCurrent().setEpfBankBranch(getCurrent().getStaff().getEpfBankBranch());
        getCurrent().setEpfBankAccount(getCurrent().getStaff().getEpfAccountNo());
    }

    ReportKeyWord reportKeyWord;

    public SalaryCycleController getSalaryCycleController() {
        return salaryCycleController;
    }

    public void setSalaryCycleController(SalaryCycleController salaryCycleController) {
        this.salaryCycleController = salaryCycleController;
    }

    public StaffLeaveFromLateAndEarlyController getStaffLeaveFromLateAndEarlyController() {
        return staffLeaveFromLateAndEarlyController;
    }

    public void setStaffLeaveFromLateAndEarlyController(StaffLeaveFromLateAndEarlyController staffLeaveFromLateAndEarlyController) {
        this.staffLeaveFromLateAndEarlyController = staffLeaveFromLateAndEarlyController;
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

    public void fetchStaffSalay() {
        String sql;
        HashMap hm = new HashMap();

        sql = "SELECT ss FROM StaffSalary ss "
                + " WHERE ss.retired=false "
                + " and ss.salaryCycle=:sc ";

        hm.put("sc", getSalaryCycle());

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
            sql += " and ss.staff.roster=:rs ";
            hm.put("rs", getReportKeyWord().getRoster());
        }

        sql += " order by ss.staff.codeInterger";

        items = getStaffSalaryFacade().findBySQL(sql, hm, TemporalType.TIMESTAMP);

    }

    public void saveSalary() {
        if (getStaffController().getSelectedList() == null) {
            return;
        }

        if (dateCheck()) {
            return;
        }

        if (items == null) {
            return;
        }

        int i = 0;

        for (StaffSalary stf : items) {
            System.err.println("Saving " + stf.getStaff().getCodeInterger() + " " + (++i));
            if (stf.getId() != null) {
                continue;
            }
            current = stf;
            save();
            updateStaffShift(stf.getStaff(), getSalaryCycle().getWorkedFromDate(), getSalaryCycle().getWorkedToDate());
            current = null;
        }

        //   createStaffSalaryTable();
    }

    private void updateStaffShift(Staff staff, Date fromDate, Date toDate) {
        List<StaffShift> staffShiftsLiePaymentAllowed = humanResourceBean.fetchStaffShiftLiePaymentAllowed(fromDate, toDate, staff);

        for (StaffShift ss : staffShiftsLiePaymentAllowed) {
            ss.setLieuPaid(true);
            staffShiftFacade.edit(ss);
        }

    }

    private void updateStaffShiftRedo(Staff staff, Date fromDate, Date toDate) {
        List<StaffShift> staffShiftsLiePaymentAllowed = humanResourceBean.fetchStaffShiftLiePaymentAllowed(fromDate, toDate, staff);

        for (StaffShift ss : staffShiftsLiePaymentAllowed) {
            ss.setLieuPaid(false);
            staffShiftFacade.edit(ss);
        }

    }

    public void createStaffSalaryTable() {
        if (getSalaryCycle() == null) {
            return;
        }
        String sql = "Select s From StaffSalary s"
                + " where s.retired=false "
                + " and s.salaryCycle.salaryFromDate>=:fd "
                + " and s.salaryCycle.salaryToDate<=:td";

        HashMap hm = new HashMap<>();
        hm.put("fd", getSalaryCycle().getSalaryFromDate());
        hm.put("td", getSalaryCycle().getSalaryToDate());

        items = getStaffSalaryFacade().findBySQL(sql, hm, TemporalType.DATE);
    }

    public StaffSalary fetchStaffSalaryTable(Staff stf, Date fromDate, Date toDate) {
        String sql = "Select s From StaffSalary s"
                + " where s.retired=false"
                + " and s.staff=:stf "
                + " and s.salaryCycle.salaryFromDate>=:fd "
                + " and s.salaryCycle.salaryToDate<=:td";

        HashMap hm = new HashMap<>();
        hm.put("stf", stf);
        hm.put("fd", fromDate);
        hm.put("td", toDate);

        return getStaffSalaryFacade().findFirstBySQL(sql, hm, TemporalType.DATE);
    }

    public List<StaffSalary> getItems() {
        if (items == null) {
            items = new ArrayList<>();
        }
        return items;
    }

    public StaffPaysheetComponentFacade getStaffPaysheetComponentFacade() {
        return staffPaysheetComponentFacade;
    }

    public void setStaffPaysheetComponentFacade(StaffPaysheetComponentFacade staffPaysheetComponentFacade) {
        this.staffPaysheetComponentFacade = staffPaysheetComponentFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public HrmVariablesController getHrmVariablesController() {
        return hrmVariablesController;
    }

    public void setHrmVariablesController(HrmVariablesController hrmVariablesController) {
        this.hrmVariablesController = hrmVariablesController;
    }

    public StaffSalaryComponantFacade getStaffSalaryComponantFacade() {
        return staffSalaryComponantFacade;
    }

    public void setStaffSalaryComponantFacade(StaffSalaryComponantFacade staffSalaryComponantFacade) {
        this.staffSalaryComponantFacade = staffSalaryComponantFacade;
    }

    public StaffEmploymentFacade getStaffEmploymentFacade() {
        return staffEmploymentFacade;
    }

    public void setStaffEmploymentFacade(StaffEmploymentFacade staffEmploymentFacade) {
        this.staffEmploymentFacade = staffEmploymentFacade;
    }

    public StaffFacade getStaffFacade() {
        return staffFacade;
    }

    public void setStaffFacade(StaffFacade staffFacade) {
        this.staffFacade = staffFacade;
    }

    public StaffController getStaffController() {
        return staffController;
    }

    public void setStaffController(StaffController staffController) {
        this.staffController = staffController;
    }

    public HumanResourceBean getHumanResourceBean() {
        return humanResourceBean;
    }

    public void setHumanResourceBean(HumanResourceBean humanResourceBean) {
        this.humanResourceBean = humanResourceBean;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public void setItems(List<StaffSalary> items) {
        this.items = items;
    }

    @FacesConverter(forClass = StaffSalary.class)
    public static class StaffSalaryConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            StaffSalaryController controller = (StaffSalaryController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "staffSalaryController");
            return controller.getStaffSalaryFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            key = Long.valueOf(value);
            return key;
        }

        String getStringKey(java.lang.Long value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof StaffSalary) {
                StaffSalary o = (StaffSalary) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + StaffSalaryController.class.getName());
            }
        }
    }

    @FacesConverter("staffSalaryCon")
    public static class StaffSalaryControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            StaffSalaryController controller = (StaffSalaryController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "staffSalaryController");
            return controller.getStaffSalaryFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            key = Long.valueOf(value);
            return key;
        }

        String getStringKey(java.lang.Long value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof StaffSalary) {
                StaffSalary o = (StaffSalary) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + StaffSalaryController.class.getName());
            }
        }
    }
}
