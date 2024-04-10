/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.hr;

import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.hr.DayType;
import com.divudi.data.hr.LeaveType;
import com.divudi.data.hr.PaysheetComponentType;
import com.divudi.data.hr.ReportKeyWord;

import com.divudi.ejb.FinalVariables;
import com.divudi.ejb.HumanResourceBean;
import com.divudi.entity.Staff;
import com.divudi.entity.hr.SalaryCycle;
import com.divudi.entity.hr.StaffPaysheetComponent;
import com.divudi.entity.hr.StaffSalary;
import com.divudi.entity.hr.StaffSalaryComponant;
import com.divudi.entity.hr.StaffShift;
import com.divudi.facade.StaffEmploymentFacade;
import com.divudi.facade.StaffFacade;
import com.divudi.facade.StaffPaysheetComponentFacade;
import com.divudi.facade.StaffSalaryComponantFacade;
import com.divudi.facade.StaffSalaryFacade;
import com.divudi.facade.StaffShiftFacade;
import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
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
public class StaffSalaryAdvanceController implements Serializable {

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

    
    private CommonFunctions commonFunctions;
    /////////////
    @Inject
    CommonController commonController;
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

        if (getCurrent().getTransAdvanceSalary() != 0) {
            StaffSalaryComponant ss = fetchSalaryAdvance(getCurrent().getStaff());

            if (ss == null) {
                ss = createStaffSalaryComponant(PaysheetComponentType.Salary_Advance_Deduction);
            }

            ss.setStaff(getCurrent().getStaff());
            ss.setComponantValue(getCurrent().getTransAdvanceSalary());
            getHumanResourceBean().setEpf(ss, getHrmVariablesController().getCurrent().getEpfRate(), getHrmVariablesController().getCurrent().getEpfCompanyRate());
            getHumanResourceBean().setEtf(ss, getHrmVariablesController().getCurrent().getEtfRate(), getHrmVariablesController().getCurrent().getEtfCompanyRate());

            if (ss.getId() == null) {
                staffSalaryComponantFacade.create(ss);
            } else {
                staffSalaryComponantFacade.edit(ss);
            }
        }

    }

