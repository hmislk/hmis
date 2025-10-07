/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.entity.pharmacy;

import com.divudi.core.entity.RetirableEntity;
import com.divudi.core.data.HistoryType;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.WebUser;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;

/**
 * StockHistory Entity
 *
 * Records historical stock positions at both BATCH and ITEM levels.
 * Each record captures stock quantities and values at three scopes:
 * 1. Department level (single department's stock)
 * 2. Institution level (aggregated across all departments in institution)
 * 3. Total level (aggregated across all institutions)
 *
 * BATCH-LEVEL TRACKING:
 * - stockQty, instituionBatchQty, totalBatchQty: Batch quantities
 * - stockPurchaseValue, stockSaleValue, stockCostValue: Department batch values
 * - institutionBatchStockValueAt*: Institution batch values
 * - totalBatchStockValueAt*: Total batch values
 *
 * ITEM-LEVEL TRACKING:
 * - itemStock, institutionItemStock, totalItemStock: Item quantities (all batches combined)
 * - itemStockValueAt*: Department item values
 * - institutionItemStockValueAt*: Institution item values
 * - totalItemStockValueAt*: Total item values
 *
 * This dual-level tracking enables both:
 * - Batch-wise reports (with batch number and expiry date)
 * - Item-wise reports (aggregated across all batches)
 *
 * @author Buddhika
 */
@Entity
public class StockHistory implements Serializable, RetirableEntity {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Temporal(javax.persistence.TemporalType.DATE)
    Date stockAt;
    @OneToOne(fetch = FetchType.LAZY)
    PharmaceuticalBillItem pbItem;

    // BATCH-LEVEL QUANTITIES
    // This is the Item Batch Stock Quantity of the department
    double stockQty; // == departmentBatchQty
    private double instituionBatchQty;
    private double totalBatchQty;

    double retailRate;

    double wholesaleRate;

    double purchaseRate;

    double costRate;
    @ManyToOne
    ItemBatch itemBatch;
    @ManyToOne
    Institution institution;
    @ManyToOne
    Department department;
    @ManyToOne
    Staff staff;

    @ManyToOne
    Item item;
    @Enumerated(EnumType.STRING)
    HistoryType historyType;

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date fromDate;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date toDate;

    // BATCH-LEVEL VALUES AT RETAIL/SALE RATE
    // This is the Item Batch Stock value at retail rate of the department
    private double stockSaleValue;
    private Double institutionBatchStockValueAtSaleRate;
    private Double totalBatchStockValueAtSaleRate;

    // BATCH-LEVEL VALUES AT PURCHASE RATE
    // This is the Item Batch Stock value at purchase rate of the department
    private double stockPurchaseValue;
    private Double institutionBatchStockValueAtPurchaseRate;
    private Double totalBatchStockValueAtPurchaseRate;

    // BATCH-LEVEL VALUES AT COST RATE
    // This is the Item Batch Stock value at cost rate of the department
    private double stockCostValue;
    private Double institutionBatchStockValueAtCostRate;
    private Double totalBatchStockValueAtCostRate;

    long hxYear;
    int hxMonth;
    int hxDate;
    int hxWeek;

    // ITEM-LEVEL QUANTITIES (All batches of an item aggregated)
    // This gives the stock of this Item for this department (all batches combined)
    private Double itemStock;
    // This gives the stock of this Item for this institution (all batches combined)
    private Double institutionItemStock;
    // This gives the stock of this Item for the whole application (all batches combined)
    private Double totalItemStock;

    // ITEM-LEVEL VALUES AT RETAIL/SALE RATE
    private Double itemStockValueAtSaleRate;
    private Double institutionItemStockValueAtSaleRate;
    private Double totalItemStockValueAtSaleRate;

    // ITEM-LEVEL VALUES AT PURCHASE RATE
    private Double itemStockValueAtPurchaseRate;
    private Double institutionItemStockValueAtPurchaseRate;
    private Double totalItemStockValueAtPurchaseRate;

    // ITEM-LEVEL VALUES AT COST RATE
    private Double itemStockValueAtCostRate;
    private Double institutionItemStockValueAtCostRate;
    private Double totalItemStockValueAtCostRate;

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

    public Date getStockAt() {
        return stockAt;
    }

    public void setStockAt(Date stockAt) {
        this.stockAt = stockAt;
    }

