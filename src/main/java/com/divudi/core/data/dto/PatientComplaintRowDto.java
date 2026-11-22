package com.divudi.core.data.dto;

import com.divudi.core.data.PatientComplaintSource;
import com.divudi.core.data.PatientComplaintStatus;
import java.io.Serializable;
import java.util.Date;

/**
 * One row of the staff Patient Complaints register (#25886).
 */
public class PatientComplaintRowDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final Date createdAt;
    private final PatientComplaintSource source;
    private final String categoryName;
    private final String complainantName;
    private final PatientComplaintStatus status;
    private final String assigneeName;
    private final Date closedAt;

    public PatientComplaintRowDto(Long id, Date createdAt, PatientComplaintSource source, String categoryName,
            String complainantName, PatientComplaintStatus status, String assigneeName, Date closedAt) {
        this.id = id;
        this.createdAt = createdAt;
        this.source = source;
        this.categoryName = categoryName;
        this.complainantName = complainantName;
        this.status = status;
        this.assigneeName = assigneeName;
        this.closedAt = closedAt;
    }

    public Long getId() {
        return id;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public PatientComplaintSource getSource() {
        return source;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getComplainantName() {
        return complainantName;
    }

    public PatientComplaintStatus getStatus() {
        return status;
    }

    public String getAssigneeName() {
        return assigneeName;
    }

    public Date getClosedAt() {
        return closedAt;
    }
}
