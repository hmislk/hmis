/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.hr;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.data.hr.LeaveType;
import com.divudi.data.hr.PaysheetComponentType;
import com.divudi.ejb.CommonFunctions;
import com.divudi.ejb.FinalVariables;
import com.divudi.ejb.HumanResourceBean;
import com.divudi.entity.Staff;
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
    ///////////
    private Date salaryFromDate;
    private Date salaryToDate;
    Date workedFromDate;
    Date workedToDate;
    //////////   
    List<StaffSalary> items;
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

    public Date getWorkedFromDate() {
        return workedFromDate;
    }

    public void setWorkedFromDate(Date workedFromDate) {
        this.workedFromDate = workedFromDate;
    }

    public Date getWorkedToDate() {
        return workedToDate;
    }

    public void setWorkedToDate(Date workedToDate) {
        this.workedToDate = workedToDate;
    }

    public FinalVariables getFinalVariables() {
        return finalVariables;
    }

    public void setFinalVariables(FinalVariables finalVariables) {
        this.finalVariables = finalVariables;
    }

    public void remove() {
        getCurrent().setRetired(true);
        getCurrent().setRetiredAt(new Date());
        getCurrent().setRetirer(getSessionController().getLoggedUser());

        getStaffSalaryFacade().edit(getCurrent());
        current = null;
    }

    private boolean errorCheck() {

        if (getCurrent().getStaff() == null) {
            UtilityController.addErrorMessage("Please Select Staff");
            return true;
        }

        return false;
    }

    public void save() {
//
//        if (errorCheck()) {
//            return;
//        }

        List<StaffSalaryComponant> list = getCurrent().getStaffSalaryComponants();

        if (getCurrent().getId() == null) {
            getCurrent().getSalaryCycle().setSalaryFromDate(getSalaryFromDate());
            getCurrent().getSalaryCycle().setSalaryToDate(getSalaryToDate());
            getCurrent().getSalaryCycle().setWorkedFromDate(getWorkedFromDate());
            getCurrent().getSalaryCycle().setWorkedToDate(getWorkedToDate());
            getCurrent().getSalaryCycle().setOverTimeFromDate(getWorkedFromDate());
            getCurrent().getSalaryCycle().setOverTimeToDate(getWorkedToDate());
            getCurrent().getSalaryCycle().setExtraDutyFromDate(getWorkedFromDate());
            getCurrent().getSalaryCycle().setExtraDutyToDate(getWorkedToDate());
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
        getStaffSalaryComponantFacade().edit(tmp);
    }

    public StaffSalaryController() {
    }

    public void makeNull() {
        current = null;
        items = null;
    }

    public void clear() {
        current = null;
        salaryFromDate = null;
        salaryToDate = null;
        workedFromDate = null;
        workedToDate = null;
        items = null;

        getStaffController().makeNull();
    }

    public StaffSalary getCurrent() {
        if (current == null) {
            current = new StaffSalary();

        }
        return current;
    }

    public void onEditListener(StaffSalary staffSalary) {
        current = staffSalary;
        System.err.println("1 " + current);

//        if (current != null) {
////            current.setTmpOtNormalSpecial(getHumanResourceBean().calculateOt(getOverTimeFromDate(), getOverTimeToDate(), getCurrent().getStaff()));
//            current.setTransStaffShiftsSalary(getHumanResourceBean().getStaffShiftFromRecordSlaryCalculated(getSalaryFromDate(), getSalaryToDate(), getCurrent().getStaff()));
//            current.setTransStaffShiftsOverTime(getHumanResourceBean().getStaffShiftFromRecordOtCalculated(getOverTimeFromDate(), getOverTimeToDate(), getCurrent().getStaff()));
//            current.setTransStaffShiftsExtraDuty(getHumanResourceBean().getStaffShiftFromRecordExtraDutyCalculated(getExtraDutyFromDate(), getExtraDutyToDate(), getCurrent().getStaff()));
//            current.setTmpExtraDutyCount(getHumanResourceBean().calExtraDuty(getExtraDutyFromDate(), getExtraDutyToDate(), getCurrent().getStaff()));
//        }
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
        ss.setCreater(getSessionController().getLoggedUser());
        ss.setStaffPaysheetComponent(getHumanResourceBean().getBasic(getCurrent().getStaff(), getSalaryToDate()));
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
            double workedWithinTimeFrameVarified = getHumanResourceBean().calculateWorkTimeAndLeave(getWorkedFromDate(), getWorkedToDate(), getCurrent().getStaff());
            Long dateCount = commonFunctions.getDayCount(getWorkedFromDate(), getWorkedToDate());
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
    private void setExtraDuty() {
        StaffSalaryComponant ss = createStaffSalaryComponant(PaysheetComponentType.ExtraDuty);
        ss.setComponantValue(humanResourceBean.calculateExtraWorkTimeValue(getWorkedFromDate(), getWorkedToDate(), getCurrent().getStaff()));
        getHumanResourceBean().setEpf(ss, getHrmVariablesController().getCurrent().getEpfRate(), getHrmVariablesController().getCurrent().getEpfCompanyRate());
        getHumanResourceBean().setEtf(ss, getHrmVariablesController().getCurrent().getEtfRate(), getHrmVariablesController().getCurrent().getEtfCompanyRate());

//        System.err.println("EXTRA " + ss.getStaffPaysheetComponent().getPaysheetComponent().getName());
        getCurrent().getStaffSalaryComponants().add(ss);

    }

    private void setHoliDayAllowance() {
        StaffSalaryComponant ss = createStaffSalaryComponant(PaysheetComponentType.PoyaAllowance);
        if (ss.getStaffPaysheetComponent() != null) {
            long count = getHumanResourceBean().calculateHolidayWork(getWorkedFromDate(), getWorkedToDate(), getCurrent().getStaff());

            double salaryValue = 0;

            if (getCurrent().getStaffSalaryComponants() == null) {
                return;
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

            //Need Calculation Sum
            ss.setComponantValue((salaryValue / finalVariables.getWorkingDaysPerMonth()) * finalVariables.getHoliDayAllowanceMultiply() * count);
            System.err.println("Sal Val " + salaryValue);
            System.err.println("No Pa " + count);
        } else {
            return;
        }

        getHumanResourceBean().setEpf(ss, getHrmVariablesController().getCurrent().getEpfRate(), getHrmVariablesController().getCurrent().getEpfCompanyRate());
        getHumanResourceBean().setEtf(ss, getHrmVariablesController().getCurrent().getEtfRate(), getHrmVariablesController().getCurrent().getEtfCompanyRate());

        System.err.println("NO " + ss.getStaffPaysheetComponent().getPaysheetComponent().getName());
        getCurrent().getStaffSalaryComponants().add(ss);

    }

    private void setDayOffAllowance() {
        System.err.println("DAY OFF Allowance");

        StaffSalaryComponant ss = createStaffSalaryComponant(PaysheetComponentType.DayOffAllowance);
        if (ss.getStaffPaysheetComponent() != null) {
            double count = getHumanResourceBean().calculateDayOffWork(getWorkedFromDate(), getWorkedToDate(), getCurrent().getStaff());
            double salaryValue = 0;

            System.err.println("Size " + getCurrent().getStaffSalaryComponants().size());

            for (StaffSalaryComponant staffSalaryComponant : getCurrent().getStaffSalaryComponants()) {
                if (staffSalaryComponant.getStaffPaysheetComponent() == null
                        || staffSalaryComponant.getStaffPaysheetComponent().getPaysheetComponent() == null) {
                    System.err.println("Continue");
                    continue;
                }
                
                System.err.println("P "+staffSalaryComponant.getStaffPaysheetComponent().getPaysheetComponent().getName()+" : "+staffSalaryComponant.getStaffPaysheetComponent().getPaysheetComponent().isIncludeForAllowance());
                

                if (staffSalaryComponant.getStaffPaysheetComponent().getPaysheetComponent().isIncludeForAllowance()) {
                    System.err.println("INNN " + staffSalaryComponant.getComponantValue());
                    salaryValue += staffSalaryComponant.getComponantValue();
                }
            }

            //Need Calculation Sum
            ss.setComponantValue((salaryValue / finalVariables.getWorkingDaysPerMonth()) * finalVariables.getDayOffAllowanceMultiply() * count);
            System.err.println("Day Off Val " + salaryValue);
            System.err.println("Day Off Count " + count);
        } else {
            return;
        }

        getHumanResourceBean().setEpf(ss, getHrmVariablesController().getCurrent().getEpfRate(), getHrmVariablesController().getCurrent().getEpfCompanyRate());
        getHumanResourceBean().setEtf(ss, getHrmVariablesController().getCurrent().getEtfRate(), getHrmVariablesController().getCurrent().getEtfCompanyRate());

        System.err.println("NO " + ss.getStaffPaysheetComponent().getPaysheetComponent().getName());
        getCurrent().getStaffSalaryComponants().add(ss);

    }

    private void setNoPay() {
        StaffSalaryComponant ss = createStaffSalaryComponant(PaysheetComponentType.No_Pay_Deduction);
        if (ss.getStaffPaysheetComponent() != null) {
            double noPayCount = getHumanResourceBean().fetchStaffLeave(getCurrent().getStaff(), LeaveType.No_Pay, getWorkedFromDate(), getWorkedToDate());
            double salaryValue = 0;

            if (getCurrent().getStaffSalaryComponants() == null) {
                return;
            }

            for (StaffSalaryComponant staffSalaryComponant : getCurrent().getStaffSalaryComponants()) {
                if (staffSalaryComponant.getStaffPaysheetComponent() == null
                        || staffSalaryComponant.getStaffPaysheetComponent().getPaysheetComponent() == null) {
                    continue;
                }

                if (staffSalaryComponant.getStaffPaysheetComponent().getPaysheetComponent().isIncludedForNoPay()) {
                    salaryValue += staffSalaryComponant.getComponantValue();
                }
            }

            //Need Calculation Sum
            ss.setComponantValue((salaryValue / finalVariables.getWorkingDaysPerMonth()) * noPayCount);
            System.err.println("Sal Val " + salaryValue);
//            System.err.println("No Pa " + noPayTime);
        } else {
            return;
        }

        getHumanResourceBean().setEpf(ss, getHrmVariablesController().getCurrent().getEpfRate(), getHrmVariablesController().getCurrent().getEpfCompanyRate());
        getHumanResourceBean().setEtf(ss, getHrmVariablesController().getCurrent().getEtfRate(), getHrmVariablesController().getCurrent().getEtfCompanyRate());

        System.err.println("NO " + ss.getStaffPaysheetComponent().getPaysheetComponent().getName());
        getCurrent().getStaffSalaryComponants().add(ss);

    }

    private boolean dateCheck() {
        if (getSalaryFromDate() == null || getSalaryToDate() == null) {
            UtilityController.addErrorMessage("Please Select Salary Date");
            return true;
        }

        if (getWorkedFromDate() == null || getWorkedToDate() == null) {
            UtilityController.addErrorMessage("Please Select Over time Date");
            return true;
        }

        if (getHumanResourceBean().checkExistingSalary(getSalaryFromDate(), getSalaryToDate(), getCurrent().getStaff())) {
            UtilityController.addErrorMessage("There is Already defined Salary for this salary cycle please edit");
            return true;
        }

        return false;

    }

    public void addSalaryComponent() {

        if (getCurrent().getStaff() != null) {

            setBasic();

            for (StaffPaysheetComponent spc : getHumanResourceBean().fetchStaffPaysheetComponent(getCurrent().getStaff(), getSalaryToDate())) {

                StaffSalaryComponant ss = new StaffSalaryComponant();
                ss.setCreatedAt(new Date());
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
            setExtraDuty();
            setNoPay();
            setHoliDayAllowance();
            setDayOffAllowance();
            setAdjustments();
        }

    }

    public void deleteSalaryComponent(StaffSalary staffSalary) {
        if (getCurrent().getId() == null) {
            return;
        }

        if (staffSalary.getStaff() != null) {

            for (StaffSalaryComponant spc : getHumanResourceBean().fetchStaffSalaryComponent(staffSalary)) {
                spc.setRetireComments("deleted");
                spc.setRetired(true);
                spc.setRetiredAt(new Date());
                spc.setRetirer(getSessionController().getLoggedUser());

                getStaffSalaryComponantFacade().edit(spc);

            }

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

    public void generate() {
        if (getStaffController().getSelectedList() == null) {
            return;
        }

        if (dateCheck()) {
            return;
        }

        items = null;

        for (Staff s : getStaffController().getSelectedList()) {
            setCurrent(getHumanResourceBean().getStaffSalary(s, getSalaryFromDate(), getSalaryToDate()));

            if (getCurrent().getId() == null) {
                addSalaryComponent();
//                save();
            } else {
                // Allready in database
            }

            getItems().add(current);
            current = null;

        }

        //   createStaffSalaryTable();
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
            current = stf;
            save();
            updateStaffShift(stf.getStaff(), getWorkedFromDate(), getWorkedToDate());
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
        String sql = "Select s From StaffSalary s"
                + " where s.retired=false "
                + " and s.salaryCycle.salaryFromDate>=:fd "
                + " and s.salaryCycle.salaryToDate<=:td";

        HashMap hm = new HashMap<>();
        hm.put("fd", getSalaryFromDate());
        hm.put("td", getSalaryToDate());

        items = getStaffSalaryFacade().findBySQL(sql, hm, TemporalType.DATE);
    }

    public StaffSalary fetchStaffSalaryTable(Staff stf) {
        String sql = "Select s From StaffSalary s"
                + " where s.retired=false"
                + " and s.staff=:stf "
                + " and s.salaryCycle.salaryFromDate>=:fd "
                + " and s.salaryCycle.salaryToDate<=:td";

        HashMap hm = new HashMap<>();
        hm.put("stf", stf);
        hm.put("fd", getSalaryFromDate());
        hm.put("td", getSalaryToDate());

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

    public Date getSalaryToDate() {
        return salaryToDate;
    }

    public void setSalaryToDate(Date salaryToDate) {
        this.salaryToDate = salaryToDate;
    }

    public Date getSalaryFromDate() {
        return salaryFromDate;
    }

    public void setSalaryFromDate(Date salaryFromDate) {
        this.salaryFromDate = salaryFromDate;

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
