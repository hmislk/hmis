package com.divudi.core.data.dto;

import java.io.Serializable;

/**
 * One row per referring doctor and item. The JPQL constructor takes the raw
 * PharmaceuticalBillItem qty (negative for a sale) and stores it negated, so
 * quantity is the signed net quantity sold.
 */
public class ReferringDoctorSaleItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long doctorId;
    private String doctorName = ReferringDoctorSaleBillDTO.NO_DOCTOR;
    private Long itemId;
    private String itemName;
    private Double quantity;
    private Double netValue;

    public ReferringDoctorSaleItemDTO() {
    }

    public ReferringDoctorSaleItemDTO(Long doctorId1, Long doctorId2, Long doctorId3,
            Long doctorId4, Long doctorId5, Long itemId, String itemName,
            Double pharmaceuticalQty, Double netValue) {
        this.doctorId = ReferringDoctorSaleBillDTO.firstNonNull(doctorId1, doctorId2, doctorId3, doctorId4, doctorId5);
        this.itemId = itemId;
        this.itemName = itemName;
        this.quantity = pharmaceuticalQty == null ? 0.0 : pharmaceuticalQty * -1;
        this.netValue = netValue == null ? 0.0 : netValue;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
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

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public Double getNetValue() {
        return netValue;
    }

    public void setNetValue(Double netValue) {
        this.netValue = netValue;
    }
}
