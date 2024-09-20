package com.divudi.entity.cashTransaction;

import com.divudi.entity.Bill;
import com.divudi.entity.WebUser;
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

    // Calculated values for each payment method
    private Double cashCalculateValue;
    private Double cardCalculateValue;
    private Double chequeCalculateValue;
    private Double slipCalculateValue;
    private Double ewalletCalculateValue;
    private Double onCallCalculateValue;
    private Double multiplePaymentMethodsCalculateValue;
    private Double staffCalculateValue;
    private Double creditCalculateValue;
    private Double staffWelfareCalculateValue;
    private Double voucherCalculateValue;
    private Double iouCalculateValue;
    private Double agentCalculateValue;
    private Double patientDepositCalculateValue;
    private Double patientPointsCalculateValue;
    private Double onlineSettlementCalculateValue;
    private Double noneCalculateValue;
    private Double youOweMeCalculateValue;

// Actual values for each payment method
    private Double cashActualValue;
    private Double cardActualValue;
    private Double chequeActualValue;
    private Double slipActualValue;
    private Double ewalletActualValue;
    private Double onCallActualValue;
    private Double multiplePaymentMethodsActualValue;
    private Double staffActualValue;
    private Double creditActualValue;
    private Double staffWelfareActualValue;
    private Double voucherActualValue;
    private Double iouActualValue;
    private Double agentActualValue;
    private Double patientDepositActualValue;
    private Double patientPointsActualValue;
    private Double onlineSettlementActualValue;
    private Double noneActualValue;
    private Double youOweMeActualValue;

