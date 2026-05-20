package com.divudi.core.data.dto.investigation;

public class IxCalDTO {

    private Long id;
    private Long calIxItemId;
    private String calIxItemName;
    private Long valIxItemId;
    private String valIxItemName;
    private String calculationType;
    private Double constantValue;
    private Double maleConstantValue;
    private Double femaleConstantValue;
    private String javascript;
    private Integer orderNo;
    private String message;

    public IxCalDTO() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCalIxItemId() { return calIxItemId; }
    public void setCalIxItemId(Long calIxItemId) { this.calIxItemId = calIxItemId; }
    public String getCalIxItemName() { return calIxItemName; }
    public void setCalIxItemName(String calIxItemName) { this.calIxItemName = calIxItemName; }
    public Long getValIxItemId() { return valIxItemId; }
    public void setValIxItemId(Long valIxItemId) { this.valIxItemId = valIxItemId; }
    public String getValIxItemName() { return valIxItemName; }
    public void setValIxItemName(String valIxItemName) { this.valIxItemName = valIxItemName; }
    public String getCalculationType() { return calculationType; }
    public void setCalculationType(String calculationType) { this.calculationType = calculationType; }
    public Double getConstantValue() { return constantValue; }
    public void setConstantValue(Double constantValue) { this.constantValue = constantValue; }
    public Double getMaleConstantValue() { return maleConstantValue; }
    public void setMaleConstantValue(Double maleConstantValue) { this.maleConstantValue = maleConstantValue; }
    public Double getFemaleConstantValue() { return femaleConstantValue; }
    public void setFemaleConstantValue(Double femaleConstantValue) { this.femaleConstantValue = femaleConstantValue; }
    public String getJavascript() { return javascript; }
    public void setJavascript(String javascript) { this.javascript = javascript; }
    public Integer getOrderNo() { return orderNo; }
    public void setOrderNo(Integer orderNo) { this.orderNo = orderNo; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
