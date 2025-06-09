package com.divudi.core.entity.cashTransaction;

import com.divudi.core.entity.RetirableEntity;
import com.divudi.core.entity.WebUser;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.Transient;

@Entity
public class Drawer implements Serializable, RetirableEntity  {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;
    private String name;
    private WebUser drawerUser;
    @ManyToOne
    private WebUser creater;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date createdAt;
    private boolean retired;
    @ManyToOne
    private WebUser retirer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date retiredAt;
    private String retireComments;

    // Current In-Hand Values for Each Payment Method
    private Double onCallInHandValue;
    private Double cashInHandValue;
    private Double cardInHandValue;
    private Double multiplePaymentMethodsInHandValue;
    private Double staffInHandValue;
    private Double creditInHandValue;
    private Double staffWelfareInHandValue;
    private Double voucherInHandValue;
    private Double iouInHandValue;
    private Double agentInHandValue;
    private Double chequeInHandValue;
    private Double slipInHandValue;
    private Double ewalletInHandValue;
    private Double patientDepositInHandValue;
    private Double patientPointsInHandValue;
    private Double onlineSettlementInHandValue;
    private Double noneInHandValue;
    private Double youOweMeInHandValue;

    // Shortage/Excess for Each Payment Method
    private Double onCallShortageOrExcess;
    private Double cashShortageOrExcess;
    private Double cardShortageOrExcess;
    private Double multiplePaymentMethodsShortageOrExcess;
    private Double staffShortageOrExcess;
    private Double creditShortageOrExcess;
    private Double staffWelfareShortageOrExcess;
    private Double voucherShortageOrExcess;
    private Double iouShortageOrExcess;
    private Double agentShortageOrExcess;
    private Double chequeShortageOrExcess;
    private Double slipShortageOrExcess;
    private Double ewalletShortageOrExcess;
    private Double patientDepositShortageOrExcess;
    private Double patientPointsShortageOrExcess;
    private Double onlineSettlementShortageOrExcess;
    private Double noneShortageOrExcess;
    private Double youOweMeShortageOrExcess;

    // Balance for Each Payment Method
    private Double onCallBalance;
    private Double cashBalance;
    private Double cardBalance;
    private Double multiplePaymentMethodsBalance;
    private Double staffBalance;
    private Double creditBalance;
    private Double staffWelfareBalance;
    private Double voucherBalance;
    private Double iouBalance;
    private Double agentBalance;
    private Double chequeBalance;
    private Double slipBalance;
    private Double ewalletBalance;
    private Double patientDepositBalance;
    private Double patientPointsBalance;
    private Double onlineSettlementBalance;
    private Double noneBalance;
    private Double youOweMeBalance;

    @Transient
    public boolean isOnCallPresent() {
        return isValuePresent(onCallInHandValue, onCallShortageOrExcess, onCallBalance);
    }

    @Transient
    public boolean isCashPresent() {
        return isValuePresent(cashInHandValue, cashShortageOrExcess, cashBalance);
    }

    @Transient
    public boolean isCardPresent() {
        return isValuePresent(cardInHandValue, cardShortageOrExcess, cardBalance);
    }

    @Transient
    public boolean isMultiplePaymentMethodsPresent() {
        return isValuePresent(multiplePaymentMethodsInHandValue, multiplePaymentMethodsShortageOrExcess, multiplePaymentMethodsBalance);
    }

    @Transient
    public boolean isStaffPresent() {
        return isValuePresent(staffInHandValue, staffShortageOrExcess, staffBalance);
    }

    @Transient
    public boolean isCreditPresent() {
        return isValuePresent(creditInHandValue, creditShortageOrExcess, creditBalance);
    }

    @Transient
    public boolean isStaffWelfarePresent() {
        return isValuePresent(staffWelfareInHandValue, staffWelfareShortageOrExcess, staffWelfareBalance);
    }

    @Transient
    public boolean isVoucherPresent() {
        return isValuePresent(voucherInHandValue, voucherShortageOrExcess, voucherBalance);
    }

