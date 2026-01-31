package com.divudi.core.data.dto;

import com.divudi.core.data.PaymentMethod;
import java.io.Serializable;
import java.util.Date;

public class BillItemDTO implements Serializable {

    private Date billCreatedAt;
    private String itemName;
    private String itemCode;
    private String billDeptId;
    private String batchNo;
    private Double qty;
    private Double costRate;
    private Double purchaseRate;
    private Double retailRate;
    private Double billNetTotal;
    private Long id;
    private Long billId;
    private Double discount;
    private PaymentMethod paymentMethod;
    private Long itemId;
    private String itemClass;
    private Double marginValue;

    public BillItemDTO() {
    }

    public BillItemDTO(Date billCreatedAt, String itemName, String itemCode,
            String billDeptId, String batchNo, Double qty,
            Double costRate, Double retailRate, Double billNetTotal) {
        this.billCreatedAt = billCreatedAt;
        this.itemName = itemName;
        this.itemCode = itemCode;
        this.billDeptId = billDeptId;
        this.batchNo = batchNo;
        this.qty = qty;
        this.costRate = costRate;
        this.retailRate = retailRate;
        this.billNetTotal = billNetTotal;
    }

    public BillItemDTO(Long id, Date billCreatedAt, Double discount, Double billNetTotal, PaymentMethod paymentMethod) {
        this.id = id;
        this.billCreatedAt = billCreatedAt;
        this.discount = discount;
        this.billNetTotal = billNetTotal;
        this.paymentMethod = paymentMethod;
    }

    //Cost of Good Sold report-sale report
    public BillItemDTO(Long id, Long billId, Date createdAt, String name, String code,
            String deptId, String batchNo, Double qty, Double costRate, Double purchaseRate,
            Double retailRate, Double netTotal) {
        this.id = id;
        this.billId = billId;
        this.billCreatedAt = createdAt;
        this.itemName = name;
        this.itemCode = code;
        this.billDeptId = deptId;
        this.batchNo = batchNo;
        this.qty = qty;
        this.costRate = costRate;
        this.purchaseRate = purchaseRate;
        this.retailRate = retailRate;
        this.billNetTotal = netTotal;
    }
    
    //Use 9B Report
    public BillItemDTO(Long id, Long billId, Long itemId, PaymentMethod paymentMethod, String itemClass, Double billNetTotal, Double discount, Double marginValue) {
        this.id = id;
        this.billId = billId;
        this.itemId = itemId;
        this.paymentMethod = paymentMethod;
        this.itemClass = itemClass;
        this.billNetTotal = billNetTotal;
        this.discount = discount;
        this.marginValue = marginValue;
    }

    //Use OPD BILL ITEM LIST FOR CREDIT COMPANIES Report
    public BillItemDTO( Long id, String itemName) {
        this.id = id;
        this.itemName = itemName;
    }
    
        
    // Calculated fields (Cost Value and Sale Value are calculated in JSF)
    public Double getCostValue() {
        return (costRate != null && qty != null) ? costRate * qty : 0.0;
    }

    public Double getSaleValue() {
        return (retailRate != null && qty != null) ? retailRate * qty : 0.0;
    }

    // Getters and setters
    public Date getBillCreatedAt() {
        return billCreatedAt;
    }

    public void setBillCreatedAt(Date billCreatedAt) {
        this.billCreatedAt = billCreatedAt;
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

    public String getBillDeptId() {
        return billDeptId;
    }

    public void setBillDeptId(String billDeptId) {
        this.billDeptId = billDeptId;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
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

    public Double getRetailRate() {
        return retailRate;
    }

    public void setRetailRate(Double retailRate) {
        this.retailRate = retailRate;
    }

    public Double getBillNetTotal() {
        return billNetTotal;
    }

    public void setBillNetTotal(Double billNetTotal) {
        this.billNetTotal = billNetTotal;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getDiscount() {
        return discount;
    }

    public void setDiscount(Double discount) {
        this.discount = discount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public Double getPurchaseRate() {
        return purchaseRate;
    }

    public void setPurchaseRate(Double purchaseRate) {
        this.purchaseRate = purchaseRate;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getItemClass() {
        return itemClass;
    }

    public void setItemClass(String itemClass) {
        this.itemClass = itemClass;
    }

    public Double getMarginValue() {
        return marginValue;
    }

    public void setMarginValue(Double marginValue) {
        this.marginValue = marginValue;
    }

}
