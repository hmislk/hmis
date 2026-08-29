package com.divudi.core.data.dto;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.Bill;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CostOfGoodSoldBillDTO implements Serializable {

    private Bill bill;
    private Date billCreatedAt;
    private String billDeptId;
    private Double netTotal;
    private PaymentMethod paymentMethod;
    private Double discount;
    private Long billId;
    private List<BillItemDTO> billItems;
    private Map<Long, Object> billItemsMap = new HashMap<>();

    public CostOfGoodSoldBillDTO() {
    }

    public CostOfGoodSoldBillDTO(Bill bill, Date billCreatedAt,
            String billDeptId, Double netTotal, PaymentMethod paymentMethod,
            Double discount, Long billId, Map<Long, Object> billItemsMap) {
        this.bill = bill;
        this.billCreatedAt = billCreatedAt;
        this.billDeptId = billDeptId;
        this.netTotal = netTotal;
        this.paymentMethod = paymentMethod;
        this.discount = discount;
        this.billId = billId;
        this.billItemsMap = billItemsMap;
    }

    public CostOfGoodSoldBillDTO(Bill bill, Date billCreatedAt,
            String billDeptId, Double netTotal, PaymentMethod paymentMethod,
            Double discount, Long billId) {
        this.bill = bill;
        this.billCreatedAt = billCreatedAt;
        this.billDeptId = billDeptId;
        this.netTotal = netTotal;
        this.paymentMethod = paymentMethod;
        this.discount = discount;
        this.billId = billId;
    }

    public Date getBillCreatedAt() {
        return billCreatedAt;
    }

    public void setBillCreatedAt(Date billCreatedAt) {
        this.billCreatedAt = billCreatedAt;
    }

    public String getBillDeptId() {
        return billDeptId;
    }

    public void setBillDeptId(String billDeptId) {
        this.billDeptId = billDeptId;
    }

    public Double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(Double netTotal) {
        this.netTotal = netTotal;
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

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public Map<Long, Object> getBillItemsMap() {
        return billItemsMap;
    }

    public void setBillItemsMap(Map<Long, Object> billItemsMap) {
        this.billItemsMap = billItemsMap;
    }

    public List<BillItemDTO> getBillItems() {
        return billItems;
    }

    public void setBillItems(List<BillItemDTO> billItems) {
        this.billItems = billItems;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

}
