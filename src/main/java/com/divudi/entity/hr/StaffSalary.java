/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity.hr;

import com.divudi.data.dataStructure.ExtraDutyCount;
import com.divudi.data.dataStructure.OtNormalSpecial;
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

/**
 *
 * @author Buddhika
 */
@Entity
public class StaffSalary implements Serializable {

    @OneToMany(mappedBy = "staffSalary", fetch = FetchType.LAZY)
    private List<StaffSalaryComponant> staffSalaryComponants;

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private double payeeValue;
    private double otValue;
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

    public List<StaffShift> getTransStaffShiftsExtraDuty() {
        return transStaffShiftsExtraDuty;
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

    public double getGrossSalary() {
        double grossSalary = 0.0;
        for (StaffSalaryComponant spc : getStaffSalaryComponants()) {
            grossSalary += spc.getComponantValue();
        }
        return grossSalary;
    }

    public double getNetSalary() {
        double netSalary = getGrossSalary() + getEpfTotal() + getEtfTotal();
        return netSalary;
    }

    public double getEpfTotal() {
        double epfTotal = 0.0;
        for (StaffSalaryComponant spc : getStaffSalaryComponants()) {
            epfTotal += spc.getEpfValue();
        }
        return epfTotal;
    }

    public double getEtfTotal() {
        double etfTotal = 0.0;
        for (StaffSalaryComponant spc : getStaffSalaryComponants()) {
            etfTotal += spc.getEtfValue();
        }
        return etfTotal;
    }

    public double getPayeeValue() {
        return payeeValue;
    }

    public void setPayeeValue(double payeeValue) {
        this.payeeValue = payeeValue;
    }

    public double getEpfCompanyTotal() {
        double epfCompanyTotal = 0.0;
        for (StaffSalaryComponant spc : getStaffSalaryComponants()) {
            epfCompanyTotal += spc.getEpfCompanyValue();
        }
        return epfCompanyTotal;
    }

    public double getEtfCompanyTotal() {
        double etfCompanyTotal = 0.0;
        for (StaffSalaryComponant spc : getStaffSalaryComponants()) {
            etfCompanyTotal += spc.getEtfCompanyValue();
        }
        return etfCompanyTotal;
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

    public double getOtValue() {
        return otValue;
    }

    public void setOtValue(double otValue) {
        this.otValue = otValue;
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

}
