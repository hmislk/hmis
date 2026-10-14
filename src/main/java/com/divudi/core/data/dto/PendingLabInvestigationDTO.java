package com.divudi.core.data.dto;

import com.divudi.core.data.lab.PatientInvestigationStatus;
import java.util.Date;

/**
 * Lightweight DTO for lab investigations not yet sent to lab that block nursing
 * discharge. Populated via a JPQL constructor query in
 * NursingDischargeController.
 */
public class PendingLabInvestigationDTO {

    private String investigationName;
    private Date orderedAt;
    private PatientInvestigationStatus status;

    public PendingLabInvestigationDTO(String investigationName, Date orderedAt, PatientInvestigationStatus status) {
        this.investigationName = investigationName;
        this.orderedAt = orderedAt;
        this.status = status;
    }

    public String getInvestigationName() {
        return investigationName;
    }

    public Date getOrderedAt() {
        return orderedAt;
    }

    public PatientInvestigationStatus getStatus() {
        return status;
    }

    public String getStatusLabel() {
        return status == null ? "" : status.getLabel();
    }
}
