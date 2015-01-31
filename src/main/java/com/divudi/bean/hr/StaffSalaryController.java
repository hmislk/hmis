/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.hr;

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
import com.divudi.entity.hr.PaysheetComponent;
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
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
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
        ////System.out.println("Runn");
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

        List<PaysheetComponent> paysheetComponentsAddition = salaryCycleController.fetchPaysheetComponentsUserDefinded(PaysheetComponentType.addition.getUserDefinedComponentsAddidtions());
        List<PaysheetComponent> paysheetComponentsSubstraction = salaryCycleController.fetchPaysheetComponentsUserDefinded(PaysheetComponentType.subtraction.getUserDefinedComponentsDeductions());

        for (PaysheetComponent psc : paysheetComponentsAddition) {
            StaffSalaryComponant c = salaryCycleController.fetchSalaryComponents(getCurrent(), psc);
            getCurrent().getTransStaffSalaryComponantsAddition().add(c);

        }
        for (PaysheetComponent psc : paysheetComponentsSubstraction) {
            StaffSalaryComponant c = salaryCycleController.fetchSalaryComponents(getCurrent(), psc);
            getCurrent().getTransStaffSalaryComponantsSubtraction().add(c);

        }

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
    private void setBasic() {
//        getHumanResourceBean().calculateBasic(getSalaryFromDate(), getSalaryToDate(), getCurrent().getStaff());

        StaffSalaryComponant ss = new StaffSalaryComponant();
        ss.setCreatedAt(new Date());
        ss.setSalaryCycle(salaryCycle);
        ss.setCreater(getSessionController().getLoggedUser());
        ss.setStaffPaysheetComponent(getHumanResourceBean().getBasic(getCurrent().getStaff(), getSalaryCycle().getSalaryToDate()));
        if (ss.getStaffPaysheetComponent() != null) {
            ss.setComponantValue(ss.getStaffPaysheetComponent().getStaffPaySheetComponentValue());
        } else {
            return;
        }

        getHumanResourceBean().setEpf(ss, getHrmVariablesController().getCurrent().getEpfRate(), getHrmVariablesController().getCurrent().getEpfCompanyRate());
        getHumanResourceBean().setEtf(ss, getHrmVariablesController().getCurrent().getEtfRate(), getHrmVariablesController().getCurrent().getEtfCompanyRate());

        System.err.println("BASIC " + ss.getStaffPaysheetComponent().getPaysheetComponent().getName());
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

    private void setOT() {

        StaffSalaryComponant ss = createStaffSalaryComponant(PaysheetComponentType.OT);

        if (ss.getStaffPaysheetComponent() != null) {
            double workedWithinTimeFrameVarified = getHumanResourceBean().calculateWorkTimeAndLeave(getSalaryCycle().getWorkedFromDate(), getSalaryCycle().getWorkedToDate(), getCurrent().getStaff());
            Long dateCount = commonFunctions.getDayCount(getSalaryCycle().getWorkedFromDate(), getSalaryCycle().getWorkedToDate());
            Long numOfWeeks = dateCount / 7;

            double overTime = humanResourceBean.getOverTimeFromRoster(getCurrent().getStaff().getWorkingTimeForOverTimePerWeek(), numOfWeeks, workedWithinTimeFrameVarified);
            double basicPerSecond = 0;

            for (StaffSalaryComponant staffSalaryComponant : getCurrent().getStaffSalaryComponants()) {
                if (staffSalaryComponant.getStaffPaysheetComponent() == null
                        || staffSalaryComponant.getStaffPaysheetComponent().getPaysheetComponent() == null) {
                    continue;
                }

                if (staffSalaryComponant.getStaffPaysheetComponent().getPaysheetComponent().getComponentType() == PaysheetComponentType.BasicSalary) {
                    basicPerSecond = staffSalaryComponant.getComponantValue() / (200 * 60 * 60);
                }
            }

            ss.setComponantValue(overTime * basicPerSecond * finalVariables.getOverTimeMultiply());
            getCurrent().setOverTimeMinute(overTime / 60);
            getCurrent().setOverTimeRatePerMinute(basicPerSecond / 60);
        } else {
            return;
        }

        getHumanResourceBean().setEpf(ss, getHrmVariablesController().getCurrent().getEpfRate(), getHrmVariablesController().getCurrent().getEpfCompanyRate());
        getHumanResourceBean().setEtf(ss, getHrmVariablesController().getCurrent().getEtfRate(), getHrmVariablesController().getCurrent().getEtfCompanyRate());

        System.err.println("OT " + ss.getStaffPaysheetComponent().getPaysheetComponent().getName());
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
    private double setExtraDuty(PaysheetComponentType paysheetComponentType, DayType dayType) {
        StaffSalaryComponant ss = createStaffSalaryComponant(paysheetComponentType);
        ss.setComponantValue(humanResourceBean.calculateExtraWorkTimeValue(getSalaryCycle().getWorkedFromDate(), getSalaryCycle().getWorkedToDate(), getCurrent().getStaff(), dayType));
        getHumanResourceBean().setEpf(ss, getHrmVariablesController().getCurrent().getEpfRate(), getHrmVariablesController().getCurrent().getEpfCompanyRate());
        getHumanResourceBean().setEtf(ss, getHrmVariablesController().getCurrent().getEtfRate(), getHrmVariablesController().getCurrent().getEtfCompanyRate());

//        System.err.println("EXTRA " + ss.getStaffPaysheetComponent().getPaysheetComponent().getName());
        getCurrent().getStaffSalaryComponants().add(ss);
        return humanResourceBean.calculateExtraWorkTime(getSalaryCycle().getWorkedFromDate(), getSalaryCycle().getWorkedToDate(), getCurrent().getStaff(), dayType);

    }

    private Long setHoliDayAllowance(PaysheetComponentType paysheetComponentType, DayType dayType) {
        long count = 0;
        StaffSalaryComponant ss = createStaffSalaryComponant(paysheetComponentType);
        if (ss.getStaffPaysheetComponent() != null) {
            count = getHumanResourceBean().calculateHolidayWork(getSalaryCycle().getWorkedFromDate(), getSalaryCycle().getWorkedToDate(), getCurrent().getStaff(), dayType);

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
                    salaryValue += staffSalaryComponant.getComponantValue();
                }
            }

            double salaryPerDay = (salaryValue / finalVariables.getWorkingDaysPerMonth());
            double value = getHumanResourceBean().calculateHolidayWork(getSalaryCycle().getWorkedFromDate(), getSalaryCycle().getWorkedToDate(), getCurrent().getStaff(), dayType, salaryPerDay);
            //Need Calculation Sum
            ss.setComponantValue(value);

            System.err.println(dayType + " Salary Allowance " + salaryValue);
            System.err.println(dayType + " count " + count);
        } else {
            return 0L;
        }

        getHumanResourceBean().setEpf(ss, getHrmVariablesController().getCurrent().getEpfRate(), getHrmVariablesController().getCurrent().getEpfCompanyRate());
        getHumanResourceBean().setEtf(ss, getHrmVariablesController().getCurrent().getEtfRate(), getHrmVariablesController().getCurrent().getEtfCompanyRate());

//        System.err.println("NO " + ss.getStaffPaysheetComponent().getPaysheetComponent().getName());
        getCurrent().getStaffSalaryComponants().add(ss);

        return count;

    }

    private Long setDayOffSleepingDayAllowance(PaysheetComponentType paysheetComponentType, DayType dayType) {
        System.err.println("DAY OFF Allowance");
        Long count = 0L;
        StaffSalaryComponant ss = createStaffSalaryComponant(paysheetComponentType);
        if (ss.getStaffPaysheetComponent() != null) {
            count = getHumanResourceBean().calculateOffDays(getSalaryCycle().getWorkedFromDate(), getSalaryCycle().getWorkedToDate(), getCurrent().getStaff(), dayType);
            double salaryValue = 0;

            System.err.println("Size " + getCurrent().getStaffSalaryComponants().size());

            for (StaffSalaryComponant staffSalaryComponant : getCurrent().getStaffSalaryComponants()) {
                if (staffSalaryComponant.getStaffPaysheetComponent() == null
                        || staffSalaryComponant.getStaffPaysheetComponent().getPaysheetComponent() == null) {
                    System.err.println("Continue");
                    continue;
                }

                System.err.println("P " + staffSalaryComponant.getStaffPaysheetComponent().getPaysheetComponent().getName() + " : " + staffSalaryComponant.getStaffPaysheetComponent().getPaysheetComponent().isIncludeForAllowance());

                if (staffSalaryComponant.getStaffPaysheetComponent().getPaysheetComponent().isIncludeForAllowance()) {
                    System.err.println("INNN " + staffSalaryComponant.getComponantValue());
                    salaryValue += staffSalaryComponant.getComponantValue();
                }
            }

            double salaryPerDay = (salaryValue / finalVariables.getWorkingDaysPerMonth());
            double value = getHumanResourceBean().calculateOffDays(getSalaryCycle().getWorkedFromDate(), getSalaryCycle().getWorkedToDate(), getCurrent().getStaff(), dayType, salaryPerDay);
            //Need Calculation Sum
            ss.setComponantValue(value);
            System.err.println("Day Off Val " + salaryValue);
            System.err.println("Day Off Count " + count);
        } else {
            return 0L;
        }

        getHumanResourceBean().setEpf(ss, getHrmVariablesController().getCurrent().getEpfRate(), getHrmVariablesController().getCurrent().getEpfCompanyRate());
        getHumanResourceBean().setEtf(ss, getHrmVariablesController().getCurrent().getEtfRate(), getHrmVariablesController().getCurrent().getEtfCompanyRate());

        System.err.println("NO " + ss.getStaffPaysheetComponent().getPaysheetComponent().getName());
        getCurrent().getStaffSalaryComponants().add(ss);
        return count;
    }

    private double setNoPay_Basic(double noPayCount) {
        double noPayValue = 0;
        double salaryValue = 0;
        StaffSalaryComponant ss = createStaffSalaryComponant(PaysheetComponentType.No_Pay_Deduction_Basic);
        if (ss.getStaffPaysheetComponent() != null) {
            noPayCount = getHumanResourceBean().fetchStaffLeave(getCurrent().getStaff(), LeaveType.No_Pay, getSalaryCycle().getWorkedFromDate(), getSalaryCycle().getWorkedToDate());
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
            noPayValue = (salaryValue / finalVariables.getWorkingDaysPerMonth()) * noPayCount;
            ss.setComponantValue(noPayValue);
            System.err.println("Sal Val " + salaryValue);
//            System.err.println("No Pa " + noPayTime);
        } else {
            return 0;
        }

        getHumanResourceBean().setEpf(ss, getHrmVariablesController().getCurrent().getEpfRate(), getHrmVariablesController().getCurrent().getEpfCompanyRate());
        getHumanResourceBean().setEtf(ss, getHrmVariablesController().getCurrent().getEtfRate(), getHrmVariablesController().getCurrent().getEtfCompanyRate());

        System.err.println("NO " + ss.getStaffPaysheetComponent().getPaysheetComponent().getName());
        getCurrent().getStaffSalaryComponants().add(ss);

        return salaryValue;
    }

    private double setNoPay_Allowance(double noPayCount) {
        double noPayValue = 0;
        double allownaceValue = 0;
        StaffSalaryComponant ss = createStaffSalaryComponant(PaysheetComponentType.No_Pay_Deduction_Allowance);
        if (ss.getStaffPaysheetComponent() != null) {

            allownaceValue = 0;

            if (getCurrent().getStaffSalaryComponants() == null) {
                return 0;
            }

            for (StaffSalaryComponant staffSalaryComponant : getCurrent().getStaffSalaryComponants()) {
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

            //Need Calculation Sum
            noPayValue = (allownaceValue / finalVariables.getWorkingDaysPerMonth()) * noPayCount;
            ss.setComponantValue(noPayValue);
            System.err.println("Sal Val " + allownaceValue);
//            System.err.println("No Pa " + noPayTime);
        } else {
            return 0;
        }

        getHumanResourceBean().setEpf(ss, getHrmVariablesController().getCurrent().getEpfRate(), getHrmVariablesController().getCurrent().getEpfCompanyRate());
        getHumanResourceBean().setEtf(ss, getHrmVariablesController().getCurrent().getEtfRate(), getHrmVariablesController().getCurrent().getEtfCompanyRate());

        System.err.println("NO " + ss.getStaffPaysheetComponent().getPaysheetComponent().getName());
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

        if (getCurrent().getStaff() != null) {

            setBasic();

            for (StaffPaysheetComponent spc : getHumanResourceBean().fetchStaffPaysheetComponent(getCurrent().getStaff(), getSalaryCycle().getSalaryToDate())) {

                StaffSalaryComponant ss = new StaffSalaryComponant();
                ss.setCreatedAt(new Date());
                ss.setSalaryCycle(salaryCycle);
                ss.setCreater(getSessionController().getLoggedUser());
                ss.setComponantValue(spc.getStaffPaySheetComponentValue());
                ss.setStaffPaysheetComponent(spc);
//                ss.setStaffSalary(getCurrent());
                getHumanResourceBean().setEpf(ss, getHrmVariablesController().getCurrent().getEpfRate(), getHrmVariablesController().getCurrent().getEpfCompanyRate());
                getHumanResourceBean().setEtf(ss, getHrmVariablesController().getCurrent().getEtfRate(), getHrmVariablesController().getCurrent().getEtfCompanyRate());

//                System.err.println("COMP " + ss.getStaffPaysheetComponent().getPaysheetComponent().getName());
                getCurrent().getStaffSalaryComponants().add(ss);
            }

            setOT();

            //Set Extra Duty Value
            double extraTime = setExtraDuty(PaysheetComponentType.ExtraDutyNormal, DayType.Normal);
            getCurrent().setExtraDutyNormalMinute(extraTime / 60);

            extraTime = setExtraDuty(PaysheetComponentType.ExtraDutyMerchantile, DayType.MurchantileHoliday);
            getCurrent().setExtraDutyMerchantileMinute(extraTime / 60);

            extraTime = setExtraDuty(PaysheetComponentType.ExtraDutyPoya, DayType.Poya);
            getCurrent().setExtraDutyPoyaMinute(extraTime / 60);

            extraTime = setExtraDuty(PaysheetComponentType.ExtraDutyDayOff, DayType.DayOff);
            getCurrent().setExtraDutyDayOffMinute(extraTime / 60);

            extraTime = setExtraDuty(PaysheetComponentType.ExtraDutySleepingDay, DayType.SleepingDay);
            getCurrent().setExtraDutySleepingDayMinute(extraTime / 60);

            Long count = setHoliDayAllowance(PaysheetComponentType.MerchantileAllowance, DayType.MurchantileHoliday);
            getCurrent().setMerchantileCount(count.doubleValue());
            count = setHoliDayAllowance(PaysheetComponentType.PoyaAllowance, DayType.Poya);
            getCurrent().setPoyaCount(count.doubleValue());
            count = setDayOffSleepingDayAllowance(PaysheetComponentType.DayOffAllowance, DayType.DayOff);
            getCurrent().setDayOffCount(count.doubleValue());
            count = setDayOffSleepingDayAllowance(PaysheetComponentType.SleepingDayAllowance, DayType.SleepingDay);
            getCurrent().setSleepingDayCount(count.doubleValue());

            double noPayCount = getHumanResourceBean().fetchStaffLeave(getCurrent().getStaff(), LeaveType.No_Pay, getSalaryCycle().getWorkedFromDate(), getSalaryCycle().getWorkedToDate());
            double basicValue = setNoPay_Basic(noPayCount);
            double allowanceValue = setNoPay_Allowance(noPayCount);
            getCurrent().setNoPayCount(noPayCount);
            setAdjustments();

            //Record Late No Pay Leave 
            //Not consider in any calcualtion is alredy with general NO Pay 
            //only for reporting purpose
            double noPayCountLate = getHumanResourceBean().fetchStaffLeaveSystem(getCurrent().getStaff(), LeaveType.No_Pay, getSalaryCycle().getWorkedFromDate(), getSalaryCycle().getWorkedToDate());
            getCurrent().setLateNoPayCount(noPayCountLate);
            getCurrent().setNoPayValueBasic((basicValue / finalVariables.getWorkingDaysPerMonth()) * noPayCountLate);
            getCurrent().setNoPayValueAllowance((allowanceValue / finalVariables.getWorkingDaysPerMonth()) * noPayCountLate);

        }

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
            System.err.println("RETURE 1");
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

            System.err.println("Return 2");
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

    public void calStaffLeaveFromLateIn(StaffShift stfCurrent, double fromTime, double toTime, double shiftCount) {

        List<StaffShift> staffShiftEarlyIn = staffLeaveFromLateAndEarlyController.fetchStaffShiftLateIn(stfCurrent.getStaff(), fromTime, toTime);
        LinkedList<StaffShift> staffShiftLateInTenMinuteLinked = new LinkedList<>();

        if (staffShiftEarlyIn != null) {
            for (StaffShift stf : staffShiftEarlyIn) {
                staffShiftLateInTenMinuteLinked.add(stf);
            }
        }

        if (staffShiftLateInTenMinuteLinked.size() >= shiftCount) {
            for (int i = 0; i < shiftCount; i++) {

                StaffShift lateShift = staffShiftLateInTenMinuteLinked.pollFirst();
                System.err.println("Late In Shift ID " + lateShift.getId());
                lateShift.setReferenceStaffShiftLateIn(stfCurrent);
                lateShift.setConsiderForLateIn(true);
                staffShiftFacade.edit(lateShift);
//                staffShiftFacade.flush();
            }

            LeaveType leaveType = humanResourceBean.getLeaveType(stfCurrent.getStaff(), commonFunctions.getFirstDayOfYear(stfCurrent.getShiftDate()), commonFunctions.getLastDayOfYear(stfCurrent.getShiftDate()));
            HrForm hr = staffLeaveFromLateAndEarlyController.saveLeaveForm(stfCurrent, leaveType, stfCurrent.getShiftDate(), stfCurrent.getShiftDate());
            staffLeaveFromLateAndEarlyController.saveStaffLeaves(stfCurrent, leaveType, hr);
            staffLeaveFromLateAndEarlyController.addLeaveDataToStaffShift(stfCurrent, leaveType, hr);
        }

        System.err.println("Automatic Late In End " + stfCurrent.getStaff().getCodeInterger());

    }

    @Inject
    StaffLeaveFromLateAndEarlyController staffLeaveFromLateAndEarlyController;

    public void calStaffLeaveFromEarlyOut(StaffShift stfCurrent, double fromTime, double toTime, double shiftCount) {
        List<StaffShift> staffShiftEarlyOut = staffLeaveFromLateAndEarlyController.fetchStaffShiftEarlyOut(stfCurrent.getStaff(), fromTime, toTime);
        LinkedList<StaffShift> staffShiftEarlyOutThirtyMinuteLinked = new LinkedList<>();

        if (staffShiftEarlyOut != null) {
            for (StaffShift stf : staffShiftEarlyOut) {
                staffShiftEarlyOutThirtyMinuteLinked.add(stf);
            }
        }

        if (staffShiftEarlyOutThirtyMinuteLinked.size() >= shiftCount) {
            for (int i = 0; i < shiftCount; i++) {
                StaffShift earlyOut = staffShiftEarlyOutThirtyMinuteLinked.pollFirst();
                System.err.println("Early Out  Shift ID " + earlyOut.getId());
                earlyOut.setReferenceStaffShiftEarlyOut(stfCurrent);
                earlyOut.setConsiderForEarlyOut(true);
                staffShiftFacade.edit(earlyOut);
            }

            LeaveType leaveType = humanResourceBean.getLeaveType(stfCurrent.getStaff(), commonFunctions.getFirstDayOfYear(stfCurrent.getShiftDate()), commonFunctions.getLastDayOfYear(stfCurrent.getShiftDate()));
            HrForm hr = staffLeaveFromLateAndEarlyController.saveLeaveForm(stfCurrent, leaveType, stfCurrent.getShiftDate(), stfCurrent.getShiftDate());
            staffLeaveFromLateAndEarlyController.saveStaffLeaves(stfCurrent, leaveType, hr);
            staffLeaveFromLateAndEarlyController.addLeaveDataToStaffShift(stfCurrent, leaveType, hr);
        }

        System.err.println("Automatic Early out END " + stfCurrent.getStaff().getCodeInterger());
    }

    public void calStaffLeaveFromEarlyOut(StaffShift stfCurrent, double fromTime) {

//        if (stfCurrent.getEarlyOutLogged() >= fromTime) {
        System.err.println("Early Out  Shift ID " + stfCurrent.getId());
        stfCurrent.setReferenceStaffShiftEarlyOut(stfCurrent);
        stfCurrent.setConsiderForEarlyOut(true);
        staffShiftFacade.edit(stfCurrent);
        LeaveType leaveType = humanResourceBean.getLeaveType(stfCurrent.getStaff(), commonFunctions.getFirstDayOfYear(stfCurrent.getShiftDate()), commonFunctions.getLastDayOfYear(stfCurrent.getShiftDate()));
        HrForm hr = staffLeaveFromLateAndEarlyController.saveLeaveForm(stfCurrent, leaveType, stfCurrent.getShiftDate(), stfCurrent.getShiftDate());
        staffLeaveFromLateAndEarlyController.saveStaffLeaves(stfCurrent, leaveType, hr);
        staffLeaveFromLateAndEarlyController.addLeaveDataToStaffShift(stfCurrent, leaveType, hr);

//        }
        System.err.println("Automatic Early out END " + stfCurrent.getStaff().getCodeInterger());
    }

    public void calStaffLeaveFromLateIn(StaffShift stfCurrent, double fromTime) {

        stfCurrent.setReferenceStaffShiftLateIn(stfCurrent);
        System.err.println("Late In  Shift ID " + stfCurrent.getId());
        stfCurrent.setConsiderForLateIn(true);
        staffShiftFacade.edit(stfCurrent);

        LeaveType leaveType = humanResourceBean.getLeaveType(stfCurrent.getStaff(), commonFunctions.getFirstDayOfYear(stfCurrent.getShiftDate()), commonFunctions.getLastDayOfYear(stfCurrent.getShiftDate()));
        HrForm hr = staffLeaveFromLateAndEarlyController.saveLeaveForm(stfCurrent, leaveType, stfCurrent.getShiftDate(), stfCurrent.getShiftDate());
        staffLeaveFromLateAndEarlyController.saveStaffLeaves(stfCurrent, leaveType, hr);
        staffLeaveFromLateAndEarlyController.addLeaveDataToStaffShift(stfCurrent, leaveType, hr);

        System.err.println("Automatic Late In END " + stfCurrent.getStaff().getCodeInterger());
    }

    private void generateAutoLeave(Staff staff, Date fromDate, Date toDate) {
        List<StaffShift> staffShifts = humanResourceBean.fetchStaffShiftForAutoLeave(staff, fromDate, toDate);

        if (staffShifts == null) {
            return;
        }

        List<StaffShift> staffShiftsTmp = new ArrayList<>();

        for (StaffShift ss : staffShifts) {
            //Automatic No Pay Diduction
            double fromMinute = 90 * 60;

            if (ss.getStaff().isAllowedLateInLeave()
                    && !ss.isConsiderForLateIn()) {
                System.err.println("******Automatic Late In Leave " + ss.getStaff().getCodeInterger());

                if (ss.getLateInVarified() >= fromMinute) {
                    calStaffLeaveFromLateIn(ss, 90 * 60);
                } else {
                    staffShiftsTmp.add(ss);
                }
            }

            if (ss.getStaff().isAllowedEarlyOutLeave()
                    && !ss.isConsiderForEarlyOut()) {
                System.err.println("******Automatic Early Out Leave " + ss.getStaff().getCodeInterger());
//                        calStaffLeaveFromEarlyOut(ss, 30 * 60, 90 * 60, 3);
                if (ss.getEarlyOutVarified() >= fromMinute) {
                    calStaffLeaveFromEarlyOut(ss, 90 * 60);
                } else {
                    staffShiftsTmp.add(ss);
                }
            }

        }

        for (StaffShift ss : staffShiftsTmp) {
            if (ss.getStaff().isAllowedLateInLeave()
                    && !ss.isConsiderForLateIn()) {
                System.err.println("******Automatic Late In Leave (Out)" + ss.getStaff().getCodeInterger());
                calStaffLeaveFromLateIn(ss, 10 * 60, 90 * 60, 3);

            }

            if (ss.getStaff().isAllowedEarlyOutLeave()
                    && !ss.isConsiderForEarlyOut()) {
                System.err.println("******Automatic Early Out Leave (Out)" + ss.getStaff().getCodeInterger());
                calStaffLeaveFromEarlyOut(ss, 30 * 60, 90 * 60, 3);

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

        for (Staff s : getStaffController().getSelectedList()) {
            setCurrent(getHumanResourceBean().getStaffSalary(s, getSalaryCycle()));
            generateAutoLeave(s, getSalaryCycle().getWorkedFromDate(), getSalaryCycle().getWorkedToDate());
            if (getCurrent().getId() == null) {
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

    public void fetchStaffSalay() {
        String sql;
        HashMap hm = new HashMap();

        sql = "SELECT ss FROM StaffSalary ss "
                + " WHERE ss.retired=false "
                + " and ss.salaryCycle=:sc ";

        hm.put("sc", getSalaryCycle());
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

        for (StaffSalary stf : items) {
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
