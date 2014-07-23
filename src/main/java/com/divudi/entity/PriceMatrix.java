/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity;

import com.divudi.data.BillType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.inward.InwardChargeType;
import com.divudi.entity.inward.AdmissionType;
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

/**
 *
 * @author Buddhika
 */
@Entity
public class PriceMatrix implements Serializable {

    static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;
    BillType billType;
    @ManyToOne
    Category category;
    @ManyToOne
    Institution institution;
    @ManyToOne
    Department department;
    @ManyToOne
    Item item;
    Double fromPrice = 0.0;
    Double toPrice = 0.0;
    Double margin = 0.0;
//Created Properties
    @ManyToOne
    WebUser creater;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date createdAt;
    //Retairing properties
    boolean retired;
    @ManyToOne
    WebUser retirer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date retiredAt;
    String retireComments;
    @ManyToOne
    private Category roomLocation;
    @ManyToOne
    Category membershipScheme;
    @ManyToOne
    PaymentScheme paymentScheme;
    @Enumerated(value = EnumType.STRING)
    private PaymentMethod paymentMethod;
    @Enumerated(value = EnumType.STRING)
    private InwardChargeType inwardChargeType;
    private double discountPercent;
    @ManyToOne
    AdmissionType admissionType;
    @ManyToOne
    Category roomCategory;

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }
    
    

    public PaymentScheme getPaymentScheme() {
        return paymentScheme;
    }

    public void setPaymentScheme(PaymentScheme paymentScheme) {
        this.paymentScheme = paymentScheme;
    }
    
    

    public Category getRoomCategory() {
        return roomCategory;
    }

    public void setRoomCategory(Category roomCategory) {
        this.roomCategory = roomCategory;
    }

    public AdmissionType getAdmissionType() {
        return admissionType;
    }

    public void setAdmissionType(AdmissionType admissionType) {
        this.admissionType = admissionType;
    }

    public BillType getBillType() {
        return billType;
    }

    public void setBillType(BillType billType) {
        this.billType = billType;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
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

    public String getRetireComments() {
        return retireComments;
    }

    public void setRetireComments(String retireComments) {
        this.retireComments = retireComments;
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

        if (!(object instanceof PriceMatrix)) {
            return false;
        }
        PriceMatrix other = (PriceMatrix) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.InwardPriceAdjustment[ id=" + id + " ]";
    }

    public Double getToPrice() {
        return toPrice;
    }

    public void setToPrice(Double toPrice) {
        this.toPrice = toPrice;
    }

    public Double getMargin() {
        return margin;
    }

    public void setMargin(Double margin) {
        this.margin = margin;
    }

    public Double getFromPrice() {
        return fromPrice;
    }

    public void setFromPrice(Double fromPrice) {
        this.fromPrice = fromPrice;
    }

    public Category getRoomLocation() {
        return roomLocation;
    }

    public void setRoomLocation(Category roomLocation) {
        this.roomLocation = roomLocation;
    }

    public Category getMembershipScheme() {
        return membershipScheme;
    }

    public void setMembershipScheme(Category membershipScheme) {
        this.membershipScheme = membershipScheme;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public InwardChargeType getInwardChargeType() {
        return inwardChargeType;
    }

    public void setInwardChargeType(InwardChargeType inwardChargeType) {
        this.inwardChargeType = inwardChargeType;
    }

    public double getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(double discountPercent) {
        this.discountPercent = discountPercent;
    }

}
