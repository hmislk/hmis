/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.entity;

import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;

/**
 *
 * @author safrin
 */
@Entity
public class BillNumber implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private Long lastBillNumber;
    @ManyToOne
    private Department department;
    @ManyToOne
    private Institution institution;
    @ManyToOne
    private Department toDepartment;
    @Enumerated(EnumType.STRING)
    private BillType billType;
    @Enumerated(EnumType.STRING)
    private BillTypeAtomic billTypeAtomic;
    @Enumerated(EnumType.STRING)
    private BillClassType billClassType;
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;
    private Integer billYear;
    //Retairing properties
    boolean retired;
    @ManyToOne
    WebUser retirer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date retiredAt;
    private String retireComments;
    // Boolean fields for OPD and Inpatient service bill tracking
    private boolean opdAndInpatientServiceBills;
    private boolean opdAndInpatientServiceBatchBills;




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

    public Long getLastBillNumber() {
        return lastBillNumber;
    }

    public void setLastBillNumber(Long lastBillNumber) {
        this.lastBillNumber = lastBillNumber;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Department getToDepartment() {
        return toDepartment;
    }

    public void setToDepartment(Department toDepartment) {
        this.toDepartment = toDepartment;
    }

    public BillType getBillType() {
        return billType;
    }

    public void setBillType(BillType billType) {
        this.billType = billType;
    }

    public BillClassType getBillClassType() {
        return billClassType;
    }

    public void setBillClassType(BillClassType billClassType) {
        this.billClassType = billClassType;
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
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof BillNumber)) {
            return false;
        }
        BillNumber other = (BillNumber) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.core.entity.BillNumber[ id=" + id + " ]";
    }

    public Integer getBillYear() {
        return billYear;
    }

    public void setBillYear(Integer billYear) {
        this.billYear = billYear;
    }

    public String getRetireComments() {
        return retireComments;
    }

    public void setRetireComments(String retireComments) {
        this.retireComments = retireComments;
    }

    public BillTypeAtomic getBillTypeAtomic() {
        return billTypeAtomic;
    }

    public void setBillTypeAtomic(BillTypeAtomic billTypeAtomic) {
        this.billTypeAtomic = billTypeAtomic;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    /**
     * Gets the flag indicating if this bill number is for OPD and Inpatient service bills
     * @return true if for OPD and Inpatient service bills, false otherwise
     */
    public boolean isOpdAndInpatientServiceBills() {
        return opdAndInpatientServiceBills;
    }

    /**
     * Sets the flag indicating if this bill number is for OPD and Inpatient service bills
     * @param opdAndInpatientServiceBills true if for OPD and Inpatient service bills, false otherwise
     */
    public void setOpdAndInpatientServiceBills(boolean opdAndInpatientServiceBills) {
        this.opdAndInpatientServiceBills = opdAndInpatientServiceBills;
    }

    /**
     * Gets the flag indicating if this bill number is for OPD and Inpatient service batch bills
     * @return true if for OPD and Inpatient service batch bills, false otherwise
     */
    public boolean isOpdAndInpatientServiceBatchBills() {
        return opdAndInpatientServiceBatchBills;
    }

    /**
     * Sets the flag indicating if this bill number is for OPD and Inpatient service batch bills
     * @param opdAndInpatientServiceBatchBills true if for OPD and Inpatient service batch bills, false otherwise
     */
    public void setOpdAndInpatientServiceBatchBills(boolean opdAndInpatientServiceBatchBills) {
        this.opdAndInpatientServiceBatchBills = opdAndInpatientServiceBatchBills;
    }

}
