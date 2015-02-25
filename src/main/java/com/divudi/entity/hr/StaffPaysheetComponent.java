/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity.hr;

import com.divudi.entity.Institution;
import com.divudi.entity.Staff;
import com.divudi.entity.WebUser;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author safrin
 */
@Entity
@XmlRootElement
public class StaffPaysheetComponent implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @ManyToOne
    private PaysheetComponent paysheetComponent;
    @ManyToOne
    private Staff staff;
    private double createdValue;
    private double modifiedValue;    
    private double staffPaySheetComponentValue;
    double dblValue;
    @Temporal(TemporalType.DATE)
    private Date fromDate;
    @Temporal(TemporalType.DATE)
    private Date toDate;
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
    //LastEdited
    @ManyToOne
    private WebUser lastEditor;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date lastEditedAt;
    @Transient
    private boolean exist;
    @ManyToOne
    private Institution bankBranch;
    private String loanNo;
    String accountNo;
    private double loanFullAmount;
    private String comment;
    boolean completed;
    private boolean sheduleForPaid;
    String chequeNumber;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date chequeDate;
    @ManyToOne
    Institution chequeBank;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date chequePaidDate;
    @ManyToOne
    WebUser chequePaidBy;
    @Temporal(javax.persistence.TemporalType.DATE)
    private Date dateAffectFrom;
    double numberOfInstallment;
//    boolean sentNetSalaryToBaBank;
    
    

    public double getNumberOfInstallment() {
        return numberOfInstallment;
    }

    public void setNumberOfInstallment(double numberOfInstallment) {
        this.numberOfInstallment = numberOfInstallment;
    }

//    public boolean isSentNetSalaryToBaBank() {
//        return sentNetSalaryToBaBank;
//    }
//
//    public void setSentNetSalaryToBaBank(boolean sentNetSalaryToBaBank) {
//        this.sentNetSalaryToBaBank = sentNetSalaryToBaBank;
//    }
    
    
    
    
    
    

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
    
    

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    
    
    public double getDblValue() {
        return dblValue;
    }

    public void setDblValue(double dblValue) {
        this.dblValue = dblValue;
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

        if (!(object instanceof StaffPaysheetComponent)) {
            return false;
        }
        StaffPaysheetComponent other = (StaffPaysheetComponent) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.hr.StaffPaysheetComponent[ id=" + id + " ]";
    }

    public PaysheetComponent getPaysheetComponent() {
        return paysheetComponent;
    }

    public void setPaysheetComponent(PaysheetComponent paysheetComponent) {
        this.paysheetComponent = paysheetComponent;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public double getStaffPaySheetComponentValue() {       
        return staffPaySheetComponentValue;
    }

    public void setStaffPaySheetComponentValue(double staffPaySheetComponentValue) {
        this.staffPaySheetComponentValue = staffPaySheetComponentValue;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
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

    public double getCreatedValue() {
        return createdValue;
    }

    public void setCreatedValue(double createdValue) {
        this.createdValue = createdValue;
    }

    public double getModifiedValue() {
        return modifiedValue;
    }

    public void setModifiedValue(double modifiedValue) {
        this.modifiedValue = modifiedValue;
    }

    public WebUser getLastEditor() {
        return lastEditor;
    }

    public void setLastEditor(WebUser lastEditor) {
        this.lastEditor = lastEditor;
    }

    public Date getLastEditedAt() {
        return lastEditedAt;
    }

    public void setLastEditedAt(Date lastEditedAt) {
        this.lastEditedAt = lastEditedAt;
    }

    public boolean isExist() {
        return exist;
    }

    public void setExist(boolean exist) {
        this.exist = exist;
    }

    public boolean getExist() {
        return exist;
    }

    public Institution getBankBranch() {
        return bankBranch;
    }

    public void setBankBranch(Institution bankBranch) {
        this.bankBranch = bankBranch;
    }

    public String getLoanNo() {
        return loanNo;
    }

    public void setLoanNo(String loanNo) {
        this.loanNo = loanNo;
    }

    public double getLoanFullAmount() {
        return loanFullAmount;
    }

    public void setLoanFullAmount(double loanFullAmount) {
        this.loanFullAmount = loanFullAmount;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Date getDateAffectFrom() {
        return dateAffectFrom;
    }

    public void setDateAffectFrom(Date dateAffectFrom) {
        this.dateAffectFrom = dateAffectFrom;
    }

    public boolean isSheduleForPaid() {
        return sheduleForPaid;
    }

    public void setSheduleForPaid(boolean sheduleForPaid) {
        this.sheduleForPaid = sheduleForPaid;
    }

    public String getChequeNumber() {
        return chequeNumber;
    }

    public void setChequeNumber(String chequeNumber) {
        this.chequeNumber = chequeNumber;
    }

    public Date getChequeDate() {
        return chequeDate;
    }

    public void setChequeDate(Date chequeDate) {
        this.chequeDate = chequeDate;
    }

    public Institution getChequeBank() {
        return chequeBank;
    }

    public void setChequeBank(Institution chequeBank) {
        this.chequeBank = chequeBank;
    }

    public Date getChequePaidDate() {
        return chequePaidDate;
    }

    public void setChequePaidDate(Date chequePaidDate) {
        this.chequePaidDate = chequePaidDate;
    }

    public WebUser getChequePaidBy() {
        return chequePaidBy;
    }

    public void setChequePaidBy(WebUser chequePaidBy) {
        this.chequePaidBy = chequePaidBy;
    }

   
}
