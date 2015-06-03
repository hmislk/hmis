/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity;

import com.divudi.bean.common.BankController;
import com.divudi.data.PaymentMethod;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;

/**
 *
 * @author Buddhika
 */
@Entity
public class Payment implements Serializable {

    static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    @ManyToOne
    Bill bill;

    @Temporal(javax.persistence.TemporalType.DATE)
    Date writtenAt;
    @Temporal(javax.persistence.TemporalType.DATE)
    Date toRealizeAt;

    @Enumerated(EnumType.STRING)
    PaymentMethod paymentMethod;

    //Realization Properties
    boolean realized;
    @Temporal(javax.persistence.TemporalType.DATE)
    Date realizedAt;
    @ManyToOne
    WebUser realiazer;
    @Lob
    String realizeComments;

    @ManyToOne
    Institution bank;

    @Lob
    String comments;

    //Created Properties
    @ManyToOne
    WebUser creater;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date createdAt;
    //Retairing properties
    boolean retired;
    @ManyToOne
    WebUser retirer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date retiredAt;
    String retireComments;
    //

    //paymentMethord Details
    private String chequeRefNo;
    @Temporal(javax.persistence.TemporalType.DATE)
    private Date chequeDate;
    private String creditCardRefNo;
    
    double paidValue;
    
    @ManyToOne
    Institution institution;
    @ManyToOne
    Department department;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public Date getWrittenAt() {
        return writtenAt;
    }

    public void setWrittenAt(Date writtenAt) {
        this.writtenAt = writtenAt;
    }

    public Date getToRealizeAt() {
        return toRealizeAt;
    }

    public void setToRealizeAt(Date toRealizeAt) {
        this.toRealizeAt = toRealizeAt;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public boolean isRealized() {
        return realized;
    }

    public void setRealized(boolean realized) {
        this.realized = realized;
    }

    public Date getRealizedAt() {
        return realizedAt;
    }

    public void setRealizedAt(Date realizedAt) {
        this.realizedAt = realizedAt;
    }

    public WebUser getRealiazer() {
        return realiazer;
    }

    public void setRealiazer(WebUser realiazer) {
        this.realiazer = realiazer;
    }

    public String getRealizeComments() {
        return realizeComments;
    }

    public void setRealizeComments(String realizeComments) {
        this.realizeComments = realizeComments;
    }

    public Institution getBank() {
        return bank;
    }

    public void setBank(Institution bank) {
        this.bank = bank;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
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

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {

        if (!(object instanceof Payment)) {
            return false;
        }
        Payment other = (Payment) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.Payment[ id=" + id + " ]";
    }

    public String getChequeRefNo() {
        return chequeRefNo;
    }

    public void setChequeRefNo(String chequeRefNo) {
        this.chequeRefNo = chequeRefNo;
    }

    public Date getChequeDate() {
        return chequeDate;
    }

    public void setChequeDate(Date chequeDate) {
        this.chequeDate = chequeDate;
    }

    public String getCreditCardRefNo() {
        return creditCardRefNo;
    }

    public void setCreditCardRefNo(String creditCardRefNo) {
        this.creditCardRefNo = creditCardRefNo;
    }

    public double getPaidValue() {
        return paidValue;
    }

    public void setPaidValue(double paidValue) {
        this.paidValue = paidValue;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

}
