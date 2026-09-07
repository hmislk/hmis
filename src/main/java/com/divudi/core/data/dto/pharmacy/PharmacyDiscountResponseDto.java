package com.divudi.core.data.dto.pharmacy;

import java.io.Serializable;

public class PharmacyDiscountResponseDto implements Serializable {

    private Long id;
    private Long categoryId;
    private String categoryName;
    private Long paymentSchemeId;
    private String paymentSchemeName;
    private String paymentMethod;
    private String billType;
    private double discountPercent;
    private boolean retired;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public Long getPaymentSchemeId() { return paymentSchemeId; }
    public void setPaymentSchemeId(Long paymentSchemeId) { this.paymentSchemeId = paymentSchemeId; }

    public String getPaymentSchemeName() { return paymentSchemeName; }
    public void setPaymentSchemeName(String paymentSchemeName) { this.paymentSchemeName = paymentSchemeName; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getBillType() { return billType; }
    public void setBillType(String billType) { this.billType = billType; }

    public double getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(double discountPercent) { this.discountPercent = discountPercent; }

    public boolean isRetired() { return retired; }
    public void setRetired(boolean retired) { this.retired = retired; }
}
