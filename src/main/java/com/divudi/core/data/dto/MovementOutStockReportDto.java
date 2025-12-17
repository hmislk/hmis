
package com.divudi.core.data.dto;

import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import java.util.Date;

/**
 *
 * @author CHINTHAKA
 * This Dto used for the generate Pharmacy Movement Out with Stock Report.
 * Path - Pharmacy analytics -> movement reports -> Pharmacy Movement Out with Stock Report
 */
public class MovementOutStockReportDto {
    
    private long billId;
    private String billNo;
    private PaymentMethod paymentMethod;
    private Date createdDate;
    private String createrName;
    private BillType billType;
    private BillTypeAtomic billTypeAtomic;
    private String patientName;
    
    private double onCallValue;
    private double cashValue;
    private double cardValue;
    private double multiplePaymentMethodsValue;
    private double staffValue;
    private double creditValue;
    private double staffWelfareValue;
    private double voucherValue;
    private double iouValue;
    private double agentValue;
    private double chequeValue;
    private double slipValue;
    private double eWalletValue;
    private double patientDepositValue;
    private double patientPointsValue;
    private double onlineSettlementValue;
    private double noneValue;
    private double opdCreditValue;
    private double inpatientCreditValue;
    
    private double grossTotal;
    private double discount;
    private double serviceCharge;
    private double tax;
    private double actualTotal;
    private double netTotal;
    private double paidTotal;

    public MovementOutStockReportDto(long billId,
            String billNo,
            PaymentMethod paymentMethod, 
            Date createdDate, 
            String createrName, 
            BillType billType, 
            BillTypeAtomic billTypeAtomic, 
            String patientName, 
            double onCallValue, 
            double cashValue, 
            double cardValue, 
            double multiplePaymentMethodsValue, 
            double staffValue, 
            double creditValue, 
            double staffWelfareValue, 
            double voucherValue, 
            double iouValue, 
            double agentValue, 
            double chequeValue, 
            double slipValue, 
            double eWalletValue, 
            double patientDepositValue, 
            double patientPointsValue, 
            double onlineSettlementValue, 
            double noneValue, 
            double grossTotal, 
            double discount, 
            double serviceCharge,
            double tax,
            double actualTotal,
            double netTotal,
            double paidTotal) {
        
        this.billId = billId;
        this.billNo = billNo;
        this.paymentMethod = paymentMethod;
        this.createdDate = createdDate;
        this.createrName = createrName;
        this.billType = billType;
        this.billTypeAtomic = billTypeAtomic;
        this.patientName = patientName;
        this.onCallValue = onCallValue;
        this.cashValue = cashValue;
        this.cardValue = cardValue;
        this.multiplePaymentMethodsValue = multiplePaymentMethodsValue;
        this.staffValue = staffValue;
        this.creditValue = creditValue;
        this.staffWelfareValue = staffWelfareValue;
        this.voucherValue = voucherValue;
        this.iouValue = iouValue;
        this.agentValue = agentValue;
        this.chequeValue = chequeValue;
        this.slipValue = slipValue;
        this.eWalletValue = eWalletValue;
        this.patientDepositValue = patientDepositValue;
        this.patientPointsValue = patientPointsValue;
        this.onlineSettlementValue = onlineSettlementValue;
        this.noneValue = noneValue;
        this.grossTotal = grossTotal; 
        this.discount = discount;
        this.serviceCharge = discount;
        this.tax = tax;
        this.actualTotal = actualTotal;
        this.netTotal = netTotal;
        this.paidTotal = paidTotal;
    }

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public double getGrossTotal() {
        return grossTotal;
    }

