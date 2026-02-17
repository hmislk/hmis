package com.divudi.core.entity;

import java.io.Serializable;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;

/**
 *
 * @author Dr Buddhika Ariyaratne
 */
@Entity
@Deprecated // Use BillFinanceDetails
public class StockBill implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private double stockValueAtPurchaseRates;
    private double stockValueAsSaleRate;
    private double stockValueAsCostRate;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER, optional = true, orphanRemoval = true)
    private Bill bill;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void copyStockBill(StockBill bill) {
        if (bill != null) {
            stockValueAsSaleRate = bill.getStockValueAsSaleRate();
            stockValueAtPurchaseRates = bill.getStockValueAtPurchaseRates();
        }
    }

    public StockBill createNewBill() {
        StockBill newBill = new StockBill();
        newBill.setStockValueAtPurchaseRates(this.getStockValueAtPurchaseRates());
        newBill.setStockValueAsSaleRate(this.getStockValueAsSaleRate());
        return newBill;
    }

    public void invertStockBillValues(Bill bill) {
        stockValueAsSaleRate = 0 - bill.getStockBill().getStockValueAsSaleRate();
        stockValueAtPurchaseRates = 0 - bill.getStockBill().getStockValueAtPurchaseRates();

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
        if (!(object instanceof StockBill)) {
            return false;
        }
        StockBill other = (StockBill) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.core.entity.StockBill[ id=" + id + " ]";
    }

    public double getStockValueAtPurchaseRates() {
        return stockValueAtPurchaseRates;
    }

    public void setStockValueAtPurchaseRates(double stockValueAtPurchaseRates) {
        this.stockValueAtPurchaseRates = stockValueAtPurchaseRates;
    }

    public double getStockValueAsSaleRate() {
        return stockValueAsSaleRate;
    }

    public void setStockValueAsSaleRate(double stockValueAsSaleRate) {
        this.stockValueAsSaleRate = stockValueAsSaleRate;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public double getStockValueAsCostRate() {
        return stockValueAsCostRate;
    }

    public void setStockValueAsCostRate(double stockValueAsCostRate) {
        this.stockValueAsCostRate = stockValueAsCostRate;
    }
    
    

}
