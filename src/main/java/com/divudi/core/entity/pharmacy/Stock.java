/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.entity.pharmacy;

import com.divudi.core.entity.RetirableEntity;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.WebUser;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.Transient;

/**
 *
 * @author safrin
 */
@Entity
@Table(name = "stock",
        indexes = {
            @Index(name = "idx_dept_stock_itemname", columnList = "DEPARTMENT_ID, STOCK, ITEMNAME"),
            @Index(name = "idx_dept_stock_code", columnList = "DEPARTMENT_ID, STOCK, CODE"),
            @Index(name = "idx_dept_stock_barcode", columnList = "DEPARTMENT_ID, STOCK, BARCODE"),
            @Index(name = "idx_dept_stock_longcode", columnList = "DEPARTMENT_ID, STOCK, LONGCODE")
        }
)
public class Stock implements Serializable, RetirableEntity {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    // ChatGPT contributed - 2025-05
    @Column(name = "ITEMNAME", length = 100)
    private String itemName;

    @Column(name = "BARCODE", length = 30)
    private String barcode;

    @Column(name = "LONGCODE")
    private Long longCode;

    @Temporal(javax.persistence.TemporalType.DATE)
    private Date dateOfExpire;

    private Double stock = 0.0;

    @Transient
    private double calculated = 0;
    @ManyToOne
    private ItemBatch itemBatch;
    @ManyToOne
    private Department department;
    @ManyToOne
    Staff staff;
    @Column(name = "CODE")
    String code;
    private Long startBarcode;
    private Long endBarcode;
    private double retailsaleRate;

    private boolean retired;

    @ManyToOne
    private WebUser retirer;

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date retiredAt;

    private String retireComments;
    private String stockLocator;

    @Transient
    private Double transItemStockQty;

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
        return "com.divudi.core.entity.pharmacy.Stock[ id=" + id + " ]";
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

    public String getStockLocator() {
        return stockLocator;
    }

    public void setStockLocator(String stockLocator) {
        this.stockLocator = stockLocator;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public Long getLongCode() {
        return longCode;
    }

    public void setLongCode(Long longCode) {
        this.longCode = longCode;
    }

    public Date getDateOfExpire() {
        return dateOfExpire;
    }

    public void setDateOfExpire(Date dateOfExpire) {
        this.dateOfExpire = dateOfExpire;
    }

    public double getRetailsaleRate() {
        return retailsaleRate;
    }

    public void setRetailsaleRate(double retailsaleRate) {
        this.retailsaleRate = retailsaleRate;
    }

}
