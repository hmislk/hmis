/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.divudi.core.entity;

import com.divudi.core.data.HistoryType;
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
 * @author L C J Samarasekara <lawan.chaamindu1234@gmail.com>
 */
@Entity
public class PatientDepositHistory implements Serializable, RetirableEntity {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    private BillItem billItem;
    @ManyToOne
    private Bill bill;
    @ManyToOne
    private BillSession billSession;
    @ManyToOne
    private PatientDeposit patientDeposit;

    private double balanceBeforeTransaction;
    private double balanceAfterTransaction;
    private double transactionValue;

    @ManyToOne
    private WebUser creater;
    @ManyToOne
    private Institution institution;
    @ManyToOne
    private Department department;
    @ManyToOne
    private Institution site;

    // This patient's aggregate balances (snapshot after transaction)
    private Double patientDepositBalanceForSite;
    private Double patientDepositBalanceForInstitution;
    private Double patientDepositBalanceForAllInstitutions;

    // All patients' aggregate balances (snapshot after transaction)
    private Double allPatientsDepositBalanceForDepartment;
    private Double allPatientsDepositBalanceForSite;
    private Double allPatientsDepositBalanceForInstitution;
    private Double allPatientsDepositBalanceForAllInstitutions;

    @Enumerated(EnumType.ORDINAL)
    private HistoryType historyType;

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date createdAt;
    //Retairing properties
    private boolean retired;
    @ManyToOne
    private WebUser retirer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date retiredAt;
    private String retireComments;
    private String referenceNumber;
    private String comment;

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
        if (!(object instanceof PatientDepositHistory)) {
            return false;
        }
        PatientDepositHistory other = (PatientDepositHistory) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.core.entity.PatientDepositHistory[ id=" + id + " ]";
    }

    public BillItem getBillItem() {
        return billItem;
    }

    public void setBillItem(BillItem billItem) {
        this.billItem = billItem;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public BillSession getBillSession() {
        return billSession;
    }

    public void setBillSession(BillSession billSession) {
        this.billSession = billSession;
    }

    public double getBalanceBeforeTransaction() {
        return balanceBeforeTransaction;
    }

    public void setBalanceBeforeTransaction(double balanceBeforeTransaction) {
        this.balanceBeforeTransaction = balanceBeforeTransaction;
    }

    public double getBalanceAfterTransaction() {
        return balanceAfterTransaction;
    }

    public void setBalanceAfterTransaction(double balanceAfterTransaction) {
        this.balanceAfterTransaction = balanceAfterTransaction;
    }

    public double getTransactionValue() {
        return transactionValue;
    }

    public void setTransactionValue(double transactionValue) {
        this.transactionValue = transactionValue;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public WebUser getCreater() {
        return creater;
    }

    public void setCreater(WebUser creater) {
        this.creater = creater;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
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

    public PatientDeposit getPatientDeposit() {
        return patientDeposit;
    }

    public void setPatientDeposit(PatientDeposit patientDeposit) {
        this.patientDeposit = patientDeposit;
    }

    public HistoryType getHistoryType() {
        return historyType;
    }

    public void setHistoryType(HistoryType historyType) {
        this.historyType = historyType;
    }

    public Institution getSite() {
        return site;
    }

    public void setSite(Institution site) {
        this.site = site;
    }

    public Double getPatientDepositBalanceForSite() {
        return patientDepositBalanceForSite;
    }

    public void setPatientDepositBalanceForSite(Double patientDepositBalanceForSite) {
        this.patientDepositBalanceForSite = patientDepositBalanceForSite;
    }

    public Double getPatientDepositBalanceForInstitution() {
        return patientDepositBalanceForInstitution;
    }

    public void setPatientDepositBalanceForInstitution(Double patientDepositBalanceForInstitution) {
        this.patientDepositBalanceForInstitution = patientDepositBalanceForInstitution;
    }

    public Double getPatientDepositBalanceForAllInstitutions() {
        return patientDepositBalanceForAllInstitutions;
    }

    public void setPatientDepositBalanceForAllInstitutions(Double patientDepositBalanceForAllInstitutions) {
        this.patientDepositBalanceForAllInstitutions = patientDepositBalanceForAllInstitutions;
    }

    public Double getAllPatientsDepositBalanceForDepartment() {
        return allPatientsDepositBalanceForDepartment;
    }

    public void setAllPatientsDepositBalanceForDepartment(Double allPatientsDepositBalanceForDepartment) {
        this.allPatientsDepositBalanceForDepartment = allPatientsDepositBalanceForDepartment;
    }

    public Double getAllPatientsDepositBalanceForSite() {
        return allPatientsDepositBalanceForSite;
    }

    public void setAllPatientsDepositBalanceForSite(Double allPatientsDepositBalanceForSite) {
        this.allPatientsDepositBalanceForSite = allPatientsDepositBalanceForSite;
    }

    public Double getAllPatientsDepositBalanceForInstitution() {
        return allPatientsDepositBalanceForInstitution;
    }

    public void setAllPatientsDepositBalanceForInstitution(Double allPatientsDepositBalanceForInstitution) {
        this.allPatientsDepositBalanceForInstitution = allPatientsDepositBalanceForInstitution;
    }

    public Double getAllPatientsDepositBalanceForAllInstitutions() {
        return allPatientsDepositBalanceForAllInstitutions;
    }

    public void setAllPatientsDepositBalanceForAllInstitutions(Double allPatientsDepositBalanceForAllInstitutions) {
        this.allPatientsDepositBalanceForAllInstitutions = allPatientsDepositBalanceForAllInstitutions;
    }

}
