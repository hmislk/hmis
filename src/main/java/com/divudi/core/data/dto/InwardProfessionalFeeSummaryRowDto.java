package com.divudi.core.data.dto;

import com.divudi.core.entity.inward.AdmissionType;
import java.io.Serializable;
import java.util.Date;

/**
 * DTO for the Professional &amp; Other Fee Summary report (2026-08-15).
 * One instance per PatientEncounter / BHT.
 *
 * professionalFeeTotal and otherFeeTotal are each already net of their own
 * discount/service-charge — see InwardProfessionalFeeSummaryController.buildRow().
 * netTotal is simply their sum, so it always reconciles.
 */
public class InwardProfessionalFeeSummaryRowDto implements Serializable {

    private Long   encounterDatabaseId;
    private String bhtNo;
    private String patientName;
    private Date   dateOfAdmission;
    private Date   dateOfDischarge;
    private String finalBillNo;
    private AdmissionType admissionType;

    private double professionalFeeTotal;
    private double otherFeeTotal;

    public double getNetTotal() {
        return professionalFeeTotal + otherFeeTotal;
    }

    public Long getEncounterDatabaseId() { return encounterDatabaseId; }
    public void setEncounterDatabaseId(Long encounterDatabaseId) { this.encounterDatabaseId = encounterDatabaseId; }

    public String getBhtNo() { return bhtNo; }
    public void setBhtNo(String bhtNo) { this.bhtNo = bhtNo; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public Date getDateOfAdmission() { return dateOfAdmission; }
    public void setDateOfAdmission(Date dateOfAdmission) { this.dateOfAdmission = dateOfAdmission; }

    public Date getDateOfDischarge() { return dateOfDischarge; }
    public void setDateOfDischarge(Date dateOfDischarge) { this.dateOfDischarge = dateOfDischarge; }

    public String getFinalBillNo() { return finalBillNo != null ? finalBillNo : ""; }
    public void setFinalBillNo(String finalBillNo) { this.finalBillNo = finalBillNo; }

    public AdmissionType getAdmissionType() { return admissionType; }
    public void setAdmissionType(AdmissionType admissionType) { this.admissionType = admissionType; }

    public double getProfessionalFeeTotal() { return professionalFeeTotal; }
    public void setProfessionalFeeTotal(double professionalFeeTotal) { this.professionalFeeTotal = professionalFeeTotal; }

    public double getOtherFeeTotal() { return otherFeeTotal; }
    public void setOtherFeeTotal(double otherFeeTotal) { this.otherFeeTotal = otherFeeTotal; }
}
