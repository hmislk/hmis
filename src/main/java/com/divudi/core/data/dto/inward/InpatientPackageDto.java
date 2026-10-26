package com.divudi.core.data.dto.inward;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InpatientPackageDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private Long admissionTypeId;
    private Long roomCategoryId;
    private Double includedRoomDurationHours;
    private Double fixedRoomCharge;
    private Map<String, Double> chargeTypeAmounts = new LinkedHashMap<>();
    private Double totalPrice;
    // null = not sent by caller (leave items untouched on update / no items on create);
    // non-null (including empty) = full-replace with exactly these items.
    private List<InpatientPackageItemDto> items;

    public InpatientPackageDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getAdmissionTypeId() {
        return admissionTypeId;
    }

    public void setAdmissionTypeId(Long admissionTypeId) {
        this.admissionTypeId = admissionTypeId;
    }

    public Long getRoomCategoryId() {
        return roomCategoryId;
    }

    public void setRoomCategoryId(Long roomCategoryId) {
        this.roomCategoryId = roomCategoryId;
    }

    public Double getIncludedRoomDurationHours() {
        return includedRoomDurationHours;
    }

    public void setIncludedRoomDurationHours(Double includedRoomDurationHours) {
        this.includedRoomDurationHours = includedRoomDurationHours;
    }

    public Double getFixedRoomCharge() {
        return fixedRoomCharge;
    }

    public void setFixedRoomCharge(Double fixedRoomCharge) {
        this.fixedRoomCharge = fixedRoomCharge;
    }

    public Map<String, Double> getChargeTypeAmounts() {
        return chargeTypeAmounts;
    }

    public void setChargeTypeAmounts(Map<String, Double> chargeTypeAmounts) {
        this.chargeTypeAmounts = chargeTypeAmounts;
    }

    public Double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(Double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public List<InpatientPackageItemDto> getItems() {
        return items;
    }

    public void setItems(List<InpatientPackageItemDto> items) {
        this.items = items;
    }
}
