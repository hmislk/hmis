package com.divudi.core.data.dto;

import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import java.io.Serializable;
import java.util.Date;

/**
 * DTO for BillItem details needed for Daily Return reports.
 */
public class BillItemDetailDTO implements Serializable {
    private String categoryName;
    private String itemName;
    private Long categoryId;
    private Long itemId;
    private Double grossValue;
    private Double hospitalFee;
    private Double discount;
    private Double professionalFee;
    private Double netValue;
    private Double qty;
    private PaymentMethod paymentMethod;
    private BillTypeAtomic billTypeAtomic;
    private BillClassType billClassType;
    private String departmentName;
    private Date createdAt;

    public BillItemDetailDTO() {
    }

    // New constructor with BillClassType (preferred)
    public BillItemDetailDTO(String categoryName, String itemName, Long categoryId, Long itemId,
                            Double grossValue, Double hospitalFee, Double discount, Double staffFee,
                            Double netValue, Double qty, PaymentMethod paymentMethod, 
                            BillTypeAtomic billTypeAtomic, BillClassType billClassType, 
                            String departmentName, Date createdAt) {
        this.categoryName = categoryName;
        this.itemName = itemName;
        this.categoryId = categoryId;
        this.itemId = itemId;
        this.grossValue = grossValue;
        this.hospitalFee = hospitalFee;
        this.discount = discount;
        this.professionalFee = staffFee;  // Map staffFee to professionalFee for consistency
        this.netValue = netValue;
        this.qty = qty;
        this.paymentMethod = paymentMethod;
        this.billTypeAtomic = billTypeAtomic;
        this.billClassType = billClassType;
        this.departmentName = departmentName;
        this.createdAt = createdAt;
    }

    // Backward compatible constructor (without BillClassType) - defaults to null BillClassType
    public BillItemDetailDTO(String categoryName, String itemName, Long categoryId, Long itemId,
                            Double grossValue, Double hospitalFee, Double discount, Double staffFee,
                            Double netValue, Double qty, PaymentMethod paymentMethod, 
                            BillTypeAtomic billTypeAtomic, String departmentName, Date createdAt) {
        this(categoryName, itemName, categoryId, itemId, grossValue, hospitalFee, discount, staffFee,
             netValue, qty, paymentMethod, billTypeAtomic, null, departmentName, createdAt);
    }

    public String getCategoryName() {
        return categoryName != null ? categoryName : "No Category";
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getItemName() {
        return itemName != null ? itemName : "No Item";
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
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

    public Double getGrossValue() {
        return grossValue != null ? grossValue : 0.0;
    }

    public void setGrossValue(Double grossValue) {
        this.grossValue = grossValue;
    }

    public Double getHospitalFee() {
        return hospitalFee != null ? hospitalFee : 0.0;
    }

    public void setHospitalFee(Double hospitalFee) {
        this.hospitalFee = hospitalFee;
    }

    public Double getDiscount() {
        return discount != null ? discount : 0.0;
    }

    public void setDiscount(Double discount) {
        this.discount = discount;
    }

    public Double getProfessionalFee() {
        return professionalFee != null ? professionalFee : 0.0;
    }

    public void setProfessionalFee(Double professionalFee) {
        this.professionalFee = professionalFee;
    }

    public Double getNetValue() {
        return netValue != null ? netValue : 0.0;
    }

    public void setNetValue(Double netValue) {
        this.netValue = netValue;
    }

    public Double getQty() {
        return qty != null ? qty : 0.0;
    }

    public void setQty(Double qty) {
        this.qty = qty;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public BillTypeAtomic getBillTypeAtomic() {
        return billTypeAtomic;
    }

    public void setBillTypeAtomic(BillTypeAtomic billTypeAtomic) {
        this.billTypeAtomic = billTypeAtomic;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public BillClassType getBillClassType() {
        return billClassType;
    }

    public void setBillClassType(BillClassType billClassType) {
        this.billClassType = billClassType;
    }
}