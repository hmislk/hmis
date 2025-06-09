package com.divudi.core.entity;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;

/**
 *
 * @author Dr M H B Ariyaratne
 *
 */
@Entity
public class PaymentHandoverItem implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    private Payment payment;

    @ManyToOne
    private WebUser creater;

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date createdAt;

    private boolean retired;

    @ManyToOne
    private WebUser retirer;

    @ManyToOne
    private WebUser currentHolder;

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date retiredAt;

    private String retireComments;

    @ManyToOne
    private Bill handoverShiftBill;
    @ManyToOne
    private Bill handoverShiftComponantBill;

    //Handover Creation
    @ManyToOne
    private Bill handoverCreatedBill;
    @ManyToOne
    private Bill handoverCreatedComponantBill;

    //Handover Accept
    @ManyToOne
    private Bill handoverAcceptBill;
    @ManyToOne
    private Bill handoverAcceptComponantBill;

    public PaymentHandoverItem() {
    }

    public PaymentHandoverItem(Payment payment) {
        this.payment = payment;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof PaymentHandoverItem)) {
            return false;
        }
        PaymentHandoverItem other = (PaymentHandoverItem) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.core.entity.PaymentHandover[ id=" + id + " ]";
    }



    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
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

    public WebUser getCurrentHolder() {
        return currentHolder;
    }

    public void setCurrentHolder(WebUser currentHolder) {
        this.currentHolder = currentHolder;
    }

    public Date getRetiredAt() {
        return retiredAt;
    }

    public void setRetiredAt(Date retiredAt) {
        this.retiredAt = retiredAt;
    }

    public String getRetireComments() {
        return retireComments;
    }

    public void setRetireComments(String retireComments) {
        this.retireComments = retireComments;
    }

    public Bill getHandoverShiftBill() {
        return handoverShiftBill;
    }

    public void setHandoverShiftBill(Bill handoverShiftBill) {
        this.handoverShiftBill = handoverShiftBill;
    }

    public Bill getHandoverShiftComponantBill() {
        return handoverShiftComponantBill;
    }

    public void setHandoverShiftComponantBill(Bill handoverShiftComponantBill) {
        this.handoverShiftComponantBill = handoverShiftComponantBill;
    }

    public Bill getHandoverCreatedBill() {
        return handoverCreatedBill;
    }

    public void setHandoverCreatedBill(Bill handoverCreatedBill) {
        this.handoverCreatedBill = handoverCreatedBill;
    }

    public Bill getHandoverCreatedComponantBill() {
        return handoverCreatedComponantBill;
    }

    public void setHandoverCreatedComponantBill(Bill handoverCreatedComponantBill) {
        this.handoverCreatedComponantBill = handoverCreatedComponantBill;
    }

    public Bill getHandoverAcceptBill() {
        return handoverAcceptBill;
    }

    public void setHandoverAcceptBill(Bill handoverAcceptBill) {
        this.handoverAcceptBill = handoverAcceptBill;
    }

    public Bill getHandoverAcceptComponantBill() {
        return handoverAcceptComponantBill;
    }

    public void setHandoverAcceptComponantBill(Bill handoverAcceptComponantBill) {
        this.handoverAcceptComponantBill = handoverAcceptComponantBill;
    }

}
