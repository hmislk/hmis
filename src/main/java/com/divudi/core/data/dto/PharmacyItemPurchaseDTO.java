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
    private BillType billType;
    private Double billTotal;
    private Double billNetTotal;
    private Double billDiscount;
    
    private Long itemId;
    private String itemName;
    private String itemCode;

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
}
