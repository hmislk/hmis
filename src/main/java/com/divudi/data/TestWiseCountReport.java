/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.divudi.data;

/**
 *
 * @author Damiya
 */
public class TestWiseCountReport {

    public TestWiseCountReport() {
    }

    private Integer serial;
    private String testName;
    private Long count;
    private Double hosFee;
    private Double ccFee;
    private Double proFee;
    private Double total;

    public TestWiseCountReport(String testName, Long count, Double hosFee, Double ccFee, Double proFee, Double total) {
        this.testName = testName;
        this.count = count;
        this.hosFee = hosFee;
        this.ccFee = ccFee;
        this.proFee = proFee;
        this.total = total;
    }
    
    

    public Integer getSerial() {
        return serial;
    }

    public void setSerial(Integer serial) {
        this.serial = serial;
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

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }
    
    
}
