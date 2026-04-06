package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * DTO for one line in the Inward Charge Type Detail report (Issue #19320).
 *
 * Each row represents either a BillItem (InwardBill) or a PatientItem (TimedItem)
 * for the selected InwardChargeType and encounter date range.
 */
public class InwardChargeTypeDetailRowDto implements Serializable {

    // -------------------------------------------------------------------------
    // Encounter identity (same for all rows belonging to the same BHT)
    // -------------------------------------------------------------------------
    private String bhtNo;
    private String patientName;
    private Date   dateOfAdmission;
    private Date   dateOfDischarge;

    // -------------------------------------------------------------------------
    // Line item detail
    // -------------------------------------------------------------------------
    private String itemName;

    /** Start time: null for regular BillItems, set for TimedItems */
    private Date   fromTime;

    /** End time: null for regular BillItems, set for TimedItems */
    private Date   toTime;

    /** Sort key: billTime for BillItems, fromTime for TimedItems */
    private Date   sortTime;

    private double grossValue;
    private double discount;
    private double netValue;

    // -------------------------------------------------------------------------
    // Getters / setters
    // -------------------------------------------------------------------------

    public String getBhtNo() { return bhtNo; }
    public void setBhtNo(String bhtNo) { this.bhtNo = bhtNo; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public Date getDateOfAdmission() { return dateOfAdmission; }
    public void setDateOfAdmission(Date dateOfAdmission) { this.dateOfAdmission = dateOfAdmission; }

    public Date getDateOfDischarge() { return dateOfDischarge; }
    public void setDateOfDischarge(Date dateOfDischarge) { this.dateOfDischarge = dateOfDischarge; }

    public String getItemName() { return itemName != null ? itemName : ""; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public Date getFromTime() { return fromTime; }
    public void setFromTime(Date fromTime) { this.fromTime = fromTime; }

    public Date getToTime() { return toTime; }
    public void setToTime(Date toTime) { this.toTime = toTime; }

    public Date getSortTime() { return sortTime; }
    public void setSortTime(Date sortTime) { this.sortTime = sortTime; }

    public double getGrossValue() { return grossValue; }
    public void setGrossValue(double grossValue) { this.grossValue = grossValue; }

    public double getDiscount() { return discount; }
    public void setDiscount(double discount) { this.discount = discount; }

    public double getNetValue() { return netValue; }
    public void setNetValue(double netValue) { this.netValue = netValue; }
}