    @Transient
    public boolean isIouPresent() {
        return isValuePresent(iouInHandValue, iouShortageOrExcess, iouBalance);
    }

    @Transient
    public boolean isAgentPresent() {
        return isValuePresent(agentInHandValue, agentShortageOrExcess, agentBalance);
    }

    @Transient
    public boolean isChequePresent() {
        return isValuePresent(chequeInHandValue, chequeShortageOrExcess, chequeBalance);
    }

    @Transient
    public boolean isSlipPresent() {
        return isValuePresent(slipInHandValue, slipShortageOrExcess, slipBalance);
    }

    @Transient
    public boolean isEwalletPresent() {
        return isValuePresent(ewalletInHandValue, ewalletShortageOrExcess, ewalletBalance);
    }

    @Transient
    public boolean isPatientDepositPresent() {
        return isValuePresent(patientDepositInHandValue, patientDepositShortageOrExcess, patientDepositBalance);
    }

    @Transient
    public boolean isPatientPointsPresent() {
        return isValuePresent(patientPointsInHandValue, patientPointsShortageOrExcess, patientPointsBalance);
    }

    @Transient
    public boolean isOnlineSettlementPresent() {
        return isValuePresent(onlineSettlementInHandValue, onlineSettlementShortageOrExcess, onlineSettlementBalance);
    }

    @Transient
    public boolean isNonePresent() {
        return isValuePresent(noneInHandValue, noneShortageOrExcess, noneBalance);
    }

    @Transient
    public boolean isYouOweMePresent() {
        return isValuePresent(youOweMeInHandValue, youOweMeShortageOrExcess, youOweMeBalance);
    }

    @Transient
    private boolean isValuePresent(Double inHandValue, Double shortageOrExcess, Double balance) {
        return (inHandValue != null && inHandValue != 0.0)
                || (shortageOrExcess != null && shortageOrExcess != 0.0)
                || (balance != null && balance != 0.0);
    }

    @Transient
    public Double getTotalInHand() {
        return sumValues(onCallInHandValue, cashInHandValue, cardInHandValue, multiplePaymentMethodsInHandValue,
                staffInHandValue, creditInHandValue, staffWelfareInHandValue, voucherInHandValue, iouInHandValue,
                agentInHandValue, chequeInHandValue, slipInHandValue, ewalletInHandValue,
                patientPointsInHandValue, onlineSettlementInHandValue, noneInHandValue, youOweMeInHandValue);
    }

    @Transient
    public Double getTotalShortageOrExcess() {
        return sumValues(onCallShortageOrExcess, cashShortageOrExcess, cardShortageOrExcess,
                multiplePaymentMethodsShortageOrExcess, staffShortageOrExcess, creditShortageOrExcess,
                staffWelfareShortageOrExcess, voucherShortageOrExcess, iouShortageOrExcess, agentShortageOrExcess,
                chequeShortageOrExcess, slipShortageOrExcess, ewalletShortageOrExcess,
                patientPointsShortageOrExcess, onlineSettlementShortageOrExcess, noneShortageOrExcess,
                youOweMeShortageOrExcess);
    }

    @Transient
    public Double getTotalBalance() {
        return sumValues(onCallBalance, cashBalance, cardBalance, multiplePaymentMethodsBalance, staffBalance,
                creditBalance, staffWelfareBalance, voucherBalance, iouBalance, agentBalance, chequeBalance,
                slipBalance, ewalletBalance, patientPointsBalance, onlineSettlementBalance,
                noneBalance, youOweMeBalance);
    }

    @Transient
    private Double sumValues(Double... values) {
        Double total = 0.0;
        for (Double value : values) {
            if (value != null) {
                total += value;
            }
        }
        return total;
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
        if (!(object instanceof Drawer)) {
            return false;
        }
        Drawer other = (Drawer) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.core.entity.Drawer[ id=" + id + " ]";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public WebUser getDrawerUser() {
        return drawerUser;
    }

