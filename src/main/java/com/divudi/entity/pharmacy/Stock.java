/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.entity.pharmacy;

import com.divudi.entity.Department;
import com.divudi.entity.Staff;
import java.io.Serializable;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

/**
 *
 * @author safrin
 */
@Entity
public class Stock implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private Double stock = 0.0;
    @Transient
    private double calculated = 0;
    @ManyToOne
    private ItemBatch itemBatch;
    @ManyToOne
    private Department department;
    @ManyToOne
    Staff staff;
    String code;
    private Long startBarcode;
    private Long endBarcode;

    @ManyToOne
    Stock parentStock;

    @OneToMany(mappedBy = "parentStock", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<Stock> childStocks;
    
    @Transient
    private Double transItemStockQty;
    

    @Transient
    private Double transItemStockQty;

 
    public List<Stock> getChildStocks() {
        return childStocks;
    }

    public void setChildStocks(List<Stock> childStocks) {
        this.childStocks = childStocks;
    }

    public Stock getParentStock() {
        return parentStock;
    }

    public void setParentStock(Stock parentStock) {
        this.parentStock = parentStock;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
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

        if (!(object instanceof Stock)) {
            return false;
        }
        Stock other = (Stock) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.pharmacy.Stock[ id=" + id + " ]";
    }

    public Double getStock() {
        return stock;
    }

    public void setStock(Double stock) {
        this.stock = stock;
    }

    public ItemBatch getItemBatch() {
        return itemBatch;
    }

    public void setItemBatch(ItemBatch itemBatch) {
        this.itemBatch = itemBatch;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public double getCalculated() {
        return calculated;
    }

    public void setCalculated(double calculated) {
        this.calculated = calculated;
    }

    public Double getTransItemStockQty() {
        return transItemStockQty;
    }

    public void setTransItemStockQty(Double transItemStockQty) {
        this.transItemStockQty = transItemStockQty;
    }

    public Long getStartBarcode() {
        return startBarcode;
    }

    public void setStartBarcode(Long startBarcode) {
        this.startBarcode = startBarcode;
    }

    public Long getEndBarcode() {
        return endBarcode;
    }

    public void setEndBarcode(Long endBarcode) {
        this.endBarcode = endBarcode;
    }

}
