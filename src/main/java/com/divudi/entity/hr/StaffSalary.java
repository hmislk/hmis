/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity.hr;

import com.divudi.data.hr.PaysheetComponentType;
import com.divudi.entity.Institution;
import com.divudi.entity.Staff;
import com.divudi.entity.WebUser;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
    @ManyToOne
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
    private double basicValue;
    double brValue;
    private double overTimeValue;
    private double extraDutyValue;
    private double noPayValueBasic;
    private double noPayValueAllowance;
    private double merchantileAllowanceValue;
    private double poyaAllowanceValue;
    private double dayOffSleepingDayAllowance;
    private double adjustmentToBasic;
    private double adjustmentToAllowance;
    private double etfSatffValue;
    private double etfCompanyValue;
    private double epfStaffValue;
    private double epfCompanyValue;
    double componentValueAddition;
    double componentValueSubstraction;
    @ManyToOne
    Institution institution;
    @Transient
    private List<StaffSalaryComponant> transStaffSalaryComponantsAddition;
    @Transient
    private List<StaffSalaryComponant> transStaffSalaryComponantsSubtraction;

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    
    
    public double getBrValue() {
        return brValue;
    }

    public void setBrValue(double brValue) {
        this.brValue = brValue;
    }

    public double getComponentValueAddition() {
        return componentValueAddition;
    }

    public void setComponentValueAddition(double componentValueAddition) {
        this.componentValueAddition = componentValueAddition;
    }

    public double getComponentValueSubstraction() {
        return componentValueSubstraction;
    }

    public void setComponentValueSubstraction(double componentValueSubstraction) {
        this.componentValueSubstraction = componentValueSubstraction;
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
                + phValue
                + merchantileAllowanceValue
                + dayOffSleepingDayAllowance
                + adjustmentToBasic;
    }

    public double getTransEpfEtfDiductableSalary() {
        return getTransGrossSalary() - noPayValueBasic;
    }

    public void calculateComponentTotal() {
        basicValue = 0;
        brValue = 0;
        overTimeValue = 0;
        extraDutyValue = 0;
        noPayValueBasic = 0;
        noPayValueAllowance = 0;
        poyaAllowanceValue = 0;
        merchantileAllowanceValue = 0;
        dayOffSleepingDayAllowance = 0;
        adjustmentToBasic = 0;
        adjustmentToAllowance = 0;
        componentValueAddition = 0;
        componentValueSubstraction = 0;

        for (StaffSalaryComponant spc : getStaffSalaryComponants()) {
            PaysheetComponentType paysheetComponentType = null;
            double value = 0;

            if (spc.getStaffPaysheetComponent() != null
                    && spc.getStaffPaysheetComponent().getPaysheetComponent() != null) {
                paysheetComponentType = spc.getStaffPaysheetComponent().getPaysheetComponent().getComponentType();
            }

            if (paysheetComponentType != null) {

                if (paysheetComponentType.getParent(paysheetComponentType) == PaysheetComponentType.addition) {
                    value = spc.getComponantValue();
                } else {
                    value = 0 - spc.getComponantValue();
                }

                switch (paysheetComponentType) {
                    case BasicSalary:
                        basicValue += value;
                        brValue += spc.getStaffPaysheetComponent().getDblValue();
                        break;
                    case OT:
                        overTimeValue += value;
                        break;
                    case ExtraDuty:
                        extraDutyValue += value;
                        break;
                    case No_Pay_Deduction_Basic:
                        noPayValueBasic += value;
                        break;
                    case No_Pay_Deduction_Allowance:
                        noPayValueAllowance += value;
                        break;
                    case MerchantileAllowance:
                        merchantileAllowanceValue += value;
                        break;
                    case PoyaAllowance:
                        poyaAllowanceValue += value;
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
                        if (paysheetComponentType.getParent(paysheetComponentType) == PaysheetComponentType.addition) {
                            componentValueAddition += value;
                        } else {
                            componentValueSubstraction += value;
                        }
                }

            }

        }

    }

    public double getTransTotalAllowance() {
        return componentValueAddition + adjustmentToAllowance + noPayValueAllowance;
    }

    public double getTransTotalDeduction() {
        return componentValueSubstraction + epfStaffValue;
    }

    public double getTransNetSalry() {
        return getTransGrossSalary() + getTransTotalAllowance() + getTransTotalDeduction();
    }

    public SalaryCycle getSalaryCycle() {
//        if (salaryCycle == null) {
//            salaryCycle = new SalaryCycle();
//        }
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

    public double getNoPayValueBasic() {
        return noPayValueBasic;
    }

    public void setNoPayValueBasic(double noPayValueBasic) {
        this.noPayValueBasic = noPayValueBasic;
    }

    public double getNoPayValueAllowance() {
        return noPayValueAllowance;
    }

    public void setNoPayValueAllowance(double noPayValueAllowance) {
        this.noPayValueAllowance = noPayValueAllowance;
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

    public List<StaffSalaryComponant> getTransStaffSalaryComponantsAddition() {
        if (transStaffSalaryComponantsAddition == null) {
            transStaffSalaryComponantsAddition = new ArrayList<>();
        }
        return transStaffSalaryComponantsAddition;
    }

    public void setTransStaffSalaryComponantsAddition(List<StaffSalaryComponant> transStaffSalaryComponantsAddition) {
        this.transStaffSalaryComponantsAddition = transStaffSalaryComponantsAddition;
    }

    public List<StaffSalaryComponant> getTransStaffSalaryComponantsSubtraction() {
        if (transStaffSalaryComponantsSubtraction == null) {
            transStaffSalaryComponantsSubtraction = new ArrayList<>();
        }
        return transStaffSalaryComponantsSubtraction;
    }

    public void setTransStaffSalaryComponantsSubtraction(List<StaffSalaryComponant> transStaffSalaryComponantsSubtraction) {
        this.transStaffSalaryComponantsSubtraction = transStaffSalaryComponantsSubtraction;
    }

    public double getMerchantileAllowanceValue() {
        return merchantileAllowanceValue;
    }

    public void setMerchantileAllowanceValue(double merchantileAllowanceValue) {
        this.merchantileAllowanceValue = merchantileAllowanceValue;
    }

    public double getPoyaAllowanceValue() {
        return poyaAllowanceValue;
    }

    public void setPoyaAllowanceValue(double poyaAllowanceValue) {
        this.poyaAllowanceValue = poyaAllowanceValue;
    }

}
