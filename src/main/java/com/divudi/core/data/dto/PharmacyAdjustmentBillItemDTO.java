package com.divudi.core.data.dto;

import com.divudi.core.data.BillTypeAtomic;
import java.io.Serializable;
import java.util.Date;

/**
 * DTO for the pharmacy adjustment bill item search page.
 * Replaces entity-level lazy loading (N+1) with a single JPQL constructor query.
 */
public class PharmacyAdjustmentBillItemDTO implements Serializable {

    private Long billId;
    private String billDeptId;
    private Date billCreatedAt;
    private BillTypeAtomic billTypeAtomic;
    private String itemCode;
    private String itemName;
    private Boolean referenceBillCancelled;
    private Date cancelledAt;
    private Boolean referenceBillRefunded;
    private Date refundedAt;

    public PharmacyAdjustmentBillItemDTO(
            Long billId, String billDeptId, Date billCreatedAt, BillTypeAtomic billTypeAtomic,
            String itemCode, String itemName,
            Boolean referenceBillCancelled, Date cancelledAt,
            Boolean referenceBillRefunded, Date refundedAt) {
        this.billId = billId;
        this.billDeptId = billDeptId;
        this.billCreatedAt = billCreatedAt;
        this.billTypeAtomic = billTypeAtomic;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.referenceBillCancelled = referenceBillCancelled;
        this.cancelledAt = cancelledAt;
        this.referenceBillRefunded = referenceBillRefunded;
        this.refundedAt = refundedAt;
    }

    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }

    public String getBillDeptId() { return billDeptId; }
    public void setBillDeptId(String billDeptId) { this.billDeptId = billDeptId; }

    public Date getBillCreatedAt() { return billCreatedAt; }
    public void setBillCreatedAt(Date billCreatedAt) { this.billCreatedAt = billCreatedAt; }

    public BillTypeAtomic getBillTypeAtomic() { return billTypeAtomic; }
    public void setBillTypeAtomic(BillTypeAtomic billTypeAtomic) { this.billTypeAtomic = billTypeAtomic; }

    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public Boolean getReferenceBillCancelled() { return referenceBillCancelled; }
    public void setReferenceBillCancelled(Boolean referenceBillCancelled) { this.referenceBillCancelled = referenceBillCancelled; }

    public Date getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Date cancelledAt) { this.cancelledAt = cancelledAt; }

    public Boolean getReferenceBillRefunded() { return referenceBillRefunded; }
    public void setReferenceBillRefunded(Boolean referenceBillRefunded) { this.referenceBillRefunded = referenceBillRefunded; }

    public Date getRefundedAt() { return refundedAt; }
    public void setRefundedAt(Date refundedAt) { this.refundedAt = refundedAt; }
}
