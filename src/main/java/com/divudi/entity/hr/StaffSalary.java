/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity.hr;

import com.divudi.data.dataStructure.ExtraDutyCount;
import com.divudi.data.dataStructure.OtNormalSpecial;
import com.divudi.data.hr.PaysheetComponentType;
import com.divudi.entity.Staff;
import com.divudi.entity.WebUser;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 *
 * @author Buddhika
 */
@Entity
@XmlRootElement
public class StaffSalary implements Serializable {

    @OneToMany(mappedBy = "staffSalary", fetch = FetchType.LAZY)
    private List<StaffSalaryComponant> staffSalaryComponants;

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private double payeeValue;
//    private double otValue;
    private double phValue;
    @ManyToOne(cascade = CascadeType.ALL)
    private SalaryCycle salaryCycle;
    @ManyToOne
    private Staff staff;
    @Transient
    private boolean exist;
    //Created Properties
    @ManyToOne
    private WebUser creater;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date createdAt;
    //Retairing properties
    private boolean retired;
    @ManyToOne
    private WebUser retirer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date retiredAt;
    private String retireComments;
    @Transient
    private OtNormalSpecial tmpOtNormalSpecial;
    @Transient
    private List<ExtraDutyCount> tmpExtraDutyCount;
    @Transient
    List<StaffShift> transStaffShiftsSalary;
    @Transient
    List<StaffShift> transStaffShiftsOverTime;
    @Transient
    List<StaffShift> transStaffShiftsExtraDuty;
    private double basicValue;
    private double overTimeValue;
    private double extraDutyValue;
    private double noPayValue;
    private double holidayAllowanceValue;
    private double dayOffSleepingDayAllowance;
    private double adjustmentToBasic;
    private double adjustmentToAllowance;
    private double etfSatffValue;
    private double etfCompanyValue;
    private double epfStaffValue;
    private double epfCompanyValue;
    double componentValue;

    public List<StaffShift> getTransStaffShiftsExtraDuty() {
        return transStaffShiftsExtraDuty;
    }

    public double getComponentValue() {
        return componentValue;
    }

    public void setComponentValue(double componentValue) {
        this.componentValue = componentValue;
    }

    public void setTransStaffShiftsExtraDuty(List<StaffShift> transStaffShiftsExtraDuty) {
        this.transStaffShiftsExtraDuty = transStaffShiftsExtraDuty;
    }

    public List<StaffShift> getTransStaffShiftsSalary() {
        return transStaffShiftsSalary;
    }

    public void setTransStaffShiftsSalary(List<StaffShift> transStaffShiftsSalary) {
        this.transStaffShiftsSalary = transStaffShiftsSalary;
    }

    public List<StaffShift> getTransStaffShiftsOverTime() {
        return transStaffShiftsOverTime;
    }

    public void setTransStaffShiftsOverTime(List<StaffShift> transStaffShiftsOverTime) {
        this.transStaffShiftsOverTime = transStaffShiftsOverTime;
    }

    public Date getFromDate() {
        return getSalaryCycle() != null ? getSalaryCycle().getSalaryFromDate() : null;
    }

    public Date getToDate() {
        return getSalaryCycle() != null ? getSalaryCycle().getSalaryToDate() : null;
    }

    @XmlTransient
    public List<StaffSalaryComponant> getStaffSalaryComponants() {
        if (staffSalaryComponants == null) {
            staffSalaryComponants = new ArrayList<>();
        }
        return staffSalaryComponants;
    }

