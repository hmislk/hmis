package com.divudi.core.data.dto;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.PaymentScheme;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

public class PharmacyIncomeBillDTO implements Serializable {
    private Long billId;
    private String deptId;
    private String patientName;
    private BillTypeAtomic billTypeAtomic;
    private Date createdAt;
    private Double netTotal;
    private PaymentMethod paymentMethod;
    private Double total;
    private PatientEncounter patientEncounter;
    private Double discount;
    private Double margin;
    private Double serviceCharge;
    private PaymentScheme paymentScheme;
    private BigDecimal totalRetailSaleValue;
    private BigDecimal totalPurchaseValue;

    public PharmacyIncomeBillDTO() {
    }

    public PharmacyIncomeBillDTO(Long billId, String deptId, String patientName, BillTypeAtomic billTypeAtomic, Date createdAt, Double netTotal,
                                 PaymentMethod paymentMethod, Double total, PatientEncounter patientEncounter, Double discount, Double margin,
                                 Double serviceCharge, PaymentScheme paymentScheme, BigDecimal totalRetailSaleValue, BigDecimal totalPurchaseValue) {
        this.billId = billId;
        this.deptId = deptId;
        this.patientName = patientName;
        this.billTypeAtomic = billTypeAtomic;
        this.createdAt = createdAt;
        this.netTotal = netTotal;
        this.paymentMethod = paymentMethod;
        this.total = total;
        this.patientEncounter = patientEncounter;
        this.discount = discount;
        this.margin = margin;
        this.serviceCharge = serviceCharge;
        this.paymentScheme = paymentScheme;
        this.totalRetailSaleValue = totalRetailSaleValue;
        this.totalPurchaseValue = totalPurchaseValue;
    }

    public String getDeptId() {
        return deptId;
    }

    public void setDeptId(String deptId) {
        this.deptId = deptId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public BillTypeAtomic getBillTypeAtomic() {
        return billTypeAtomic;
    }

    public void setBillTypeAtomic(BillTypeAtomic billTypeAtomic) {
        this.billTypeAtomic = billTypeAtomic;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(Double netTotal) {
        this.netTotal = netTotal;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(PatientEncounter patientEncounter) {
        this.patientEncounter = patientEncounter;
    }

    public Double getDiscount() {
        return discount;
    }

    public void setDiscount(Double discount) {
        this.discount = discount;
    }

    public Double getMargin() {
        return margin;
    }

    public void setMargin(Double margin) {
        this.margin = margin;
    }

    public Double getServiceCharge() {
        return serviceCharge;
    }

    public void setServiceCharge(Double serviceCharge) {
        this.serviceCharge = serviceCharge;
    }

    public PaymentScheme getPaymentScheme() {
        return paymentScheme;
    }

    public void setPaymentScheme(PaymentScheme paymentScheme) {
        this.paymentScheme = paymentScheme;
    }

    public BigDecimal getTotalRetailSaleValue() {
        return totalRetailSaleValue;
    }

    public void setTotalRetailSaleValue(BigDecimal totalRetailSaleValue) {
        this.totalRetailSaleValue = totalRetailSaleValue;
    }

    public BigDecimal getTotalPurchaseValue() {
        return totalPurchaseValue;
    }

    public void setTotalPurchaseValue(BigDecimal totalPurchaseValue) {
        this.totalPurchaseValue = totalPurchaseValue;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }
}
