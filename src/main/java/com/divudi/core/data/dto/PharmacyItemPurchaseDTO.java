package com.divudi.core.data.dto;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Item;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import java.io.Serializable;
import java.util.Date;

public class PharmacyItemPurchaseDTO implements Serializable {

    // Legacy entity references - KEEP for backward compatibility
    private Bill bill;
    private Item item;
    private Double qty;
    private Double freeQty;
    
    // New primitive fields for proper DTO usage
    private Long billId;
    private String billDeptId;
    private Date billCreatedAt;
    private String billInstitutionName;
    private String billDepartmentName;
    private String billFromInstitutionName;
    private String billCreaterName;
    private BillType billType;
    private BillTypeAtomic billTypeAtomic;
    private Boolean cancelled;
    private Boolean refunded;
    private Double billTotal;
    private Double billNetTotal;
    private Double billDiscount;
    
    private Long itemId;
    private String itemName;
    private String itemCode;
    
    // Batch and pricing information
    private String batchNo;
    private Double purchaseRate;
    private Double costRate;
    private Double retailRate;
    private Double marginValue;

    // LEGACY constructor - KEEP for backward compatibility
    public PharmacyItemPurchaseDTO(Bill bill, Item item, Double qty, Double freeQty) {
        this.bill = bill;
        this.item = item;
        this.qty = qty;
        this.freeQty = freeQty;
    }

    // NEW constructor for proper DTO usage with primitives only
    public PharmacyItemPurchaseDTO(Long billId, String billDeptId, Date billCreatedAt, 
                                   String billInstitutionName, String billDepartmentName, 
                                   String billFromInstitutionName, BillType billType,
                                   Double billTotal, Double billNetTotal, Double billDiscount,
                                   Long itemId, String itemName, String itemCode,
                                   Double qty, Double freeQty) {
        this.billId = billId;
        this.billDeptId = billDeptId;
        this.billCreatedAt = billCreatedAt;
        this.billInstitutionName = billInstitutionName;
        this.billDepartmentName = billDepartmentName;
        this.billFromInstitutionName = billFromInstitutionName;
        this.billType = billType;
        this.billTotal = billTotal;
        this.billNetTotal = billNetTotal;
        this.billDiscount = billDiscount;
        this.itemId = itemId;
        this.itemName = itemName;
        this.itemCode = itemCode;
        this.qty = qty;
        this.freeQty = freeQty;
    }

    // Constructor for BY_BILL report (no item details)
    public PharmacyItemPurchaseDTO(Long billId, String billDeptId, Date billCreatedAt, 
                                   String billInstitutionName, String billDepartmentName, 
                                   String billFromInstitutionName, BillType billType,
                                   Double billTotal, Double billNetTotal, Double billDiscount,
                                   Double qty, Double freeQty) {
        this.billId = billId;
        this.billDeptId = billDeptId;
        this.billCreatedAt = billCreatedAt;
        this.billInstitutionName = billInstitutionName;
        this.billDepartmentName = billDepartmentName;
        this.billFromInstitutionName = billFromInstitutionName;
        this.billType = billType;
        this.billTotal = billTotal;
        this.billNetTotal = billNetTotal;
        this.billDiscount = billDiscount;
        this.qty = qty;
        this.freeQty = freeQty;
    }

    // Constructor for BY_BILL report without qty and free qty
    public PharmacyItemPurchaseDTO(Long billId, String billDeptId, Date billCreatedAt,
                                   String billInstitutionName, String billDepartmentName,
                                   String billFromInstitutionName, BillType billType,
                                   Double billTotal, Double billNetTotal, Double billDiscount) {
        this.billId = billId;
        this.billDeptId = billDeptId;
        this.billCreatedAt = billCreatedAt;
        this.billInstitutionName = billInstitutionName;
        this.billDepartmentName = billDepartmentName;
        this.billFromInstitutionName = billFromInstitutionName;
        this.billType = billType;
        this.billTotal = billTotal;
        this.billNetTotal = billNetTotal;
        this.billDiscount = billDiscount;
    }