    public void setStaffSalaryComponants(List<StaffSalaryComponant> staffSalaryComponants) {
        this.staffSalaryComponants = staffSalaryComponants;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {

        if (!(object instanceof StaffSalary)) {
            return false;
        }
        StaffSalary other = (StaffSalary) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.hr.StaffSalary[ id=" + id + " ]";
    }

    public double getPayeeValue() {
        return payeeValue;
    }

    public void setPayeeValue(double payeeValue) {
        this.payeeValue = payeeValue;
    }

    public void calcualteEpfAndEtf() {
        etfCompanyValue = 0.0;
        etfSatffValue = 0;
        epfCompanyValue = 0;
        epfStaffValue = 0;
        for (StaffSalaryComponant spc : getStaffSalaryComponants()) {
            etfCompanyValue += spc.getEtfCompanyValue();
            etfSatffValue += spc.getEtfValue();
            epfCompanyValue += spc.getEpfCompanyValue();
            epfStaffValue += spc.getEpfValue();
        }

    }

    public double getTransGrossSalary() {
        return basicValue
                + overTimeValue
                + extraDutyValue
                + noPayValue
                + holidayAllowanceValue
                + dayOffSleepingDayAllowance
                + adjustmentToBasic
                + adjustmentToAllowance
                + componentValue;
    }

    public void calculateComponentTotal() {
        basicValue = 0;
        overTimeValue = 0;
        extraDutyValue = 0;
        noPayValue = 0;
        holidayAllowanceValue = 0;
        dayOffSleepingDayAllowance = 0;
        adjustmentToBasic = 0;
        adjustmentToAllowance = 0;
        componentValue = 0;

        for (StaffSalaryComponant spc : getStaffSalaryComponants()) {
            PaysheetComponentType paysheetComponentType = null;
            double value = 0;

            if (spc.getStaffPaysheetComponent() != null
                    && spc.getStaffPaysheetComponent().getPaysheetComponent() != null) {
                paysheetComponentType = spc.getStaffPaysheetComponent().getPaysheetComponent().getComponentType();
            }

            if (paysheetComponentType == null) {
                componentValue += spc.getComponantValue();
            } else {

                if (paysheetComponentType.is(PaysheetComponentType.addition)) {
                    value = spc.getComponantValue();
                } else {
                    value = 0 - spc.getComponantValue();
                }

                switch (paysheetComponentType) {
                    case BasicSalary:
                        basicValue += value;
                        break;
                    case OT:
                        overTimeValue += value;
                        break;
                    case ExtraDuty:
                        extraDutyValue += value;
                        break;
                    case No_Pay_Deduction:
                        noPayValue += value;
                        break;
                    case HolidayAllowance:
                        holidayAllowanceValue += value;
                        break;
                    case DayOffAllowance:
                        dayOffSleepingDayAllowance += value;
                        break;
                    case AdjustmentAllowanceAdd:
                    case AdjustmentAllowanceSub:
                        adjustmentToAllowance += value;
                        break;
                    case AdjustmentBasicAdd:
                    case AdjustmentBasicSub:
                        adjustmentToBasic += value;
                        break;
                    default:
                        componentValue += value;
                }

            }

        }

    }

    public SalaryCycle getSalaryCycle() {
        if (salaryCycle == null) {
            salaryCycle = new SalaryCycle();
        }
        return salaryCycle;
    }

    public void setSalaryCycle(SalaryCycle salaryCycle) {

        this.salaryCycle = salaryCycle;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public WebUser getCreater() {
        return creater;
    }

    public void setCreater(WebUser creater) {
        this.creater = creater;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isRetired() {
        return retired;
    }

    public void setRetired(boolean retired) {
        this.retired = retired;
    }

    public WebUser getRetirer() {
        return retirer;
    }

    public void setRetirer(WebUser retirer) {
        this.retirer = retirer;
    }

    public Date getRetiredAt() {
        return retiredAt;
    }

    public void setRetiredAt(Date retiredAt) {
        this.retiredAt = retiredAt;
    }

    public String getRetireComments() {
        return retireComments;
    }

    public void setRetireComments(String retireComments) {
        this.retireComments = retireComments;
    }

    public double getPhValue() {
        return phValue;
    }

    public void setPhValue(double phValue) {
        this.phValue = phValue;
    }

    public boolean isExist() {
        return exist;
    }

    public boolean getExist() {
        return exist;
    }

    public void setExist(boolean exist) {
        this.exist = exist;
    }

    public OtNormalSpecial getTmpOtNormalSpecial() {
        return tmpOtNormalSpecial;
    }

    public void setTmpOtNormalSpecial(OtNormalSpecial tmpOtNormalSpecial) {
        this.tmpOtNormalSpecial = tmpOtNormalSpecial;
    }

    public List<ExtraDutyCount> getTmpExtraDutyCount() {
        return tmpExtraDutyCount;
    }

    public void setTmpExtraDutyCount(List<ExtraDutyCount> tmpExtraDutyCount) {
        this.tmpExtraDutyCount = tmpExtraDutyCount;
    }

    public double getBasicValue() {
        return basicValue;
    }

    public void setBasicValue(double basicValue) {
        this.basicValue = basicValue;
    }

    public double getOverTimeValue() {
        return overTimeValue;
    }

    public void setOverTimeValue(double overTimeValue) {
        this.overTimeValue = overTimeValue;
    }

    public double getExtraDutyValue() {
        return extraDutyValue;
    }

    public void setExtraDutyValue(double extraDutyValue) {
        this.extraDutyValue = extraDutyValue;
    }

    public double getNoPayValue() {
        return noPayValue;
    }

    public void setNoPayValue(double noPayValue) {
        this.noPayValue = noPayValue;
    }

    public double getHolidayAllowanceValue() {
        return holidayAllowanceValue;
    }

    public void setHolidayAllowanceValue(double holidayAllowanceValue) {
        this.holidayAllowanceValue = holidayAllowanceValue;
    }

    public double getDayOffSleepingDayAllowance() {
        return dayOffSleepingDayAllowance;
    }

    public void setDayOffSleepingDayAllowance(double dayOffSleepingDayAllowance) {
        this.dayOffSleepingDayAllowance = dayOffSleepingDayAllowance;
    }

    public double getAdjustmentToBasic() {
        return adjustmentToBasic;
    }

    public void setAdjustmentToBasic(double adjustmentToBasic) {
        this.adjustmentToBasic = adjustmentToBasic;
    }

    public double getAdjustmentToAllowance() {
        return adjustmentToAllowance;
    }

    public void setAdjustmentToAllowance(double adjustmentToAllowance) {
        this.adjustmentToAllowance = adjustmentToAllowance;
    }

    public double getEtfSatffValue() {
        return etfSatffValue;
    }

    public void setEtfSatffValue(double etfSatffValue) {
        this.etfSatffValue = etfSatffValue;
    }

    public double getEtfCompanyValue() {
        return etfCompanyValue;
    }

    public void setEtfCompanyValue(double etfCompanyValue) {
        this.etfCompanyValue = etfCompanyValue;
    }

    public double getEpfStaffValue() {
        return epfStaffValue;
    }

    public void setEpfStaffValue(double epfStaffValue) {
        this.epfStaffValue = epfStaffValue;
    }

    public double getEpfCompanyValue() {
        return epfCompanyValue;
    }

    public void setEpfCompanyValue(double epfCompanyValue) {
        this.epfCompanyValue = epfCompanyValue;
    }

}