//    private void updateComponent(List<StaffSalaryComponant> list) {
//        for (StaffSalaryComponant ssc : list) {
////            ssc.setStaffSalary(getCurrent());
//          
//            
//            if (ssc.getId() == null) {
//                getStaffSalaryComponantFacade().create(ssc);
//            } else {
//                getStaffSalaryComponantFacade().edit(ssc);
//            }
//        }
//
//    }
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
        ////////// // System.out.println("Runn");
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

    public StaffSalaryAdvanceController() {
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

//    public void onEditListener(StaffSalary staffSalary) {
//        current = staffSalary;
//
//        if (current == null) {
//            return;
//        }
//
//        List<PaysheetComponent> paysheetComponentsAddition = salaryCycleController.fetchPaysheetComponents(PaysheetComponentType.addition.getUserDefinedComponentsAddidtionsWithPerformance());
//        List<PaysheetComponent> paysheetComponentsSubstraction = salaryCycleController.fetchPaysheetComponents(PaysheetComponentType.subtraction.getUserDefinedComponentsDeductionsWithSalaryAdvance());
//
//        for (PaysheetComponent psc : paysheetComponentsAddition) {
//            StaffSalaryComponant c = salaryCycleController.fetchSalaryComponents(getCurrent(), psc, false);
//            getCurrent().getTransStaffSalaryComponantsAddition().add(c);
//
//        }
//        for (PaysheetComponent psc : paysheetComponentsSubstraction) {
//            StaffSalaryComponant c = salaryCycleController.fetchSalaryComponents(getCurrent(), psc, false);
//            getCurrent().getTransStaffSalaryComponantsSubtraction().add(c);
//
//        }
//
//    }
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

//    private boolean checkDateRange(Date date) {
//        if (date == null) {
//            return false;
//        }
//
//        if ((getSalaryCycle().getSalaryFromDate().getTime() < date.getTime()
//                && getSalaryCycle().getSalaryToDate().getTime() > date.getTime())) {
//
//            return true;
//        }
//
//        return false;
//
//    }
    private double calValue(double value) {

        if (value == 0) {
            return 0;
        }

        double workedDays = humanResourceBean.calculateWorkedDaysForSalary(salaryCycle.getSalaryAdvanceFromDate(), salaryCycle.getSalaryAdvanceToDate(), getCurrent().getStaff());
        if (workedDays >= finalVariables.getWorkingDaysPerMonth()) {
            return value;
        } else {
            double dbl = (value / finalVariables.getWorkingDaysPerMonth());
            dbl = dbl * workedDays;

            return dbl;
        }

    }

    private double calValue(double value, double percentage) {

        if (value == 0 || percentage == 0) {
            return 0;
        }

        double workedDays = humanResourceBean.calculateWorkedDaysForSalary(salaryCycle.getSalaryAdvanceFromDate(), salaryCycle.getSalaryAdvanceToDate(), getCurrent().getStaff());
        if (workedDays >= finalVariables.getWorkingDaysPerMonth()) {
            return value * (percentage / 100);
        } else {
            double dbl = (value / finalVariables.getWorkingDaysPerMonth());
            dbl = dbl * workedDays * (percentage / 100);

            return dbl;
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
        double workedWithinTimeFrameVarified = getHumanResourceBean().calculateWorkTimeForOverTime(getSalaryCycle().getWorkedFromDate(), getSalaryCycle().getWorkedToDate(), getCurrent().getStaff());
        workedWithinTimeFrameVarified += getHumanResourceBean().calculateLeaveTimeForOverTime(getSalaryCycle().getWorkedFromDate(), getSalaryCycle().getWorkedToDate(), getCurrent().getStaff());
        Long dateCount = commonFunctions.getDayCount(getSalaryCycle().getWorkedFromDate(), getSalaryCycle().getWorkedToDate());
        Long numOfWeeks = dateCount / 7;
        Double overTimeSec = humanResourceBean.getOverTimeFromRoster(getCurrent().getStaff().getWorkingTimeForOverTimePerWeek(), numOfWeeks, workedWithinTimeFrameVarified);
        return (overTimeSec.longValue() / 60L);
    }

    public double getOverTimeValuePerMinute() {

        return roundOff(humanResourceBean.getOverTimeValue(getCurrent().getStaff(), getSalaryCycle().getSalaryToDate()) / (200 * 60));

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

    public void setOT() {

        StaffSalaryComponant ss = createStaffSalaryComponant(PaysheetComponentType.OT);

        if (ss.getStaffPaysheetComponent() != null) {
            Long overTimeMinute = calculateOverTimeMinute();
            double otValuePerMinute = getOverTimeValuePerMinute();
            ss.setComponantValue(overTimeMinute * otValuePerMinute * finalVariables.getOverTimeMultiply());
            getCurrent().setOverTimeMinute(overTimeMinute);
            getCurrent().setBasicRatePerMinute(otValuePerMinute);
            getCurrent().setOverTimeRatePerMinute(otValuePerMinute);
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
        ss.setComponantValue(humanResourceBean.calculateExtraWorkTimeValue(getSalaryCycle().getWorkedFromDate(), getSalaryCycle().getWorkedToDate(), getCurrent().getStaff(), dayType));
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
            count = getHumanResourceBean().calculateHolidayWork(
                    getSalaryCycle().getSalaryAdvanceFromDate(), getSalaryCycle().getSalaryAdvanceToDate(), getCurrent().getStaff(), dayType);

            double salaryValue = 0;

            if (getCurrent().getStaffSalaryComponants() == null) {
                return count;
            }

            for (StaffSalaryComponant staffSalaryComponant : getCurrent().getStaffSalaryComponants()) {
                if (staffSalaryComponant.getStaffPaysheetComponent() == null
                        || staffSalaryComponant.getStaffPaysheetComponent().getPaysheetComponent() == null) {
                    continue;
                }

                if (staffSalaryComponant.getStaffPaysheetComponent().getPaysheetComponent().isIncludeForAllowance()) {
                    salaryValue += calValue(staffSalaryComponant.getComponantValue());
                }
            }

            if (salaryValue != 0) {
                double salaryPerDay = roundOff(salaryValue / finalVariables.getWorkingDaysPerMonth());
                double value = getHumanResourceBean().calculateHolidayWork(
                        getSalaryCycle().getSalaryAdvanceFromDate(), getSalaryCycle().getSalaryAdvanceToDate(), getCurrent().getStaff(), dayType, salaryPerDay);
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
            count = getHumanResourceBean().calculateOffDays(
                    getSalaryCycle().getSalaryAdvanceFromDate(), getSalaryCycle().getSalaryAdvanceToDate(), getCurrent().getStaff(), dayType);
            double salaryValue = 0;
            for (StaffSalaryComponant staffSalaryComponant : getCurrent().getStaffSalaryComponants()) {
                if (staffSalaryComponant.getStaffPaysheetComponent() == null
                        || staffSalaryComponant.getStaffPaysheetComponent().getPaysheetComponent() == null) {
                    continue;
                }

                if (staffSalaryComponant.getStaffPaysheetComponent().getPaysheetComponent().isIncludeForAllowance()) {
                    salaryValue += calValue(staffSalaryComponant.getComponantValue());
                }
            }

            if (salaryValue != 0) {
                double salaryPerDay = roundOff(salaryValue / finalVariables.getWorkingDaysPerMonth());
                double value = getHumanResourceBean().calculateOffDays(
                        getSalaryCycle().getSalaryAdvanceFromDate(), getSalaryCycle().getSalaryAdvanceToDate(), getCurrent().getStaff(), dayType, salaryPerDay);
                //Need Calculation Sum
                ss.setComponantValue(value);
            }
        } else {
            return 0.0;
        }

        getHumanResourceBean().setEpf(ss, getHrmVariablesController().getCurrent().getEpfRate(), getHrmVariablesController().getCurrent().getEpfCompanyRate());
        getHumanResourceBean().setEtf(ss, getHrmVariablesController().getCurrent().getEtfRate(), getHrmVariablesController().getCurrent().getEtfCompanyRate());

        getCurrent().getStaffSalaryComponants().add(ss);
        return count;
    }

    private double setNoPay_Basic(double noPayCount) {
        double noPayValue = 0;
        double salaryValue = 0;
        StaffSalaryComponant ss = createStaffSalaryComponant(PaysheetComponentType.No_Pay_Deduction_Basic);
        if (ss.getStaffPaysheetComponent() != null) {
            noPayCount = getHumanResourceBean().fetchStaffLeave(getCurrent().getStaff(), LeaveType.No_Pay,
                    getSalaryCycle().getSalaryAdvanceFromDate(), getSalaryCycle().getSalaryAdvanceToDate());
            salaryValue = 0;

            if (getCurrent().getStaffSalaryComponants() == null) {
                return 0;
            }

            for (StaffSalaryComponant staffSalaryComponant : getCurrent().getStaffSalaryComponants()) {
                if (staffSalaryComponant.getStaffPaysheetComponent() == null
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
            noPayCount = getHumanResourceBean().fetchStaffLeaveAddedLeave(getCurrent().getStaff(), LeaveType.No_Pay,
                    getSalaryCycle().getSalaryAdvanceFromDate(), getSalaryCycle().getSalaryAdvanceToDate());
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
            JsfUtil.addErrorMessage("Please Select Salary Cycle");
            return true;
        }

        if (getSalaryCycle().getSalaryAdvanceFromDate() == null || getSalaryCycle().getSalaryAdvanceToDate() == null) {
            JsfUtil.addErrorMessage("Please Select Salary Date");
            return true;
        }

        if (getSalaryCycle().getWorkedFromDate() == null || getSalaryCycle().getWorkedToDate() == null) {
            JsfUtil.addErrorMessage("Please Select Over time Date");
            return true;
        }

//        if (getHumanResourceBean().checkExistingSalary(getSalaryCycle().getSalaryFromDate(), getSalaryToDate(), getCurrent().getStaff())) {
//            JsfUtil.addErrorMessage("There is Already defined Salary for this salary cycle please edit");
//            return true;
//        }
        return false;

    }

    public void addSalaryComponent() {

        if (getCurrent().getStaff() != null) {

            setBasic();
            setPerformaceAllovance();

            List<StaffPaysheetComponent> listAdd = getHumanResourceBean().fetchStaffPaysheetComponent(getCurrent().getStaff(), getSalaryCycle().getSalaryToDate(), PaysheetComponentType.addition.getUserDefinedComponentsAddidtions());

            if (listAdd != null) {
                for (StaffPaysheetComponent spc : listAdd) {
                    StaffSalaryComponant ss = new StaffSalaryComponant();
                    ss.setCreatedAt(new Date());
                    ss.setStaff(getCurrent().getStaff());
                    ss.setSalaryCycle(salaryCycle);
                    ss.setCreater(getSessionController().getLoggedUser());
                    ss.setStaffPaysheetComponent(spc);
                    ss.setComponantValue(calValue(spc.getStaffPaySheetComponentValue()));
                    getHumanResourceBean().setEpf(ss, getHrmVariablesController().getCurrent().getEpfRate(), getHrmVariablesController().getCurrent().getEpfCompanyRate());
                    getHumanResourceBean().setEtf(ss, getHrmVariablesController().getCurrent().getEtfRate(), getHrmVariablesController().getCurrent().getEtfCompanyRate());
                    getCurrent().getStaffSalaryComponants().add(ss);
                }
            }

            List<StaffPaysheetComponent> listSub = getHumanResourceBean().fetchStaffPaysheetComponent(getCurrent().getStaff(), getSalaryCycle().getSalaryToDate(), PaysheetComponentType.subtraction.getUserDefinedComponentsDeductions());

            if (listSub != null) {
                for (StaffPaysheetComponent spc : listSub) {
                    if ((spc.getPaysheetComponent().getComponentType() == PaysheetComponentType.LoanInstallemant
                            && spc.isCompleted())
                            || spc.getPaysheetComponent().getComponentType() == PaysheetComponentType.LoanNetSalary
                            || spc.getPaysheetComponent().getComponentType() == PaysheetComponentType.Salary_Advance_Deduction) {
                        continue;
                    }

                    getCurrent().calculateComponentTotal();
                    getCurrent().calcualteEpfAndEtf();

//                    double salaryValueForDiduction = getCurrent().getTransGrossSalary() + getCurrent().getTransTotalAllowance() + getCurrent().getTransTotalDeduction();
//                    if ((salaryValueForDiduction - spc.getStaffPaySheetComponentValue()) > 0) {
//                        continue;
//                    }
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

            getCurrent().setTransAdvanceSalary(fetchAndSetSalaryAdvance());

            setOT();

            //Set Extra Duty Value
            double extraTimeMinute = setExtraDuty(PaysheetComponentType.ExtraDutyNormal, DayType.Normal);
            getCurrent().setExtraDutyNormalMinute(extraTimeMinute);
            extraTimeMinute = setExtraDuty(PaysheetComponentType.ExtraDutyMerchantile, DayType.MurchantileHoliday);
            getCurrent().setExtraDutyMerchantileMinute(extraTimeMinute);
            extraTimeMinute = setExtraDuty(PaysheetComponentType.ExtraDutyPoya, DayType.Poya);
            getCurrent().setExtraDutyPoyaMinute(extraTimeMinute);
            extraTimeMinute = setExtraDuty(PaysheetComponentType.ExtraDutyDayOff, DayType.DayOff);
            getCurrent().setExtraDutyDayOffMinute(extraTimeMinute);
            extraTimeMinute = setExtraDuty(PaysheetComponentType.ExtraDutySleepingDay, DayType.SleepingDay);
            getCurrent().setExtraDutySleepingDayMinute(extraTimeMinute);

            //Set Holiday Allowance
            Double count = setHoliDayAllowance(PaysheetComponentType.MerchantileAllowance, DayType.MurchantileHoliday);
            getCurrent().setMerchantileCount(count.doubleValue());
            count = setHoliDayAllowance(PaysheetComponentType.PoyaAllowance, DayType.Poya);
            getCurrent().setPoyaCount(count.doubleValue());
            count = setDayOffSleepingDayAllowance(PaysheetComponentType.DayOffAllowance, DayType.DayOff);
            getCurrent().setDayOffCount(count.doubleValue());
            count = setDayOffSleepingDayAllowance(PaysheetComponentType.SleepingDayAllowance, DayType.SleepingDay);
            getCurrent().setSleepingDayCount(count.doubleValue());

            double noPayCount = getHumanResourceBean().fetchStaffLeave(getCurrent().getStaff(), LeaveType.No_Pay,
                    getSalaryCycle().getSalaryAdvanceFromDate(), getSalaryCycle().getSalaryAdvanceToDate());
            double basicValue = setNoPay_Basic(noPayCount);
            setNoPay_Allowance();
            getCurrent().setNoPayCount(noPayCount);
            setAdjustments();

            //Record Late No Pay Leave 
            //Not consider in any calcualtion is alredy with general NO Pay 
            //only for reporting purpose
            double noPayCountLate = getHumanResourceBean().fetchStaffLeaveSystem(getCurrent().getStaff(), LeaveType.No_Pay,
                    getSalaryCycle().getSalaryAdvanceFromDate(), getSalaryCycle().getSalaryAdvanceToDate());
            getCurrent().setLateNoPayCount(noPayCountLate);
            getCurrent().setLateNoPayBasicValue(0 - roundOff(basicValue / finalVariables.getWorkingDaysPerMonth()) * noPayCountLate);

        }

    }

    private double fetchAndSetSalaryAdvance() {
        String sql = "select sc from StaffSalaryComponant sc "
                + " where sc.retired=false "
                + " and sc.salaryCycle=:sc "
                + " and sc.staff=:stf";
        HashMap hm = new HashMap();
        hm.put("sc", getSalaryCycle());
        hm.put("stf", getCurrent().getStaff());
        StaffSalaryComponant salaryComponant = staffSalaryComponantFacade.findFirstByJpql(sql, hm);

        if (salaryComponant != null) {
            getCurrent().getStaffSalaryComponants().add(salaryComponant);
            return salaryComponant.getComponantValue();
        }
        return 0;
    }

    private StaffSalaryComponant fetchSalaryAdvance(Staff staff) {
        String sql = "select sc from StaffSalaryComponant sc "
                + " where sc.retired=false "
                + " and sc.salaryCycle=:sc "
                + " and sc.staff=:stf";
        HashMap hm = new HashMap();
        hm.put("sc", getSalaryCycle());
        hm.put("stf", staff);
        StaffSalaryComponant salaryComponant = staffSalaryComponantFacade.findFirstByJpql(sql, hm);

        return salaryComponant;

    }

    public void deleteAll() {
        if (items == null) {
            return;
        }

        for (StaffSalary s : items) {
            deleteSalaryComponent(s);
        }
    }

    public void deleteSalaryComponent(StaffSalary staffSalary) {
        if (staffSalary.getId() == null) {
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

            updateStaffShiftRedo(staffSalary.getStaff(),
                    staffSalary.getSalaryCycle().getSalaryAdvanceFromDate(),
                    staffSalary.getSalaryCycle().getSalaryAdvanceToDate());

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

        JsfUtil.addSuccessMessage("Record Succesfully Deleted");
    }
//
//    public void calStaffLeaveFromLateIn(StaffShift stfCurrent, double fromTime, double toTime, double shiftCount) {
//
//        List<StaffShift> staffShiftEarlyIn = staffLeaveFromLateAndEarlyController.fetchStaffShiftLateIn(stfCurrent.getStaff(), fromTime, toTime);
//        LinkedList<StaffShift> staffShiftLateInTenMinuteLinked = new LinkedList<>();
//
//        if (staffShiftEarlyIn != null) {
//            for (StaffShift stf : staffShiftEarlyIn) {
//                staffShiftLateInTenMinuteLinked.add(stf);
//            }
//        }
//
//        if (staffShiftLateInTenMinuteLinked.size() >= shiftCount) {
//            for (int i = 0; i < shiftCount; i++) {
//
//                StaffShift lateShift = staffShiftLateInTenMinuteLinked.pollFirst();
//                System.err.println("Late In Shift ID " + lateShift.getId());
//                lateShift.setReferenceStaffShiftLateIn(stfCurrent);
//                lateShift.setConsiderForLateIn(true);
//                staffShiftFacade.edit(lateShift);
////                staffShiftFacade.flush();
//            }
//
//            LeaveType leaveType = humanResourceBean.getLeaveType(stfCurrent.getStaff(), commonFunctions.getFirstDayOfYear(stfCurrent.getShiftDate()), commonFunctions.getLastDayOfYear(stfCurrent.getShiftDate()));
//            HrForm hr = staffLeaveFromLateAndEarlyController.saveLeaveForm(stfCurrent, leaveType, stfCurrent.getShiftDate(), stfCurrent.getShiftDate());
//            staffLeaveFromLateAndEarlyController.saveStaffLeaves(stfCurrent, leaveType, hr);
//            staffLeaveFromLateAndEarlyController.addLeaveDataToStaffShift(stfCurrent, leaveType, hr);
//        }
//
//        System.err.println("Automatic Late In End " + stfCurrent.getStaff().getCodeInterger());
//
//    }

    @Inject
    StaffLeaveFromLateAndEarlyController staffLeaveFromLateAndEarlyController;
//
//    public void calStaffLeaveFromEarlyOut(StaffShift stfCurrent, double fromTime, double toTime, double shiftCount) {
//        List<StaffShift> staffShiftEarlyOut = staffLeaveFromLateAndEarlyController.fetchStaffShiftEarlyOut(stfCurrent.getStaff(), fromTime, toTime);
//        LinkedList<StaffShift> staffShiftEarlyOutThirtyMinuteLinked = new LinkedList<>();
//
//        if (staffShiftEarlyOut != null) {
//            for (StaffShift stf : staffShiftEarlyOut) {
//                staffShiftEarlyOutThirtyMinuteLinked.add(stf);
//            }
//        }
//
//        if (staffShiftEarlyOutThirtyMinuteLinked.size() >= shiftCount) {
//            for (int i = 0; i < shiftCount; i++) {
//                StaffShift earlyOut = staffShiftEarlyOutThirtyMinuteLinked.pollFirst();
//                System.err.println("Early Out  Shift ID " + earlyOut.getId());
//                earlyOut.setReferenceStaffShiftEarlyOut(stfCurrent);
//                earlyOut.setConsiderForEarlyOut(true);
//                staffShiftFacade.edit(earlyOut);
//            }
//
//            LeaveType leaveType = humanResourceBean.getLeaveType(stfCurrent.getStaff(), commonFunctions.getFirstDayOfYear(stfCurrent.getShiftDate()), commonFunctions.getLastDayOfYear(stfCurrent.getShiftDate()));
//            HrForm hr = staffLeaveFromLateAndEarlyController.saveLeaveForm(stfCurrent, leaveType, stfCurrent.getShiftDate(), stfCurrent.getShiftDate());
//            staffLeaveFromLateAndEarlyController.saveStaffLeaves(stfCurrent, leaveType, hr);
//            staffLeaveFromLateAndEarlyController.addLeaveDataToStaffShift(stfCurrent, leaveType, hr);
//        }
//
//        System.err.println("Automatic Early out END " + stfCurrent.getStaff().getCodeInterger());
//    }

//    public void calStaffLeaveFromEarlyOut(StaffShift stfCurrent, double fromTime) {
//
////        if (stfCurrent.getEarlyOutLogged() >= fromTime) {
//        System.err.println("Early Out  Shift ID " + stfCurrent.getId());
//        stfCurrent.setReferenceStaffShiftEarlyOut(stfCurrent);
//        stfCurrent.setConsiderForEarlyOut(true);
//        staffShiftFacade.edit(stfCurrent);
//        LeaveType leaveType = humanResourceBean.getLeaveType(stfCurrent.getStaff(), commonFunctions.getFirstDayOfYear(stfCurrent.getShiftDate()), commonFunctions.getLastDayOfYear(stfCurrent.getShiftDate()));
//        HrForm hr = staffLeaveFromLateAndEarlyController.saveLeaveForm(stfCurrent, leaveType, stfCurrent.getShiftDate(), stfCurrent.getShiftDate());
//        staffLeaveFromLateAndEarlyController.saveStaffLeaves(stfCurrent, leaveType, hr);
//        staffLeaveFromLateAndEarlyController.addLeaveDataToStaffShift(stfCurrent, leaveType, hr);
//
////        }
//        System.err.println("Automatic Early out END " + stfCurrent.getStaff().getCodeInterger());
//    }
//    public void calStaffLeaveFromLateIn(StaffShift stfCurrent, double fromTime) {
//
//        stfCurrent.setReferenceStaffShiftLateIn(stfCurrent);
//        System.err.println("Late In  Shift ID " + stfCurrent.getId());
//        stfCurrent.setConsiderForLateIn(true);
//        staffShiftFacade.edit(stfCurrent);
//
//        LeaveType leaveType = humanResourceBean.getLeaveType(stfCurrent.getStaff(), commonFunctions.getFirstDayOfYear(stfCurrent.getShiftDate()), commonFunctions.getLastDayOfYear(stfCurrent.getShiftDate()));
//        HrForm hr = staffLeaveFromLateAndEarlyController.saveLeaveForm(stfCurrent, leaveType, stfCurrent.getShiftDate(), stfCurrent.getShiftDate());
//        staffLeaveFromLateAndEarlyController.saveStaffLeaves(stfCurrent, leaveType, hr);
//        staffLeaveFromLateAndEarlyController.addLeaveDataToStaffShift(stfCurrent, leaveType, hr);
//
//        System.err.println("Automatic Late In END " + stfCurrent.getStaff().getCodeInterger());
//    }
//
//    private void generateAutoLeave(Staff staff, Date fromDate, Date toDate) {
//        List<StaffShift> staffShifts = humanResourceBean.fetchStaffShiftForAutoLeave(staff, fromDate, toDate);
//
//        if (staffShifts == null) {
//            return;
//        }
//
//        List<StaffShift> staffShiftsTmp = new ArrayList<>();
//
//        for (StaffShift ss : staffShifts) {
//            //Automatic No Pay Diduction
//            double fromMinute = 90 * 60;
//
//            if (ss.getStaff().isAllowedLateInLeave()
//                    && !ss.isConsiderForLateIn()) {
//                System.err.println("******Automatic Late In Leave " + ss.getStaff().getCodeInterger());
//
//                if (ss.getLateInVarified() > fromMinute) {
//                    calStaffLeaveFromLateIn(ss, 90 * 60);
//                } else {
//                    staffShiftsTmp.add(ss);
//                }
//            }
//
//            if (ss.getStaff().isAllowedEarlyOutLeave()
//                    && !ss.isConsiderForEarlyOut()) {
//                System.err.println("******Automatic Early Out Leave " + ss.getStaff().getCodeInterger());
////                        calStaffLeaveFromEarlyOut(ss, 30 * 60, 90 * 60, 3);
//                if (ss.getEarlyOutVarified() > fromMinute) {
//                    calStaffLeaveFromEarlyOut(ss, 90 * 60);
//                } else {
//                    staffShiftsTmp.add(ss);
//                }
//            }
//
//        }
//
//        for (StaffShift ss : staffShiftsTmp) {
//            if (ss.getStaff().isAllowedLateInLeave()
//                    && !ss.isConsiderForLateIn()) {
//                System.err.println("******Automatic Late In Leave (Out) " + ss.getStaff().getCodeInterger());
//                calStaffLeaveFromLateIn(ss, 10 * 60, 90 * 60, 3);
//
//            }
//
//            if (ss.getStaff().isAllowedEarlyOutLeave()
//                    && !ss.isConsiderForEarlyOut()) {
//                System.err.println("******Automatic Early Out Leave (Out) " + ss.getStaff().getCodeInterger());
//                calStaffLeaveFromEarlyOut(ss, 30 * 60, 90 * 60, 3);
//
//            }
//
//        }
//    }
//
    public void generate() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        if (getStaffController().getSelectedList() == null) {
            JsfUtil.addErrorMessage("Pls Select Staff");
            return;
        }

        if (dateCheck()) {
            return;
        }

        items = null;
        boolean flag = false;

        for (Staff s : getStaffController().getSelectedList()) {
            setCurrent(getHumanResourceBean().getStaffSalary(s, getSalaryCycle()));
//            generateAutoLeave(s, getSalaryCycle().getSalaryAdvanceFromDate(), getSalaryCycle().getSalaryAdvanceToDate());
            if (getCurrent().getId() == null) {
                fetchAndSetBankData();
                addSalaryComponent();
//                save();
            } else {
                // Allready in database
                flag = true;
                break;
            }

            getCurrent().calculateComponentTotal();
            getCurrent().calcualteEpfAndEtf();
            getItems().add(current);
            current = null;

        }

        if (flag) {
            items = null;
            JsfUtil.addErrorMessage("There is allready salary generated .please delete generated salary");
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

        StaffPaysheetComponent staffPaysheetComponent = getStaffPaysheetComponentFacade().findFirstByJpql(sql, hm, TemporalType.DATE);
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

        items = getStaffSalaryFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public void saveSalary() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        if (getStaffController().getSelectedList() == null) {
            return;
        }

        if (dateCheck()) {
            return;
        }

        if (items == null) {
            return;
        }

        for (StaffSalary stf : items) {
            if (stf.getId() != null) {
                continue;
            }
            current = stf;
            save();
//            updateStaffShift(stf.getStaff(), getSalaryCycle().getWorkedFromDate(), getSalaryCycle().getWorkedToDate());
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

//    public void createStaffSalaryTable() {
//        if (getSalaryCycle() == null) {
//            return;
//        }
//        String sql = "Select s From StaffSalary s"
//                + " where s.retired=false "
//                + " and s.salaryCycle.salaryFromDate>=:fd "
//                + " and s.salaryCycle.salaryToDate<=:td";
//
//        HashMap hm = new HashMap<>();
//        hm.put("fd", getSalaryCycle().getSalaryFromDate());
//        hm.put("td", getSalaryCycle().getSalaryToDate());
//
//        items = getStaffSalaryFacade().findByJpql(sql, hm, TemporalType.DATE);
//    }
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

        return getStaffSalaryFacade().findFirstByJpql(sql, hm, TemporalType.DATE);
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
            StaffSalaryAdvanceController controller = (StaffSalaryAdvanceController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "staffSalaryAdvanceController");
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
                        + object.getClass().getName() + "; expected type: " + StaffSalaryAdvanceController.class.getName());
            }
        }
    }


    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

}