    public void setGrossTotal(double grossTotal) {
        this.grossTotal = grossTotal;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public double getServiceCharge() {
        return serviceCharge;
    }

    public void setServiceCharge(double serviceCharge) {
        this.serviceCharge = serviceCharge;
    }

    public double getTax() {
        return tax;
    }

    public void setTax(double tax) {
        this.tax = tax;
    }

    public double getActualTotal() {
        return actualTotal;
    }

    public void setActualTotal(double actualTotal) {
        this.actualTotal = actualTotal;
    }

    public double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(double netTotal) {
        this.netTotal = netTotal;
    }

    public double getPaidTotal() {
        return paidTotal;
    }

    public void setPaidTotal(double paidTotal) {
        this.paidTotal = paidTotal;
    }
    
    

    public long getBillId() {
        return billId;
    }

    public void setBillId(long billId) {
        this.billId = billId;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public String getCreaterName() {
        return createrName;
    }

    public void setCreaterName(String createrName) {
        this.createrName = createrName;
    }

    public BillType getBillType() {
        return billType;
    }

    public void setBillType(BillType billType) {
        this.billType = billType;
    }

    public BillTypeAtomic getBillTypeAtomic() {
        return billTypeAtomic;
    }

    public void setBillTypeAtomic(BillTypeAtomic billTypeAtomic) {
        this.billTypeAtomic = billTypeAtomic;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public double getOnCallValue() {
        return onCallValue;
    }

    public void setOnCallValue(double onCallValue) {
        this.onCallValue = onCallValue;
    }

    public double getCashValue() {
        return cashValue;
    }

    public void setCashValue(double cashValue) {
        this.cashValue = cashValue;
    }

    public double getCardValue() {
        return cardValue;
    }

    public void setCardValue(double cardValue) {
        this.cardValue = cardValue;
    }

    public double getMultiplePaymentMethodsValue() {
        return multiplePaymentMethodsValue;
    }

    public void setMultiplePaymentMethodsValue(double multiplePaymentMethodsValue) {
        this.multiplePaymentMethodsValue = multiplePaymentMethodsValue;
    }

    public double getStaffValue() {
        return staffValue;
    }

    public void setStaffValue(double staffValue) {
        this.staffValue = staffValue;
    }

    public double getCreditValue() {
        return creditValue;
    }

    public void setCreditValue(double creditValue) {
        this.creditValue = creditValue;
    }

    public double getStaffWelfareValue() {
        return staffWelfareValue;
    }

    public void setStaffWelfareValue(double staffWelfareValue) {
        this.staffWelfareValue = staffWelfareValue;
    }

    public double getVoucherValue() {
        return voucherValue;
    }

    public void setVoucherValue(double voucherValue) {
        this.voucherValue = voucherValue;
    }

    public double getIouValue() {
        return iouValue;
    }

    public void setIouValue(double iouValue) {
        this.iouValue = iouValue;
    }

    public double getAgentValue() {
        return agentValue;
    }

    public void setAgentValue(double agentValue) {
        this.agentValue = agentValue;
    }

    public double getChequeValue() {
        return chequeValue;
    }

    public void setChequeValue(double chequeValue) {
        this.chequeValue = chequeValue;
    }

    public double getSlipValue() {
        return slipValue;
    }

    public void setSlipValue(double slipValue) {
        this.slipValue = slipValue;
    }

    public double geteWalletValue() {
        return eWalletValue;
    }

    public void seteWalletValue(double eWalletValue) {
        this.eWalletValue = eWalletValue;
    }

    public double getPatientDepositValue() {
        return patientDepositValue;
    }

    public void setPatientDepositValue(double patientDepositValue) {
        this.patientDepositValue = patientDepositValue;
    }

    public double getPatientPointsValue() {
        return patientPointsValue;
    }

    public void setPatientPointsValue(double patientPointsValue) {
        this.patientPointsValue = patientPointsValue;
    }

    public double getOnlineSettlementValue() {
        return onlineSettlementValue;
    }

    public void setOnlineSettlementValue(double onlineSettlementValue) {
        this.onlineSettlementValue = onlineSettlementValue;
    }

    public double getNoneValue() {
        return noneValue;
    }

    public void setNoneValue(double noneValue) {
        this.noneValue = noneValue;
    }

    public double getOpdCreditValue() {
        return opdCreditValue;
    }

    public void setOpdCreditValue(double opdCreditValue) {
        this.opdCreditValue = opdCreditValue;
    }

    public double getInpatientCreditValue() {
        return inpatientCreditValue;
    }

    public void setInpatientCreditValue(double inpatientCreditValue) {
        this.inpatientCreditValue = inpatientCreditValue;
    }
    
    
    
}
