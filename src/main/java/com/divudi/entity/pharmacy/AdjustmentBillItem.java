/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.divudi.entity.pharmacy;

import com.divudi.entity.BillItem;
import com.divudi.entity.WebUser;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;

/**
 *
 * @author Damiya
 */
@Entity
public class AdjustmentBillItem implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    
    private double qty;
    private double afterQty;
    
    private double purchaseRate;
    private double afterpurchaseRate;
    
    private double retailRate;
    private double afterretailRate;
    
    private double wholesaleRate;
    private double afterwholesaleRate;
    
    @Temporal(javax.persistence.TemporalType.DATE)
    private Date expiryDate;
    @Temporal(javax.persistence.TemporalType.DATE)
    private Date afterexpiryDate;
    
    //Created Properties
    @ManyToOne
    private WebUser creater;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date createdAt;
    //Retairing properties 
    private boolean retired;
    @ManyToOne
    private WebUser retirer;

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date retiredAt;

    private String retireComments;
    //Editer Properties
    @ManyToOne

    private WebUser editer;

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date editedAt;
    
    
    @OneToOne(fetch = FetchType.EAGER)
    private BillItem billItem;

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
        if (!(object instanceof AdjustmentBillItem)) {
            return false;
        }
        AdjustmentBillItem other = (AdjustmentBillItem) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.pharmacy.AdjustmentBillItem[ id=" + id + " ]";
    }

    public double getQty() {
        return qty;
    }

    public void setQty(double qty) {
        this.qty = qty;
    }

    public double getAfterQty() {
        return afterQty;
    }

    public void setAfterQty(double afterQty) {
        this.afterQty = afterQty;
    }

    public double getPurchaseRate() {
        return purchaseRate;
    }

    public void setPurchaseRate(double purchaseRate) {
        this.purchaseRate = purchaseRate;
    }

    public double getAfterpurchaseRate() {
        return afterpurchaseRate;
    }

    public void setAfterpurchaseRate(double afterpurchaseRate) {
        this.afterpurchaseRate = afterpurchaseRate;
    }

    public double getRetailRate() {
        return retailRate;
    }

    public void setRetailRate(double retailRate) {
        this.retailRate = retailRate;
    }

    public double getAfterretailRate() {
        return afterretailRate;
    }

    public void setAfterretailRate(double afterretailRate) {
        this.afterretailRate = afterretailRate;
    }

    public double getWholesaleRate() {
        return wholesaleRate;
    }

    public void setWholesaleRate(double wholesaleRate) {
        this.wholesaleRate = wholesaleRate;
    }

    public double getAfterwholesaleRate() {
        return afterwholesaleRate;
    }

    public void setAfterwholesaleRate(double afterwholesaleRate) {
        this.afterwholesaleRate = afterwholesaleRate;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Date getAfterexpiryDate() {
        return afterexpiryDate;
    }

    public void setAfterexpiryDate(Date afterexpiryDate) {
        this.afterexpiryDate = afterexpiryDate;
    }

    public BillItem getBillItem() {
        return billItem;
    }

    public void setBillItem(BillItem billItem) {
        this.billItem = billItem;
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

    public WebUser getEditer() {
        return editer;
    }

    public void setEditer(WebUser editer) {
        this.editer = editer;
    }

    public Date getEditedAt() {
        return editedAt;
    }

    public void setEditedAt(Date editedAt) {
        this.editedAt = editedAt;
    }
    
}
