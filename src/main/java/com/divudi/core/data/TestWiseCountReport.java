/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.divudi.core.data;

import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Person;

/**
 *
 * @author Damiya
 */
public class TestWiseCountReport {

    public TestWiseCountReport() {
    }

    private Integer serial;
    private Long testId;
    private String testName;
    private String testCode;
    private Person doctor;
    private Long count;
    private Double hosFee;
    private Double ccFee;
    private Double proFee;
    private Double total;
    private Double discount;
    private boolean refunded;
    private boolean cancelled;
    private BillItem billItem;
    private Double reagentFee;
    private Double otherFee;
    
    private int check;

    public TestWiseCountReport(String testName, int check) {
        this.testName = testName;
        this.check = check;
    }

    public TestWiseCountReport(String testName, Long count, Double hosFee, Double ccFee, Double proFee, Double total) {
        this.testName = testName;
        this.count = count;
        this.hosFee = hosFee;
        this.ccFee = ccFee;
        this.proFee = proFee;
        this.total = total;
    }

    public TestWiseCountReport(String testCode, String testName, Long count, Double hosFee, Double ccFee, Double proFee, Double total) {
        this.testCode = testCode;
        this.testName = testName;
        this.count = count;
        this.hosFee = hosFee;
        this.ccFee = ccFee;
        this.proFee = proFee;
        this.total = total;
    }

    public TestWiseCountReport(Long testId, String testCode, String testName, Long count, Double hosFee, Double ccFee, Double proFee, Double total) {
        this.testId = testId;
        this.testCode = testCode;
        this.testName = testName;
        this.count = count;
        this.hosFee = hosFee;
        this.ccFee = ccFee;
        this.proFee = proFee;
        this.total = total;
    }

    public TestWiseCountReport(String testName, Long count, Double hosFee, Double ccFee, Double proFee, Double discount, Double total) {
        this.testName = testName;
        this.count = count;
        this.hosFee = hosFee;
        this.ccFee = ccFee;
        this.proFee = proFee;
        this.discount = discount;
        this.total = total;
    }

    public TestWiseCountReport(Person person, Long count, Double hosFee, Double ccFee, Double proFee, Double discount, Double total, BillItem bi) {
        this.doctor = person;
        this.count = count;
        this.hosFee = hosFee;
        this.ccFee = ccFee;
        this.proFee = proFee;
        this.discount = discount;
        this.total = total;
        this.billItem = bi;
    }

    public TestWiseCountReport(String testName, Long count, Double hosFee, Double ccFee, Double proFee, Double regFee, Double otherFee, Double discount, Double total) {
        this.testName = testName;
        this.count = count;
        this.hosFee = hosFee;
        this.ccFee = ccFee;
        this.proFee = proFee;
        this.reagentFee = regFee;
        this.otherFee = otherFee;
        this.discount = discount;
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

    public Double getDiscount() {
        return discount;
    }

    public void setDiscount(Double discount) {
        this.discount = discount;
    }

    public Person getDoctor() {
        return doctor;
    }

    public void setDoctor(Person doctor) {
        this.doctor = doctor;
    }

    public boolean isRefunded() {
        return refunded;
    }

    public void setRefunded(boolean refunded) {
        this.refunded = refunded;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public BillItem getBillItem() {
        return billItem;
    }

    public void setBillItem(BillItem billItem) {
        this.billItem = billItem;
    }

    public Double getReagentFee() {
        if(reagentFee == null){
            reagentFee = 0.0;
        }
        return reagentFee;
    }

    public void setReagentFee(Double reagentFee) {
        this.reagentFee = reagentFee;
    }

    public Double getOtherFee() {
        if(otherFee == null){
            otherFee = 0.0;
        }
        return otherFee;
    }

    public void setOtherFee(Double otherFee) {
        this.otherFee = otherFee;
    }

    public int getCheck() {
        return check;
    }

    public void setCheck(int check) {
        this.check = check;
    }

    public String getTestCode() {
        return testCode;
    }

    public void setTestCode(String testCode) {
        this.testCode = testCode;
    }

    public Long getTestId() {
        return testId;
    }

    public void setTestId(Long testId) {
        this.testId = testId;
    }
}