// Difference values for each payment method (calculated as actualValue - calculateValue)
    private Double cashDifferenceValue;
    private Double cardDifferenceValue;
    private Double chequeDifferenceValue;
    private Double slipDifferenceValue;
    private Double ewalletDifferenceValue;
    private Double onCallDifferenceValue;
    private Double multiplePaymentMethodsDifferenceValue;
    private Double staffDifferenceValue;
    private Double creditDifferenceValue;
    private Double staffWelfareDifferenceValue;
    private Double voucherDifferenceValue;
    private Double iouDifferenceValue;
    private Double agentDifferenceValue;
    private Double patientDepositDifferenceValue;
    private Double patientPointsDifferenceValue;
    private Double onlineSettlementDifferenceValue;
    private Double noneDifferenceValue;
    private Double youOweMeDifferenceValue;

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
        return "com.divudi.entity.cashTransaction.DetailedFinancialBill[ id=" + id + " ]";
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

    public Double getCashCalculateValue() {
        return cashCalculateValue;
    }

    public void setCashCalculateValue(Double cashCalculateValue) {
        this.cashCalculateValue = cashCalculateValue;
    }

    public Double getCardCalculateValue() {
        return cardCalculateValue;
    }

    public void setCardCalculateValue(Double cardCalculateValue) {
        this.cardCalculateValue = cardCalculateValue;
    }

    public Double getChequeCalculateValue() {
        return chequeCalculateValue;
    }

    public void setChequeCalculateValue(Double chequeCalculateValue) {
        this.chequeCalculateValue = chequeCalculateValue;
    }

    public Double getSlipCalculateValue() {
        return slipCalculateValue;
    }

    public void setSlipCalculateValue(Double slipCalculateValue) {
        this.slipCalculateValue = slipCalculateValue;
    }

    public Double getEwalletCalculateValue() {
        return ewalletCalculateValue;
    }

    public void setEwalletCalculateValue(Double ewalletCalculateValue) {
        this.ewalletCalculateValue = ewalletCalculateValue;
    }

    public Double getOnCallCalculateValue() {
        return onCallCalculateValue;
    }

    public void setOnCallCalculateValue(Double onCallCalculateValue) {
        this.onCallCalculateValue = onCallCalculateValue;
    }

    public Double getMultiplePaymentMethodsCalculateValue() {
        return multiplePaymentMethodsCalculateValue;
    }

    public void setMultiplePaymentMethodsCalculateValue(Double multiplePaymentMethodsCalculateValue) {
        this.multiplePaymentMethodsCalculateValue = multiplePaymentMethodsCalculateValue;
    }

    public Double getStaffCalculateValue() {
        return staffCalculateValue;
    }

    public void setStaffCalculateValue(Double staffCalculateValue) {
        this.staffCalculateValue = staffCalculateValue;
    }

    public Double getCreditCalculateValue() {
        return creditCalculateValue;
    }

    public void setCreditCalculateValue(Double creditCalculateValue) {
        this.creditCalculateValue = creditCalculateValue;
    }

    public Double getStaffWelfareCalculateValue() {
        return staffWelfareCalculateValue;
    }

    public void setStaffWelfareCalculateValue(Double staffWelfareCalculateValue) {
        this.staffWelfareCalculateValue = staffWelfareCalculateValue;
    }

    public Double getVoucherCalculateValue() {
        return voucherCalculateValue;
    }

    public void setVoucherCalculateValue(Double voucherCalculateValue) {
        this.voucherCalculateValue = voucherCalculateValue;
    }

    public Double getIouCalculateValue() {
        return iouCalculateValue;
    }

    public void setIouCalculateValue(Double iouCalculateValue) {
        this.iouCalculateValue = iouCalculateValue;
    }

    public Double getAgentCalculateValue() {
        return agentCalculateValue;
    }

    public void setAgentCalculateValue(Double agentCalculateValue) {
        this.agentCalculateValue = agentCalculateValue;
    }

    public Double getPatientDepositCalculateValue() {
        return patientDepositCalculateValue;
    }

    public void setPatientDepositCalculateValue(Double patientDepositCalculateValue) {
        this.patientDepositCalculateValue = patientDepositCalculateValue;
    }

    public Double getPatientPointsCalculateValue() {
        return patientPointsCalculateValue;
    }

    public void setPatientPointsCalculateValue(Double patientPointsCalculateValue) {
        this.patientPointsCalculateValue = patientPointsCalculateValue;
    }

    public Double getOnlineSettlementCalculateValue() {
        return onlineSettlementCalculateValue;
    }

    public void setOnlineSettlementCalculateValue(Double onlineSettlementCalculateValue) {
        this.onlineSettlementCalculateValue = onlineSettlementCalculateValue;
    }

    public Double getNoneCalculateValue() {
        return noneCalculateValue;
    }

    public void setNoneCalculateValue(Double noneCalculateValue) {
        this.noneCalculateValue = noneCalculateValue;
    }

    public Double getYouOweMeCalculateValue() {
        return youOweMeCalculateValue;
    }

    public void setYouOweMeCalculateValue(Double youOweMeCalculateValue) {
        this.youOweMeCalculateValue = youOweMeCalculateValue;
    }

    public Double getCashActualValue() {
        return cashActualValue;
    }

    public void setCashActualValue(Double cashActualValue) {
        this.cashActualValue = cashActualValue;
    }

    public Double getCardActualValue() {
        return cardActualValue;
    }

    public void setCardActualValue(Double cardActualValue) {
        this.cardActualValue = cardActualValue;
    }

    public Double getChequeActualValue() {
        return chequeActualValue;
    }

    public void setChequeActualValue(Double chequeActualValue) {
        this.chequeActualValue = chequeActualValue;
    }

    public Double getSlipActualValue() {
        return slipActualValue;
    }

    public void setSlipActualValue(Double slipActualValue) {
        this.slipActualValue = slipActualValue;
    }

    public Double getEwalletActualValue() {
        return ewalletActualValue;
    }

    public void setEwalletActualValue(Double ewalletActualValue) {
        this.ewalletActualValue = ewalletActualValue;
    }

    public Double getOnCallActualValue() {
        return onCallActualValue;
    }

    public void setOnCallActualValue(Double onCallActualValue) {
        this.onCallActualValue = onCallActualValue;
    }

    public Double getMultiplePaymentMethodsActualValue() {
        return multiplePaymentMethodsActualValue;
    }

    public void setMultiplePaymentMethodsActualValue(Double multiplePaymentMethodsActualValue) {
        this.multiplePaymentMethodsActualValue = multiplePaymentMethodsActualValue;
    }

    public Double getStaffActualValue() {
        return staffActualValue;
    }

    public void setStaffActualValue(Double staffActualValue) {
        this.staffActualValue = staffActualValue;
    }

    public Double getCreditActualValue() {
        return creditActualValue;
    }

    public void setCreditActualValue(Double creditActualValue) {
        this.creditActualValue = creditActualValue;
    }

    public Double getStaffWelfareActualValue() {
        return staffWelfareActualValue;
    }

    public void setStaffWelfareActualValue(Double staffWelfareActualValue) {
        this.staffWelfareActualValue = staffWelfareActualValue;
    }

    public Double getVoucherActualValue() {
        return voucherActualValue;
    }

    public void setVoucherActualValue(Double voucherActualValue) {
        this.voucherActualValue = voucherActualValue;
    }

    public Double getIouActualValue() {
        return iouActualValue;
    }

    public void setIouActualValue(Double iouActualValue) {
        this.iouActualValue = iouActualValue;
    }

    public Double getAgentActualValue() {
        return agentActualValue;
    }

    public void setAgentActualValue(Double agentActualValue) {
        this.agentActualValue = agentActualValue;
    }

    public Double getPatientDepositActualValue() {
        return patientDepositActualValue;
    }

    public void setPatientDepositActualValue(Double patientDepositActualValue) {
        this.patientDepositActualValue = patientDepositActualValue;
    }

    public Double getPatientPointsActualValue() {
        return patientPointsActualValue;
    }

    public void setPatientPointsActualValue(Double patientPointsActualValue) {
        this.patientPointsActualValue = patientPointsActualValue;
    }

    public Double getOnlineSettlementActualValue() {
        return onlineSettlementActualValue;
    }

    public void setOnlineSettlementActualValue(Double onlineSettlementActualValue) {
        this.onlineSettlementActualValue = onlineSettlementActualValue;
    }

    public Double getNoneActualValue() {
        return noneActualValue;
    }

    public void setNoneActualValue(Double noneActualValue) {
        this.noneActualValue = noneActualValue;
    }

    public Double getYouOweMeActualValue() {
        return youOweMeActualValue;
    }

    public void setYouOweMeActualValue(Double youOweMeActualValue) {
        this.youOweMeActualValue = youOweMeActualValue;
    }

    public Double getCashDifferenceValue() {
        return cashDifferenceValue;
    }

    public void setCashDifferenceValue(Double cashDifferenceValue) {
        this.cashDifferenceValue = cashDifferenceValue;
    }

    public Double getCardDifferenceValue() {
        return cardDifferenceValue;
    }

    public void setCardDifferenceValue(Double cardDifferenceValue) {
        this.cardDifferenceValue = cardDifferenceValue;
    }

    public Double getChequeDifferenceValue() {
        return chequeDifferenceValue;
    }

    public void setChequeDifferenceValue(Double chequeDifferenceValue) {
        this.chequeDifferenceValue = chequeDifferenceValue;
    }

    public Double getSlipDifferenceValue() {
        return slipDifferenceValue;
    }

    public void setSlipDifferenceValue(Double slipDifferenceValue) {
        this.slipDifferenceValue = slipDifferenceValue;
    }

    public Double getEwalletDifferenceValue() {
        return ewalletDifferenceValue;
    }

    public void setEwalletDifferenceValue(Double ewalletDifferenceValue) {
        this.ewalletDifferenceValue = ewalletDifferenceValue;
    }

    public Double getOnCallDifferenceValue() {
        return onCallDifferenceValue;
    }

    public void setOnCallDifferenceValue(Double onCallDifferenceValue) {
        this.onCallDifferenceValue = onCallDifferenceValue;
    }

    public Double getMultiplePaymentMethodsDifferenceValue() {
        return multiplePaymentMethodsDifferenceValue;
    }

    public void setMultiplePaymentMethodsDifferenceValue(Double multiplePaymentMethodsDifferenceValue) {
        this.multiplePaymentMethodsDifferenceValue = multiplePaymentMethodsDifferenceValue;
    }

    public Double getStaffDifferenceValue() {
        return staffDifferenceValue;
    }

    public void setStaffDifferenceValue(Double staffDifferenceValue) {
        this.staffDifferenceValue = staffDifferenceValue;
    }

    public Double getCreditDifferenceValue() {
        return creditDifferenceValue;
    }

    public void setCreditDifferenceValue(Double creditDifferenceValue) {
        this.creditDifferenceValue = creditDifferenceValue;
    }

    public Double getStaffWelfareDifferenceValue() {
        return staffWelfareDifferenceValue;
    }

    public void setStaffWelfareDifferenceValue(Double staffWelfareDifferenceValue) {
        this.staffWelfareDifferenceValue = staffWelfareDifferenceValue;
    }

    public Double getVoucherDifferenceValue() {
        return voucherDifferenceValue;
    }

    public void setVoucherDifferenceValue(Double voucherDifferenceValue) {
        this.voucherDifferenceValue = voucherDifferenceValue;
    }

    public Double getIouDifferenceValue() {
        return iouDifferenceValue;
    }

    public void setIouDifferenceValue(Double iouDifferenceValue) {
        this.iouDifferenceValue = iouDifferenceValue;
    }

    public Double getAgentDifferenceValue() {
        return agentDifferenceValue;
    }

    public void setAgentDifferenceValue(Double agentDifferenceValue) {
        this.agentDifferenceValue = agentDifferenceValue;
    }

    public Double getPatientDepositDifferenceValue() {
        return patientDepositDifferenceValue;
    }

    public void setPatientDepositDifferenceValue(Double patientDepositDifferenceValue) {
        this.patientDepositDifferenceValue = patientDepositDifferenceValue;
    }

    public Double getPatientPointsDifferenceValue() {
        return patientPointsDifferenceValue;
    }

    public void setPatientPointsDifferenceValue(Double patientPointsDifferenceValue) {
        this.patientPointsDifferenceValue = patientPointsDifferenceValue;
    }

    public Double getOnlineSettlementDifferenceValue() {
        return onlineSettlementDifferenceValue;
    }

    public void setOnlineSettlementDifferenceValue(Double onlineSettlementDifferenceValue) {
        this.onlineSettlementDifferenceValue = onlineSettlementDifferenceValue;
    }

    public Double getNoneDifferenceValue() {
        return noneDifferenceValue;
    }

    public void setNoneDifferenceValue(Double noneDifferenceValue) {
        this.noneDifferenceValue = noneDifferenceValue;
    }

    public Double getYouOweMeDifferenceValue() {
        return youOweMeDifferenceValue;
    }

    public void setYouOweMeDifferenceValue(Double youOweMeDifferenceValue) {
        this.youOweMeDifferenceValue = youOweMeDifferenceValue;
    }

    
    @Transient
    public boolean isCashValuePresent() {
        return isValuePresent(cashCalculateValue, cashActualValue, cashDifferenceValue);
    }

    @Transient
    public boolean isCardValuePresent() {
        return isValuePresent(cardCalculateValue, cardActualValue, cardDifferenceValue);
    }

    @Transient
    public boolean isChequeValuePresent() {
        return isValuePresent(chequeCalculateValue, chequeActualValue, chequeDifferenceValue);
    }

    @Transient
    public boolean isSlipValuePresent() {
        return isValuePresent(slipCalculateValue, slipActualValue, slipDifferenceValue);
    }

    @Transient
    public boolean isEwalletValuePresent() {
        return isValuePresent(ewalletCalculateValue, ewalletActualValue, ewalletDifferenceValue);
    }

    @Transient
    public boolean isOnCallValuePresent() {
        return isValuePresent(onCallCalculateValue, onCallActualValue, onCallDifferenceValue);
    }

    @Transient
    public boolean isMultiplePaymentMethodsValuePresent() {
        return isValuePresent(multiplePaymentMethodsCalculateValue, multiplePaymentMethodsActualValue, multiplePaymentMethodsDifferenceValue);
    }

    @Transient
    public boolean isStaffValuePresent() {
        return isValuePresent(staffCalculateValue, staffActualValue, staffDifferenceValue);
    }

    @Transient
    public boolean isCreditValuePresent() {
        return isValuePresent(creditCalculateValue, creditActualValue, creditDifferenceValue);
    }

    @Transient
    public boolean isStaffWelfareValuePresent() {
        return isValuePresent(staffWelfareCalculateValue, staffWelfareActualValue, staffWelfareDifferenceValue);
    }

    @Transient
    public boolean isVoucherValuePresent() {
        return isValuePresent(voucherCalculateValue, voucherActualValue, voucherDifferenceValue);
    }

    @Transient
    public boolean isIouValuePresent() {
        return isValuePresent(iouCalculateValue, iouActualValue, iouDifferenceValue);
    }

    @Transient
    public boolean isAgentValuePresent() {
        return isValuePresent(agentCalculateValue, agentActualValue, agentDifferenceValue);
    }

    @Transient
    public boolean isPatientDepositValuePresent() {
        return isValuePresent(patientDepositCalculateValue, patientDepositActualValue, patientDepositDifferenceValue);
    }

    @Transient
    public boolean isPatientPointsValuePresent() {
        return isValuePresent(patientPointsCalculateValue, patientPointsActualValue, patientPointsDifferenceValue);
    }

    @Transient
    public boolean isOnlineSettlementValuePresent() {
        return isValuePresent(onlineSettlementCalculateValue, onlineSettlementActualValue, onlineSettlementDifferenceValue);
    }

    @Transient
    public boolean isNoneValuePresent() {
        return isValuePresent(noneCalculateValue, noneActualValue, noneDifferenceValue);
    }

    @Transient
    public boolean isYouOweMeValuePresent() {
        return isValuePresent(youOweMeCalculateValue, youOweMeActualValue, youOweMeDifferenceValue);
    }

// Helper method to check if any of the given values are present
    private boolean isValuePresent(Double calculateValue, Double actualValue, Double differenceValue) {
        return (calculateValue != null && calculateValue != 0.0)
                || (actualValue != null && actualValue != 0.0)
                || (differenceValue != null && differenceValue != 0.0);
    }

}
