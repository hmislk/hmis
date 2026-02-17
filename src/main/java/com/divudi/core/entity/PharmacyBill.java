package com.divudi.core.entity;

import java.io.Serializable;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;

/**
 *
 * @author Dr M H B Ariyaratne
 *
 */
@Entity
public class PharmacyBill implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    // Bidirectional One-to-One relationship with Bill
    @OneToOne(mappedBy = "pharmacyBill", cascade = CascadeType.ALL)
    private Bill bill;

    // Sale value
    private Double saleValue;

    // Purchase value
    private Double purchaseValue;

    // Free value (Purchase)
    private Double freeValuePurchase;

    // Free value (Sale)
    private Double freeValueSale;

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
        if (!(object instanceof PharmacyBill)) {
            return false;
        }
        PharmacyBill other = (PharmacyBill) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.core.entity.PharmacyBill[ id=" + id + " ]";
    }

    public Double getSaleValue() {
        return saleValue;
    }

    public void setSaleValue(Double saleValue) {
        this.saleValue = saleValue;
    }

    public Double getPurchaseValue() {
        return purchaseValue;
    }

    public void setPurchaseValue(Double purchaseValue) {
        this.purchaseValue = purchaseValue;
    }

    public Double getFreeValuePurchase() {
        return freeValuePurchase;
    }

    public void setFreeValuePurchase(Double freeValuePurchase) {
        this.freeValuePurchase = freeValuePurchase;
    }

    public Double getFreeValueSale() {
        return freeValueSale;
    }

    public void setFreeValueSale(Double freeValueSale) {
        this.freeValueSale = freeValueSale;
    }

    // Clone method
    public PharmacyBill cloneWithoutIdAndBill() {
        PharmacyBill clonedPharmacyBill = new PharmacyBill();
        clonedPharmacyBill.setSaleValue(this.saleValue);
        clonedPharmacyBill.setPurchaseValue(this.purchaseValue);
        clonedPharmacyBill.setFreeValuePurchase(this.freeValuePurchase);
        clonedPharmacyBill.setFreeValueSale(this.freeValueSale);
        return clonedPharmacyBill;
    }

    public void invertValues() {
        if (this.saleValue != null) {
            this.saleValue = -this.saleValue; // Inverts the sale value
        }
        if (this.purchaseValue != null) {
            this.purchaseValue = -this.purchaseValue; // Inverts the purchase value
        }
        if (this.freeValuePurchase != null) {
            this.freeValuePurchase = -this.freeValuePurchase; // Inverts the free purchase value
        }
        if (this.freeValueSale != null) {
            this.freeValueSale = -this.freeValueSale; // Inverts the free sale value
        }
    }

    public void copyValue(PharmacyBill pharmacyBill) {
        this.saleValue = pharmacyBill.getSaleValue();
        this.purchaseValue = pharmacyBill.getPurchaseValue();
        this.freeValuePurchase = pharmacyBill.getFreeValuePurchase();
        this.freeValueSale = pharmacyBill.getFreeValueSale();
    }

    public void invertAndAssignValues(PharmacyBill pharmacyBill) {
    if (pharmacyBill != null) {
        // Inverting and assigning values from another PharmacyBill instance
        if (pharmacyBill.getSaleValue() != null) {
            this.setSaleValue(-pharmacyBill.getSaleValue());
        }
        if (pharmacyBill.getPurchaseValue() != null) {
            this.setPurchaseValue(-pharmacyBill.getPurchaseValue());
        }
        if (pharmacyBill.getFreeValuePurchase() != null) {
            this.setFreeValuePurchase(-pharmacyBill.getFreeValuePurchase());
        }
        if (pharmacyBill.getFreeValueSale() != null) {
            this.setFreeValueSale(-pharmacyBill.getFreeValueSale());
        }
    }
}


    // Clone method with inverted values
    public PharmacyBill cloneWithInvertedValues() {
        PharmacyBill clonedPharmacyBill = new PharmacyBill();

        // Copy values and invert them during the cloning
        if (this.saleValue != null) {
            clonedPharmacyBill.setSaleValue(-this.saleValue); // Inverts the sale value
        }
        if (this.purchaseValue != null) {
            clonedPharmacyBill.setPurchaseValue(-this.purchaseValue); // Inverts the purchase value
        }
        if (this.freeValuePurchase != null) {
            clonedPharmacyBill.setFreeValuePurchase(-this.freeValuePurchase); // Inverts the free purchase value
        }
        if (this.freeValueSale != null) {
            clonedPharmacyBill.setFreeValueSale(-this.freeValueSale); // Inverts the free sale value
        }

        return clonedPharmacyBill;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

}
