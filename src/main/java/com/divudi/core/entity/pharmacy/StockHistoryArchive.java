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
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;

/**
 * Archive table for old StockHistory rows.
 *
 * Same schema as StockHistory but used for long-term retention. Rows older
 * than the configured retention window are moved here by the archival job
 * (issue #20726).
 *
 * Notes:
 * - No @GeneratedValue on id: rows keep their original StockHistory id so
 *   downstream references (e.g. audit logs, exported reports) remain stable.
 * - Same field order/types as StockHistory so a column-list-based
 *   INSERT...SELECT lines up reliably.
 */
@Entity
@Table(name = "STOCKHISTORYARCHIVE")
public class StockHistoryArchive implements Serializable, RetirableEntity {

    private static final long serialVersionUID = 1L;
    @Id
    private Long id;

    @Temporal(javax.persistence.TemporalType.DATE)
    Date stockAt;
    @OneToOne(fetch = FetchType.LAZY)
    PharmaceuticalBillItem pbItem;

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

    private double stockSaleValue;
    private Double institutionBatchStockValueAtSaleRate;
    private Double totalBatchStockValueAtSaleRate;

    private double stockPurchaseValue;
    private Double institutionBatchStockValueAtPurchaseRate;
    private Double totalBatchStockValueAtPurchaseRate;

    private double stockCostValue;
    private Double institutionBatchStockValueAtCostRate;
    private Double totalBatchStockValueAtCostRate;

    long hxYear;
    int hxMonth;
    int hxDate;
    int hxWeek;

    double stockQty;
    private double instituionBatchQty;
    private double totalBatchQty;

    private Double itemStock;
    private Double institutionItemStock;
    private Double totalItemStock;

    private Double itemStockValueAtSaleRate;
    private Double institutionItemStockValueAtSaleRate;
    private Double totalItemStockValueAtSaleRate;

    private Double itemStockValueAtPurchaseRate;
    private Double institutionItemStockValueAtPurchaseRate;
    private Double totalItemStockValueAtPurchaseRate;

    private Double itemStockValueAtCostRate;
    private Double institutionItemStockValueAtCostRate;
    private Double totalItemStockValueAtCostRate;

    @ManyToOne
    private WebUser creater;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date createdAt;
    private boolean retired;
    @ManyToOne
    private WebUser retirer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date retiredAt;
    private String retireComments;

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date archivedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Date getStockAt() { return stockAt; }
    public void setStockAt(Date stockAt) { this.stockAt = stockAt; }

    public PharmaceuticalBillItem getPbItem() { return pbItem; }
    public void setPbItem(PharmaceuticalBillItem pbItem) { this.pbItem = pbItem; }

    public double getRetailRate() { return retailRate; }
    public void setRetailRate(double retailRate) { this.retailRate = retailRate; }

    public double getWholesaleRate() { return wholesaleRate; }
    public void setWholesaleRate(double wholesaleRate) { this.wholesaleRate = wholesaleRate; }

    public double getPurchaseRate() { return purchaseRate; }
    public void setPurchaseRate(double purchaseRate) { this.purchaseRate = purchaseRate; }

    public double getCostRate() { return costRate; }
    public void setCostRate(double costRate) { this.costRate = costRate; }

    public ItemBatch getItemBatch() { return itemBatch; }
    public void setItemBatch(ItemBatch itemBatch) { this.itemBatch = itemBatch; }

