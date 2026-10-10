/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.admissioncharge;

/**
 * DTO for updating an existing {@code AdmissionChargeItem} (issue #23594).
 * All fields optional - only fields present are applied.
 *
 * <p>{@code admissionTypeId} and {@code paymentMethod} are two of the three
 * columns of the row's uniqueness key, and {@code null} is itself a
 * meaningful value for both ("any admission type" / "both payment methods").
 * A plain JSON body cannot distinguish "field omitted" from "field sent as
 * null", so clearing either column back to null is done explicitly with
 * {@link #clearAdmissionType} / {@link #clearPaymentMethod} rather than by
 * sending the field as null.</p>
 *
 * @author Buddhika
 */
public class AdmissionChargeItemUpdateRequestDTO {

    private Long itemId;
    private Long admissionTypeId;
    /** When true, sets admissionType to null ("any"). Ignored if admissionTypeId is present. */
    private Boolean clearAdmissionType;
    private String paymentMethod;
    /** When true, sets paymentMethod to null ("both"). Ignored if paymentMethod is present. */
    private Boolean clearPaymentMethod;
    private Double price;
    private Double qty;
    private Integer orderNo;

    public AdmissionChargeItemUpdateRequestDTO() {
    }

    public boolean isValid() {
        return itemId != null || admissionTypeId != null
                || Boolean.TRUE.equals(clearAdmissionType)
                || paymentMethod != null
                || Boolean.TRUE.equals(clearPaymentMethod)
                || price != null || qty != null || orderNo != null;
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

    public Boolean getClearAdmissionType() {
        return clearAdmissionType;
    }

    public void setClearAdmissionType(Boolean clearAdmissionType) {
        this.clearAdmissionType = clearAdmissionType;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Boolean getClearPaymentMethod() {
        return clearPaymentMethod;
    }

    public void setClearPaymentMethod(Boolean clearPaymentMethod) {
        this.clearPaymentMethod = clearPaymentMethod;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Double getQty() {
        return qty;
    }

    public void setQty(Double qty) {
        this.qty = qty;
    }

    public Integer getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(Integer orderNo) {
        this.orderNo = orderNo;
    }
}