    // Constructor for Pharmacy Issue list with billTypeAtomic, cancelled, and refunded fields
    public PharmacyItemPurchaseDTO(Long billId, String billDeptId, Date billCreatedAt,
                                   String billInstitutionName, String billDepartmentName,
                                   String billFromInstitutionName, BillType billType,
                                   BillTypeAtomic billTypeAtomic, Boolean cancelled, Boolean refunded,
                                   Double billTotal, Double billNetTotal, Double billDiscount) {
        this.billId = billId;
        this.billDeptId = billDeptId;
        this.billCreatedAt = billCreatedAt;
        this.billInstitutionName = billInstitutionName;
        this.billDepartmentName = billDepartmentName;
        this.billFromInstitutionName = billFromInstitutionName;
        this.billType = billType;
        this.billTypeAtomic = billTypeAtomic;
        this.cancelled = cancelled;
        this.refunded = refunded;
        this.billTotal = billTotal;
        this.billNetTotal = billNetTotal;
        this.billDiscount = billDiscount;
    }

//    // Constructor for disposal item reports without batch information (10 parameters)
//    public PharmacyItemPurchaseDTO(Long billId, String billDeptId, Date billCreatedAt, 
//                                   String itemCode, String itemName, String billFromInstitutionName, 
//                                   BillType billType, Double billNetTotal, Double qty, Double billTotal) {
//        this.billId = billId;
//        this.billDeptId = billDeptId;
//        this.billCreatedAt = billCreatedAt;
//        this.itemCode = itemCode;
//        this.itemName = itemName;
//        this.billFromInstitutionName = billFromInstitutionName;
//        this.billType = billType;
//        this.billNetTotal = billNetTotal;
//        this.qty = qty;
//        this.billTotal = billTotal;
//    }

    // Constructor for disposal item reports with batch information (14 parameters)
    public PharmacyItemPurchaseDTO(Long billId, String billDeptId, Date billCreatedAt, 
                                   String itemCode, String itemName, String billFromInstitutionName, 
                                   BillType billType, Double billNetTotal, Double qty, Double billTotal,
                                   String batchNo, Double purchaseRate, Double retailRate, Double marginValue) {
        this.billId = billId;
        this.billDeptId = billDeptId;
        this.billCreatedAt = billCreatedAt;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.billFromInstitutionName = billFromInstitutionName;
        this.billType = billType;
        this.billNetTotal = billNetTotal;
        this.qty = qty;
        this.billTotal = billTotal;
        this.batchNo = batchNo;
        this.purchaseRate = purchaseRate;
        this.retailRate = retailRate;
        this.marginValue = marginValue;
    }

    public PharmacyItemPurchaseDTO(Long billId, String billDeptId, Date billCreatedAt,
                                   String itemCode, String itemName, BillType billType,
                                   String billFromInstitutionName, Double billNetTotal,
                                   Double qty, Double billTotal) {
        this.billId = billId;
        this.billDeptId = billDeptId;
        this.billCreatedAt = billCreatedAt;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.billType = billType;
        this.billFromInstitutionName = billFromInstitutionName;
        this.billNetTotal = billNetTotal;
        this.qty = qty;
        this.billTotal = billTotal;
    }

    // Constructor for Direct Purchase table (10 parameters) - LEGACY - KEEP for backward compatibility
    public PharmacyItemPurchaseDTO(Long billId, String billDeptId, String billFromInstitutionName,
                                   String billCreaterName, Date billCreatedAt, Double purchaseRate,
                                   Double retailRate, Double qty, Double freeQty, Double billNetTotal) {
        this.billId = billId;
        this.billDeptId = billDeptId;
        this.billFromInstitutionName = billFromInstitutionName;
        this.billCreaterName = billCreaterName;
        this.billCreatedAt = billCreatedAt;
        this.purchaseRate = purchaseRate;
        this.retailRate = retailRate;
        this.qty = qty;
        this.freeQty = freeQty;
        this.billNetTotal = billNetTotal;
    }

    // Constructor for Direct Purchase table with costRate (11 parameters)
    public PharmacyItemPurchaseDTO(Long billId, String billDeptId, String billFromInstitutionName,
                                   String billCreaterName, Date billCreatedAt, Double purchaseRate,
                                   Double costRate, Double retailRate, Double qty, Double freeQty, Double billNetTotal) {
        this.billId = billId;
        this.billDeptId = billDeptId;
        this.billFromInstitutionName = billFromInstitutionName;
        this.billCreaterName = billCreaterName;
        this.billCreatedAt = billCreatedAt;
        this.purchaseRate = purchaseRate;
        this.costRate = costRate;
        this.retailRate = retailRate;
        this.qty = qty;
        this.freeQty = freeQty;
        this.billNetTotal = billNetTotal;
    }

    // Constructor for Direct Purchase table with costRate and itemName (12 parameters)
    public PharmacyItemPurchaseDTO(Long billId, String billDeptId, String billFromInstitutionName,
                                   String billCreaterName, Date billCreatedAt, Double purchaseRate,
                                   Double costRate, Double retailRate, Double qty, Double freeQty,
                                   Double billNetTotal, String itemName) {
        this.billId = billId;
        this.billDeptId = billDeptId;
        this.billFromInstitutionName = billFromInstitutionName;
        this.billCreaterName = billCreaterName;
        this.billCreatedAt = billCreatedAt;
        this.purchaseRate = purchaseRate;
        this.costRate = costRate;
        this.retailRate = retailRate;
        this.qty = qty;
        this.freeQty = freeQty;
        this.billNetTotal = billNetTotal;
        this.itemName = itemName;
    }

