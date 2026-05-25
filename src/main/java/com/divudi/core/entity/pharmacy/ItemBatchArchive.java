/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.entity.pharmacy;

import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.WebUser;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Archive table for expired zero-stock ItemBatch rows.
 *
 * Same schema as ItemBatch but used for long-term retention. Eligible rows
 * (expired AND zero stock across all departments) are moved here by the
 * archival job (issue #20724).
 *
 * No @GeneratedValue: rows keep their original ItemBatch id so downstream
 * references (StockHistory.itemBatch, PharmaceuticalBillItem.archivedItemBatch)
 * remain stable.
 */
@Entity
@Table(name = "ITEMBATCHARCHIVE")
public class ItemBatchArchive implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private Long id;

    @Temporal(TemporalType.TIMESTAMP)
    private Date dateOfManufacture;

    @Temporal(TemporalType.TIMESTAMP)
    private Date dateOfExpire;

    private String batchNo;

    @ManyToOne
    private Item item;

    double purcahseRate;
    double retailsaleRate;
    double wholesaleRate;
    private Double costRate;

    @ManyToOne
    Category make;

    String modal;
    String serialNo;

    @Lob
    String description;

    String barcode;
    String registrationNo;
    String chassisNo;
    String engineNo;
    String colour;
    int numberOfAccessories;
    String warrentyCertificateNumber;
    long warrentyDuration;
    double totalAcquicitionCost;
    double deprecitionRate;

    @Lob
    String otherNotes;

    @ManyToOne
    Institution manufacturer;

    @ManyToOne
    BillItem lastPurchaseBillItem;

    private boolean retired;

    @ManyToOne
    private WebUser retirer;

    @Temporal(TemporalType.TIMESTAMP)
    private Date retiredAt;

    private String retireComments;

    @Temporal(TemporalType.TIMESTAMP)
    private Date archivedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Date getDateOfManufacture() { return dateOfManufacture; }
    public void setDateOfManufacture(Date dateOfManufacture) { this.dateOfManufacture = dateOfManufacture; }

    public Date getDateOfExpire() { return dateOfExpire; }
    public void setDateOfExpire(Date dateOfExpire) { this.dateOfExpire = dateOfExpire; }

    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }

    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }

    public double getPurcahseRate() { return purcahseRate; }
    public void setPurcahseRate(double purcahseRate) { this.purcahseRate = purcahseRate; }

    public double getRetailsaleRate() { return retailsaleRate; }
    public void setRetailsaleRate(double retailsaleRate) { this.retailsaleRate = retailsaleRate; }

    public double getWholesaleRate() { return wholesaleRate; }
    public void setWholesaleRate(double wholesaleRate) { this.wholesaleRate = wholesaleRate; }

    public Double getCostRate() { return costRate; }
    public void setCostRate(Double costRate) { this.costRate = costRate; }

    public Category getMake() { return make; }
    public void setMake(Category make) { this.make = make; }

    public String getModal() { return modal; }
    public void setModal(String modal) { this.modal = modal; }

    public String getSerialNo() { return serialNo; }
    public void setSerialNo(String serialNo) { this.serialNo = serialNo; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public String getRegistrationNo() { return registrationNo; }
    public void setRegistrationNo(String registrationNo) { this.registrationNo = registrationNo; }

    public String getChassisNo() { return chassisNo; }
    public void setChassisNo(String chassisNo) { this.chassisNo = chassisNo; }

    public String getEngineNo() { return engineNo; }
    public void setEngineNo(String engineNo) { this.engineNo = engineNo; }

    public String getColour() { return colour; }
    public void setColour(String colour) { this.colour = colour; }

    public int getNumberOfAccessories() { return numberOfAccessories; }
    public void setNumberOfAccessories(int numberOfAccessories) { this.numberOfAccessories = numberOfAccessories; }

    public String getWarrentyCertificateNumber() { return warrentyCertificateNumber; }
    public void setWarrentyCertificateNumber(String warrentyCertificateNumber) { this.warrentyCertificateNumber = warrentyCertificateNumber; }

    public long getWarrentyDuration() { return warrentyDuration; }
    public void setWarrentyDuration(long warrentyDuration) { this.warrentyDuration = warrentyDuration; }

    public double getTotalAcquicitionCost() { return totalAcquicitionCost; }
    public void setTotalAcquicitionCost(double totalAcquicitionCost) { this.totalAcquicitionCost = totalAcquicitionCost; }

    public double getDeprecitionRate() { return deprecitionRate; }
    public void setDeprecitionRate(double deprecitionRate) { this.deprecitionRate = deprecitionRate; }

    public String getOtherNotes() { return otherNotes; }
    public void setOtherNotes(String otherNotes) { this.otherNotes = otherNotes; }

    public Institution getManufacturer() { return manufacturer; }
    public void setManufacturer(Institution manufacturer) { this.manufacturer = manufacturer; }

    public BillItem getLastPurchaseBillItem() { return lastPurchaseBillItem; }
    public void setLastPurchaseBillItem(BillItem lastPurchaseBillItem) { this.lastPurchaseBillItem = lastPurchaseBillItem; }

    public boolean isRetired() { return retired; }
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
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof ItemBatchArchive)) {
            return false;
        }
        ItemBatchArchive other = (ItemBatchArchive) object;
        return !((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id)));
    }

    @Override
    public String toString() {
        return "ItemBatchArchive[ id=" + id + " ]";
    }
}