    public Institution getInstitution() { return institution; }
    public void setInstitution(Institution institution) { this.institution = institution; }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }

    public Staff getStaff() { return staff; }
    public void setStaff(Staff staff) { this.staff = staff; }

    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }

    public HistoryType getHistoryType() { return historyType; }
    public void setHistoryType(HistoryType historyType) { this.historyType = historyType; }

    public Date getFromDate() { return fromDate; }
    public void setFromDate(Date fromDate) { this.fromDate = fromDate; }

    public Date getToDate() { return toDate; }
    public void setToDate(Date toDate) { this.toDate = toDate; }

    public double getStockSaleValue() { return stockSaleValue; }
    public void setStockSaleValue(double stockSaleValue) { this.stockSaleValue = stockSaleValue; }

    public Double getInstitutionBatchStockValueAtSaleRate() { return institutionBatchStockValueAtSaleRate; }
    public void setInstitutionBatchStockValueAtSaleRate(Double v) { this.institutionBatchStockValueAtSaleRate = v; }

    public Double getTotalBatchStockValueAtSaleRate() { return totalBatchStockValueAtSaleRate; }
    public void setTotalBatchStockValueAtSaleRate(Double v) { this.totalBatchStockValueAtSaleRate = v; }

    public double getStockPurchaseValue() { return stockPurchaseValue; }
    public void setStockPurchaseValue(double stockPurchaseValue) { this.stockPurchaseValue = stockPurchaseValue; }

    public Double getInstitutionBatchStockValueAtPurchaseRate() { return institutionBatchStockValueAtPurchaseRate; }
    public void setInstitutionBatchStockValueAtPurchaseRate(Double v) { this.institutionBatchStockValueAtPurchaseRate = v; }

    public Double getTotalBatchStockValueAtPurchaseRate() { return totalBatchStockValueAtPurchaseRate; }
    public void setTotalBatchStockValueAtPurchaseRate(Double v) { this.totalBatchStockValueAtPurchaseRate = v; }

    public double getStockCostValue() { return stockCostValue; }
    public void setStockCostValue(double stockCostValue) { this.stockCostValue = stockCostValue; }

    public Double getInstitutionBatchStockValueAtCostRate() { return institutionBatchStockValueAtCostRate; }
    public void setInstitutionBatchStockValueAtCostRate(Double v) { this.institutionBatchStockValueAtCostRate = v; }

    public Double getTotalBatchStockValueAtCostRate() { return totalBatchStockValueAtCostRate; }
    public void setTotalBatchStockValueAtCostRate(Double v) { this.totalBatchStockValueAtCostRate = v; }

    public long getHxYear() { return hxYear; }
    public void setHxYear(long hxYear) { this.hxYear = hxYear; }

    public int getHxMonth() { return hxMonth; }
    public void setHxMonth(int hxMonth) { this.hxMonth = hxMonth; }

    public int getHxDate() { return hxDate; }
    public void setHxDate(int hxDate) { this.hxDate = hxDate; }

    public int getHxWeek() { return hxWeek; }
    public void setHxWeek(int hxWeek) { this.hxWeek = hxWeek; }

    public double getStockQty() { return stockQty; }
    public void setStockQty(double stockQty) { this.stockQty = stockQty; }

    public double getInstituionBatchQty() { return instituionBatchQty; }
    public void setInstituionBatchQty(double instituionBatchQty) { this.instituionBatchQty = instituionBatchQty; }

    public double getTotalBatchQty() { return totalBatchQty; }
    public void setTotalBatchQty(double totalBatchQty) { this.totalBatchQty = totalBatchQty; }

    public Double getItemStock() { return itemStock; }
    public void setItemStock(Double itemStock) { this.itemStock = itemStock; }

    public Double getInstitutionItemStock() { return institutionItemStock; }
    public void setInstitutionItemStock(Double institutionItemStock) { this.institutionItemStock = institutionItemStock; }

    public Double getTotalItemStock() { return totalItemStock; }
    public void setTotalItemStock(Double totalItemStock) { this.totalItemStock = totalItemStock; }

    public Double getItemStockValueAtSaleRate() { return itemStockValueAtSaleRate; }
    public void setItemStockValueAtSaleRate(Double v) { this.itemStockValueAtSaleRate = v; }

    public Double getInstitutionItemStockValueAtSaleRate() { return institutionItemStockValueAtSaleRate; }
    public void setInstitutionItemStockValueAtSaleRate(Double v) { this.institutionItemStockValueAtSaleRate = v; }

    public Double getTotalItemStockValueAtSaleRate() { return totalItemStockValueAtSaleRate; }
    public void setTotalItemStockValueAtSaleRate(Double v) { this.totalItemStockValueAtSaleRate = v; }

    public Double getItemStockValueAtPurchaseRate() { return itemStockValueAtPurchaseRate; }
    public void setItemStockValueAtPurchaseRate(Double v) { this.itemStockValueAtPurchaseRate = v; }

    public Double getInstitutionItemStockValueAtPurchaseRate() { return institutionItemStockValueAtPurchaseRate; }
    public void setInstitutionItemStockValueAtPurchaseRate(Double v) { this.institutionItemStockValueAtPurchaseRate = v; }

    public Double getTotalItemStockValueAtPurchaseRate() { return totalItemStockValueAtPurchaseRate; }
    public void setTotalItemStockValueAtPurchaseRate(Double v) { this.totalItemStockValueAtPurchaseRate = v; }

    public Double getItemStockValueAtCostRate() { return itemStockValueAtCostRate; }
    public void setItemStockValueAtCostRate(Double v) { this.itemStockValueAtCostRate = v; }

    public Double getInstitutionItemStockValueAtCostRate() { return institutionItemStockValueAtCostRate; }
    public void setInstitutionItemStockValueAtCostRate(Double v) { this.institutionItemStockValueAtCostRate = v; }

    public Double getTotalItemStockValueAtCostRate() { return totalItemStockValueAtCostRate; }
    public void setTotalItemStockValueAtCostRate(Double v) { this.totalItemStockValueAtCostRate = v; }

    public WebUser getCreater() { return creater; }
    public void setCreater(WebUser creater) { this.creater = creater; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean isRetired() { return retired; }
    @Override
    public void setRetired(boolean retired) { this.retired = retired; }

    public WebUser getRetirer() { return retirer; }
    public void setRetirer(WebUser retirer) { this.retirer = retirer; }

    public Date getRetiredAt() { return retiredAt; }
    public void setRetiredAt(Date retiredAt) { this.retiredAt = retiredAt; }

    public String getRetireComments() { return retireComments; }
    public void setRetireComments(String retireComments) { this.retireComments = retireComments; }

    public Date getArchivedAt() { return archivedAt; }
    public void setArchivedAt(Date archivedAt) { this.archivedAt = archivedAt; }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof StockHistoryArchive)) {
            return false;
        }
        StockHistoryArchive other = (StockHistoryArchive) object;
        return !((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id)));
    }

    @Override
    public String toString() {
        return "com.divudi.core.entity.pharmacy.StockHistoryArchive[ id=" + id + " ]";
    }
}
