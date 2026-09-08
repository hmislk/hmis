/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.entity.inward;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.RetirableEntity;
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
 * One configured routine charge that is billed automatically on every matching
 * admission (issue #23594).
 *
 * <p>Rows are resolved per distinct {@link #item}, in two steps: admission type
 * is the outer filter, payment method the inner one. A {@code null} in either
 * column means "applies to all", which is how a charge that costs the same
 * either way is configured with a single row.</p>
 *
 * <p><b>Configuration trap:</b> because step one is a <i>filter</i>, an
 * admission-type-specific row set completely replaces the {@code null} set for
 * that item. Configuring {@code (Admission Charge, Day Case, Cash)} and
 * forgetting the Day Case credit row leaves a credit Day Case admission with no
 * admission charge at all. The management page warns about this.</p>
 *
 * <p>{@link #item} is mandatory and must carry both an {@code inwardChargeType}
 * (which gives the interim bill its charge-type breakdown) and a
 * {@code department} (which decides how the generated bills are bundled).</p>
 *
 * @author Buddhika
 */
@Entity
public class AdmissionChargeItem implements Serializable, RetirableEntity {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The service billed. Required - the BillItem must carry an Item so it has
     * an inwardChargeType and a department.
     */
    @ManyToOne
    private Item item;

    /**
     * The admission type this row applies to. Null means "any admission type".
     */
    @ManyToOne
    private AdmissionType admissionType;

    /**
     * Cash or Credit only - the only two values an admission can hold (see
     * EnumController.getPaymentMethodForAdmission()). Null means "both".
     */
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    private double price;

    private Double qty = 1.0;

    private int orderNo;

    @ManyToOne
    private WebUser creater;
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    private boolean retired;
    @ManyToOne
    private WebUser retirer;
    @Temporal(TemporalType.TIMESTAMP)
    private Date retiredAt;
    private String retireComments;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public AdmissionType getAdmissionType() {
        return admissionType;
    }

    public void setAdmissionType(AdmissionType admissionType) {
        this.admissionType = admissionType;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public Double getQty() {
        return qty;
    }

    public void setQty(Double qty) {
        this.qty = qty;
    }

    public int getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(int orderNo) {
        this.orderNo = orderNo;
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

    @Override
    public boolean isRetired() {
        return retired;
    }

    @Override
    public void setRetired(boolean retired) {
        this.retired = retired;
    }

    @Override
    public WebUser getRetirer() {
        return retirer;
    }

    @Override
    public void setRetirer(WebUser retirer) {
        this.retirer = retirer;
    }

    @Override
    public Date getRetiredAt() {
        return retiredAt;
    }

    @Override
    public void setRetiredAt(Date retiredAt) {
        this.retiredAt = retiredAt;
    }

    @Override
    public String getRetireComments() {
        return retireComments;
    }

    @Override
    public void setRetireComments(String retireComments) {
        this.retireComments = retireComments;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof AdmissionChargeItem)) {
            return false;
        }
        AdmissionChargeItem other = (AdmissionChargeItem) object;
        return (this.id != null || other.id == null) && (this.id == null || this.id.equals(other.id));
    }

    @Override
    public String toString() {
        return "com.divudi.core.entity.inward.AdmissionChargeItem[ id=" + id + " ]";
    }
}
