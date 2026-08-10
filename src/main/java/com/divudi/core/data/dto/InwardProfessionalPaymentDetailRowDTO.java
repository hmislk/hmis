package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * One consultant+speciality block within an admission of the Detailed
 * Inpatient Professional Payment Report (issue #22803). Carries the full,
 * per-fee-row Added/Paid detail (unlike the aggregated
 * {@link InwardProfessionalPaymentReportRowDTO} used by the Summary report).
 * Built in Java by grouping {@link InwardProfessionalPaymentFeeRowDTO} rows -
 * not JPQL-constructed, since it carries nested collections.
 */
public class InwardProfessionalPaymentDetailRowDTO implements Serializable {

    private String consultantName;
    private String specialityName;
    private List<Double> addedFeeValues = new ArrayList<>();
    private List<Date> addedFeeDates = new ArrayList<>();
    private List<Double> paidFeeValues = new ArrayList<>();
    private List<Date> paidFeeDates = new ArrayList<>();
    private List<String> paidBillNumbers = new ArrayList<>();
    private double sumAddedFee;
    private double sumPaidFee;

    public InwardProfessionalPaymentDetailRowDTO() {
    }

    public String getConsultantName() {
        return consultantName;
    }

    public void setConsultantName(String consultantName) {
        this.consultantName = consultantName;
    }

    public String getSpecialityName() {
        return specialityName;
    }

    public void setSpecialityName(String specialityName) {
        this.specialityName = specialityName;
    }

    public List<Double> getAddedFeeValues() {
        return addedFeeValues;
    }

    public void setAddedFeeValues(List<Double> addedFeeValues) {
        this.addedFeeValues = addedFeeValues;
    }

    public List<Date> getAddedFeeDates() {
        return addedFeeDates;
    }

    public void setAddedFeeDates(List<Date> addedFeeDates) {
        this.addedFeeDates = addedFeeDates;
    }

    public List<Double> getPaidFeeValues() {
        return paidFeeValues;
    }

    public void setPaidFeeValues(List<Double> paidFeeValues) {
        this.paidFeeValues = paidFeeValues;
    }

    public List<Date> getPaidFeeDates() {
        return paidFeeDates;
    }

    public void setPaidFeeDates(List<Date> paidFeeDates) {
        this.paidFeeDates = paidFeeDates;
    }

    public List<String> getPaidBillNumbers() {
        return paidBillNumbers;
    }

    public void setPaidBillNumbers(List<String> paidBillNumbers) {
        this.paidBillNumbers = paidBillNumbers;
    }

    public double getSumAddedFee() {
        return sumAddedFee;
    }

    public void setSumAddedFee(double sumAddedFee) {
        this.sumAddedFee = sumAddedFee;
    }

    public double getSumPaidFee() {
        return sumPaidFee;
    }

    public void setSumPaidFee(double sumPaidFee) {
        this.sumPaidFee = sumPaidFee;
    }

    public int rowCount() {
        return Math.max(addedFeeValues.size(), paidFeeValues.size());
    }

    // Issue #22803 - rowspan for this consultant block's own columns
    // (Consultant/Speciality) and for the admission columns it contributes
    // to: its data rows, plus the Total row and the Balance-to-Pay row.
    public int getBlockRowSpan() {
        return Math.max(rowCount(), 1) + 2;
    }

    // So the xhtml can ui:repeat over a plain index list without EL arithmetic.
    public List<Integer> getIndexes() {
        return IntStream.range(0, rowCount()).boxed().collect(Collectors.toList());
    }
}
