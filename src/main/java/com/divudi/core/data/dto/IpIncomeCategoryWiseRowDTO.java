package com.divudi.core.data.dto;

import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillType;
import com.divudi.core.data.PaymentMethod;

public class IpIncomeCategoryWiseRowDTO {

    private final Long billId;
    private final BillClassType billClassType;
    private final BillType billType;
    private final Double billDiscount;
    private final String billDeptId;
    private final Double grossValue;
    private final Double hospitalFee;
    private final Double discount;
    private final Double staffFee;
    private final Double netValue;
    private final Long itemId;
    private final String itemName;
    private final Long categoryId;
    private final String categoryName;
    private final PaymentMethod encounterPaymentMethod;
    private final PaymentMethod billPaymentMethod;

    public IpIncomeCategoryWiseRowDTO(Long billId,
            BillClassType billClassType,
            BillType billType,
            Double billDiscount,
            String billDeptId,
            Double grossValue,
            Double hospitalFee,
            Double discount,
            Double staffFee,
            Double netValue,
            Long itemId,
            String itemName,
            Long categoryId,
            String categoryName,
            PaymentMethod encounterPaymentMethod,
            PaymentMethod billPaymentMethod) {
        this.billId = billId;
        this.billClassType = billClassType;
        this.billType = billType;
        this.billDiscount = billDiscount;
        this.billDeptId = billDeptId;
        this.grossValue = grossValue;
        this.hospitalFee = hospitalFee;
        this.discount = discount;
        this.staffFee = staffFee;
        this.netValue = netValue;
        this.itemId = itemId;
        this.itemName = itemName;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.encounterPaymentMethod = encounterPaymentMethod;
        this.billPaymentMethod = billPaymentMethod;
    }

    public Long getBillId() {
        return billId;
    }

    public BillClassType getBillClassType() {
        return billClassType;
    }

    public BillType getBillType() {
        return billType;
    }

    public Double getBillDiscount() {
        return billDiscount;
    }

    public String getBillDeptId() {
        return billDeptId;
    }

    public Double getGrossValue() {
        return grossValue;
    }

    public Double getHospitalFee() {
        return hospitalFee;
    }

    public Double getDiscount() {
        return discount;
    }

    public Double getStaffFee() {
        return staffFee;
    }

    public Double getNetValue() {
        return netValue;
    }

    public Long getItemId() {
        return itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public PaymentMethod getEncounterPaymentMethod() {
        return encounterPaymentMethod;
    }

    public PaymentMethod getBillPaymentMethod() {
        return billPaymentMethod;
    }
}
