/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity.hr;

import com.divudi.data.hr.PaysheetComponentType;
import com.divudi.entity.Staff;
import com.divudi.entity.WebUser;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Buddhika
 */
@Entity
@XmlRootElement
public class StaffSalaryComponant implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private double componantValue;
    private double epfValue;
    private double etfValue;
    private double epfCompanyValue;
    private double etfCompanyValue;
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
    @ManyToOne
    StaffPaysheetComponent staffPaysheetComponent;
    @ManyToOne
    private StaffPaysheetComponent staffPaysheetComponentPercentage;
    @ManyToOne
    StaffSalary staffSalary;
    @ManyToOne
    Staff staff;
    @ManyToOne
    SalaryCycle salaryCycle;
    //////////
    @Temporal(javax.persistence.TemporalType.DATE)
    private Date lastEditedAt;
    @ManyToOne
    private WebUser lastEditor;
    @Transient
    String transName;
    private boolean paid;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date paidAt;
    @ManyToOne
    private WebUser paidBy;

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }
    
  
    
    private double roundOff(double d) {
        DecimalFormat newFormat = new DecimalFormat("#.##");
        try {
            return Double.valueOf(newFormat.format(d));
        } catch (Exception e) {
            return 0;
        }
    }

    public String getTransName() {
        return transName;
    }

    public void setTransName(String transName) {
        this.transName = transName;
    }

    public StaffSalaryComponant() {
    }

    public StaffSalaryComponant(double componantValue, PaysheetComponent paysheetComponent) {
        this.componantValue = roundOff(componantValue);
        this.staffPaysheetComponent = new StaffPaysheetComponent();
        this.staffPaysheetComponent.setPaysheetComponent(paysheetComponent);

    }

    public SalaryCycle getSalaryCycle() {
        return salaryCycle;
    }

    public void setSalaryCycle(SalaryCycle salaryCycle) {
        this.salaryCycle = salaryCycle;
    }

    public double getComponantValue() {
        return componantValue;
    }

    public void calComponentValue() {

    }

    public void setComponantValue(double componantValue) {
        if (getStaffPaysheetComponent() != null
                && getStaffPaysheetComponent().getPaysheetComponent() != null
                && getStaffPaysheetComponent().getPaysheetComponent().getComponentType() != null
                && getStaffPaysheetComponent().getPaysheetComponent().getComponentType().is(PaysheetComponentType.subtraction)) {

            this.componantValue = roundOff(0 - componantValue);
        } else {
            this.componantValue = roundOff(componantValue);
        }
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

    public StaffPaysheetComponent getStaffPaysheetComponent() {
        return staffPaysheetComponent;
    }

    public void setStaffPaysheetComponent(StaffPaysheetComponent staffPaysheetComponent) {
        this.staffPaysheetComponent = staffPaysheetComponent;
    }

    public StaffSalary getStaffSalary() {
        return staffSalary;
    }

    public void setStaffSalary(StaffSalary staffSalary) {
        this.staffSalary = staffSalary;
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

        if (!(object instanceof StaffSalaryComponant)) {
            return false;
        }
        StaffSalaryComponant other = (StaffSalaryComponant) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.hr.StaffSalaryComponant[ id=" + id + " ]";
    }

    public Date getLastEditedAt() {
        return lastEditedAt;
    }

    public void setLastEditedAt(Date lastEditedAt) {
        this.lastEditedAt = lastEditedAt;
    }

    public WebUser getLastEditor() {
        return lastEditor;
    }

    public void setLastEditor(WebUser lastEditor) {
        this.lastEditor = lastEditor;
    }

    public double getEpfValue() {
        return 0 - epfValue;
    }

    public void setEpfValue(double epfValue) {
        this.epfValue = roundOff(epfValue);
    }

    public double getEtfValue() {
        return 0 - etfValue;
    }

    public void setEtfValue(double etfValue) {
        this.etfValue = roundOff(etfValue);
    }

    public double getEpfCompanyValue() {
        return 0 - epfCompanyValue;
    }

    public void setEpfCompanyValue(double epfCompanyValue) {
        this.epfCompanyValue = roundOff(epfCompanyValue);
    }

    public double getEtfCompanyValue() {
        return 0 - etfCompanyValue;
    }

    public void setEtfCompanyValue(double etfCompanyValue) {
        this.etfCompanyValue = roundOff(etfCompanyValue);
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    public Date getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Date paidAt) {
        this.paidAt = paidAt;
    }

    public WebUser getPaidBy() {
        return paidBy;
    }

    public void setPaidBy(WebUser paidBy) {
        this.paidBy = paidBy;
    }

    public StaffPaysheetComponent getStaffPaysheetComponentPercentage() {
        return staffPaysheetComponentPercentage;
    }

    public void setStaffPaysheetComponentPercentage(StaffPaysheetComponent staffPaysheetComponentPercentage) {
        this.staffPaysheetComponentPercentage = staffPaysheetComponentPercentage;
    }
    
    

}
