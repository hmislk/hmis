/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.entity;

import com.divudi.data.HistoryType;
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
import javax.persistence.Transient;

/**
 *
 * @author safrin
 */
@Entity
public class AgentHistory implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @ManyToOne
    BillItem billItem;
    @ManyToOne
    Bill bill;
    @ManyToOne
    BillSession billSession;
    
    double balanceBeforeTransaction;
    double balanceAfterTransaction;
    double transactionValue;
    
    
    private double companyTransactionValue;
    private double companyBalanceBefore;
    private double companyBalanceAfter;
    
    
    private double agentTransactionValue;
    private double agentBalanceBefore;
    private double agentBalanceAfter;
    
    
    private double collectingCentertransactionValue;
    @Enumerated(EnumType.STRING)
    HistoryType historyType;
    //Created Properties
    @ManyToOne
    private WebUser creater;
    @ManyToOne
    Institution institution;
    @ManyToOne
    private Department department;
    @ManyToOne
    private Institution agency;
    
    
    
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date createdAt;
    //Retairing properties
    private boolean retired;
    @ManyToOne
    private WebUser retirer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date retiredAt;
    private String retireComments;
    String referenceNumber;
    String comment;
    @Transient
    double transCumilativeTotal;
    
    
    

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public HistoryType getHistoryType() {
        return historyType;
    }

    public void setHistoryType(HistoryType historyType) {
        this.historyType = historyType;
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Integer getReferenceNoInteger() {
        Integer referenceNoInterger = 0;
        if (referenceNumber == null || referenceNumber.isEmpty()) {
            return 0;
        }
        try {
            referenceNoInterger = Integer.parseInt(referenceNumber);
        } catch (Exception e) {
        }

        return referenceNoInterger;

    }

    public double getTransCumilativeTotal() {
        return transCumilativeTotal;
    }

    public void setTransCumilativeTotal(double transCumilativeTotal) {
        this.transCumilativeTotal = transCumilativeTotal;
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
        if (!(object instanceof AgentHistory)) {
            return false;
        }
        AgentHistory other = (AgentHistory) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.AgentHistory[ id=" + id + " ]";
    }

    public double getBalanceAfterTransaction() {
        return balanceAfterTransaction;
    }

    public void setBalanceAfterTransaction(double balanceAfterTransaction) {
        this.balanceAfterTransaction = balanceAfterTransaction;
    }

    public double getCollectingCentertransactionValue() {
        return collectingCentertransactionValue;
    }

    public void setCollectingCentertransactionValue(double collectingCentertransactionValue) {
        this.collectingCentertransactionValue = collectingCentertransactionValue;
    }

    public double getCompanyTransactionValue() {
        return companyTransactionValue;
    }

    public void setCompanyTransactionValue(double companyTransactionValue) {
        this.companyTransactionValue = companyTransactionValue;
    }

    public double getCompanyBalanceBefore() {
        return companyBalanceBefore;
    }

    public void setCompanyBalanceBefore(double companyBalanceBefore) {
        this.companyBalanceBefore = companyBalanceBefore;
    }

    public double getCompanyBalanceAfter() {
        return companyBalanceAfter;
    }

    public void setCompanyBalanceAfter(double companyBalanceAfter) {
        this.companyBalanceAfter = companyBalanceAfter;
    }

    public double getAgentTransactionValue() {
        return agentTransactionValue;
    }

    public void setAgentTransactionValue(double agentTransactionValue) {
        this.agentTransactionValue = agentTransactionValue;
    }

    public double getAgentBalanceBefore() {
        return agentBalanceBefore;
    }

    public void setAgentBalanceBefore(double agentBalanceBefore) {
        this.agentBalanceBefore = agentBalanceBefore;
    }

    public double getAgentBalanceAfter() {
        return agentBalanceAfter;
    }

    public void setAgentBalanceAfter(double agentBalanceAfter) {
        this.agentBalanceAfter = agentBalanceAfter;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Institution getAgency() {
        return agency;
    }

    public void setAgency(Institution agency) {
        this.agency = agency;
    }

    public void setBeforeBallance(double ballance) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

}
