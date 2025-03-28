package com.divudi.core.entity.cashTransaction;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.WebUser;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.Transient;

/**
 *
 * @author Dr M H B Ariyaratne, buddhika.ari@gmail.com
 *
 */
@Entity
public class DetailedFinancialBill implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @OneToOne
    private Bill bill;

    private Double floatInValue;
    private Double floatOutValue;
    private Double floatBalanceValue;



    // Calculated values for each payment method
    private Double cashValue;
    private Double cardValue;
    private Double chequeValue;
    private Double slipValue;
    private Double ewalletValue;
    private Double onCallValue;
    private Double multiplePaymentMethodsValue;
    private Double staffValue;
    private Double creditValue;
    private Double staffWelfareValue;
    private Double voucherValue;
    private Double iouValue;
    private Double agentValue;
    private Double patientDepositValue;
    private Double patientPointsValue;
    private Double onlineSettlementValue;
    private Double noneValue;
    private Double youOweMeValue;

    // Handover values for each payment method
    private Double cashHandoverValue;
    private Double cardHandoverValue;
    private Double chequeHandoverValue;
    private Double slipHandoverValue;
    private Double ewalletHandoverValue;
    private Double onCallHandoverValue;
    private Double multiplePaymentMethodsHandoverValue;
    private Double staffHandoverValue;
    private Double creditHandoverValue;
    private Double staffWelfareHandoverValue;
    private Double voucherHandoverValue;
    private Double iouHandoverValue;
    private Double agentHandoverValue;
    private Double patientDepositHandoverValue;
    private Double patientPointsHandoverValue;
    private Double onlineSettlementHandoverValue;
    private Double noneHandoverValue;
    private Double youOweMeHandoverValue;

    // Difference values for each payment method (calculated as actualValue - calculateValue)
    private Double cashDifference;
    private Double cardDifference;
    private Double chequeDifference;
    private Double slipDifference;
    private Double ewalletDifference;
    private Double onCallDifference;
    private Double multiplePaymentMethodsDifference;
    private Double staffDifference;
    private Double creditDifference;
    private Double staffWelfareDifference;
    private Double voucherDifference;
    private Double iouDifference;
    private Double agentDifference;
    private Double patientDepositDifference;
    private Double patientPointsDifference;
    private Double onlineSettlementDifference;
    private Double noneDifference;
    private Double youOweMeDifference;

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
        if (!(object instanceof DetailedFinancialBill)) {
            return false;
        }
        DetailedFinancialBill other = (DetailedFinancialBill) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.core.entity.cashTransaction.DetailedFinancialBill[ id=" + id + " ]";
    }

    @Transient
    public boolean isCashValuePresent() {
        return isValuePresent(cashValue, cashHandoverValue, cashDifference);
    }

    @Transient
    public boolean isCardValuePresent() {
        return isValuePresent(cardValue, cardHandoverValue, cardDifference);
    }

    @Transient
    public boolean isChequeValuePresent() {
        return isValuePresent(chequeValue, chequeHandoverValue, chequeDifference);
    }

    @Transient
    public boolean isSlipValuePresent() {
        return isValuePresent(slipValue, slipHandoverValue, slipDifference);
    }

    @Transient
    public boolean isEwalletValuePresent() {
        return isValuePresent(ewalletValue, ewalletHandoverValue, ewalletDifference);
    }

    @Transient
    public boolean isOnCallValuePresent() {
        return isValuePresent(onCallValue, onCallHandoverValue, onCallDifference);
    }

    @Transient
    public boolean isMultiplePaymentMethodsValuePresent() {
        return isValuePresent(multiplePaymentMethodsValue, multiplePaymentMethodsHandoverValue, multiplePaymentMethodsDifference);
    }

    @Transient
    public boolean isStaffValuePresent() {
        return isValuePresent(staffValue, staffHandoverValue, staffDifference);
    }

    @Transient
    public boolean isCreditValuePresent() {
        return isValuePresent(creditValue, creditHandoverValue, creditDifference);
    }

    @Transient
    public boolean isStaffWelfareValuePresent() {
        return isValuePresent(staffWelfareValue, staffWelfareHandoverValue, staffWelfareDifference);
    }

    @Transient
    public boolean isVoucherValuePresent() {
        return isValuePresent(voucherValue, voucherHandoverValue, voucherDifference);
    }

    @Transient
    public boolean isIouValuePresent() {
        return isValuePresent(iouValue, iouHandoverValue, iouDifference);
    }

    @Transient
    public boolean isAgentValuePresent() {
        return isValuePresent(agentValue, agentHandoverValue, agentDifference);
    }

    @Transient
    public boolean isPatientDepositValuePresent() {
        return isValuePresent(patientDepositValue, patientDepositHandoverValue, patientDepositDifference);
    }

    @Transient
    public boolean isPatientPointsValuePresent() {
        return isValuePresent(patientPointsValue, patientPointsHandoverValue, patientPointsDifference);
    }

    @Transient
    public boolean isOnlineSettlementValuePresent() {
        return isValuePresent(onlineSettlementValue, onlineSettlementHandoverValue, onlineSettlementDifference);
    }

    @Transient
    public boolean isNoneValuePresent() {
        return isValuePresent(noneValue, noneHandoverValue, noneDifference);
    }

    @Transient
    public boolean isYouOweMeValuePresent() {
        return isValuePresent(youOweMeValue, youOweMeHandoverValue, youOweMeDifference);
    }

