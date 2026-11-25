package com.divudi.core.entity;

import com.divudi.core.data.CreditCompanyClaimStatus;
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
 * A claim sent to a credit company for a set of OPD credit bills (#25847).
 */
@Entity
public class CreditCompanyClaim implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Our claim number, from the department yearly bill-number generator. */
    private String claimNo;
    /** The company's own reference for this claim. */
    private String companyReference;

    @ManyToOne
    private Institution creditCompany;
    @ManyToOne
    private Department department;
    @ManyToOne
    private Institution institution;

    @Enumerated(EnumType.STRING)
    private CreditCompanyClaimStatus status;

    private double claimedTotal;
    private int resubmitCount;

    @ManyToOne
    private WebUser creater;
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date submittedAt;
    @ManyToOne
    private WebUser submitter;

    private String rejectReason;
    @Temporal(TemporalType.TIMESTAMP)
    private Date rejectedAt;
    @ManyToOne
    private WebUser rejecter;

    private String closeReason;
    @Temporal(TemporalType.TIMESTAMP)
    private Date closedAt;
    @ManyToOne
    private WebUser closedBy;

    @Temporal(TemporalType.TIMESTAMP)
    private Date cancelledAt;
    @ManyToOne
    private WebUser cancelledBy;

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

    public String getClaimNo() {
        return claimNo;
    }

    public void setClaimNo(String claimNo) {
        this.claimNo = claimNo;
    }

    public String getCompanyReference() {
        return companyReference;
    }

    public void setCompanyReference(String companyReference) {
        this.companyReference = companyReference;
    }

    public Institution getCreditCompany() {
        return creditCompany;
    }

    public void setCreditCompany(Institution creditCompany) {
        this.creditCompany = creditCompany;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public CreditCompanyClaimStatus getStatus() {
        return status;
    }

    public void setStatus(CreditCompanyClaimStatus status) {
        this.status = status;
    }

    public double getClaimedTotal() {
        return claimedTotal;
    }

    public void setClaimedTotal(double claimedTotal) {
        this.claimedTotal = claimedTotal;
    }

    public int getResubmitCount() {
        return resubmitCount;
    }

    public void setResubmitCount(int resubmitCount) {
        this.resubmitCount = resubmitCount;
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

    public Date getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Date submittedAt) {
        this.submittedAt = submittedAt;
    }

    public WebUser getSubmitter() {
        return submitter;
    }

    public void setSubmitter(WebUser submitter) {
        this.submitter = submitter;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public String getCloseReason() {
        return closeReason;
    }

    public void setCloseReason(String closeReason) {
        this.closeReason = closeReason;
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

    public Date getRejectedAt() {
        return rejectedAt;
    }

    public void setRejectedAt(Date rejectedAt) {
        this.rejectedAt = rejectedAt;
    }

    public WebUser getRejecter() {
        return rejecter;
    }

    public void setRejecter(WebUser rejecter) {
        this.rejecter = rejecter;
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
        if (!(object instanceof CreditCompanyClaim)) {
            return false;
        }
        CreditCompanyClaim other = (CreditCompanyClaim) object;
        return !((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id)));
    }

    @Override
    public String toString() {
        return "CreditCompanyClaim[id=" + id + ", claimNo=" + claimNo + "]";
    }
}
