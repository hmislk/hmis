/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dataStructure;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.inward.AdmissionType;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author safrin
 */
public class AdmissionTypeBills {

    private AdmissionType admissionType;
    private List<Bill> bills=new ArrayList<>();
    private double total;

    public AdmissionType getAdmissionType() {
        return admissionType;
    }

    public void setAdmissionType(AdmissionType admissionType) {
        this.admissionType = admissionType;
    }

    public List<Bill> getBills() {
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public double getTotal() {

        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }
}
