package com.divudi.core.entity;

import com.divudi.core.data.PatientComplaintSource;
import com.divudi.core.data.PatientComplaintStatus;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * A complaint from a patient, entered by staff on site or submitted by the
 * patient in the client portal (#25886). One register for both.
 */
@Entity
public class PatientComplaint implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Null for an on-site complaint from someone who is not a registered patient. */
    @ManyToOne
    private Patient patient;
    /** Set when there is no patient record. */
    private String complainantName;
    private String complainantPhone;

    @Enumerated(EnumType.STRING)
    private PatientComplaintSource source;
    @ManyToOne
    private PatientComplaintCategory category;
    @Lob
    private String description;

    @Enumerated(EnumType.STRING)
    private PatientComplaintStatus status;
    @ManyToOne
    private WebUser assignee;
    @Lob
    private String response;

    @ManyToOne
    private Institution institution;
    @ManyToOne
    private Department department;

    @ManyToOne
    private WebUser creater;
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date closedAt;
    @ManyToOne
    private WebUser closedBy;

    private boolean retired;
    @ManyToOne
    private WebUser retirer;
    @Temporal(TemporalType.TIMESTAMP)
    private Date retiredAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public String getComplainantName() {
        return complainantName;
    }

    public void setComplainantName(String complainantName) {
        this.complainantName = complainantName;
    }

    public String getComplainantPhone() {
        return complainantPhone;
    }

    public void setComplainantPhone(String complainantPhone) {
        this.complainantPhone = complainantPhone;
    }

    public PatientComplaintSource getSource() {
        return source;
    }

    public void setSource(PatientComplaintSource source) {
        this.source = source;
    }

    public PatientComplaintCategory getCategory() {
        return category;
    }

    public void setCategory(PatientComplaintCategory category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PatientComplaintStatus getStatus() {
        return status;
    }

    public void setStatus(PatientComplaintStatus status) {
        this.status = status;
    }

    public WebUser getAssignee() {
        return assignee;
    }

    public void setAssignee(WebUser assignee) {
        this.assignee = assignee;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public WebUser getCreater() {
        return creater;
    }

    public void setCreater(WebUser creater) {
        this.creater = creater;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Date getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Date closedAt) {
        this.closedAt = closedAt;
    }

    public WebUser getClosedBy() {
        return closedBy;
    }

    public void setClosedBy(WebUser closedBy) {
        this.closedBy = closedBy;
    }

    public boolean isRetired() {
        return retired;
    }

    public void setRetired(boolean retired) {
        this.retired = retired;
    }

    public WebUser getRetirer() {
        return retirer;
    }

    public void setRetirer(WebUser retirer) {
        this.retirer = retirer;
    }

    public Date getRetiredAt() {
        return retiredAt;
    }

    public void setRetiredAt(Date retiredAt) {
        this.retiredAt = retiredAt;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof PatientComplaint)) {
            return false;
        }
        PatientComplaint other = (PatientComplaint) object;
        return this.id != null && this.id.equals(other.id);
    }

    @Override
    public String toString() {
        return "com.divudi.core.entity.PatientComplaint[ id=" + id + " ]";
    }
}
