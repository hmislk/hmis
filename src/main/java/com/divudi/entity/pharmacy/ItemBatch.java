/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.entity.pharmacy;

import com.divudi.entity.BillItem;
import com.divudi.entity.Category;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 * @author safrin
 */
@Entity
public class ItemBatch implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
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
    
    public String getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(String serialNo) {
        this.serialNo = serialNo;
    }

    
    
    public BillItem getLastPurchaseBillItem() {
        return lastPurchaseBillItem;
    }

    public void setLastPurchaseBillItem(BillItem lastPurchaseBillItem) {
        this.lastPurchaseBillItem = lastPurchaseBillItem;
    }
    
    public String getWarrentyCertificateNumber() {
        return warrentyCertificateNumber;
    }

    public void setWarrentyCertificateNumber(String warrentyCertificateNumber) {
        this.warrentyCertificateNumber = warrentyCertificateNumber;
    }

    public long getWarrentyDuration() {
        return warrentyDuration;
    }

    public void setWarrentyDuration(long warrentyDuration) {
        this.warrentyDuration = warrentyDuration;
    }

    public double getTotalAcquicitionCost() {
        return totalAcquicitionCost;
    }

    public void setTotalAcquicitionCost(double totalAcquicitionCost) {
        this.totalAcquicitionCost = totalAcquicitionCost;
    }

    public double getDeprecitionRate() {
        return deprecitionRate;
    }

    public void setDeprecitionRate(double deprecitionRate) {
        this.deprecitionRate = deprecitionRate;
    }

    public String getOtherNotes() {
        return otherNotes;
    }

    public void setOtherNotes(String otherNotes) {
        this.otherNotes = otherNotes;
    }

    public Institution getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(Institution manufacturer) {
        this.manufacturer = manufacturer;
    }
    
    
    
    

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getRegistrationNo() {
        return registrationNo;
    }

    public void setRegistrationNo(String registrationNo) {
        this.registrationNo = registrationNo;
    }

    public String getChassisNo() {
        return chassisNo;
    }

    public void setChassisNo(String chassisNo) {
        this.chassisNo = chassisNo;
    }

    public String getEngineNo() {
        return engineNo;
    }

    public void setEngineNo(String engineNo) {
        this.engineNo = engineNo;
    }

    public String getColour() {
        return colour;
    }

    public void setColour(String colour) {
        this.colour = colour;
    }

    public int getNumberOfAccessories() {
        return numberOfAccessories;
    }

    public void setNumberOfAccessories(int numberOfAccessories) {
        this.numberOfAccessories = numberOfAccessories;
    }
    
    
    

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
    

    public Category getMake() {
        return make;
    }

    public void setMake(Category make) {
        this.make = make;
    }

    public String getModal() {
        return modal;
    }

    public void setModal(String modal) {
        this.modal = modal;
    }
    
    

    public double getPurcahseRate() {
        return purcahseRate;
    }

    public void setPurcahseRate(double purcahseRate) {
        this.purcahseRate = purcahseRate;
    }

    public double getRetailsaleRate() {
        return retailsaleRate;
    }

    public void setRetailsaleRate(double retailsaleRate) {
        this.retailsaleRate = retailsaleRate;
    }

    public double getWholesaleRate() {
        return wholesaleRate;
    }

    public void setWholesaleRate(double wholesaleRate) {
        this.wholesaleRate = wholesaleRate;
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
        
        if (!(object instanceof ItemBatch)) {
            return false;
        }
        ItemBatch other = (ItemBatch) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.pharmacy.ItemBatch[ id=" + id + " ]";
    }

    public Date getDateOfManufacture() {
        return dateOfManufacture;
    }

    public void setDateOfManufacture(Date dateOfManufacture) {
        this.dateOfManufacture = dateOfManufacture;
    }

    public Date getDateOfExpire() {
        return dateOfExpire;
    }

    public void setDateOfExpire(Date dateOfExpire) {
        this.dateOfExpire = dateOfExpire;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }
}
