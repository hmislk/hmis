/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity.pharmacy;

import com.divudi.entity.Bill;
import com.divudi.entity.Item;
import com.divudi.entity.WebUser;
import java.io.Serializable;
import java.util.Date;
import javax.annotation.Generated;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;

/**
 *
 * @author buddhika
 */
@Entity

public class StockVarientBillItem implements Serializable {

     @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private static final long serialVersionUID = 1L;
    @ManyToOne
    private Item item;
    @ManyToOne
    private StockVarientBillItem referenceStockVariantBillItem;
    @ManyToOne
    private Bill bill;
    private double systemStock;
    private double calCulatedStock;
    private double physicalStock;
    private double adjustedStock;
    private double purchaseRate;
    /////////////////
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

    public void clone(StockVarientBillItem args) {
        item = args.getItem();
        systemStock = args.getSystemStock();
        calCulatedStock = args.getCalCulatedStock();
        physicalStock = args.getPhysicalStock();
        adjustedStock = args.getAdjustedStock();

    }

    public double getSystemStock() {
        return systemStock;
    }

    public void setSystemStock(double systemStock) {
        this.systemStock = systemStock;
    }

    public double getCalCulatedStock() {
        return calCulatedStock;
    }

    public void setCalCulatedStock(double calCulatedStock) {
        this.calCulatedStock = calCulatedStock;
    }

    public double getPhysicalStock() {
        return physicalStock;
    }

    public void setPhysicalStock(double physicalStock) {
        this.physicalStock = physicalStock;
    }

    public double getAdjustedStock() {
        return adjustedStock;
    }

    public void setAdjustedStock(double adjustedStock) {
        this.adjustedStock = adjustedStock;
    }

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

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
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

    public double getPurchaseRate() {
        return purchaseRate;
    }

    public void setPurchaseRate(double purchaseRate) {
        this.purchaseRate = purchaseRate;
    }

    public StockVarientBillItem getReferenceStockVariantBillItem() {
        return referenceStockVariantBillItem;
    }

    public void setReferenceStockVariantBillItem(StockVarientBillItem referenceStockVariantBillItem) {
        this.referenceStockVariantBillItem = referenceStockVariantBillItem;
    }

}
