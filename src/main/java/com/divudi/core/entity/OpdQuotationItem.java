package com.divudi.core.entity;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/**
 * One service line of an {@link OpdQuotation} (#25885). Values are the prices
 * quoted on the day; they are not locked and not used when billing.
 */
@Entity
public class OpdQuotationItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private OpdQuotation quotation;
    @ManyToOne
    private Item item;
    /** Item name as quoted, so a reprint matches the original even if the item is renamed. */
    private String itemName;

    private double quantity;
    private double grossValue;
    private double discount;
    private double netValue;

    private boolean retired;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OpdQuotation getQuotation() {
        return quotation;
    }

    public void setQuotation(OpdQuotation quotation) {
        this.quotation = quotation;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public double getGrossValue() {
        return grossValue;
    }

    public void setGrossValue(double grossValue) {
        this.grossValue = grossValue;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public double getNetValue() {
        return netValue;
    }

    public void setNetValue(double netValue) {
        this.netValue = netValue;
    }

    public boolean isRetired() {
        return retired;
    }

    public void setRetired(boolean retired) {
        this.retired = retired;
    }
}
