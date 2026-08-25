package com.divudi.core.data.dto;

import java.io.Serializable;

/**
 * One doctor+speciality summary row within an admission block of the
 * Inpatient Professional Payment Report (issue #22800). Built in Java by
 * grouping {@link InwardProfessionalPaymentFeeRowDTO} rows — not
 * JPQL-constructed, since it aggregates and string-joins across multiple
 * charge entries.
 */
public class InwardProfessionalPaymentReportRowDTO implements Serializable {

    private String consultantName;
    private String specialityName;
    private double sumAddedFee;
    private double sumPaidFee;
    private String paymentBillNumbers;
    private String paymentDates;

    public InwardProfessionalPaymentReportRowDTO() {
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

    public String getPaymentBillNumbers() {
        return paymentBillNumbers;
    }

    public void setPaymentBillNumbers(String paymentBillNumbers) {
        this.paymentBillNumbers = paymentBillNumbers;
    }

    public String getPaymentDates() {
        return paymentDates;
    }

    public void setPaymentDates(String paymentDates) {
        this.paymentDates = paymentDates;
    }
}
