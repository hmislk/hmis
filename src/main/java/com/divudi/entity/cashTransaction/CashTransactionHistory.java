/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity.cashTransaction;

import com.divudi.data.HistoryType;
import com.divudi.entity.WebUser;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;

/**
 *
 * @author safrin
 */
@Entity
public class CashTransactionHistory implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Temporal(javax.persistence.TemporalType.DATE)
    private Date cashAt;

    @OneToOne
    private CashTransaction cashTransaction;

    private double cashBallance;
    private double creditCardBallance;
    private double chequeBallance;
    private double slipBallance;

    @Enumerated(EnumType.STRING)
    private HistoryType historyType;

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date fromDate;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date toDate;
    private long hxYear;
    private int hxMonth;
    private int hxDate;
    private int hxWeek;

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
        if (!(object instanceof CashTransactionHistory)) {
            return false;
        }
        CashTransactionHistory other = (CashTransactionHistory) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.CashTransactionHistory[ id=" + id + " ]";
    }

    public Date getCashAt() {
        return cashAt;
    }

    public void setCashAt(Date cashAt) {
        this.cashAt = cashAt;
    }

    public CashTransaction getCashTransaction() {
        return cashTransaction;
    }

    public void setCashTransaction(CashTransaction cashTransaction) {
        this.cashTransaction = cashTransaction;
    }

    public HistoryType getHistoryType() {
        return historyType;
    }

    public void setHistoryType(HistoryType historyType) {
        this.historyType = historyType;
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

    public long getHxYear() {
        return hxYear;
    }

    public void setHxYear(long hxYear) {
        this.hxYear = hxYear;
    }

    public int getHxMonth() {
        return hxMonth;
    }

    public void setHxMonth(int hxMonth) {
        this.hxMonth = hxMonth;
    }

    public int getHxDate() {
        return hxDate;
    }

    public void setHxDate(int hxDate) {
        this.hxDate = hxDate;
    }

    public int getHxWeek() {
        return hxWeek;
    }

    public void setHxWeek(int hxWeek) {
        this.hxWeek = hxWeek;
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

    public double getCashBallance() {
        return cashBallance;
    }

    public void setCashBallance(double cashBallance) {
        this.cashBallance = cashBallance;
    }

    public double getCreditCardBallance() {
        return creditCardBallance;
    }

    public void setCreditCardBallance(double creditCardBallance) {
        this.creditCardBallance = creditCardBallance;
    }

    public double getChequeBallance() {
        return chequeBallance;
    }

    public void setChequeBallance(double chequeBallance) {
        this.chequeBallance = chequeBallance;
    }

    public double getSlipBallance() {
        return slipBallance;
    }

    public void setSlipBallance(double slipBallance) {
        this.slipBallance = slipBallance;
    }

}
