package com.divudi.core.data.dto;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;

import java.io.Serializable;
import java.util.Date;

/**
 * Drill-down row for the "Drug Return Op" COGS row (see
 * PharmacyReportController.calculateDrugReturnOp() /
 * processOpDrugReturnDto()). Populated directly by a single JPQL
 * {@code SELECT NEW} constructor query so the date basis, item join path,
 * and valuation formula live in exactly one place, matching the COGS row's
 * retrievePurchaseAndCostValues() rather than being re-derived independently
 * across the XHTML table, the Excel export, and the PDF export (see #22919).
 */
public class OpDrugReturnRowDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long billItemId;
    private Long billId;
    private String deptId;
    private BillTypeAtomic billTypeAtomic;
    private Long referenceBillId;
    private String referenceBillDeptId;
    private Date createdAt;
    private String itemName;
    private String itemCode;
    private Double qty;
    private Double costRate;
    private Double costValue;
    private Double purchaseRate;
    private Double purchaseValue;
    private Double mrpRate;
    private Double mrpValue;
    private PaymentMethod paymentMethod;
    private Double discount;
    private Double total;

    public OpDrugReturnRowDto() {
    }

    public OpDrugReturnRowDto(Long billItemId,
                               Long billId,
                               String deptId,
                               BillTypeAtomic billTypeAtomic,
                               Long referenceBillId,
                               String referenceBillDeptId,
                               Date createdAt,
                               String itemName,
                               String itemCode,
                               Double qty,
                               Double costRate,
                               Double costValue,
                               Double purchaseRate,
                               Double purchaseValue,
                               Double mrpRate,
                               Double mrpValue,
                               PaymentMethod paymentMethod,
                               Double discount,
                               Double total) {
        this.billItemId = billItemId;
        this.billId = billId;
        this.deptId = deptId;
        this.billTypeAtomic = billTypeAtomic;
        this.referenceBillId = referenceBillId;
        this.referenceBillDeptId = referenceBillDeptId;
        this.createdAt = createdAt;
        this.itemName = itemName;
        this.itemCode = itemCode;
        this.qty = qty;
        this.costRate = costRate != null ? costRate : 0.0;
        this.costValue = costValue != null ? costValue : 0.0;
        this.purchaseRate = purchaseRate;
        this.purchaseValue = purchaseValue;
        this.mrpRate = mrpRate;
        this.mrpValue = mrpValue;
        this.paymentMethod = paymentMethod;
        this.discount = discount != null ? discount : 0.0;
        this.total = total != null ? total : 0.0;
    }

    public Long getBillItemId() {
        return billItemId;
    }

    public void setBillItemId(Long billItemId) {
        this.billItemId = billItemId;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public String getDeptId() {
        return deptId;
    }

    public void setDeptId(String deptId) {
        this.deptId = deptId;
    }

    public BillTypeAtomic getBillTypeAtomic() {
        return billTypeAtomic;
    }

    public void setBillTypeAtomic(BillTypeAtomic billTypeAtomic) {
        this.billTypeAtomic = billTypeAtomic;
    }

    public Long getReferenceBillId() {
        return referenceBillId;
    }

    public void setReferenceBillId(Long referenceBillId) {
        this.referenceBillId = referenceBillId;
    }

    public String getReferenceBillDeptId() {
        return referenceBillDeptId;
    }

    public void setReferenceBillDeptId(String referenceBillDeptId) {
        this.referenceBillDeptId = referenceBillDeptId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public Double getQty() {
        return qty;
    }

    public void setQty(Double qty) {
        this.qty = qty;
    }

    public Double getCostRate() {
        return costRate;
    }

    public void setCostRate(Double costRate) {
        this.costRate = costRate;
    }

    public Double getCostValue() {
        return costValue;
    }

    public void setCostValue(Double costValue) {
        this.costValue = costValue;
    }

    public Double getPurchaseRate() {
        return purchaseRate;
    }

    public void setPurchaseRate(Double purchaseRate) {
        this.purchaseRate = purchaseRate;
    }

    public Double getPurchaseValue() {
        return purchaseValue;
    }

    public void setPurchaseValue(Double purchaseValue) {
        this.purchaseValue = purchaseValue;
    }

    public Double getMrpRate() {
        return mrpRate;
    }

    public void setMrpRate(Double mrpRate) {
        this.mrpRate = mrpRate;
    }

    public Double getMrpValue() {
        return mrpValue;
    }

    public void setMrpValue(Double mrpValue) {
        this.mrpValue = mrpValue;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Double getDiscount() {
        return discount;
    }

    public void setDiscount(Double discount) {
        this.discount = discount;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }
}
