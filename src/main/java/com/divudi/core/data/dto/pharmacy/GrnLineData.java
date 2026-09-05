package com.divudi.core.data.dto.pharmacy;

import java.util.Date;

/**
 * Carries one GRN (Goods Received Note, "Manage Costing = true" flow) line's
 * already-computed values from the controller to
 * {@code GrnCostingNativeSqlService}. Plain POJO -- no {@code SELECT NEW}
 * JPQL constructor is needed since this DTO never comes back out of a query;
 * it is built in-memory by the controller and passed in as a write payload.
 * Mirrors {@code PurchaseOrderRequestLineData}'s role for the PO Request
 * native-SQL flow (issue #22727 / PR #22871).
 * Related issue: #22874
 */
public class GrnLineData {

    private Long billItemId;
    private Long pharmaceuticalBillItemId;
    private Long billItemFinanceDetailsId;
    private long itemId;

    // Whether the item is an Ampp (packs-based) vs Amp (units-based) --
    // drives unit/pack rate branching, mirroring PurchaseOrderRequestLineData.isAmpp().
    private boolean ampp;

    // Links this GRN line's own BillItem back to the PO's BillItem
    // (persisted as the legacy-typo'd referanceBillItem_ID column).
    private Long referenceBillItemId;

    private int serialNo;
    private long createrId;

    private double quantity;
    private double freeQuantity;
    private double quantityByUnits;
    private double freeQuantityByUnits;

    private double lineGrossRate;
    private double lineDiscountRate;
    private double lineNetRate;
    private double grossRate;

    private double lineGrossTotal;
    private double lineNetTotal;
    private double grossTotal;

    private double retailSaleRate;
    private double retailSaleRatePerUnit;
    private double wholesaleRate;
    private double wholesaleRatePerUnit;

    private double unitsPerPack;

    private double valueAtPurchaseRate;
    private double valueAtRetailRate;

    private double lineCost;
    private double lineCostRate;
    private double valueAtCostRate;
    private double totalCostRate;

    // Date of Expiry / Batch No, entered at Save time -- carried through to
    // pharmaceuticalbillitem.doe/stringValue so they survive a reload before
    // Finalize/Approve. Missing from this DTO was the root cause of #22892
    // (Save-stage native SQL silently dropped these two fields, unlike the
    // Approve-stage GrnApproveLineData which already carried its own
    // expiryDate/batchNo pair). Named batchNumber, not batchNo, to avoid
    // field-hiding/method-overriding GrnApproveLineData's distinct batchNo
    // field of the same name.
    private Date doe;
    private String batchNumber;

    // Free-text "Comments" (regular item lines) / "Description" (Bill Expense
    // lines) field -- backs BillItem.descreption / billitem.DESCREPTION.
    // Missing from this DTO was the root cause of #22997, same bug class as
    // #22892's doe/batchNumber omission in the same native-SQL save path.
    private String description;

    // Used only by expense lines (GrnCostingNativeSqlService.saveExpenseLine) --
    // mirrors BillItem.consideredForCosting, whether this expense feeds the
    // bill-level cost-rate distribution or is display-only.
    private boolean consideredForCosting = true;

    public Long getBillItemId() {
        return billItemId;
    }

    public void setBillItemId(Long billItemId) {
        this.billItemId = billItemId;
    }

    public Long getPharmaceuticalBillItemId() {
        return pharmaceuticalBillItemId;
    }

    public void setPharmaceuticalBillItemId(Long pharmaceuticalBillItemId) {
        this.pharmaceuticalBillItemId = pharmaceuticalBillItemId;
    }

    public Long getBillItemFinanceDetailsId() {
        return billItemFinanceDetailsId;
    }

    public void setBillItemFinanceDetailsId(Long billItemFinanceDetailsId) {
        this.billItemFinanceDetailsId = billItemFinanceDetailsId;
    }

    public long getItemId() {
        return itemId;
    }

    public void setItemId(long itemId) {
        this.itemId = itemId;
    }

    public boolean isAmpp() {
        return ampp;
    }

    public void setAmpp(boolean ampp) {
        this.ampp = ampp;
    }

    public Long getReferenceBillItemId() {
        return referenceBillItemId;
    }

    public void setReferenceBillItemId(Long referenceBillItemId) {
        this.referenceBillItemId = referenceBillItemId;
    }

    public int getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(int serialNo) {
        this.serialNo = serialNo;
    }

    public long getCreaterId() {
        return createrId;
    }

