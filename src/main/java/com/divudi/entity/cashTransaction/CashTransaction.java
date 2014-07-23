/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity.cashTransaction;

import com.divudi.data.InOutType;
import com.divudi.entity.Bill;
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
public class CashTransaction implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Enumerated(EnumType.STRING)
    private InOutType inOutType;
    @OneToOne(mappedBy = "cashTransaction")
    private Bill bill;
    @ManyToOne
    private Drawer drawer;
    @OneToOne(mappedBy = "cashTransaction")
    private CashTransactionHistory cashTransactionHistory;
    /////////////
    private double tenderedAmount;
    private double ballanceAmount;
    /////////////////
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
///////////////////
    private Double qty1;
    private Double qty2;
    private Double qty5;
    private Double qty10;
    private Double qty20;
    private Double qty50;
    private Double qty100;
    private Double qty200;
    private Double qty500;
    private Double qty1000;
    private Double qty2000;
    private Double qty5000;
    private Double qty10000;
    Double creditCardValue;
    Double chequeValue;
    Double slipValue;
    Double cashValue;

    public Double getCashValue() {
        return cashValue;
    }

    public void setCashValue(Double cashValue) {
        this.cashValue = cashValue;
    }

    public Double getCreditCardValue() {
        return creditCardValue;
    }

    public void setCreditCardValue(Double creditCardValue) {
        this.creditCardValue = creditCardValue;
    }

    public Double getChequeValue() {
        return chequeValue;
    }

    public void setChequeValue(Double chequeValue) {
        this.chequeValue = chequeValue;
    }

    public Double getSlipValue() {
        return slipValue;
    }

    public void setSlipValue(Double slipValue) {
        this.slipValue = slipValue;
    }

    public Drawer getDrawer() {
        return drawer;
    }

    public void setDrawer(Drawer drawer) {
        this.drawer = drawer;
    }

    public void copyQty(CashTransaction cashTransaction) {
        qty1 = cashTransaction.getQty1();
        qty10 = cashTransaction.getQty10();
        qty100 = cashTransaction.getQty100();
        qty1000 = cashTransaction.getQty1000();
        qty2 = cashTransaction.getQty2();
        qty20 = cashTransaction.getQty20();
        qty200 = cashTransaction.getQty200();
        qty2000 = cashTransaction.getQty2000();
        qty5 = cashTransaction.getQty5();
        qty50 = cashTransaction.getQty50();
        qty500 = cashTransaction.getQty500();
        qty5000 = cashTransaction.getQty5000();
        cashValue = cashTransaction.getCashValue();
        creditCardValue = cashTransaction.getCreditCardValue();
        chequeValue = cashTransaction.getChequeValue();
        slipValue = cashTransaction.getSlipValue();

    }

    public void invertQty(CashTransaction cashTransaction) {
        if (cashTransaction.getQty1() != null) {
            qty1 = 0 - cashTransaction.getQty1();
        }
        if (cashTransaction.getQty10() != null) {
            qty10 = 0 - cashTransaction.getQty10();
        }
        if (cashTransaction.getQty100() != null) {
            qty100 = 0 - cashTransaction.getQty100();
        }
        if (cashTransaction.getQty1000() != null) {
            qty1000 = cashTransaction.getQty1000();
        }

        if (cashTransaction.getQty2() != null) {
            qty2 = 0 - cashTransaction.getQty2();
        }
        if (cashTransaction.getQty20() != null) {
            qty20 = 0 - cashTransaction.getQty20();
        }
        if (cashTransaction.getQty200() != null) {
            qty200 = 0 - cashTransaction.getQty200();
        }
        if (cashTransaction.getQty2000() != null) {
            qty2000 = cashTransaction.getQty2000();
        }

        if (cashTransaction.getQty5() != null) {
            qty5 = 0 - cashTransaction.getQty5();
        }
        if (cashTransaction.getQty50() != null) {
            qty50 = 0 - cashTransaction.getQty50();
        }
        if (cashTransaction.getQty500() != null) {
            qty500 = 0 - cashTransaction.getQty500();
        }
        if (cashTransaction.getQty5000() != null) {
            qty5000 = cashTransaction.getQty5000();
        }

        if (cashTransaction.getCashValue() != null) {
            cashValue = 0 - cashTransaction.getCashValue();
        }
        if (cashTransaction.getCreditCardValue() != null) {
            creditCardValue = 0 - cashTransaction.getCreditCardValue();
        }
        if (cashTransaction.getChequeValue() != null) {
            chequeValue = 0 - cashTransaction.getChequeValue();
        }
        if (cashTransaction.getSlipValue() != null) {
            slipValue = 0 - cashTransaction.getSlipValue();
        }

    }

    public Double getQty1() {
        return qty1;
    }

    public void setQty1(Double qty1) {
        this.qty1 = qty1;
    }

    public Double getQty2() {
        return qty2;
    }

    public void setQty2(Double qty2) {
        this.qty2 = qty2;
    }

    public Double getQty5() {
        return qty5;
    }

    public void setQty5(Double qty5) {
        this.qty5 = qty5;
    }

    public Double getQty10() {
        return qty10;
    }

    public void setQty10(Double qty10) {
        this.qty10 = qty10;
    }

    public Double getQty20() {
        return qty20;
    }

    public void setQty20(Double qty20) {
        this.qty20 = qty20;
    }

    public Double getQty50() {
        return qty50;
    }

    public void setQty50(Double qty50) {
        this.qty50 = qty50;
    }

    public Double getQty100() {
        return qty100;
    }

    public void setQty100(Double qty100) {
        this.qty100 = qty100;
    }

    public Double getQty200() {
        return qty200;
    }

    public void setQty200(Double qty200) {
        this.qty200 = qty200;
    }

    public Double getQty500() {
        return qty500;
    }

    public void setQty500(Double qty500) {
        this.qty500 = qty500;
    }

    public Double getQty1000() {
        return qty1000;
    }

    public void setQty1000(Double qty1000) {
        this.qty1000 = qty1000;
    }

    public Double getQty2000() {
        return qty2000;
    }

    public void setQty2000(Double qty2000) {
        this.qty2000 = qty2000;
    }

    public Double getQty5000() {
        return qty5000;
    }

    public void setQty5000(Double qty5000) {
        this.qty5000 = qty5000;
    }

    public Double getQty10000() {
        return qty10000;
    }

    public void setQty10000(Double qty10000) {
        this.qty10000 = qty10000;
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
        if (!(object instanceof CashTransaction)) {
            return false;
        }
        CashTransaction other = (CashTransaction) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.CashTransaction[ id=" + id + " ]";
    }

    public double getTenderedAmount() {
        return tenderedAmount;
    }

    public void setTenderedAmount(double tenderedAmount) {
        this.tenderedAmount = tenderedAmount;
    }

    public double getBallanceAmount() {
        return ballanceAmount;
    }

    public void setBallanceAmount(double ballanceAmount) {
        this.ballanceAmount = ballanceAmount;
    }

    public InOutType getInOutType() {
        return inOutType;
    }

    public void setInOutType(InOutType inOutType) {
        this.inOutType = inOutType;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
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

    public CashTransactionHistory getCashTransactionHistory() {
        return cashTransactionHistory;
    }

    public void setCashTransactionHistory(CashTransactionHistory cashTransactionHistory) {
        this.cashTransactionHistory = cashTransactionHistory;
    }

}
