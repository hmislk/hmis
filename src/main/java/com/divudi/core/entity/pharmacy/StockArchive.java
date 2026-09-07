/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.entity.pharmacy;

import com.divudi.core.entity.Department;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.WebUser;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;

/**
 * Archive table for zero-stock Stock rows whose ItemBatch has been archived.
 *
 * Same schema as Stock but used for long-term retention. Rows are moved here
 * as part of the ItemBatch archival job (issue #20724) before the parent
 * ItemBatch row is deleted.
 *
 * No @GeneratedValue: rows keep their original Stock id.
 */
@Entity
@Table(name = "STOCKARCHIVE")
public class StockArchive implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private Long id;

    @Deprecated
    @Column
    private String itemName;

    @Deprecated
    @Column
    private String barcode;

    @Deprecated
    @Column
    private Long longCode;

    @Deprecated
    @Temporal(javax.persistence.TemporalType.DATE)
    private Date dateOfExpire;

    private Double stock = 0.0;

    @ManyToOne
    private ItemBatchArchive itemBatch;

    @ManyToOne
    private Department department;

    @ManyToOne
    Staff staff;

    @Deprecated
    @Column(name = "CODE")
    String code;

    @Deprecated
    private Long startBarcode;

    @Deprecated
    private Long endBarcode;

    @Deprecated
    private double retailsaleRate;

    private boolean retired;

    @ManyToOne
    private WebUser retirer;

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date retiredAt;

    private String retireComments;
    private String stockLocator;

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date archivedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Double getStock() { return stock; }
    public void setStock(Double stock) { this.stock = stock; }

    public ItemBatchArchive getItemBatch() { return itemBatch; }
    public void setItemBatch(ItemBatchArchive itemBatch) { this.itemBatch = itemBatch; }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }

    public Staff getStaff() { return staff; }
    public void setStaff(Staff staff) { this.staff = staff; }

    public boolean isRetired() { return retired; }
    public void setRetired(boolean retired) { this.retired = retired; }

    public WebUser getRetirer() { return retirer; }
    public void setRetirer(WebUser retirer) { this.retirer = retirer; }

    public Date getRetiredAt() { return retiredAt; }
    public void setRetiredAt(Date retiredAt) { this.retiredAt = retiredAt; }

    public String getRetireComments() { return retireComments; }
    public void setRetireComments(String retireComments) { this.retireComments = retireComments; }

    public String getStockLocator() { return stockLocator; }
    public void setStockLocator(String stockLocator) { this.stockLocator = stockLocator; }

    public Date getArchivedAt() { return archivedAt; }
    public void setArchivedAt(Date archivedAt) { this.archivedAt = archivedAt; }

    @Deprecated public String getItemName() { return itemName; }
    @Deprecated public void setItemName(String itemName) { this.itemName = itemName; }
    @Deprecated public String getBarcode() { return barcode; }
    @Deprecated public void setBarcode(String barcode) { this.barcode = barcode; }
    @Deprecated public Long getLongCode() { return longCode; }
    @Deprecated public void setLongCode(Long longCode) { this.longCode = longCode; }
    @Deprecated public Date getDateOfExpire() { return dateOfExpire; }
    @Deprecated public void setDateOfExpire(Date dateOfExpire) { this.dateOfExpire = dateOfExpire; }
    @Deprecated public double getRetailsaleRate() { return retailsaleRate; }
    @Deprecated public void setRetailsaleRate(double retailsaleRate) { this.retailsaleRate = retailsaleRate; }
    @Deprecated public Long getStartBarcode() { return startBarcode; }
    @Deprecated public void setStartBarcode(Long startBarcode) { this.startBarcode = startBarcode; }
    @Deprecated public Long getEndBarcode() { return endBarcode; }
    @Deprecated public void setEndBarcode(Long endBarcode) { this.endBarcode = endBarcode; }
    @Deprecated public String getCode() { return code; }
    @Deprecated public void setCode(String code) { this.code = code; }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof StockArchive)) {
            return false;
        }
        StockArchive other = (StockArchive) object;
        return !((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id)));
    }

    @Override
    public String toString() {
        return "StockArchive[ id=" + id + " ]";
    }
}
