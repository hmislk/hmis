/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.admissioncharge;

/**
 * DTO for creating a new {@code AdmissionChargeItem} (issue #23594).
 *
 * <p>{@code admissionTypeId} absent/null means "applies to any admission
 * type" and {@code paymentMethod} absent/null means "applies to both Cash and
 * Credit" - both are legitimate configurations, not omissions.</p>
 *
 * @author Buddhika
 */
public class AdmissionChargeItemCreateRequestDTO {

    private Long itemId; // required
    private Long admissionTypeId; // optional - null means "any admission type"
    private String paymentMethod; // optional - "Cash" or "Credit"; null means "both"
    private double price; // required, must be >= 0
    private Double qty; // optional, defaults to 1.0
    private int orderNo; // optional, defaults to 0

    public AdmissionChargeItemCreateRequestDTO() {
    }

    public boolean isValid() {
        return itemId != null;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Long getAdmissionTypeId() {
        return admissionTypeId;
    }

    public void setAdmissionTypeId(Long admissionTypeId) {
        this.admissionTypeId = admissionTypeId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public Double getQty() {
        return qty;
    }

    public void setQty(Double qty) {
        this.qty = qty;
    }

    public int getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(int orderNo) {
        this.orderNo = orderNo;
    }
}
