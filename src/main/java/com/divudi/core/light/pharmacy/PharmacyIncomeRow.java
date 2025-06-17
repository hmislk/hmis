package com.divudi.core.light.pharmacy;

import com.divudi.core.data.BillTypeAtomic;
import java.util.Date;

/**
 * Lightweight DTO for pharmacy income queries.
 */
public class PharmacyIncomeRow {
    private Long billId;
    private String billNo;
    private BillTypeAtomic billTypeAtomic;
    private String patientName;
    private String bhtNo;
    private Date billDate;
    private String itemName;
    private Double qty;
    private Double retailRate;
    private Double purchaseRate;

    public PharmacyIncomeRow() {
    }

    public PharmacyIncomeRow(Long billId,
                             String billNo,
                             BillTypeAtomic billTypeAtomic,
                             String patientName,
                             String bhtNo,
                             Date billDate,
                             String itemName,
                             Double qty,
                             Double retailRate,
                             Double purchaseRate) {
        this.billId = billId;
        this.billNo = billNo;
        this.billTypeAtomic = billTypeAtomic;
        this.patientName = patientName;
        this.bhtNo = bhtNo;
        this.billDate = billDate;
        this.itemName = itemName;
        this.qty = qty;
        this.retailRate = retailRate;
        this.purchaseRate = purchaseRate;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
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

    public String getBhtNo() {
        return bhtNo;
    }

    public void setBhtNo(String bhtNo) {
        this.bhtNo = bhtNo;
    }

    public Date getBillDate() {
        return billDate;
    }

    public void setBillDate(Date billDate) {
        this.billDate = billDate;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
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
}