    // Constructor for Direct Purchase table with BigDecimal types (12 parameters)
    // NEW: Uses BigDecimal for proper financial precision
    public PharmacyItemPurchaseDTO(Long billId, String billDeptId, String billFromInstitutionName,
                                   String billCreaterName, Date billCreatedAt,
                                   java.math.BigDecimal purchaseRate, java.math.BigDecimal costRate,
                                   java.math.BigDecimal retailRate, java.math.BigDecimal qty,
                                   java.math.BigDecimal freeQty, java.math.BigDecimal billNetTotal,
                                   String itemName) {
        this.billId = billId;
        this.billDeptId = billDeptId;
        this.billFromInstitutionName = billFromInstitutionName;
        this.billCreaterName = billCreaterName;
        this.billCreatedAt = billCreatedAt;
        this.purchaseRate = (purchaseRate != null) ? purchaseRate.doubleValue() : null;
        this.costRate = (costRate != null) ? costRate.doubleValue() : null;
        this.retailRate = (retailRate != null) ? retailRate.doubleValue() : null;
        this.qty = (qty != null) ? qty.doubleValue() : null;
        this.freeQty = (freeQty != null) ? freeQty.doubleValue() : null;
        this.billNetTotal = (billNetTotal != null) ? billNetTotal.doubleValue() : null;
        this.itemName = itemName;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Double getQty() {
        return qty;
    }

    public void setQty(Double qty) {
        this.qty = qty;
    }

    public Double getFreeQty() {
        return freeQty;
    }

    public void setFreeQty(Double freeQty) {
        this.freeQty = freeQty;
    }

    // Getters and setters for primitive DTO fields
    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }

    public String getBillDeptId() { return billDeptId; }
    public void setBillDeptId(String billDeptId) { this.billDeptId = billDeptId; }

    public Date getBillCreatedAt() { return billCreatedAt; }
    public void setBillCreatedAt(Date billCreatedAt) { this.billCreatedAt = billCreatedAt; }

    public String getBillInstitutionName() { return billInstitutionName; }
    public void setBillInstitutionName(String billInstitutionName) { this.billInstitutionName = billInstitutionName; }

    public String getBillDepartmentName() { return billDepartmentName; }
    public void setBillDepartmentName(String billDepartmentName) { this.billDepartmentName = billDepartmentName; }

    public String getBillFromInstitutionName() { return billFromInstitutionName; }
    public void setBillFromInstitutionName(String billFromInstitutionName) { this.billFromInstitutionName = billFromInstitutionName; }

    public String getBillCreaterName() { return billCreaterName; }
    public void setBillCreaterName(String billCreaterName) { this.billCreaterName = billCreaterName; }

    public BillType getBillType() { return billType; }
    public void setBillType(BillType billType) { this.billType = billType; }

    public BillTypeAtomic getBillTypeAtomic() { return billTypeAtomic; }
    public void setBillTypeAtomic(BillTypeAtomic billTypeAtomic) { this.billTypeAtomic = billTypeAtomic; }

    public Boolean getCancelled() { return cancelled; }
    public void setCancelled(Boolean cancelled) { this.cancelled = cancelled; }

    public Boolean getRefunded() { return refunded; }
    public void setRefunded(Boolean refunded) { this.refunded = refunded; }

    public Double getBillTotal() { return billTotal; }
    public void setBillTotal(Double billTotal) { this.billTotal = billTotal; }

    public Double getBillNetTotal() { return billNetTotal; }
    public void setBillNetTotal(Double billNetTotal) { this.billNetTotal = billNetTotal; }

    public Double getBillDiscount() { return billDiscount; }
    public void setBillDiscount(Double billDiscount) { this.billDiscount = billDiscount; }

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }

    // Getter and setter methods for batch information
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }

    public Double getPurchaseRate() { return purchaseRate; }
    public void setPurchaseRate(Double purchaseRate) { this.purchaseRate = purchaseRate; }

    public Double getCostRate() { return costRate; }
    public void setCostRate(Double costRate) { this.costRate = costRate; }

    public Double getRetailRate() { return retailRate; }
    public void setRetailRate(Double retailRate) { this.retailRate = retailRate; }

    public Double getMarginValue() { return marginValue; }
    public void setMarginValue(Double marginValue) { this.marginValue = marginValue; }
}
