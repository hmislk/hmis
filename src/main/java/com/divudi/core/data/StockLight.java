package com.divudi.core.data;

import java.io.Serializable;
import java.util.Date;

public class StockLight  implements Serializable  {
    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String itemName;
    private final String code;
    private final String barcode;
    private final Date dateOfExpire;
    private final Double retailsaleRate;
    private final Double stockQty;

    public StockLight(Long id, String itemName, String code, String barcode, Date dateOfExpire, Double retailsaleRate, Double stockQty) {
        this.id = id;
        this.itemName = itemName;
        this.code = code;
        this.barcode = barcode;
        this.dateOfExpire = dateOfExpire;
        this.retailsaleRate = retailsaleRate;
        this.stockQty = stockQty;
    }

    public Long getId() {
        return id;
    }

    public String getItemName() {
        return itemName;
    }

    public String getCode() {
        return code;
    }

    public String getBarcode() {
        return barcode;
    }

    public Date getDateOfExpire() {
        return dateOfExpire;
    }

    public Double getRetailsaleRate() {
        return retailsaleRate;
    }

    public Double getStockQty() {
        return stockQty;
    }
    
    

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof StockLight)) {
            return false;
        }
        StockLight other = (StockLight) object;
        return (this.id != null || other.id == null) && (this.id == null || this.id.equals(other.id));
    }

    @Override
    public String toString() {
        return "StockLight{" + "id=" + id + ", itemName=" + itemName + '}';
    }

}
