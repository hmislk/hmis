package com.divudi.core.data.dto;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Item;
import com.divudi.core.data.BillType;
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
    private Double billTotal;
    private Double billNetTotal;
    private Double billDiscount;
    
    private Long itemId;
    private String itemName;
    private String itemCode;
    
    // Batch and pricing information
    private String batchNo;
    private Double purchaseRate;
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

    // Constructor for Direct Purchase table (11 parameters)
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

    public Double getRetailRate() { return retailRate; }
    public void setRetailRate(Double retailRate) { this.retailRate = retailRate; }

    public Double getMarginValue() { return marginValue; }
    public void setMarginValue(Double marginValue) { this.marginValue = marginValue; }
}
