/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data.dataStructure;

import com.divudi.entity.BillItem;

/**
 *
 * @author safrin
 */
public class BillItemWithFee {

    private BillItem billItem;
    private double count;
    private double hospitalFee;
    double reagentFee;
    double hospitalFeeMargin;
    double hospitalFeeDiscount;
    double hospitalFeeGross;
    private double outSideFee;
    double outSideFeeGross;
    double outSideFeeMargin;
    double outSideFeeDiscount;
    private double proFee;
    private double vatFee;
    private double total;
    String staffsNames;
    double staffFee;
 

    public double getHospitalFee() {
        return hospitalFee;
    }

    public void setHospitalFee(double hospitalFee) {
        this.hospitalFee = hospitalFee;
    }

    public double getProFee() {
        return proFee;
    }

    public void setProFee(double proFee) {
        this.proFee = proFee;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public double getCount() {
        return count;
    }

    public void setCount(double count) {
        this.count = count;
    }

    public BillItem getBillItem() {
        return billItem;
    }

    public void setBillItem(BillItem billItem) {
        this.billItem = billItem;
    }

    public double getOutSideFee() {
        return outSideFee;
    }

    public void setOutSideFee(double outSideFee) {
        this.outSideFee = outSideFee;
    }

    public double getHospitalFeeMargin() {
        return hospitalFeeMargin;
    }

    public void setHospitalFeeMargin(double hospitalFeeMargin) {
        this.hospitalFeeMargin = hospitalFeeMargin;
    }

    public double getHospitalFeeDiscount() {
        return hospitalFeeDiscount;
    }

    public void setHospitalFeeDiscount(double hospitalFeeDiscount) {
        this.hospitalFeeDiscount = hospitalFeeDiscount;
    }

    public double getHospitalFeeGross() {
        return hospitalFeeGross;
    }

    public void setHospitalFeeGross(double hospitalFeeGross) {
        this.hospitalFeeGross = hospitalFeeGross;
    }

    public double getOutSideFeeGross() {
        return outSideFeeGross;
    }

    public void setOutSideFeeGross(double outSideFeeGross) {
        this.outSideFeeGross = outSideFeeGross;
    }

    public double getOutSideFeeMargin() {
        return outSideFeeMargin;
    }

    public void setOutSideFeeMargin(double outSideFeeMargin) {
        this.outSideFeeMargin = outSideFeeMargin;
    }

    public double getOutSideFeeDiscount() {
        return outSideFeeDiscount;
    }

    public void setOutSideFeeDiscount(double outSideFeeDiscount) {
        this.outSideFeeDiscount = outSideFeeDiscount;
    }

    public double getReagentFee() {
        return reagentFee;
    }

    public void setReagentFee(double reagentFee) {
        this.reagentFee = reagentFee;
    }

    public String getStaffsNames() {
        return staffsNames;
    }

    public void setStaffsNames(String staffsNames) {
        this.staffsNames = staffsNames;
    }

    public double getVatFee() {
        return vatFee;
    }

    public void setVatFee(double vatFee) {
        this.vatFee = vatFee;
    }

    public double getStaffFee() {
        return staffFee;
    }

    public void setStaffFee(double staffFee) {
        this.staffFee = staffFee;
    }
    
}
