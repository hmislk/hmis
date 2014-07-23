/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.dataStructure;

import com.divudi.entity.Staff;

/**
 *
 * @author safrin
 */
public class ChannelDoctor {

    private Staff consultant;
    private double billCount;
    private double billCanncelCount;
    private double refundedCount;
    private double billCount_c;
    private double billCanncelCount_c;
    private double refundedCount_c;
    private double creditCount;
    private double creditCancelledCount;
    ///////////////////
    private double billFee;
    private double billCanncelFee;
    private double refundFee;
    private double billFee_c;
    private double billCanncelFee_c;
    private double refundedFee_c;
    private double creditFee;
    private double creditCancelFee;
    ////////////////////////
    private double absentCount;

    public double getCommon() {
        return 1;
    }

    public Staff getConsultant() {
        return consultant;
    }

    public void setConsultant(Staff consultant) {
        this.consultant = consultant;
    }

    public double getRefundedCount() {
        return refundedCount;
    }

    public void setRefundedCount(double refundedCount) {
        this.refundedCount = refundedCount;
    }

    public double getAbsentCount() {
        return absentCount;
    }

    public void setAbsentCount(double absentCount) {
        this.absentCount = absentCount;
    }

    public double getBillCount() {
        return billCount;
    }

    public void setBillCount(double billCount) {
        this.billCount = billCount;
    }

    public double getCreditCount() {
        return creditCount;
    }

    public void setCreditCount(double creditCount) {
        this.creditCount = creditCount;
    }

    public double getCreditCancelledCount() {
        return creditCancelledCount;
    }

    public void setCreditCancelledCount(double creditCancelledCount) {
        this.creditCancelledCount = creditCancelledCount;
    }

    public double getBillCanncelCount() {
        return billCanncelCount;
    }

    public void setBillCanncelCount(double billCanncelCount) {
        this.billCanncelCount = billCanncelCount;
    }

    public double getBillCount_c() {
        return billCount_c;
    }

    public void setBillCount_c(double billCount_c) {
        this.billCount_c = billCount_c;
    }

    public double getBillCanncelCount_c() {
        return billCanncelCount_c;
    }

    public void setBillCanncelCount_c(double billCanncelCount_c) {
        this.billCanncelCount_c = billCanncelCount_c;
    }

    public double getRefundedCount_c() {
        return refundedCount_c;
    }

    public void setRefundedCount_c(double refundedCount_c) {
        this.refundedCount_c = refundedCount_c;
    }

    public double getBillFee() {
        return billFee;
    }

    public void setBillFee(double billFee) {
        this.billFee = billFee;
    }

    public double getBillCanncelFee() {
        return billCanncelFee;
    }

    public void setBillCanncelFee(double billCanncelFee) {
        this.billCanncelFee = billCanncelFee;
    }

    public double getRefundFee() {
        return refundFee;
    }

    public void setRefundFee(double refundFee) {
        this.refundFee = refundFee;
    }

    public double getCreditFee() {
        return creditFee;
    }

    public void setCreditFee(double creditFee) {
        this.creditFee = creditFee;
    }

    public double getCreditCancelFee() {
        return creditCancelFee;
    }

    public void setCreditCancelFee(double creditCancelFee) {
        this.creditCancelFee = creditCancelFee;
    }

    public double getBillFee_c() {
        return billFee_c;
    }

    public void setBillFee_c(double billFee_c) {
        this.billFee_c = billFee_c;
    }

    public double getBillCanncelFee_c() {
        return billCanncelFee_c;
    }

    public void setBillCanncelFee_c(double billCanncelFee_c) {
        this.billCanncelFee_c = billCanncelFee_c;
    }

    public double getRefundedFee_c() {
        return refundedFee_c;
    }

    public void setRefundedFee_c(double refundedFee_c) {
        this.refundedFee_c = refundedFee_c;
    }
}
