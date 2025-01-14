/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.entity.pharmacy;

import com.divudi.bean.common.TimeUtils;
import com.divudi.entity.BillItem;
import com.divudi.entity.Category;
import com.divudi.entity.Institution;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.Transient;

/**
 *
 * @author Buddhika
 */
@Entity
public class PharmaceuticalBillItem implements Serializable {

    @OneToOne(mappedBy = "pbItem")
    private StockHistory stockHistory;
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
//    @JoinColumn(name = "bill_item_id") // This is the owning side, so it should have the @JoinColumn annotation.
    private BillItem billItem;

    @Temporal(javax.persistence.TemporalType.DATE)
    private Date doe;
    @ManyToOne
    private ItemBatch itemBatch;
    private String stringValue;

    private double qty;
    private double qtyPacks;

    private double freeQty;
    private double freeQtyPacks;

    private double remainingFreeQty;
    private double remainingFreeQtyPack;

    private double purchaseRate;
    private double purchaseRatePack;

    private double retailRate;
    private double retailRatePack;
    
    private double purchaseValue;
    private double purchaseRatePackValue;

    private double retailValue;
    private double retailPackValue;

    private double wholesaleRate;
    private double wholesaleRatePack;

    private double lastPurchaseRate;
    private double lastPurchaseRatePack;

    @ManyToOne
    private Stock stock;
    @ManyToOne
    private Stock staffStock;

    @ManyToOne
    private Category make;
    private String model;
    private String code;
    private String serialNo;
    @Lob
    private String description;
    private String barcode;
    private String registrationNo;
    private String chassisNo;
    private String engineNo;
    private String colour;
    private int numberOfAccessories;
    private String warrentyCertificateNumber;
    private long warrentyDuration;
    private double totalAcquicitionCost;
    private double deprecitionRate;
    @Lob
    private String otherNotes;

    @ManyToOne
    private Institution manufacturer;

    @Transient
    private double transQtyPlusFreeQty;
    @Transient
    private double transAbsoluteQtyPlusFreeQty;
    @Transient
    private boolean transThisIsStockOut;
    @Transient
    private boolean transThisIsStockIn;

