package com.divudi.core.data.dto;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.DepartmentType;
import java.io.Serializable;
import java.util.Date;

/**
 * One free-item line received through a GRN or Direct Purchase.
 */
public class PharmacyFreeItemReceivedDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long billId;
    private Date receivedDate;
    private String grnNo;
    private String invoiceNo;
    private BillTypeAtomic billTypeAtomic;
    private String departmentName;
    private DepartmentType departmentType;
    private Long supplierId;
    private String supplierName;
    private Long itemId;
    private String itemCode;
    private String itemName;
    private Double paidQty;
    private Double freeQty;
    private Double purchaseRate;
    private Long billItemId;
    private double returnedPaidQty;
    private double returnedFreeQty;

    public PharmacyFreeItemReceivedDTO() {
    }

    public PharmacyFreeItemReceivedDTO(Long billId, Date receivedDate, String grnNo, String invoiceNo,
            BillTypeAtomic billTypeAtomic, String departmentName, DepartmentType departmentType,
            Long supplierId, String supplierName, Long itemId, String itemCode, String itemName,
            Double paidQty, Double freeQty, Double purchaseRate) {
        this.billId = billId;
        this.receivedDate = receivedDate;
        this.grnNo = grnNo;
        this.invoiceNo = invoiceNo;
        this.billTypeAtomic = billTypeAtomic;
        this.departmentName = departmentName;
        this.departmentType = departmentType;
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.itemId = itemId;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.paidQty = paidQty;
        this.freeQty = freeQty;
        this.purchaseRate = purchaseRate;
    }

    /**
     * Same as the 15-argument constructor, plus the GRN / Direct Purchase bill
     * item id so that returns against the line can be matched (issue #24667).
     */
    public PharmacyFreeItemReceivedDTO(Long billItemId, Long billId, Date receivedDate, String grnNo, String invoiceNo,
            BillTypeAtomic billTypeAtomic, String departmentName, DepartmentType departmentType,
            Long supplierId, String supplierName, Long itemId, String itemCode, String itemName,
            Double paidQty, Double freeQty, Double purchaseRate) {
        this(billId, receivedDate, grnNo, invoiceNo, billTypeAtomic, departmentName, departmentType,
                supplierId, supplierName, itemId, itemCode, itemName, paidQty, freeQty, purchaseRate);
        this.billItemId = billItemId;
    }

    /**
     * Paid quantity received less the paid quantity returned to the supplier.
     */
    public double getNetPaidQty() {
        double q = paidQty == null ? 0.0 : paidQty;
        return q - returnedPaidQty;
    }

    /**
     * Free quantity received less the free quantity returned to the supplier.
     */
    public double getNetFreeQty() {
        double q = freeQty == null ? 0.0 : freeQty;
        return q - returnedFreeQty;
    }

    /**
     * Value of the free quantity still held, i.e. after free returns.
     */
    public double getFreeValue() {
        double r = purchaseRate == null ? 0.0 : purchaseRate;
        return getNetFreeQty() * r;
    }

    public String getBillTypeLabel() {
        if (billTypeAtomic == null) {
            return "";
        }
        if (billTypeAtomic == BillTypeAtomic.PHARMACY_GRN) {
            return "GRN";
        }
        if (billTypeAtomic == BillTypeAtomic.PHARMACY_DIRECT_PURCHASE) {
            return "Direct Purchase";
        }
        String l = billTypeAtomic.getLabel();
        return l != null ? l : billTypeAtomic.name();
    }

    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }
    public Date getReceivedDate() { return receivedDate; }
    public void setReceivedDate(Date receivedDate) { this.receivedDate = receivedDate; }
    public String getGrnNo() { return grnNo; }
    public void setGrnNo(String grnNo) { this.grnNo = grnNo; }
    public String getInvoiceNo() { return invoiceNo; }
    public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }
    public BillTypeAtomic getBillTypeAtomic() { return billTypeAtomic; }
    public void setBillTypeAtomic(BillTypeAtomic billTypeAtomic) { this.billTypeAtomic = billTypeAtomic; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public DepartmentType getDepartmentType() { return departmentType; }
    public void setDepartmentType(DepartmentType departmentType) { this.departmentType = departmentType; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public Double getPaidQty() { return paidQty; }
    public void setPaidQty(Double paidQty) { this.paidQty = paidQty; }
    public Double getFreeQty() { return freeQty; }
    public void setFreeQty(Double freeQty) { this.freeQty = freeQty; }
    public Double getPurchaseRate() { return purchaseRate; }
    public void setPurchaseRate(Double purchaseRate) { this.purchaseRate = purchaseRate; }
    public Long getBillItemId() { return billItemId; }
    public void setBillItemId(Long billItemId) { this.billItemId = billItemId; }
    public double getReturnedPaidQty() { return returnedPaidQty; }
    public void setReturnedPaidQty(double returnedPaidQty) { this.returnedPaidQty = returnedPaidQty; }
    public double getReturnedFreeQty() { return returnedFreeQty; }
    public void setReturnedFreeQty(double returnedFreeQty) { this.returnedFreeQty = returnedFreeQty; }
}
