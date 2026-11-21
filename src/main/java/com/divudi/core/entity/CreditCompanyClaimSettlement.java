package com.divudi.core.entity;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Links a credit collection (settlement) bill to a claim and to the claimed
 * bill it paid (#25847). One row per paid bill per settlement. When the
 * settlement bill is cancelled the row is retired, not deleted.
 */
@Entity
public class CreditCompanyClaimSettlement implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private CreditCompanyClaim claim;
    @ManyToOne
    private Bill settlementBill;
    @ManyToOne
    private Bill claimedBill;

    private double amount;

    @ManyToOne
    private WebUser creater;
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

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

    public CreditCompanyClaim getClaim() {
        return claim;
    }

    public void setClaim(CreditCompanyClaim claim) {
        this.claim = claim;
    }

    public Bill getSettlementBill() {
        return settlementBill;
    }

    public void setSettlementBill(Bill settlementBill) {
        this.settlementBill = settlementBill;
    }

    public Bill getClaimedBill() {
        return claimedBill;
    }

    public void setClaimedBill(Bill claimedBill) {
        this.claimedBill = claimedBill;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
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
        if (!(object instanceof CreditCompanyClaimSettlement)) {
            return false;
        }
        CreditCompanyClaimSettlement other = (CreditCompanyClaimSettlement) object;
        return !((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id)));
    }

    @Override
    public String toString() {
        return "CreditCompanyClaimSettlement[id=" + id + "]";
    }
}