    public String getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(String serialNo) {
        this.serialNo = serialNo;
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

    public String getWarrentyDurationInYearsAndDays() {
        return TimeUtils.millisToYearsAndDates(warrentyDuration);
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

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void copy(PharmaceuticalBillItem ph) {
        if (ph == null) {
            return;
        }
        qty = ph.qty;
        qtyPacks = ph.qtyPacks;

        freeQty = ph.freeQty;
        freeQtyPacks = ph.freeQtyPacks;

        purchaseRate = ph.purchaseRate;
        purchaseRatePack = ph.purchaseRatePack;

        retailRate = ph.retailRate;
        retailRatePack = ph.retailRatePack;

        lastPurchaseRate = ph.lastPurchaseRate;
        lastPurchaseRatePack = ph.lastPurchaseRatePack;

        remainingFreeQty = ph.remainingFreeQty;
        remainingFreeQtyPack = ph.remainingFreeQtyPack;

        wholesaleRate = ph.wholesaleRate;
        wholesaleRatePack = ph.wholesaleRatePack;

        doe = ph.getDoe();
        stringValue = ph.getStringValue();
        itemBatch = ph.getItemBatch();

        stock = ph.getStock();
        staffStock = ph.getStaffStock();
        stringValue = ph.getStringValue();
        //  remainingQty=ph.getRemainingQty();

        make = ph.getMake();
        model = ph.getModel();
        code = ph.getCode();
        description = ph.getDescription();
        barcode = ph.getBarcode();
        serialNo = ph.getSerialNo();
        registrationNo = ph.getRegistrationNo();
        chassisNo = ph.getChassisNo();
        engineNo = ph.getEngineNo();
        colour = ph.getColour();
        warrentyCertificateNumber = ph.getWarrentyCertificateNumber();
        warrentyDuration = ph.getWarrentyDuration();
        deprecitionRate = ph.getDeprecitionRate();
        manufacturer = ph.getManufacturer();
        otherNotes = ph.getOtherNotes();

    }

    public void invertValue(PharmaceuticalBillItem ph) {
        if (ph == null) {
            return;
        }
        qty = 0 - ph.qty;
        qtyPacks = 0 - ph.qtyPacks;
        freeQty = 0 - ph.freeQty;
        freeQtyPacks = 0 - ph.freeQtyPacks;
    }

    public Stock getStock() {
        return stock;
    }

    public void setStock(Stock stock) {
        this.stock = stock;
    }

    public StockHistory getStockHistory() {
        return stockHistory;
    }

    public void setStockHistory(StockHistory stockHistory) {
        this.stockHistory = stockHistory;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BillItem getBillItem() {
        return billItem;
    }

    public void setBillItem(BillItem billItem) {
        this.billItem = billItem;
    }

    public Date getDoe() {
        return doe;
    }

    public void setDoe(Date doe) {
        this.doe = doe;
    }

    public ItemBatch getItemBatch() {
        return itemBatch;
    }

    public void setItemBatch(ItemBatch itemBatch) {
        this.itemBatch = itemBatch;
        if (itemBatch != null) {
            retailRate = itemBatch.getRetailsaleRate();
            purchaseRate = itemBatch.getPurcahseRate();

        }
    }

    public double getQty() {
//        if (getBillItem() != null && getBillItem().getItem() instanceof Ampp || getBillItem() != null && getBillItem().getItem() instanceof Vmpp) {
//            return qty / getBillItem().getItem().getDblValue();
//        } else {
//            return qty;
//        }
        return qty;
    }

    
    public void setQty(double qty) {
//        if (getBillItem() != null && getBillItem().getItem() instanceof Ampp || getBillItem() != null && getBillItem().getItem() instanceof Vmpp) {
//            this.qty = qty * getBillItem().getItem().getDblValue();
//        } else {
//            this.qty = qty;
//        }
        this.qty = qty;
    }

    @Deprecated //use qty
    public double getQtyInUnit() {
        return qty;
    }

    @Deprecated //use qty
    public void setQtyInUnit(double qty) {
        this.qty = qty;
    }

    @Deprecated //use free qty
    public double getFreeQtyInUnit() {
        return freeQty;
    }

    @Deprecated //use free qty
    public void setFreeQtyInUnit(double freeQty) {
        this.freeQty = freeQty;
    }

    public double getFreeQty() {
//        if (getBillItem() != null && getBillItem().getItem() instanceof Ampp
//                || getBillItem() != null && getBillItem().getItem() instanceof Vmpp) {
//            return freeQty / getBillItem().getItem().getDblValue();
//        } else {
//            return freeQty;
//        }
        return freeQty;
    }

    public void setFreeQty(double freeQty) {
//        if (getBillItem() != null && getBillItem().getItem() instanceof Ampp
//                || getBillItem() != null && getBillItem().getItem() instanceof Vmpp) {
//            this.freeQty = freeQty * getBillItem().getItem().getDblValue();
//        } else {
//            this.freeQty = freeQty;
//        }
        this.freeQty = freeQty;
    }

    public double getPurchaseRate() {
//        if (getBillItem() != null && getBillItem().getItem() instanceof Ampp
//                || getBillItem() != null && getBillItem().getItem() instanceof Vmpp) {
//            return purchaseRate * getBillItem().getItem().getDblValue();
//        } else {
//            return purchaseRate;
//        }
        return purchaseRate;

    }

    @Deprecated //use purchaseRate
    public double getPurchaseRateInUnit() {
        return purchaseRate;
    }

    @Deprecated //use purchaseRate
    public void setPurchaseRateInUnit(double purchaseRate) {
        this.purchaseRate = purchaseRate;
    }

    public void setPurchaseRate(double purchaseRate) {
//        if (getBillItem() != null && getBillItem().getItem() instanceof Ampp
//                || getBillItem() != null && getBillItem().getItem() instanceof Vmpp) {
//            this.purchaseRate = purchaseRate / getBillItem().getItem().getDblValue();
//        } else {
//            this.purchaseRate = purchaseRate;
//        }
        this.purchaseRate = purchaseRate;
    }

    @Deprecated //use retailRate
    public double getRetailRateInUnit() {
        return retailRate;
    }

    @Deprecated //use RetailRate
    public void setRetailRateInUnit(double retailRate) {
        this.retailRate = retailRate;
    }

    public double getRetailRate() {
//        if (getBillItem() != null && getBillItem().getItem() instanceof Ampp
//                || getBillItem() != null && getBillItem().getItem() instanceof Vmpp) {
//            return retailRate * getBillItem().getItem().getDblValue();
//        } else {
//            return retailRate;
//        }
        return retailRate;
    }

    public void setRetailRate(double retailRate) {
//        if (getBillItem() != null && getBillItem().getItem() instanceof Ampp
//                || getBillItem() != null && getBillItem().getItem() instanceof Vmpp) {
//            this.retailRate = retailRate / getBillItem().getItem().getDblValue();
//        } else {
//            this.retailRate = retailRate;
//        }
        this.retailRate = retailRate;
    }

    public double getWholesaleRate() {
        return wholesaleRate;
    }

    public void setWholesaleRate(double wholesaleRate) {
        this.wholesaleRate = wholesaleRate;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {

        if (!(object instanceof PharmaceuticalBillItem)) {
            return false;
        }
        PharmaceuticalBillItem other = (PharmaceuticalBillItem) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.pharmacy.PharmaceuticalBillItem[ id=" + id + " ]";
    }

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public double getLastPurchaseRateInUnit() {

        return lastPurchaseRate;

    }

    public void setLastPurchaseRateInUnit(double lastPurchaseRate) {

        this.lastPurchaseRate = lastPurchaseRate;

    }

    public double getLastPurchaseRate() {
//        if (getBillItem() != null && getBillItem().getItem() instanceof Ampp
//                || getBillItem() != null && getBillItem().getItem() instanceof Vmpp) {
//            return lastPurchaseRate * getBillItem().getItem().getDblValue();
//        } else {
//            return lastPurchaseRate;
//        }
        return lastPurchaseRate;
    }

    public void setLastPurchaseRate(double lastPurchaseRate) {
//        if (getBillItem() != null && getBillItem().getItem() instanceof Ampp
//                || getBillItem() != null && getBillItem().getItem() instanceof Vmpp) {
//            this.lastPurchaseRate = lastPurchaseRate / getBillItem().getItem().getDblValue();
//        } else {
//            this.lastPurchaseRate = lastPurchaseRate;
//        }
        this.lastPurchaseRate = lastPurchaseRate;
    }

    public Stock getStaffStock() {
        return staffStock;
    }

    public void setStaffStock(Stock staffStock) {
        this.staffStock = staffStock;
    }

    public Category getMake() {
        return make;
    }

    public void setMake(Category make) {
        this.make = make;
    }

    public double getRemainingFreeQty() {
        return remainingFreeQty;
    }

    public void setRemainingFreeQty(double remainingFreeQty) {
        this.remainingFreeQty = remainingFreeQty;
    }

    public double getTransQtyPlusFreeQty() {
        transQtyPlusFreeQty = getQtyInUnit() + getFreeQtyInUnit();
        return transQtyPlusFreeQty;
    }

    public double getTransAbsoluteQtyPlusFreeQty() {
        transAbsoluteQtyPlusFreeQty = Math.abs(getTransQtyPlusFreeQty());
        return transAbsoluteQtyPlusFreeQty;
    }

    public boolean isTransThisIsStockOut() {
        if (getTransQtyPlusFreeQty() < 0) {
            transThisIsStockOut = true;
        }
        return transThisIsStockOut;
    }

    public boolean isTransThisIsStockIn() {
        if (getTransQtyPlusFreeQty() > 0) {
            transThisIsStockIn = true;
        }
        return transThisIsStockIn;
    }

    public double getQtyPacks() {
        return qtyPacks;
    }

    public void setQtyPacks(double qtyPacks) {
        this.qtyPacks = qtyPacks;
    }

    public double getFreeQtyPacks() {
        return freeQtyPacks;
    }

    public void setFreeQtyPacks(double freeQtyPacks) {
        this.freeQtyPacks = freeQtyPacks;
    }

    public double getPurchaseRatePack() {
        return purchaseRatePack;
    }

    public void setPurchaseRatePack(double purchaseRatePack) {
        this.purchaseRatePack = purchaseRatePack;
    }

    public double getRetailRatePack() {
        return retailRatePack;
    }

    public void setRetailRatePack(double retailRatePack) {
        this.retailRatePack = retailRatePack;
    }

    public double getRemainingFreeQtyPack() {
        return remainingFreeQtyPack;
    }

    public void setRemainingFreeQtyPack(double remainingFreeQtyPack) {
        this.remainingFreeQtyPack = remainingFreeQtyPack;
    }

    public double getWholesaleRatePack() {
        return wholesaleRatePack;
    }

    public void setWholesaleRatePack(double wholesaleRatePack) {
        this.wholesaleRatePack = wholesaleRatePack;
    }

    public double getLastPurchaseRatePack() {
        return lastPurchaseRatePack;
    }

    public void setLastPurchaseRatePack(double lastPurchaseRatePack) {
        this.lastPurchaseRatePack = lastPurchaseRatePack;
    }

    public double getPurchaseValue() {
        return purchaseValue;
    }

    public void setPurchaseValue(double purchaseValue) {
        this.purchaseValue = purchaseValue;
    }

    public double getPurchaseRatePackValue() {
        return purchaseRatePackValue;
    }

    public void setPurchaseRatePackValue(double purchaseRatePackValue) {
        this.purchaseRatePackValue = purchaseRatePackValue;
    }

    public double getRetailValue() {
        return retailValue;
    }

    public void setRetailValue(double retailValue) {
        this.retailValue = retailValue;
    }

    public double getRetailPackValue() {
        return retailPackValue;
    }

    public void setRetailPackValue(double retailPackValue) {
        this.retailPackValue = retailPackValue;
    }
    
    
    

}
