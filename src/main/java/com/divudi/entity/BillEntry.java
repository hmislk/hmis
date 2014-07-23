/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity;

import java.io.Serializable;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;

/**
 *
 * @author Buddhika
 */
@Entity
public class BillEntry implements Serializable {

    static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;
    @Transient
    BillItem billItem;
    @Transient
    List<BillComponent> lstBillComponents;
    @Transient
    List<BillFee> lstBillFees;
    @Transient
    List<BillSession> lstBillSessions;
    @Transient
    List<BillItem> lsyBillItems;
    Double totDiscount;
    Double totCash;
    Double totCredit;
    Double totCreditCard;
    @Transient
    Bill bill;

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public BillItem getBillItem() {
        return billItem;
    }

    public void setBillItem(BillItem billItem) {
        this.billItem = billItem;
    }

    public List<BillComponent> getLstBillComponents() {
        return lstBillComponents;
    }

    public void setLstBillComponents(List<BillComponent> lstBillComponents) {
        this.lstBillComponents = lstBillComponents;
    }

    public List<BillFee> getLstBillFees() {
        return lstBillFees;
    }

    public void setLstBillFees(List<BillFee> lstBillFees) {
        this.lstBillFees = lstBillFees;
    }

    public List<BillSession> getLstBillSessions() {
        return lstBillSessions;
    }

    public void setLstBillSessions(List<BillSession> lstBillSessions) {
        this.lstBillSessions = lstBillSessions;
    }

    public List<BillItem> getLsyBillItems() {
        return lsyBillItems;
    }

    public void setLsyBillItems(List<BillItem> lsyBillItems) {
        this.lsyBillItems = lsyBillItems;
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

        if (!(object instanceof BillEntry)) {
            return false;
        }
        BillEntry other = (BillEntry) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.BillEntry[ id=" + id + " ]";
    }

    public Double getTotDiscount() {
        return totDiscount;
    }

    public void setTotDiscount(Double totDiscount) {
        this.totDiscount = totDiscount;
    }

    public Double getTotCash() {
        return totCash;
    }

    public void setTotCash(Double totCash) {
        this.totCash = totCash;
    }

    public Double getTotCredit() {
        return totCredit;
    }

    public void setTotCredit(Double totCredit) {
        this.totCredit = totCredit;
    }

    public Double getTotCreditCard() {
        return totCreditCard;
    }

    public void setTotCreditCard(Double totCreditCard) {
        this.totCreditCard = totCreditCard;
    }
}
