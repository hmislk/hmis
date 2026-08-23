/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dataStructure;

import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.inward.PatientRoom;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author safrin
 */
public class ChargeItemTotal {

    private InwardChargeType inwardChargeType;
    private double total = 0;
    private double discount = 0;
    private double chargeTypeDiscount = 0;
    private double netTotal = 0;
    private double adjustedTotal = 0.0;
    private double gross = 0;
    private double margin = 0;
    private double vat = 0;
    private String comments;
    private List<PatientRoom> patientRooms;
    List<BillFee> billFees;
    /**
     * Names of the specific Outside Charge items whose value was folded into
     * this charge type's total (issue #22989). Populated only for charge
     * types that received an Outside Charge contribution; empty otherwise.
     * Display-only — does not affect total/discount/validation calculations.
     */
    private List<String> additionalChargeItemNames;

    public List<String> getAdditionalChargeItemNames() {
        if (additionalChargeItemNames == null) {
            additionalChargeItemNames = new ArrayList<>();
        }
        return additionalChargeItemNames;
    }

    public void setAdditionalChargeItemNames(List<String> additionalChargeItemNames) {
        this.additionalChargeItemNames = additionalChargeItemNames;
    }

    public List<BillFee> getBillFees() {
        return billFees;
    }

    public void setBillFees(List<BillFee> billFees) {
        this.billFees = billFees;
    }

    public InwardChargeType getInwardChargeType() {
        return inwardChargeType;
    }

    public void setInwardChargeType(InwardChargeType inwardChargeType) {
        this.inwardChargeType = inwardChargeType;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public double getChargeTypeDiscount() {
        return chargeTypeDiscount;
    }

    public void setChargeTypeDiscount(double chargeTypeDiscount) {
        this.chargeTypeDiscount = chargeTypeDiscount;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public double getNetTotal() {
        netTotal = total - discount - chargeTypeDiscount;
        return netTotal;
    }



    public double getAdjustedTotal() {
        return adjustedTotal;
    }

    public void setAdjustedTotal(double adjustedTotal) {
        this.adjustedTotal = adjustedTotal;
    }

    public double getGross() {
        return gross;
    }

    public void setGross(double gross) {
        this.gross = gross;
    }

    public double getMargin() {
        return margin;
    }

    public void setMargin(double margin) {
        this.margin = margin;
    }

    public double getVat() {
        return vat;
    }

    public void setVat(double vat) {
        this.vat = vat;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public List<PatientRoom> getPatientRooms() {
        if (patientRooms == null) {
            patientRooms = new ArrayList<>();
        }
        return patientRooms;
    }

    public void setPatientRooms(List<PatientRoom> patientRooms) {
        this.patientRooms = patientRooms;
    }

}
