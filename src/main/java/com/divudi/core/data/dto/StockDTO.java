package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

public class StockDTO implements Serializable {
    private Long id;
    private String itemName;
    private String code;
    private String genericName;
    private Double retailRate;
    private Double stockQty;
    private Date dateOfExpire;

    public StockDTO() {
    }

    public StockDTO(Long id, String itemName, String code, String genericName,
                     Double retailRate, Double stockQty, Date dateOfExpire) {
        this.id = id;
        this.itemName = itemName;
        this.code = code;
        this.genericName = genericName;
        this.retailRate = retailRate;
        this.stockQty = stockQty;
        this.dateOfExpire = dateOfExpire;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getGenericName() {
        return genericName;
    }

    public void setGenericName(String genericName) {
        this.genericName = genericName;
    }

    public Double getRetailRate() {
        return retailRate;
    }

    public void setRetailRate(Double retailRate) {
        this.retailRate = retailRate;
    }

    public Double getStockQty() {
        return stockQty;
    }

    public void setStockQty(Double stockQty) {
        this.stockQty = stockQty;
    }

    public Date getDateOfExpire() {
        return dateOfExpire;
    }

    public void setDateOfExpire(Date dateOfExpire) {
        this.dateOfExpire = dateOfExpire;
    }
}
