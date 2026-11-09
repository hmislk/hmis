package com.divudi.core.data.dto;

import com.divudi.core.data.BillTypeAtomic;
import java.io.Serializable;
import java.util.Date;

/**
 * One bill item row of the Inpatient Direct Issue report (#24760).
 * Rates are Double because they come from LEFT JOINs and may be null.
 */
public class InpatientDirectIssueItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long billItemId;
    private Long billId;
    private String billNo;
    private Date billDate;
    private BillTypeAtomic billTypeAtomic;
    private Long departmentId;
    private String departmentName;
    private Long encounterId;
    private String bhtNo;
    private String patientName;
    private String patientPhn;
    private Long itemId;
    private String itemCode;
    private String itemName;
    private Double qty;
    private Double retailRate;
    private Double costRate;
    private Double purchaseRate;
    private Double grossValue;

    // Computed by the controller, signed (negative for returns/cancellations)
    private double signedQty;
    private double retailValue;
    private double costValue;
    private boolean negative;

    public InpatientDirectIssueItemDTO() {
    }

    public InpatientDirectIssueItemDTO(Long billItemId, Long billId, String billNo, Date billDate,
            BillTypeAtomic billTypeAtomic, Long departmentId, String departmentName,
            Long encounterId, String bhtNo, String patientName, String patientPhn,
            Long itemId, String itemCode, String itemName, Double qty,
            Double retailRate, Double costRate, Double purchaseRate, Double grossValue) {
        this.billItemId = billItemId;
        this.billId = billId;
        this.billNo = billNo;
        this.billDate = billDate;
        this.billTypeAtomic = billTypeAtomic;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.encounterId = encounterId;
        this.bhtNo = bhtNo;
        this.patientName = patientName;
        this.patientPhn = patientPhn;
        this.itemId = itemId;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.qty = qty;
        this.retailRate = retailRate;
        this.costRate = costRate;
        this.purchaseRate = purchaseRate;
        this.grossValue = grossValue;
    }

    public Long getBillItemId() { return billItemId; }
    public Long getBillId() { return billId; }
    public String getBillNo() { return billNo; }
    public Date getBillDate() { return billDate; }
    public BillTypeAtomic getBillTypeAtomic() { return billTypeAtomic; }
    public Long getDepartmentId() { return departmentId; }
    public String getDepartmentName() { return departmentName; }
    public Long getEncounterId() { return encounterId; }
    public String getBhtNo() { return bhtNo; }
    public String getPatientName() { return patientName; }
    public String getPatientPhn() { return patientPhn; }
    public Long getItemId() { return itemId; }
    public String getItemCode() { return itemCode; }
    public String getItemName() { return itemName; }
    public Double getQty() { return qty; }
    public Double getRetailRate() { return retailRate; }
    public Double getCostRate() { return costRate; }
    public Double getPurchaseRate() { return purchaseRate; }
    public Double getGrossValue() { return grossValue; }

    public double getSignedQty() { return signedQty; }
    public void setSignedQty(double signedQty) { this.signedQty = signedQty; }
    public double getRetailValue() { return retailValue; }
    public void setRetailValue(double retailValue) { this.retailValue = retailValue; }
    public double getCostValue() { return costValue; }
    public void setCostValue(double costValue) { this.costValue = costValue; }
    public boolean isNegative() { return negative; }
    public void setNegative(boolean negative) { this.negative = negative; }
}
