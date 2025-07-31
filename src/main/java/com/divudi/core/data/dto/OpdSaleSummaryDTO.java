package com.divudi.core.data.dto;

import java.io.Serializable;

public class OpdSaleSummaryDTO implements Serializable {
    private String categoryName;
    private String itemName;
    private Long itemCount;
    private Double hospitalFee;
    private Double professionalFee;
    private Double grossAmount;
    private Double discountAmount;
    private Double netTotal;

    public OpdSaleSummaryDTO() {
    }

    public OpdSaleSummaryDTO(String categoryName, String itemName, Long itemCount,
                              Double hospitalFee, Double professionalFee,
                              Double grossAmount, Double discountAmount,
                              Double netTotal) {
        this.categoryName = categoryName;
        this.itemName = itemName;
        this.itemCount = itemCount;
        this.hospitalFee = hospitalFee;
        this.professionalFee = professionalFee;
        this.grossAmount = grossAmount;
        this.discountAmount = discountAmount;
        this.netTotal = netTotal;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Long getItemCount() {
        return itemCount;
    }

    public void setItemCount(Long itemCount) {
        this.itemCount = itemCount;
    }

    public Double getHospitalFee() {
        return hospitalFee;
    }

    public void setHospitalFee(Double hospitalFee) {
        this.hospitalFee = hospitalFee;
    }

    public Double getProfessionalFee() {
        return professionalFee;
    }

    public void setProfessionalFee(Double professionalFee) {
        this.professionalFee = professionalFee;
    }

    public Double getGrossAmount() {
        return grossAmount;
    }

    public void setGrossAmount(Double grossAmount) {
        this.grossAmount = grossAmount;
    }

    public Double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(Double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public Double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(Double netTotal) {
        this.netTotal = netTotal;
    }
}
