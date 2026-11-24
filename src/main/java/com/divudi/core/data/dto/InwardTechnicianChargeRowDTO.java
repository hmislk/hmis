package com.divudi.core.data.dto;

import com.divudi.core.data.Title;
import java.io.Serializable;
import java.util.Date;

/**
 * Flat, JPQL-projectable row for one technician fee ({@code BillFee} with
 * Fee Category = Technician Fee) on an inpatient professional-fee bill, used by
 * the Inward Technician Charges Report (issue #25818). Grouping by technician
 * and the subtotals happen in Java, not here.
 */
public class InwardTechnicianChargeRowDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String bhtNo;
    private Long staffId;
    private String staffName;
    private Title staffTitle;
    private Date feeAddedDate;
    private double addedFeeValue;
    private double paidFeeValue;

    public InwardTechnicianChargeRowDTO() {
    }

    public InwardTechnicianChargeRowDTO(String bhtNo, Long staffId, String staffName, Title staffTitle,
            Date feeAddedDate, Double addedFeeValue, Double paidFeeValue) {
        this.bhtNo = bhtNo;
        this.staffId = staffId;
        this.staffName = staffName;
        this.staffTitle = staffTitle;
        this.feeAddedDate = feeAddedDate;
        this.addedFeeValue = addedFeeValue != null ? addedFeeValue : 0.0;
        this.paidFeeValue = paidFeeValue != null ? paidFeeValue : 0.0;
    }

    // Technician name prefixed with the title, e.g. "Mr. A Perera".
    public String getTechnicianName() {
        if (staffName == null || staffName.trim().isEmpty()) {
            return "";
        }
        return staffTitle != null ? staffTitle.getLabel().trim() + " " + staffName.trim() : staffName.trim();
    }

    public String getBhtNo() {
        return bhtNo;
    }

    public void setBhtNo(String bhtNo) {
        this.bhtNo = bhtNo;
    }

    public Long getStaffId() {
        return staffId;
    }

    public void setStaffId(Long staffId) {
        this.staffId = staffId;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public Title getStaffTitle() {
        return staffTitle;
    }

    public void setStaffTitle(Title staffTitle) {
        this.staffTitle = staffTitle;
    }

    public Date getFeeAddedDate() {
        return feeAddedDate;
    }

    public void setFeeAddedDate(Date feeAddedDate) {
        this.feeAddedDate = feeAddedDate;
    }

    public double getAddedFeeValue() {
        return addedFeeValue;
    }

    public void setAddedFeeValue(double addedFeeValue) {
        this.addedFeeValue = addedFeeValue;
    }

    public double getPaidFeeValue() {
        return paidFeeValue;
    }

    public void setPaidFeeValue(double paidFeeValue) {
        this.paidFeeValue = paidFeeValue;
    }
}
