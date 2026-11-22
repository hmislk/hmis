package com.divudi.core.data.dto;

import com.divudi.core.data.PatientComplaintStatus;
import java.io.Serializable;
import java.util.Date;

/**
 * A complaint as the patient sees it in the client portal (#25886). It holds
 * only the status, the closing response and dates: no staff names, no
 * assignee, no internal notes. The response is dropped unless the complaint
 * is Closed, so a reopened complaint does not show its earlier answer.
 */
public class PatientComplaintPortalRowDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String categoryName;
    private final String description;
    private final PatientComplaintStatus status;
    private final Date createdAt;
    private final Date closedAt;
    private final String response;

    public PatientComplaintPortalRowDto(Long id, String categoryName, String description,
            PatientComplaintStatus status, Date createdAt, Date closedAt, String response) {
        this.id = id;
        this.categoryName = categoryName;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.closedAt = status == PatientComplaintStatus.CLOSED ? closedAt : null;
        this.response = status == PatientComplaintStatus.CLOSED ? response : null;
    }

    public Long getId() {
        return id;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getDescription() {
        return description;
    }

    public PatientComplaintStatus getStatus() {
        return status;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getClosedAt() {
        return closedAt;
    }

    public String getResponse() {
        return response;
    }
}
