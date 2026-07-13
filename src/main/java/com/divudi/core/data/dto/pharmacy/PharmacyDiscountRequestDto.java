package com.divudi.core.data.dto.pharmacy;

import java.io.Serializable;

public class PharmacyDiscountRequestDto implements Serializable {

    private Long categoryId;
    private Long paymentSchemeId;
    private String paymentSchemeName;
    private String paymentMethod;
    private String billType;
    private Double discountPercent;
    private String retireComments;

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public Long getPaymentSchemeId() { return paymentSchemeId; }
    public void setPaymentSchemeId(Long paymentSchemeId) { this.paymentSchemeId = paymentSchemeId; }

    public String getPaymentSchemeName() { return paymentSchemeName; }
    public void setPaymentSchemeName(String paymentSchemeName) { this.paymentSchemeName = paymentSchemeName; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getBillType() { return billType; }
    public void setBillType(String billType) { this.billType = billType; }

    public Double getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(Double discountPercent) { this.discountPercent = discountPercent; }

    public String getRetireComments() { return retireComments; }
    public void setRetireComments(String retireComments) { this.retireComments = retireComments; }
}
