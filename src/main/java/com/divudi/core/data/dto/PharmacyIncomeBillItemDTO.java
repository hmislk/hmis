package com.divudi.core.data.dto;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.PaymentScheme;

import java.io.Serializable;
import java.util.Date;

public class PharmacyIncomeBillItemDTO implements Serializable {

    private Long billId;
    private Long billItemId;
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
    private PaymentScheme paymentScheme;

    private Double qty;
    private Double retailRate;
    private Double purchaseRate;
    private Double costRate;
    private Double netRate;
    private String itemName;

    public PharmacyIncomeBillItemDTO() {
    }

    public PharmacyIncomeBillItemDTO(Long billId, Long billItemId, String deptId, String patientName, BillTypeAtomic billTypeAtomic,
                                     Date createdAt, Double netTotal, PaymentMethod paymentMethod, Double total,
                                     PatientEncounter patientEncounter, Double qty, Double retailRate, Double purchaseRate,
                                     Double netRate, String itemName) {
        this.deptId = deptId;
        this.patientName = patientName;
        this.billTypeAtomic = billTypeAtomic;
        this.createdAt = createdAt;
        this.netTotal = netTotal;
        this.paymentMethod = paymentMethod;
        this.total = total;
        this.patientEncounter = patientEncounter;
        this.qty = qty;
        this.retailRate = retailRate;
        this.purchaseRate = purchaseRate;
        this.netRate = netRate;
        this.itemName = itemName;
        this.billId = billId;
        this.billItemId = billItemId;
    }

    public PharmacyIncomeBillItemDTO(Long billId, Long billItemId, String deptId, String patientName, BillTypeAtomic billTypeAtomic,
                                     Date createdAt, Double netTotal, PaymentMethod paymentMethod, Double total,
                                     PatientEncounter patientEncounter, Double qty, Double retailRate, Double purchaseRate,
                                     Double costRate, Double netRate, String itemName) {
        this.deptId = deptId;
        this.patientName = patientName;
        this.billTypeAtomic = billTypeAtomic;
        this.createdAt = createdAt;
        this.netTotal = netTotal;
        this.paymentMethod = paymentMethod;
        this.total = total;
        this.patientEncounter = patientEncounter;
        this.qty = qty;
        this.retailRate = retailRate;
        this.purchaseRate = purchaseRate;
        this.costRate = costRate;
        this.netRate = netRate;
        this.itemName = itemName;
        this.billId = billId;
        this.billItemId = billItemId;
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

    public PaymentScheme getPaymentScheme() {
        return paymentScheme;
    }

    public void setPaymentScheme(PaymentScheme paymentScheme) {
        this.paymentScheme = paymentScheme;
    }

    public Double getQty() {
        return qty;
    }

    public void setQty(Double qty) {
        this.qty = qty;
    }

    public Double getRetailRate() {
        return retailRate;
    }

    public void setRetailRate(Double retailRate) {
        this.retailRate = retailRate;
    }

    public Double getPurchaseRate() {
        return purchaseRate;
    }

    public void setPurchaseRate(Double purchaseRate) {
        this.purchaseRate = purchaseRate;
    }

    public Double getNetRate() {
        return netRate;
    }

    public void setNetRate(Double netRate) {
        this.netRate = netRate;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public Long getBillItemId() {
        return billItemId;
    }

    public void setBillItemId(Long billItemId) {
        this.billItemId = billItemId;
    }

    public Double getCostRate() {
        return costRate;
    }

    public void setCostRate(Double costRate) {
        this.costRate = costRate;
    }
}
