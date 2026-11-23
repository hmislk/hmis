package com.divudi.core.data.dto;

import java.io.Serializable;

/**
 * One row of the EMR visits by diagnosis report: a diagnosis and the number of
 * distinct OPD visits on which it was recorded.
 */
public class EmrDiagnosisCountDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long diagnosisId;
    private String diagnosisName;
    private Long visitCount;

    public EmrDiagnosisCountDTO() {
    }

    public EmrDiagnosisCountDTO(Long diagnosisId, String diagnosisName, Long visitCount) {
        this.diagnosisId = diagnosisId;
        this.diagnosisName = diagnosisName;
        this.visitCount = visitCount;
    }

    public Long getDiagnosisId() {
        return diagnosisId;
    }

    public void setDiagnosisId(Long diagnosisId) {
        this.diagnosisId = diagnosisId;
    }

    public String getDiagnosisName() {
        return diagnosisName;
    }

    public void setDiagnosisName(String diagnosisName) {
        this.diagnosisName = diagnosisName;
    }

    public Long getVisitCount() {
        return visitCount;
    }

    public void setVisitCount(Long visitCount) {
        this.visitCount = visitCount;
    }
}
