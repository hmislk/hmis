/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.admissioncharge;

/**
 * DTO representing a single {@code AdmissionChargeItem} configuration row
 * (issue #23594).
 *
 * <p>{@code admissionTypeId}/{@code admissionTypeName} null means "applies to
 * any admission type" and {@code paymentMethod} null means "applies to both
 * Cash and Credit" - see {@code AdmissionChargeItem} for the two-dimension
 * resolution rule.</p>
 *
 * @author Buddhika
 */
public class AdmissionChargeItemDTO {

    private Long id;
    private Long itemId;
    private String itemName;
    private Long itemDepartmentId;
    private String itemDepartmentName;
    private String inwardChargeType;
    private Long admissionTypeId;
    private String admissionTypeName;
    private String paymentMethod;
    private double price;
    private Double qty;
    private int orderNo;
    private boolean retired;

    public AdmissionChargeItemDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Long getItemDepartmentId() {
        return itemDepartmentId;
    }

    public void setItemDepartmentId(Long itemDepartmentId) {
        this.itemDepartmentId = itemDepartmentId;
    }

    public String getItemDepartmentName() {
        return itemDepartmentName;
    }

    public void setItemDepartmentName(String itemDepartmentName) {
        this.itemDepartmentName = itemDepartmentName;
    }

    public String getInwardChargeType() {
        return inwardChargeType;
    }

    public void setInwardChargeType(String inwardChargeType) {
        this.inwardChargeType = inwardChargeType;
    }

    public Long getAdmissionTypeId() {
        return admissionTypeId;
    }

    public void setAdmissionTypeId(Long admissionTypeId) {
        this.admissionTypeId = admissionTypeId;
    }

    public String getAdmissionTypeName() {
        return admissionTypeName;
    }

    public void setAdmissionTypeName(String admissionTypeName) {
        this.admissionTypeName = admissionTypeName;
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

    public boolean isRetired() {
        return retired;
    }

    public void setRetired(boolean retired) {
        this.retired = retired;
    }
}