    public void setCreaterId(long createrId) {
        this.createrId = createrId;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public double getFreeQuantity() {
        return freeQuantity;
    }

    public void setFreeQuantity(double freeQuantity) {
        this.freeQuantity = freeQuantity;
    }

    public double getQuantityByUnits() {
        return quantityByUnits;
    }

    public void setQuantityByUnits(double quantityByUnits) {
        this.quantityByUnits = quantityByUnits;
    }

    public double getFreeQuantityByUnits() {
        return freeQuantityByUnits;
    }

    public void setFreeQuantityByUnits(double freeQuantityByUnits) {
        this.freeQuantityByUnits = freeQuantityByUnits;
    }

    public double getLineGrossRate() {
        return lineGrossRate;
    }

    public void setLineGrossRate(double lineGrossRate) {
        this.lineGrossRate = lineGrossRate;
    }

    public double getLineDiscountRate() {
        return lineDiscountRate;
    }

    public void setLineDiscountRate(double lineDiscountRate) {
        this.lineDiscountRate = lineDiscountRate;
    }

    public double getLineNetRate() {
        return lineNetRate;
    }

    public void setLineNetRate(double lineNetRate) {
        this.lineNetRate = lineNetRate;
    }

    public double getGrossRate() {
        return grossRate;
    }

    public void setGrossRate(double grossRate) {
        this.grossRate = grossRate;
    }

    public double getLineGrossTotal() {
        return lineGrossTotal;
    }

    public void setLineGrossTotal(double lineGrossTotal) {
        this.lineGrossTotal = lineGrossTotal;
    }

    public double getLineNetTotal() {
        return lineNetTotal;
    }

    public void setLineNetTotal(double lineNetTotal) {
        this.lineNetTotal = lineNetTotal;
    }

    public double getGrossTotal() {
        return grossTotal;
    }

    public void setGrossTotal(double grossTotal) {
        this.grossTotal = grossTotal;
    }

    public double getRetailSaleRate() {
        return retailSaleRate;
    }

    public void setRetailSaleRate(double retailSaleRate) {
        this.retailSaleRate = retailSaleRate;
    }

    public double getRetailSaleRatePerUnit() {
        return retailSaleRatePerUnit;
    }

    public void setRetailSaleRatePerUnit(double retailSaleRatePerUnit) {
        this.retailSaleRatePerUnit = retailSaleRatePerUnit;
    }

    public double getWholesaleRate() {
        return wholesaleRate;
    }

    public void setWholesaleRate(double wholesaleRate) {
        this.wholesaleRate = wholesaleRate;
    }

    public double getWholesaleRatePerUnit() {
        return wholesaleRatePerUnit;
    }

    public void setWholesaleRatePerUnit(double wholesaleRatePerUnit) {
        this.wholesaleRatePerUnit = wholesaleRatePerUnit;
    }

    public double getUnitsPerPack() {
        return unitsPerPack;
    }

    public void setUnitsPerPack(double unitsPerPack) {
        this.unitsPerPack = unitsPerPack;
    }

    public double getValueAtPurchaseRate() {
        return valueAtPurchaseRate;
    }

    public void setValueAtPurchaseRate(double valueAtPurchaseRate) {
        this.valueAtPurchaseRate = valueAtPurchaseRate;
    }

    public double getValueAtRetailRate() {
        return valueAtRetailRate;
    }

    public void setValueAtRetailRate(double valueAtRetailRate) {
        this.valueAtRetailRate = valueAtRetailRate;
    }

    public double getLineCost() {
        return lineCost;
    }

    public void setLineCost(double lineCost) {
        this.lineCost = lineCost;
    }

    public double getLineCostRate() {
        return lineCostRate;
    }

    public void setLineCostRate(double lineCostRate) {
        this.lineCostRate = lineCostRate;
    }

    public Date getDoe() {
        return doe;
    }

    public void setDoe(Date doe) {
        this.doe = doe;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getValueAtCostRate() {
        return valueAtCostRate;
    }

    public void setValueAtCostRate(double valueAtCostRate) {
        this.valueAtCostRate = valueAtCostRate;
    }

    public double getTotalCostRate() {
        return totalCostRate;
    }

    public void setTotalCostRate(double totalCostRate) {
        this.totalCostRate = totalCostRate;
    }

    public boolean isConsideredForCosting() {
        return consideredForCosting;
    }

    public void setConsideredForCosting(boolean consideredForCosting) {
        this.consideredForCosting = consideredForCosting;
    }
}
