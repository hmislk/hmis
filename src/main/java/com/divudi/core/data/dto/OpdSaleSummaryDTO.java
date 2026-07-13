package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

public class OpdSaleSummaryDTO implements Serializable {
    // Display fields
    private String categoryName;
    private String itemName;
    private Long itemCount;
    private Double hospitalFee;
    private Double professionalFee;
    private Double grossAmount;
    private Double discountAmount;
    private Double netTotal;

    // Navigation fields - IDs for entity lookup
    private Long categoryId;
    private Long itemId;

    // Doctor/staff assigned to this item
    private String staffName;

    // Per-billing-instance fields (issue #21920): one row per BillItem so re-billing,
    // cancellation and refund events are preserved as separate rows instead of being
    // aggregated/overwritten. Populated only by the itemized service instance query.
    private Long billItemId;
    private Date billedDate;
    private String bhtNo;
    private String patientName;

    // Service charge (issue #22050): inward margin (BillItem.marginValue) added on top
    // of the gross amount. Net = Gross + Service Charge - Discount. Zero for OPD rows.
    private Double serviceCharge = 0.0;

    public OpdSaleSummaryDTO() {
    }

    // CRITICAL: Keep existing constructor for backward compatibility
    public OpdSaleSummaryDTO(String categoryName, String itemName, Long itemCount,
                              Double hospitalFee, Double professionalFee,
                              Double grossAmount, Double discountAmount,
                              Double netTotal) {
        this.categoryName = categoryName;
        this.itemName = itemName;
        this.itemCount = itemCount;
        this.hospitalFee = hospitalFee;
        this.professionalFee = professionalFee;
        this.grossAmount = grossAmount;
        this.discountAmount = discountAmount;
        this.netTotal = netTotal;
    }

    // NEW: Constructor with IDs for navigation support
    public OpdSaleSummaryDTO(Long categoryId, String categoryName,
                              Long itemId, String itemName,
                              Long itemCount,
                              Double hospitalFee, Double professionalFee,
                              Double grossAmount, Double discountAmount,
                              Double netTotal) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.itemId = itemId;
        this.itemName = itemName;
        this.itemCount = itemCount;
        this.hospitalFee = hospitalFee;
        this.professionalFee = professionalFee;
        this.grossAmount = grossAmount;
        this.discountAmount = discountAmount;
        this.netTotal = netTotal;
    }

    // NEW: Constructor with staff name for doctor display in reports
    public OpdSaleSummaryDTO(Long categoryId, String categoryName,
                              Long itemId, String itemName,
                              Long itemCount,
                              Double hospitalFee, Double professionalFee,
                              Double grossAmount, Double discountAmount,
                              Double netTotal,
                              String staffName) {
        this(categoryId, categoryName, itemId, itemName, itemCount,
             hospitalFee, professionalFee, grossAmount, discountAmount, netTotal);
        this.staffName = staffName;
    }

    // NEW (issue #21920): Per-billing-instance constructor. One row per BillItem, with
    // billed date and patient (BHT + name) so inpatient billing history is preserved
    // and traceable. Used by the Itemized Service Summary (combined OPD + Inward) report.
    public OpdSaleSummaryDTO(Long categoryId, String categoryName,
                              Long itemId, String itemName,
                              Long billItemId, Date billedDate,
                              String bhtNo, String patientName,
                              Long itemCount,
                              Double hospitalFee, Double professionalFee,
                              Double grossAmount, Double discountAmount,
                              Double netTotal) {
        this(categoryId, categoryName, itemId, itemName, itemCount,
             hospitalFee, professionalFee, grossAmount, discountAmount, netTotal);
        this.billItemId = billItemId;
        this.billedDate = billedDate;
        this.bhtNo = bhtNo;
        this.patientName = patientName;
    }

    // NEW (issue #22050): Per-billing-instance constructor with the service charge
    // (inward margin). Delegates to the #21920 per-instance constructor.
    public OpdSaleSummaryDTO(Long categoryId, String categoryName,
                              Long itemId, String itemName,
                              Long billItemId, Date billedDate,
                              String bhtNo, String patientName,
                              Long itemCount,
                              Double hospitalFee, Double professionalFee,
                              Double grossAmount, Double discountAmount,
                              Double netTotal,
                              Double serviceCharge) {
        this(categoryId, categoryName, itemId, itemName, billItemId, billedDate,
             bhtNo, patientName, itemCount,
             hospitalFee, professionalFee, grossAmount, discountAmount, netTotal);
        this.serviceCharge = serviceCharge != null ? serviceCharge : 0.0;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Long getItemCount() {
        return itemCount;
    }

    public void setItemCount(Long itemCount) {
        this.itemCount = itemCount;
    }

    public Double getHospitalFee() {
        return hospitalFee;
    }

    public void setHospitalFee(Double hospitalFee) {
        this.hospitalFee = hospitalFee;
    }

    public Double getProfessionalFee() {
        return professionalFee;
    }

    public void setProfessionalFee(Double professionalFee) {
        this.professionalFee = professionalFee;
    }

    public Double getGrossAmount() {
        return grossAmount;
    }

    public void setGrossAmount(Double grossAmount) {
        this.grossAmount = grossAmount;
    }

    public Double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(Double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public Double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(Double netTotal) {
        this.netTotal = netTotal;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public Long getBillItemId() {
        return billItemId;
    }

    public void setBillItemId(Long billItemId) {
        this.billItemId = billItemId;
    }

    public Date getBilledDate() {
        return billedDate;
    }

    public void setBilledDate(Date billedDate) {
        this.billedDate = billedDate;
    }

    public String getBhtNo() {
        return bhtNo;
    }

    public void setBhtNo(String bhtNo) {
        this.bhtNo = bhtNo;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public Double getServiceCharge() {
        return serviceCharge;
    }

    public void setServiceCharge(Double serviceCharge) {
        this.serviceCharge = serviceCharge;
    }
}
