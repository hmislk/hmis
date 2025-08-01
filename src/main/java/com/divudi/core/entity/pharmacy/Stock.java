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
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.Transient;

/**
 *
 * @author safrin
 */
@Entity
public class Stock implements Serializable, RetirableEntity {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Deprecated // Use Item Batch > Item > barcode
    @Column
    private String itemName;

    @Deprecated // Use Item Batch > Item > barcode
    @Column
    private String barcode;

    @Deprecated // Use Item Batch > Item
    @Column
    private Long longCode;

    @Deprecated // Use Item Batch > DOE
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
    @Deprecated // Use Item Batch > Item > Code
    @Column(name = "CODE")
    String code;
    @Deprecated // WIll remove all later
    private Long startBarcode;
    @Deprecated //Will remove all later
    private Long endBarcode;
    @Deprecated // Use Item Stock > Retail Sale
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

    @Deprecated
    public String getItemName() {
        return itemName;
    }

    @Deprecated
    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    @Deprecated
    public String getBarcode() {
        return barcode;
    }

    @Deprecated
    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    @Deprecated
    public Long getLongCode() {
        return longCode;
    }

    @Deprecated
    public void setLongCode(Long longCode) {
        this.longCode = longCode;
    }

    @Deprecated
    public Date getDateOfExpire() {
        return dateOfExpire;
    }

    @Deprecated
    public void setDateOfExpire(Date dateOfExpire) {
        this.dateOfExpire = dateOfExpire;
    }

    @Deprecated
    public double getRetailsaleRate() {
        return retailsaleRate;
    }

    @Deprecated
    public void setRetailsaleRate(double retailsaleRate) {
        this.retailsaleRate = retailsaleRate;
    }

}