    public void setDrawerUser(WebUser drawerUser) {
        this.drawerUser = drawerUser;
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

    public Double getOnCallInHandValue() {
        return onCallInHandValue;
    }

    public void setOnCallInHandValue(Double onCallInHandValue) {
        this.onCallInHandValue = onCallInHandValue;
    }

    public Double getCashInHandValue() {
        return cashInHandValue;
    }

    public void setCashInHandValue(Double cashInHandValue) {
        this.cashInHandValue = cashInHandValue;
    }

    public Double getCardInHandValue() {
        return cardInHandValue;
    }

    public void setCardInHandValue(Double cardInHandValue) {
        this.cardInHandValue = cardInHandValue;
    }

    public Double getMultiplePaymentMethodsInHandValue() {
        return multiplePaymentMethodsInHandValue;
    }

    public void setMultiplePaymentMethodsInHandValue(Double multiplePaymentMethodsInHandValue) {
        this.multiplePaymentMethodsInHandValue = multiplePaymentMethodsInHandValue;
    }

    public Double getStaffInHandValue() {
        return staffInHandValue;
    }

    public void setStaffInHandValue(Double staffInHandValue) {
        this.staffInHandValue = staffInHandValue;
    }

    public Double getCreditInHandValue() {
        return creditInHandValue;
    }

    public void setCreditInHandValue(Double creditInHandValue) {
        this.creditInHandValue = creditInHandValue;
    }

    public Double getStaffWelfareInHandValue() {
        return staffWelfareInHandValue;
    }

    public void setStaffWelfareInHandValue(Double staffWelfareInHandValue) {
        this.staffWelfareInHandValue = staffWelfareInHandValue;
    }

    public Double getVoucherInHandValue() {
        return voucherInHandValue;
    }

    public void setVoucherInHandValue(Double voucherInHandValue) {
        this.voucherInHandValue = voucherInHandValue;
    }

    public Double getIouInHandValue() {
        return iouInHandValue;
    }

    public void setIouInHandValue(Double iouInHandValue) {
        this.iouInHandValue = iouInHandValue;
    }

    public Double getAgentInHandValue() {
        return agentInHandValue;
    }

    public void setAgentInHandValue(Double agentInHandValue) {
        this.agentInHandValue = agentInHandValue;
    }

    public Double getChequeInHandValue() {
        return chequeInHandValue;
    }

    public void setChequeInHandValue(Double chequeInHandValue) {
        this.chequeInHandValue = chequeInHandValue;
    }

    public Double getSlipInHandValue() {
        return slipInHandValue;
    }

    public void setSlipInHandValue(Double slipInHandValue) {
        this.slipInHandValue = slipInHandValue;
    }

    public Double getEwalletInHandValue() {
        return ewalletInHandValue;
    }

    public void setEwalletInHandValue(Double ewalletInHandValue) {
        this.ewalletInHandValue = ewalletInHandValue;
    }

    public Double getPatientDepositInHandValue() {
        return patientDepositInHandValue;
    }

    public void setPatientDepositInHandValue(Double patientDepositInHandValue) {
        this.patientDepositInHandValue = patientDepositInHandValue;
    }

    public Double getPatientPointsInHandValue() {
        return patientPointsInHandValue;
    }

    public void setPatientPointsInHandValue(Double patientPointsInHandValue) {
        this.patientPointsInHandValue = patientPointsInHandValue;
    }

    public Double getOnlineSettlementInHandValue() {
        return onlineSettlementInHandValue;
    }

    public void setOnlineSettlementInHandValue(Double onlineSettlementInHandValue) {
        this.onlineSettlementInHandValue = onlineSettlementInHandValue;
    }

    public Double getNoneInHandValue() {
        return noneInHandValue;
    }

    public void setNoneInHandValue(Double noneInHandValue) {
        this.noneInHandValue = noneInHandValue;
    }

    public Double getYouOweMeInHandValue() {
        return youOweMeInHandValue;
    }

    public void setYouOweMeInHandValue(Double youOweMeInHandValue) {
        this.youOweMeInHandValue = youOweMeInHandValue;
    }

    public Double getOnCallShortageOrExcess() {
        return onCallShortageOrExcess;
    }

    public void setOnCallShortageOrExcess(Double onCallShortageOrExcess) {
        this.onCallShortageOrExcess = onCallShortageOrExcess;
    }

    public Double getCashShortageOrExcess() {
        return cashShortageOrExcess;
    }

    public void setCashShortageOrExcess(Double cashShortageOrExcess) {
        this.cashShortageOrExcess = cashShortageOrExcess;
    }

    public Double getCardShortageOrExcess() {
        return cardShortageOrExcess;
    }

    public void setCardShortageOrExcess(Double cardShortageOrExcess) {
        this.cardShortageOrExcess = cardShortageOrExcess;
    }

    public Double getMultiplePaymentMethodsShortageOrExcess() {
        return multiplePaymentMethodsShortageOrExcess;
    }

    public void setMultiplePaymentMethodsShortageOrExcess(Double multiplePaymentMethodsShortageOrExcess) {
        this.multiplePaymentMethodsShortageOrExcess = multiplePaymentMethodsShortageOrExcess;
    }

    public Double getStaffShortageOrExcess() {
        return staffShortageOrExcess;
    }

    public void setStaffShortageOrExcess(Double staffShortageOrExcess) {
        this.staffShortageOrExcess = staffShortageOrExcess;
    }

    public Double getCreditShortageOrExcess() {
        return creditShortageOrExcess;
    }

    public void setCreditShortageOrExcess(Double creditShortageOrExcess) {
        this.creditShortageOrExcess = creditShortageOrExcess;
    }

    public Double getStaffWelfareShortageOrExcess() {
        return staffWelfareShortageOrExcess;
    }

    public void setStaffWelfareShortageOrExcess(Double staffWelfareShortageOrExcess) {
        this.staffWelfareShortageOrExcess = staffWelfareShortageOrExcess;
    }

    public Double getVoucherShortageOrExcess() {
        return voucherShortageOrExcess;
    }

    public void setVoucherShortageOrExcess(Double voucherShortageOrExcess) {
        this.voucherShortageOrExcess = voucherShortageOrExcess;
    }

    public Double getIouShortageOrExcess() {
        return iouShortageOrExcess;
    }

    public void setIouShortageOrExcess(Double iouShortageOrExcess) {
        this.iouShortageOrExcess = iouShortageOrExcess;
    }

    public Double getAgentShortageOrExcess() {
        return agentShortageOrExcess;
    }

    public void setAgentShortageOrExcess(Double agentShortageOrExcess) {
        this.agentShortageOrExcess = agentShortageOrExcess;
    }

    public Double getChequeShortageOrExcess() {
        return chequeShortageOrExcess;
    }

    public void setChequeShortageOrExcess(Double chequeShortageOrExcess) {
        this.chequeShortageOrExcess = chequeShortageOrExcess;
    }

    public Double getSlipShortageOrExcess() {
        return slipShortageOrExcess;
    }

    public void setSlipShortageOrExcess(Double slipShortageOrExcess) {
        this.slipShortageOrExcess = slipShortageOrExcess;
    }

    public Double getEwalletShortageOrExcess() {
        return ewalletShortageOrExcess;
    }

    public void setEwalletShortageOrExcess(Double ewalletShortageOrExcess) {
        this.ewalletShortageOrExcess = ewalletShortageOrExcess;
    }

    public Double getPatientDepositShortageOrExcess() {
        return patientDepositShortageOrExcess;
    }

    public void setPatientDepositShortageOrExcess(Double patientDepositShortageOrExcess) {
        this.patientDepositShortageOrExcess = patientDepositShortageOrExcess;
    }

    public Double getPatientPointsShortageOrExcess() {
        return patientPointsShortageOrExcess;
    }

    public void setPatientPointsShortageOrExcess(Double patientPointsShortageOrExcess) {
        this.patientPointsShortageOrExcess = patientPointsShortageOrExcess;
    }

    public Double getOnlineSettlementShortageOrExcess() {
        return onlineSettlementShortageOrExcess;
    }

    public void setOnlineSettlementShortageOrExcess(Double onlineSettlementShortageOrExcess) {
        this.onlineSettlementShortageOrExcess = onlineSettlementShortageOrExcess;
    }

    public Double getNoneShortageOrExcess() {
        return noneShortageOrExcess;
    }

    public void setNoneShortageOrExcess(Double noneShortageOrExcess) {
        this.noneShortageOrExcess = noneShortageOrExcess;
    }

    public Double getYouOweMeShortageOrExcess() {
        return youOweMeShortageOrExcess;
    }

    public void setYouOweMeShortageOrExcess(Double youOweMeShortageOrExcess) {
        this.youOweMeShortageOrExcess = youOweMeShortageOrExcess;
    }

    public Double getOnCallBalance() {
        return onCallBalance;
    }

    public void setOnCallBalance(Double onCallBalance) {
        this.onCallBalance = onCallBalance;
    }

    public Double getCashBalance() {
        return cashBalance;
    }

    public void setCashBalance(Double cashBalance) {
        this.cashBalance = cashBalance;
    }

    public Double getCardBalance() {
        return cardBalance;
    }

    public void setCardBalance(Double cardBalance) {
        this.cardBalance = cardBalance;
    }

    public Double getMultiplePaymentMethodsBalance() {
        return multiplePaymentMethodsBalance;
    }

    public void setMultiplePaymentMethodsBalance(Double multiplePaymentMethodsBalance) {
        this.multiplePaymentMethodsBalance = multiplePaymentMethodsBalance;
    }

    public Double getStaffBalance() {
        return staffBalance;
    }

    public void setStaffBalance(Double staffBalance) {
        this.staffBalance = staffBalance;
    }

    public Double getCreditBalance() {
        return creditBalance;
    }

    public void setCreditBalance(Double creditBalance) {
        this.creditBalance = creditBalance;
    }

    public Double getStaffWelfareBalance() {
        return staffWelfareBalance;
    }

    public void setStaffWelfareBalance(Double staffWelfareBalance) {
        this.staffWelfareBalance = staffWelfareBalance;
    }

    public Double getVoucherBalance() {
        return voucherBalance;
    }

    public void setVoucherBalance(Double voucherBalance) {
        this.voucherBalance = voucherBalance;
    }

    public Double getIouBalance() {
        return iouBalance;
    }

    public void setIouBalance(Double iouBalance) {
        this.iouBalance = iouBalance;
    }

    public Double getAgentBalance() {
        return agentBalance;
    }

    public void setAgentBalance(Double agentBalance) {
        this.agentBalance = agentBalance;
    }

    public Double getChequeBalance() {
        return chequeBalance;
    }

    public void setChequeBalance(Double chequeBalance) {
        this.chequeBalance = chequeBalance;
    }

    public Double getSlipBalance() {
        return slipBalance;
    }

    public void setSlipBalance(Double slipBalance) {
        this.slipBalance = slipBalance;
    }

    public Double getEwalletBalance() {
        return ewalletBalance;
    }

    public void setEwalletBalance(Double ewalletBalance) {
        this.ewalletBalance = ewalletBalance;
    }

    public Double getPatientDepositBalance() {
        return patientDepositBalance;
    }

    public void setPatientDepositBalance(Double patientDepositBalance) {
        this.patientDepositBalance = patientDepositBalance;
    }

    public Double getPatientPointsBalance() {
        return patientPointsBalance;
    }

    public void setPatientPointsBalance(Double patientPointsBalance) {
        this.patientPointsBalance = patientPointsBalance;
    }

    public Double getOnlineSettlementBalance() {
        return onlineSettlementBalance;
    }

    public void setOnlineSettlementBalance(Double onlineSettlementBalance) {
        this.onlineSettlementBalance = onlineSettlementBalance;
    }

    public Double getNoneBalance() {
        return noneBalance;
    }

    public void setNoneBalance(Double noneBalance) {
        this.noneBalance = noneBalance;
    }

    public Double getYouOweMeBalance() {
        return youOweMeBalance;
    }

    public void setYouOweMeBalance(Double youOweMeBalance) {
        this.youOweMeBalance = youOweMeBalance;
    }

}
