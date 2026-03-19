package com.divudi.core.entity.inward;

import com.divudi.core.data.inward.TransferRequestStatus;
import com.divudi.core.entity.WebUser;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 * @author Dr M H B Ariyaratne
 */
@Entity
public class PatientTransferRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    private Admission admission;

    /**
     * null = admission handover; non-null = ward-to-ward transfer
     */
    @ManyToOne
    private PatientRoom fromPatientRoom;

    @ManyToOne
    private RoomFacilityCharge toRoomFacilityCharge;

    @Enumerated(EnumType.STRING)
    private TransferRequestStatus status;

    @Temporal(TemporalType.TIMESTAMP)
    private Date initiatedAt;

    @ManyToOne
    private WebUser initiatedBy;

    @Temporal(TemporalType.TIMESTAMP)
    private Date acceptedAt;

    @ManyToOne
    private WebUser acceptedBy;

    @Temporal(TemporalType.TIMESTAMP)
    private Date cancelledAt;

    @ManyToOne
    private WebUser cancelledBy;

    private String notes;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @ManyToOne
    private WebUser creater;

    private boolean retired;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Admission getAdmission() {
        return admission;
    }

    public void setAdmission(Admission admission) {
        this.admission = admission;
    }

    public PatientRoom getFromPatientRoom() {
        return fromPatientRoom;
    }

    public void setFromPatientRoom(PatientRoom fromPatientRoom) {
        this.fromPatientRoom = fromPatientRoom;
    }

    public RoomFacilityCharge getToRoomFacilityCharge() {
        return toRoomFacilityCharge;
    }

    public void setToRoomFacilityCharge(RoomFacilityCharge toRoomFacilityCharge) {
        this.toRoomFacilityCharge = toRoomFacilityCharge;
    }

    public TransferRequestStatus getStatus() {
        return status;
    }

    public void setStatus(TransferRequestStatus status) {
        this.status = status;
    }

    public Date getInitiatedAt() {
        return initiatedAt;
    }

    public void setInitiatedAt(Date initiatedAt) {
        this.initiatedAt = initiatedAt;
    }

    public WebUser getInitiatedBy() {
        return initiatedBy;
    }

    public void setInitiatedBy(WebUser initiatedBy) {
        this.initiatedBy = initiatedBy;
    }

    public Date getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(Date acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public WebUser getAcceptedBy() {
        return acceptedBy;
    }

    public void setAcceptedBy(WebUser acceptedBy) {
        this.acceptedBy = acceptedBy;
    }

    public Date getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Date cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public WebUser getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(WebUser cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public WebUser getCreater() {
        return creater;
    }

    public void setCreater(WebUser creater) {
        this.creater = creater;
    }

    public boolean isRetired() {
        return retired;
    }

    public void setRetired(boolean retired) {
        this.retired = retired;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof PatientTransferRequest)) {
            return false;
        }
        PatientTransferRequest other = (PatientTransferRequest) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.core.entity.inward.PatientTransferRequest[ id=" + id + " ]";
    }
}
