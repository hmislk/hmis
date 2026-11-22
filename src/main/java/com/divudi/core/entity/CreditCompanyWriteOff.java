package com.divudi.core.entity;

import com.divudi.core.data.CreditCompanyWriteOffStatus;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
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
 * A request to write off part or all of the due of an OPD credit bill (#25847).
 * The bill's {@code writtenOffAmount} is the sum of its APPROVED write-offs.
 */
@Entity
public class CreditCompanyWriteOff implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The credit bill whose due is written off. */
    @ManyToOne
    private Bill bill;
    /** The open claim holding the bill when the request was made, if any. */
    @ManyToOne
    private CreditCompanyClaim claim;

    private double amount;
    @Column(length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    private CreditCompanyWriteOffStatus status;

    @ManyToOne
    private WebUser requestedBy;
    @Temporal(TemporalType.TIMESTAMP)
    private Date requestedAt;

    @ManyToOne
    private WebUser decidedBy;
    @Temporal(TemporalType.TIMESTAMP)
    private Date decidedAt;
    @Column(length = 500)
    private String decisionComment;

    @ManyToOne
    private WebUser cancelledBy;
    @Temporal(TemporalType.TIMESTAMP)
    private Date cancelledAt;
    @Column(length = 500)
    private String cancelReason;

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

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public CreditCompanyClaim getClaim() {
        return claim;
    }

    public void setClaim(CreditCompanyClaim claim) {
        this.claim = claim;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public CreditCompanyWriteOffStatus getStatus() {
        return status;
    }

    public void setStatus(CreditCompanyWriteOffStatus status) {
        this.status = status;
    }

    public WebUser getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(WebUser requestedBy) {
        this.requestedBy = requestedBy;
    }

    public Date getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Date requestedAt) {
        this.requestedAt = requestedAt;
    }

    public WebUser getDecidedBy() {
        return decidedBy;
    }

    public void setDecidedBy(WebUser decidedBy) {
        this.decidedBy = decidedBy;
    }

    public Date getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(Date decidedAt) {
        this.decidedAt = decidedAt;
    }

    public String getDecisionComment() {
        return decisionComment;
    }

    public void setDecisionComment(String decisionComment) {
        this.decisionComment = decisionComment;
    }

    public WebUser getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(WebUser cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

    public Date getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Date cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
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
        if (!(object instanceof CreditCompanyWriteOff)) {
            return false;
        }
        CreditCompanyWriteOff other = (CreditCompanyWriteOff) object;
        return !((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id)));
    }

    @Override
    public String toString() {
        return "CreditCompanyWriteOff[id=" + id + "]";
    }
}