    public PharmaceuticalBillItem getPbItem() {
        return pbItem;
    }

    public void setPbItem(PharmaceuticalBillItem pbItem) {
        this.pbItem = pbItem;
    }

    public double getStockQty() {
        return stockQty;
    }

    public void setStockQty(double stockQty) {
        this.stockQty = stockQty;
    }

    public double getRetailRate() {
        return retailRate;
    }

    public void setRetailRate(double retailRate) {
        this.retailRate = retailRate;
    }

    public double getWholesaleRate() {
        return wholesaleRate;
    }

    public void setWholesaleRate(double wholesaleRate) {
        this.wholesaleRate = wholesaleRate;
    }

    public double getPurchaseRate() {
        return purchaseRate;
    }

    public void setPurchaseRate(double purchaseRate) {
        this.purchaseRate = purchaseRate;
    }

    public double getCostRate() {
        return costRate;
    }

    public void setCostRate(double costRate) {
        this.costRate = costRate;
    }

    public ItemBatch getItemBatch() {
        return itemBatch;
    }

    public void setItemBatch(ItemBatch itemBatch) {
        this.itemBatch = itemBatch;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
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

    public double getStockSaleValue() {
        return stockSaleValue;
    }

    public void setStockSaleValue(double stockSaleValue) {
        this.stockSaleValue = stockSaleValue;
    }

    public double getStockPurchaseValue() {
        return stockPurchaseValue;
    }

    public void setStockPurchaseValue(double stockPurchaseValue) {
        this.stockPurchaseValue = stockPurchaseValue;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public HistoryType getHistoryType() {
        return historyType;
    }

    public void setHistoryType(HistoryType historyType) {
        this.historyType = historyType;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public long getHxYear() {
        return hxYear;
    }

    public void setHxYear(long hxYear) {
        this.hxYear = hxYear;
    }

    public int getHxMonth() {
        return hxMonth;
    }

    public void setHxMonth(int hxMonth) {
        this.hxMonth = hxMonth;
    }

    public int getHxDate() {
        return hxDate;
    }

    public void setHxDate(int hxDate) {
        this.hxDate = hxDate;
    }

    public int getHxWeek() {
        return hxWeek;
    }

    public void setHxWeek(int hxWeek) {
        this.hxWeek = hxWeek;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {

        if (!(object instanceof StockHistory)) {
            return false;
        }
        StockHistory other = (StockHistory) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.core.entity.pharmacy.StockHistory[ id=" + id + " ]";
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

    public Double getItemStock() {
        return itemStock;
    }

    public void setItemStock(Double itemStock) {
        this.itemStock = itemStock;
    }

    public Double getInstitutionItemStock() {
        return institutionItemStock;
    }

    public void setInstitutionItemStock(Double institutionItemStock) {
        this.institutionItemStock = institutionItemStock;
    }

    public Double getTotalItemStock() {
        return totalItemStock;
    }

    public void setTotalItemStock(Double totalItemStock) {
        this.totalItemStock = totalItemStock;
    }

    public Double getItemStockValueAtSaleRate() {
        return itemStockValueAtSaleRate;
    }

    public void setItemStockValueAtSaleRate(Double itemStockValueAtSaleRate) {
        this.itemStockValueAtSaleRate = itemStockValueAtSaleRate;
    }

    public Double getInstitutionItemStockValueAtSaleRate() {
        return institutionItemStockValueAtSaleRate;
    }

    public void setInstitutionItemStockValueAtSaleRate(Double institutionItemStockValueAtSaleRate) {
        this.institutionItemStockValueAtSaleRate = institutionItemStockValueAtSaleRate;
    }

    public Double getTotalItemStockValueAtSaleRate() {
        return totalItemStockValueAtSaleRate;
    }

    public void setTotalItemStockValueAtSaleRate(Double totalItemStockValueAtSaleRate) {
        this.totalItemStockValueAtSaleRate = totalItemStockValueAtSaleRate;
    }

    public Double getItemStockValueAtPurchaseRate() {
        return itemStockValueAtPurchaseRate;
    }

    public void setItemStockValueAtPurchaseRate(Double itemStockValueAtPurchaseRate) {
        this.itemStockValueAtPurchaseRate = itemStockValueAtPurchaseRate;
    }

    public Double getInstitutionItemStockValueAtPurchaseRate() {
        return institutionItemStockValueAtPurchaseRate;
    }

    public void setInstitutionItemStockValueAtPurchaseRate(Double institutionItemStockValueAtPurchaseRate) {
        this.institutionItemStockValueAtPurchaseRate = institutionItemStockValueAtPurchaseRate;
    }

    public Double getTotalItemStockValueAtPurchaseRate() {
        return totalItemStockValueAtPurchaseRate;
    }

    public void setTotalItemStockValueAtPurchaseRate(Double totalItemStockValueAtPurchaseRate) {
        this.totalItemStockValueAtPurchaseRate = totalItemStockValueAtPurchaseRate;
    }

    public Double getItemStockValueAtCostRate() {
        return itemStockValueAtCostRate;
    }

    public void setItemStockValueAtCostRate(Double itemStockValueAtCostRate) {
        this.itemStockValueAtCostRate = itemStockValueAtCostRate;
    }

    public Double getInstitutionItemStockValueAtCostRate() {
        return institutionItemStockValueAtCostRate;
    }

    public void setInstitutionItemStockValueAtCostRate(Double institutionItemStockValueAtCostRate) {
        this.institutionItemStockValueAtCostRate = institutionItemStockValueAtCostRate;
    }

    public Double getTotalItemStockValueAtCostRate() {
        return totalItemStockValueAtCostRate;
    }

    public void setTotalItemStockValueAtCostRate(Double totalItemStockValueAtCostRate) {
        this.totalItemStockValueAtCostRate = totalItemStockValueAtCostRate;
    }

    public double getStockCostValue() {
        return stockCostValue;
    }

    public void setStockCostValue(double stockCostValue) {
        this.stockCostValue = stockCostValue;
    }

    public Double getInstitutionBatchStockValueAtSaleRate() {
        return institutionBatchStockValueAtSaleRate;
    }

    public void setInstitutionBatchStockValueAtSaleRate(Double institutionBatchStockValueAtSaleRate) {
        this.institutionBatchStockValueAtSaleRate = institutionBatchStockValueAtSaleRate;
    }

    public Double getTotalBatchStockValueAtSaleRate() {
        return totalBatchStockValueAtSaleRate;
    }

    public void setTotalBatchStockValueAtSaleRate(Double totalBatchStockValueAtSaleRate) {
        this.totalBatchStockValueAtSaleRate = totalBatchStockValueAtSaleRate;
    }

    public Double getInstitutionBatchStockValueAtPurchaseRate() {
        return institutionBatchStockValueAtPurchaseRate;
    }

    public void setInstitutionBatchStockValueAtPurchaseRate(Double institutionBatchStockValueAtPurchaseRate) {
        this.institutionBatchStockValueAtPurchaseRate = institutionBatchStockValueAtPurchaseRate;
    }

    public Double getTotalBatchStockValueAtPurchaseRate() {
        return totalBatchStockValueAtPurchaseRate;
    }

    public void setTotalBatchStockValueAtPurchaseRate(Double totalBatchStockValueAtPurchaseRate) {
        this.totalBatchStockValueAtPurchaseRate = totalBatchStockValueAtPurchaseRate;
    }

    public Double getInstitutionBatchStockValueAtCostRate() {
        return institutionBatchStockValueAtCostRate;
    }

    public void setInstitutionBatchStockValueAtCostRate(Double institutionBatchStockValueAtCostRate) {
        this.institutionBatchStockValueAtCostRate = institutionBatchStockValueAtCostRate;
    }

    public Double getTotalBatchStockValueAtCostRate() {
        return totalBatchStockValueAtCostRate;
    }

    public void setTotalBatchStockValueAtCostRate(Double totalBatchStockValueAtCostRate) {
        this.totalBatchStockValueAtCostRate = totalBatchStockValueAtCostRate;
    }

    public double getInstituionBatchQty() {
        return instituionBatchQty;
    }

    public void setInstituionBatchQty(double instituionBatchQty) {
        this.instituionBatchQty = instituionBatchQty;
    }

    public double getTotalBatchQty() {
        return totalBatchQty;
    }

    public void setTotalBatchQty(double totalBatchQty) {
        this.totalBatchQty = totalBatchQty;
    }

    
    
}
