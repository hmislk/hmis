package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * One admission block of the Inpatient Professional Payment Report (issue
 * #22800), bound as the outer {@code p:dataTable} row with
 * {@code detailRows} bound to the nested {@code p:subTable}. Built in Java,
 * not JPQL-constructed, since it carries a nested collection.
 */
public class InwardProfessionalPaymentAdmissionGroupDTO implements Serializable {

    private String bhtNo;
    private Date dateOfAdmission;
    private Date dateOfDischarge;
    private String firstFinalBillNo;
    private String confirmedFinalBillNo;
    private List<InwardProfessionalPaymentReportRowDTO> detailRows = new ArrayList<>();

    public InwardProfessionalPaymentAdmissionGroupDTO() {
    }

    public String getBhtNo() {
        return bhtNo;
    }

    public void setBhtNo(String bhtNo) {
        this.bhtNo = bhtNo;
    }

    public Date getDateOfAdmission() {
        return dateOfAdmission;
    }

    public void setDateOfAdmission(Date dateOfAdmission) {
        this.dateOfAdmission = dateOfAdmission;
    }

    public Date getDateOfDischarge() {
        return dateOfDischarge;
    }

    public void setDateOfDischarge(Date dateOfDischarge) {
        this.dateOfDischarge = dateOfDischarge;
    }

    public String getFirstFinalBillNo() {
        return firstFinalBillNo;
    }

    public void setFirstFinalBillNo(String firstFinalBillNo) {
        this.firstFinalBillNo = firstFinalBillNo;
    }

    public String getConfirmedFinalBillNo() {
        return confirmedFinalBillNo;
    }

    public void setConfirmedFinalBillNo(String confirmedFinalBillNo) {
        this.confirmedFinalBillNo = confirmedFinalBillNo;
    }

    public List<InwardProfessionalPaymentReportRowDTO> getDetailRows() {
        return detailRows;
    }

    public void setDetailRows(List<InwardProfessionalPaymentReportRowDTO> detailRows) {
        this.detailRows = detailRows;
    }
}