// Helper method to check if any of the given values are present
    private boolean isValuePresent(Double calculatedValue, Double handoverValue, Double differenceValue) {
        return (calculatedValue != null && calculatedValue != 0.0)
                || (handoverValue != null && handoverValue != 0.0)
                || (differenceValue != null && differenceValue != 0.0);
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public Double getCashValue() {
        return cashValue;
    }

    public void setCashValue(Double cashValue) {
        this.cashValue = cashValue;
    }

    public Double getCardValue() {
        return cardValue;
    }

    public void setCardValue(Double cardValue) {
        this.cardValue = cardValue;
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

    public Double getEwalletValue() {
        return ewalletValue;
    }

    public void setEwalletValue(Double ewalletValue) {
        this.ewalletValue = ewalletValue;
    }

    public Double getOnCallValue() {
        return onCallValue;
    }

    public void setOnCallValue(Double onCallValue) {
        this.onCallValue = onCallValue;
    }

    public Double getMultiplePaymentMethodsValue() {
        return multiplePaymentMethodsValue;
    }

    public void setMultiplePaymentMethodsValue(Double multiplePaymentMethodsValue) {
        this.multiplePaymentMethodsValue = multiplePaymentMethodsValue;
    }

    public Double getStaffValue() {
        return staffValue;
    }

    public void setStaffValue(Double staffValue) {
        this.staffValue = staffValue;
    }

    public Double getCreditValue() {
        return creditValue;
    }

    public void setCreditValue(Double creditValue) {
        this.creditValue = creditValue;
    }

    public Double getStaffWelfareValue() {
        return staffWelfareValue;
    }

    public void setStaffWelfareValue(Double staffWelfareValue) {
        this.staffWelfareValue = staffWelfareValue;
    }

    public Double getVoucherValue() {
        return voucherValue;
    }

    public void setVoucherValue(Double voucherValue) {
        this.voucherValue = voucherValue;
    }

    public Double getIouValue() {
        return iouValue;
    }

    public void setIouValue(Double iouValue) {
        this.iouValue = iouValue;
    }

    public Double getAgentValue() {
        return agentValue;
    }

    public void setAgentValue(Double agentValue) {
        this.agentValue = agentValue;
    }

    public Double getPatientDepositValue() {
        return patientDepositValue;
    }

    public void setPatientDepositValue(Double patientDepositValue) {
        this.patientDepositValue = patientDepositValue;
    }

    public Double getPatientPointsValue() {
        return patientPointsValue;
    }

    public void setPatientPointsValue(Double patientPointsValue) {
        this.patientPointsValue = patientPointsValue;
    }

    public Double getOnlineSettlementValue() {
        return onlineSettlementValue;
    }

    public void setOnlineSettlementValue(Double onlineSettlementValue) {
        this.onlineSettlementValue = onlineSettlementValue;
    }

    public Double getNoneValue() {
        return noneValue;
    }

    public void setNoneValue(Double noneValue) {
        this.noneValue = noneValue;
    }

    public Double getYouOweMeValue() {
        return youOweMeValue;
    }

    public void setYouOweMeValue(Double youOweMeValue) {
        this.youOweMeValue = youOweMeValue;
    }

    public Double getCashHandoverValue() {
        return cashHandoverValue;
    }

    public void setCashHandoverValue(Double cashHandoverValue) {
        this.cashHandoverValue = cashHandoverValue;
    }

    public Double getCardHandoverValue() {
        return cardHandoverValue;
    }

    public void setCardHandoverValue(Double cardHandoverValue) {
        this.cardHandoverValue = cardHandoverValue;
    }

    public Double getChequeHandoverValue() {
        return chequeHandoverValue;
    }

    public void setChequeHandoverValue(Double chequeHandoverValue) {
        this.chequeHandoverValue = chequeHandoverValue;
    }

    public Double getSlipHandoverValue() {
        return slipHandoverValue;
    }

    public void setSlipHandoverValue(Double slipHandoverValue) {
        this.slipHandoverValue = slipHandoverValue;
    }

    public Double getEwalletHandoverValue() {
        return ewalletHandoverValue;
    }

    public void setEwalletHandoverValue(Double ewalletHandoverValue) {
        this.ewalletHandoverValue = ewalletHandoverValue;
    }

    public Double getOnCallHandoverValue() {
        return onCallHandoverValue;
    }

    public void setOnCallHandoverValue(Double onCallHandoverValue) {
        this.onCallHandoverValue = onCallHandoverValue;
    }

    public Double getMultiplePaymentMethodsHandoverValue() {
        return multiplePaymentMethodsHandoverValue;
    }

    public void setMultiplePaymentMethodsHandoverValue(Double multiplePaymentMethodsHandoverValue) {
        this.multiplePaymentMethodsHandoverValue = multiplePaymentMethodsHandoverValue;
    }

    public Double getStaffHandoverValue() {
        return staffHandoverValue;
    }

    public void setStaffHandoverValue(Double staffHandoverValue) {
        this.staffHandoverValue = staffHandoverValue;
    }

    public Double getCreditHandoverValue() {
        return creditHandoverValue;
    }

    public void setCreditHandoverValue(Double creditHandoverValue) {
        this.creditHandoverValue = creditHandoverValue;
    }

    public Double getStaffWelfareHandoverValue() {
        return staffWelfareHandoverValue;
    }

    public void setStaffWelfareHandoverValue(Double staffWelfareHandoverValue) {
        this.staffWelfareHandoverValue = staffWelfareHandoverValue;
    }

    public Double getVoucherHandoverValue() {
        return voucherHandoverValue;
    }

    public void setVoucherHandoverValue(Double voucherHandoverValue) {
        this.voucherHandoverValue = voucherHandoverValue;
    }

    public Double getIouHandoverValue() {
        return iouHandoverValue;
    }

    public void setIouHandoverValue(Double iouHandoverValue) {
        this.iouHandoverValue = iouHandoverValue;
    }

    public Double getAgentHandoverValue() {
        return agentHandoverValue;
    }

    public void setAgentHandoverValue(Double agentHandoverValue) {
        this.agentHandoverValue = agentHandoverValue;
    }

    public Double getPatientDepositHandoverValue() {
        return patientDepositHandoverValue;
    }

    public void setPatientDepositHandoverValue(Double patientDepositHandoverValue) {
        this.patientDepositHandoverValue = patientDepositHandoverValue;
    }

    public Double getPatientPointsHandoverValue() {
        return patientPointsHandoverValue;
    }

    public void setPatientPointsHandoverValue(Double patientPointsHandoverValue) {
        this.patientPointsHandoverValue = patientPointsHandoverValue;
    }

    public Double getOnlineSettlementHandoverValue() {
        return onlineSettlementHandoverValue;
    }

    public void setOnlineSettlementHandoverValue(Double onlineSettlementHandoverValue) {
        this.onlineSettlementHandoverValue = onlineSettlementHandoverValue;
    }

    public Double getNoneHandoverValue() {
        return noneHandoverValue;
    }

    public void setNoneHandoverValue(Double noneHandoverValue) {
        this.noneHandoverValue = noneHandoverValue;
    }

    public Double getYouOweMeHandoverValue() {
        return youOweMeHandoverValue;
    }

    public void setYouOweMeHandoverValue(Double youOweMeHandoverValue) {
        this.youOweMeHandoverValue = youOweMeHandoverValue;
    }

    public Double getCashDifference() {
        return cashDifference;
    }

    public void setCashDifference(Double cashDifference) {
        this.cashDifference = cashDifference;
    }

    public Double getCardDifference() {
        return cardDifference;
    }

    public void setCardDifference(Double cardDifference) {
        this.cardDifference = cardDifference;
    }

    public Double getChequeDifference() {
        return chequeDifference;
    }

    public void setChequeDifference(Double chequeDifference) {
        this.chequeDifference = chequeDifference;
    }

    public Double getSlipDifference() {
        return slipDifference;
    }

    public void setSlipDifference(Double slipDifference) {
        this.slipDifference = slipDifference;
    }

    public Double getEwalletDifference() {
        return ewalletDifference;
    }

    public void setEwalletDifference(Double ewalletDifference) {
        this.ewalletDifference = ewalletDifference;
    }

    public Double getOnCallDifference() {
        return onCallDifference;
    }

    public void setOnCallDifference(Double onCallDifference) {
        this.onCallDifference = onCallDifference;
    }

    public Double getMultiplePaymentMethodsDifference() {
        return multiplePaymentMethodsDifference;
    }

    public void setMultiplePaymentMethodsDifference(Double multiplePaymentMethodsDifference) {
        this.multiplePaymentMethodsDifference = multiplePaymentMethodsDifference;
    }

    public Double getStaffDifference() {
        return staffDifference;
    }

    public void setStaffDifference(Double staffDifference) {
        this.staffDifference = staffDifference;
    }

    public Double getCreditDifference() {
        return creditDifference;
    }

    public void setCreditDifference(Double creditDifference) {
        this.creditDifference = creditDifference;
    }

    public Double getStaffWelfareDifference() {
        return staffWelfareDifference;
    }

    public void setStaffWelfareDifference(Double staffWelfareDifference) {
        this.staffWelfareDifference = staffWelfareDifference;
    }

    public Double getVoucherDifference() {
        return voucherDifference;
    }

    public void setVoucherDifference(Double voucherDifference) {
        this.voucherDifference = voucherDifference;
    }

    public Double getIouDifference() {
        return iouDifference;
    }

    public void setIouDifference(Double iouDifference) {
        this.iouDifference = iouDifference;
    }

    public Double getAgentDifference() {
        return agentDifference;
    }

    public void setAgentDifference(Double agentDifference) {
        this.agentDifference = agentDifference;
    }

    public Double getPatientDepositDifference() {
        return patientDepositDifference;
    }

    public void setPatientDepositDifference(Double patientDepositDifference) {
        this.patientDepositDifference = patientDepositDifference;
    }

    public Double getPatientPointsDifference() {
        return patientPointsDifference;
    }

    public void setPatientPointsDifference(Double patientPointsDifference) {
        this.patientPointsDifference = patientPointsDifference;
    }

    public Double getOnlineSettlementDifference() {
        return onlineSettlementDifference;
    }

    public void setOnlineSettlementDifference(Double onlineSettlementDifference) {
        this.onlineSettlementDifference = onlineSettlementDifference;
    }

    public Double getNoneDifference() {
        return noneDifference;
    }

    public void setNoneDifference(Double noneDifference) {
        this.noneDifference = noneDifference;
    }

    public Double getYouOweMeDifference() {
        return youOweMeDifference;
    }

    public void setYouOweMeDifference(Double youOweMeDifference) {
        this.youOweMeDifference = youOweMeDifference;
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

    public Double getFloatInValue() {
        return floatInValue;
    }

    public void setFloatInValue(Double floatInValue) {
        this.floatInValue = floatInValue;
    }

    public Double getFloatOutValue() {
        return floatOutValue;
    }

    public void setFloatOutValue(Double floatOutValue) {
        this.floatOutValue = floatOutValue;
    }

    public Double getFloatBalanceValue() {
        return floatBalanceValue;
    }

    public void setFloatBalanceValue(Double floatBalanceValue) {
        this.floatBalanceValue = floatBalanceValue;
    }



}
