package com.divudi.core.data.dto;

import java.io.Serializable;

public class TestCountDTO implements Serializable {
    private Long testId;
    private String testCode;
    private String testName;
    private Long count;
    private Double hosFee;
    private Double ccFee;
    private Double proFee;
    private Double reagentFee;
    private Double otherFee;
    private Double discount;
    private Double total;

    public TestCountDTO() {
    }

    public TestCountDTO(Long testId, String testCode, String testName,
                        Long count, Double hosFee, Double ccFee, Double proFee,
                        Double reagentFee, Double otherFee, Double discount, Double total) {
        this.testId = testId;
        this.testCode = testCode;
        this.testName = testName;
        this.count = count;
        this.hosFee = hosFee;
        this.ccFee = ccFee;
        this.proFee = proFee;
        this.reagentFee = reagentFee;
        this.otherFee = otherFee;
        this.discount = discount;
        this.total = total;
    }

    public TestCountDTO(Long testId, String testName, Long count, Double reagentFee) {
        this.testId = testId;
        this.testName = testName;
        this.count = count;
        this.reagentFee = reagentFee;
    }
    
    public Long getTestId() {
        return testId;
    }

    public void setTestId(Long testId) {
        this.testId = testId;
    }

    public String getTestCode() {
        return testCode;
    }

    public void setTestCode(String testCode) {
        this.testCode = testCode;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public Double getHosFee() {
        return hosFee;
    }

    public void setHosFee(Double hosFee) {
        this.hosFee = hosFee;
    }

    public Double getCcFee() {
        return ccFee;
    }

    public void setCcFee(Double ccFee) {
        this.ccFee = ccFee;
    }

    public Double getProFee() {
        return proFee;
    }

    public void setProFee(Double proFee) {
        this.proFee = proFee;
    }

    public Double getReagentFee() {
        return reagentFee;
    }

    public void setReagentFee(Double reagentFee) {
        this.reagentFee = reagentFee;
    }

    public Double getOtherFee() {
        return otherFee;
    }

    public void setOtherFee(Double otherFee) {
        this.otherFee = otherFee;
    }

    public Double getDiscount() {
        return discount;
    }

    public void setDiscount(Double discount) {
        this.discount = discount;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }
}
