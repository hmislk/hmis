package com.divudi.core.data.dto;

import com.divudi.core.data.PatientComplaintAction;
import com.divudi.core.data.PatientComplaintStatus;
import java.io.Serializable;
import java.util.Date;

/**
 * One line of a complaint's history on the staff detail page (#25886).
 */
public class PatientComplaintLogRowDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Date createdAt;
    private final PatientComplaintAction action;
    private final PatientComplaintStatus fromStatus;
    private final PatientComplaintStatus toStatus;
    private final String note;
    private final String userName;

    public PatientComplaintLogRowDto(Date createdAt, PatientComplaintAction action, PatientComplaintStatus fromStatus,
            PatientComplaintStatus toStatus, String note, String userName) {
        this.createdAt = createdAt;
        this.action = action;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.note = note;
        this.userName = userName;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public PatientComplaintAction getAction() {
        return action;
    }

    public PatientComplaintStatus getFromStatus() {
        return fromStatus;
    }

    public PatientComplaintStatus getToStatus() {
        return toStatus;
    }

    public String getNote() {
        return note;
    }

    /** Staff name, or "Patient (online)" when the patient made the entry. */
    public String getUserName() {
        return userName == null || userName.trim().isEmpty() ? "Patient (online)" : userName;
    }
}
