/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.dataStructure;

import com.divudi.data.inward.InwardChargeType;
import com.divudi.entity.BillFee;
import com.divudi.entity.inward.PatientRoom;
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
    private double netTotal = 0;
    private double adjustedTotal = 0.0;
    private List<PatientRoom> patientRooms;
    List<BillFee> billFees;

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

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public double getNetTotal() {
        netTotal = total - discount;
        return netTotal;
    }

 

    public double getAdjustedTotal() {
        return adjustedTotal;
    }

    public void setAdjustedTotal(double adjustedTotal) {
        this.adjustedTotal = adjustedTotal;
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
