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
    private List<InwardProfessionalPaymentReportRowDTO> detailRows = new ArrayList<>();
    // Issue #22803 - per-consultant blocks (with per-fee-row detail) for the
    // Detailed report; independent of detailRows above, which keeps feeding
    // the Summary report.
    private List<InwardProfessionalPaymentDetailRowDTO> detailedRows = new ArrayList<>();

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

    public List<InwardProfessionalPaymentReportRowDTO> getDetailRows() {
        return detailRows;
    }

    public void setDetailRows(List<InwardProfessionalPaymentReportRowDTO> detailRows) {
        this.detailRows = detailRows;
    }

    public List<InwardProfessionalPaymentDetailRowDTO> getDetailedRows() {
        return detailedRows;
    }

    public void setDetailedRows(List<InwardProfessionalPaymentDetailRowDTO> detailedRows) {
        this.detailedRows = detailedRows;
    }

    // Issue #22803 - rowspan for the Summary table's shared admission columns
    // (BHT/Admitted/Discharged/Final Bill Number): one row per consultant, or
    // 1 for a bare admission with no professional fees.
    public int getSummaryAdmissionRowSpan() {
        return detailRows == null || detailRows.isEmpty() ? 1 : detailRows.size();
    }

    // Issue #22803 - rowspan for the Detailed table's shared admission
    // columns: sum of every consultant block's own span (see
    // InwardProfessionalPaymentDetailRowDTO.getBlockRowSpan()), or 1 for a
    // bare admission with no professional fees.
    public int getDetailedAdmissionRowSpan() {
        if (detailedRows == null || detailedRows.isEmpty()) {
            return 1;
        }
        int total = 0;
        for (InwardProfessionalPaymentDetailRowDTO detail : detailedRows) {
            total += detail.getBlockRowSpan();
        }
        return total;
    }
}
