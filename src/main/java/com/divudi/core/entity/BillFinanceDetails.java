package com.divudi.core.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 * @author Dr Buddhika Ariyaratne
 *
 */
@Entity
public class BillFinanceDetails implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    // Inverse side of the relationship - Bill entity owns the FK BILLFINANCEDETAILS_ID
    @OneToOne(mappedBy = "billFinanceDetails", cascade = CascadeType.ALL)
    private Bill bill;

    // ------------------ DISCOUNTS ------------------
    // Discount applied directly to the Bill (not tied to specific lines)
    @Column(precision = 18, scale = 4, nullable = true)
    private BigDecimal billDiscount;

    // Total of all line-level discounts (sum of discounts on individual BillItems)
    @Column(precision = 18, scale = 4, nullable = true)
    private BigDecimal lineDiscount;

    // Total discount (bill-level + all line-level)
    @Column(precision = 18, scale = 4, nullable = true)
    private BigDecimal totalDiscount;

    // ------------------ EXPENSES ------------------
    // Expense applied to the Bill itself (e.g., delivery fee, service charge)
    @Column(precision = 18, scale = 4, nullable = true)
    private BigDecimal billExpense;

    // Total of all expenses from individual BillItems
    @Column(precision = 18, scale = 4, nullable = true)
    private BigDecimal lineExpense;

    // Total expense (bill-level + all line-level)
    @Column(precision = 18, scale = 4, nullable = true)
    private BigDecimal totalExpense;

    // Expenses considered for costing calculation
    @Column(precision = 18, scale = 4, nullable = true)
    private BigDecimal billExpensesConsideredForCosting;

    // Expenses not considered for costing calculation
    @Column(precision = 18, scale = 4, nullable = true)
    private BigDecimal billExpensesNotConsideredForCosting;

    // ------------------ COST ------------------
    // Cost incurred for the Bill as a whole (not specific to lines)
    @Column(precision = 18, scale = 4, nullable = true)
    private BigDecimal billCostValue;

    // Sum of cost values from each BillItem
    @Column(precision = 18, scale = 4, nullable = true)
    private BigDecimal lineCostValue;

    // Total cost value for all BillItems (excluding discounts/taxes)
    @Column(precision = 18, scale = 4, nullable = true)
    private BigDecimal totalCostValue;

    @Column(precision = 18, scale = 4, nullable = true)
    private BigDecimal totalCostValueFree;

    @Column(precision = 18, scale = 4, nullable = true)
    private BigDecimal totalCostValueNonFree;

    // ------------------ TAXES ------------------
    // Tax applied to the whole Bill (e.g., VAT)
    @Column(precision = 18, scale = 4, nullable = true)
    private BigDecimal billTaxValue;

    // Total of tax amounts from all BillItems
    @Column(precision = 18, scale = 4, nullable = true)
    private BigDecimal itemTaxValue;

    // Total tax (bill-level + all line-level)
    @Column(precision = 18, scale = 4, nullable = true)
    private BigDecimal totalTaxValue;

    // ------------------ VALUES ------------------
    // Total purchase value for all BillItems (excluding discounts/taxes)
    @Column(precision = 18, scale = 4, nullable = true)
    private BigDecimal totalPurchaseValue;

    @Column(precision = 18, scale = 4, nullable = true)
    private BigDecimal totalPurchaseValueFree;

    @Column(precision = 18, scale = 4, nullable = true)
    private BigDecimal totalPurchaseValueNonFree;

    // Estimated value of items given free of charge
    @Column(precision = 18, scale = 4, nullable = true)
    @Deprecated // User 
    private BigDecimal totalOfFreeItemValues;

    @Column(precision = 18, scale = 4, nullable = true)
    private BigDecimal totalOfFreeItemValuesFree;

    @Column(precision = 18, scale = 4, nullable = true)
    private BigDecimal totalOfFreeItemValuesNonFree;

    // Expected total if all items sold at retail rate
    @Column(precision = 18, scale = 4, nullable = true)
    private BigDecimal totalRetailSaleValue;

    @Column(precision = 18, scale = 4, nullable = true)
    private BigDecimal totalRetailSaleValueFree;

    @Column(precision = 18, scale = 4, nullable = true)
    private BigDecimal totalRetailSaleValueNonFree;

    // Expected total if all items sold at wholesale rate
    @Column(precision = 18, scale = 4, nullable = true)
    private BigDecimal totalWholesaleValue;

    @Column(precision = 18, scale = 4, nullable = true)
    private BigDecimal totalWholesaleValueFree;

    @Column(precision = 18, scale = 4, nullable = true)
    private BigDecimal totalWholesaleValueNonFree;

    // Totals for purchase rate adjustments
    @Column(precision = 18, scale = 4, nullable = true)
    private BigDecimal totalBeforeAdjustmentValue;

    @Column(precision = 18, scale = 4, nullable = true)
    private BigDecimal totalAfterAdjustmentValue;

    // ------------------ QUANTITIES ------------------
    // Total quantity of all BillItems (excluding free)
    @Column(precision = 18, scale = 4, nullable = true)
    private BigDecimal totalQuantity;

    // Total of free quantities across BillItems
    @Column(precision = 18, scale = 4, nullable = true)
    private BigDecimal totalFreeQuantity;

    // Quantity in atomic units (e.g., tablets instead of boxes)
    @Column(precision = 18, scale = 4, nullable = true)
    private BigDecimal totalQuantityInAtomicUnitOfMeasurement;

    // Free quantity in atomic units
    @Column(precision = 18, scale = 4, nullable = true)
    private BigDecimal totalFreeQuantityInAtomicUnitOfMeasurement;

    @Column(precision = 18, scale = 4, nullable = true)
    private BigDecimal lineGrossTotal;
    @Column(precision = 18, scale = 4, nullable = true)
    private BigDecimal billGrossTotal;
    @Column(precision = 18, scale = 4, nullable = true)
    private BigDecimal grossTotal;

    // Value after deductions
    @Column(precision = 18, scale = 4, nullable = true)
    private BigDecimal lineNetTotal;
    @Column(precision = 18, scale = 4, nullable = true)
    private BigDecimal billNetTotal;
    @Column(precision = 18, scale = 4, nullable = true)
    private BigDecimal netTotal;

    // Actual physical net value (entered by user during GRN return)
    @Column(precision = 18, scale = 4, nullable = true)
    private BigDecimal actualNetValue;

    // Adjustment between calculated net total and actual physical net value
    // netValueAdjustment = actualNetValue - netTotal
    @Column(precision = 18, scale = 4, nullable = true)
    private BigDecimal netValueAdjustment;

//    // Payment method totals
//    private BigDecimal totalPaidAsCash = BigDecimal.ZERO;
//    private BigDecimal totalPaidAsCard = BigDecimal.ZERO;
//    private BigDecimal totalPaidAsMultiplePaymentMethods = BigDecimal.ZERO;
//    private BigDecimal totalPaidAsStaff = BigDecimal.ZERO;
//    private BigDecimal totalPaidAsCredit = BigDecimal.ZERO;
//    private BigDecimal totalPaidAsStaffWelfare = BigDecimal.ZERO;
//    private BigDecimal totalPaidAsVoucher = BigDecimal.ZERO;
//    private BigDecimal totalPaidAsIOU = BigDecimal.ZERO;
//    private BigDecimal totalPaidAsAgent = BigDecimal.ZERO;
//    private BigDecimal totalPaidAsCheque = BigDecimal.ZERO;
//    private BigDecimal totalPaidAsSlip = BigDecimal.ZERO;
//    private BigDecimal totalPaidAsEwallet = BigDecimal.ZERO;
//    private BigDecimal totalPaidAsPatientDeposit = BigDecimal.ZERO;
//    private BigDecimal totalPaidAsPatientPoints = BigDecimal.ZERO;
//    private BigDecimal totalPaidAsOnlineSettlement = BigDecimal.ZERO;
//    private BigDecimal totalPaidAsOnCall = BigDecimal.ZERO;
//    private BigDecimal totalPaidAsYouOweMe = BigDecimal.ZERO;
//    private BigDecimal totalPaidAsNone = BigDecimal.ZERO;

    public BillFinanceDetails() {
        createdAt = new Date();
    }

    public BillFinanceDetails(Bill bill) {
        this.bill = bill;
        createdAt = new Date();
    }

    @Override
    public BillFinanceDetails clone() {
        BillFinanceDetails clone = new BillFinanceDetails();

        // ------------------ DISCOUNTS ------------------
        clone.setBillDiscount(this.billDiscount);
        clone.setLineDiscount(this.lineDiscount);
        clone.setTotalDiscount(this.totalDiscount);

        // ------------------ EXPENSES ------------------
        clone.setBillExpense(this.billExpense);
        clone.setLineExpense(this.lineExpense);
        clone.setTotalExpense(this.totalExpense);
        clone.setBillExpensesConsideredForCosting(this.billExpensesConsideredForCosting);
        clone.setBillExpensesNotConsideredForCosting(this.billExpensesNotConsideredForCosting);

        // ------------------ COST ------------------
        clone.setBillCostValue(this.billCostValue);
        clone.setLineCostValue(this.lineCostValue);
        clone.setTotalCostValue(this.totalCostValue);
        clone.setTotalCostValueFree(this.totalCostValueFree);
        clone.setTotalCostValueNonFree(this.totalCostValueNonFree);

        // ------------------ TAXES ------------------
        clone.setBillTaxValue(this.billTaxValue);
        clone.setItemTaxValue(this.itemTaxValue);
        clone.setTotalTaxValue(this.totalTaxValue);

        // ------------------ VALUES ------------------
        clone.setTotalPurchaseValue(this.totalPurchaseValue);
        clone.setTotalPurchaseValueFree(this.totalPurchaseValueFree);
        clone.setTotalPurchaseValueNonFree(this.totalPurchaseValueNonFree);
        clone.setTotalOfFreeItemValues(this.totalOfFreeItemValues);
        clone.setTotalOfFreeItemValuesFree(this.totalOfFreeItemValuesFree);
        clone.setTotalOfFreeItemValuesNonFree(this.totalOfFreeItemValuesNonFree);
        clone.setTotalRetailSaleValue(this.totalRetailSaleValue);
        clone.setTotalRetailSaleValueFree(this.totalRetailSaleValueFree);
        clone.setTotalRetailSaleValueNonFree(this.totalRetailSaleValueNonFree);
        clone.setTotalWholesaleValue(this.totalWholesaleValue);
        clone.setTotalWholesaleValueFree(this.totalWholesaleValueFree);
        clone.setTotalWholesaleValueNonFree(this.totalWholesaleValueNonFree);
        clone.setTotalBeforeAdjustmentValue(this.totalBeforeAdjustmentValue);
        clone.setTotalAfterAdjustmentValue(this.totalAfterAdjustmentValue);

        // ------------------ QUANTITIES ------------------
        clone.setTotalQuantity(this.totalQuantity);
        clone.setTotalFreeQuantity(this.totalFreeQuantity);
        clone.setTotalQuantityInAtomicUnitOfMeasurement(this.totalQuantityInAtomicUnitOfMeasurement);
        clone.setTotalFreeQuantityInAtomicUnitOfMeasurement(this.totalFreeQuantityInAtomicUnitOfMeasurement);

        // ------------------ GROSS & NET ------------------
        clone.setLineGrossTotal(this.lineGrossTotal);
        clone.setBillGrossTotal(this.billGrossTotal);
        clone.setGrossTotal(this.grossTotal);
        clone.setLineNetTotal(this.lineNetTotal);
        clone.setBillNetTotal(this.billNetTotal);
        clone.setNetTotal(this.netTotal);
        clone.setActualNetValue(this.actualNetValue);
        clone.setNetValueAdjustment(this.netValueAdjustment);

//        // ------------------ PAYMENT METHODS ------------------
//        clone.setTotalPaidAsCash(this.totalPaidAsCash);
//        clone.setTotalPaidAsCard(this.totalPaidAsCard);
//        clone.setTotalPaidAsMultiplePaymentMethods(this.totalPaidAsMultiplePaymentMethods);
//        clone.setTotalPaidAsStaff(this.totalPaidAsStaff);
//        clone.setTotalPaidAsCredit(this.totalPaidAsCredit);
//        clone.setTotalPaidAsStaffWelfare(this.totalPaidAsStaffWelfare);
//        clone.setTotalPaidAsVoucher(this.totalPaidAsVoucher);
//        clone.setTotalPaidAsIOU(this.totalPaidAsIOU);
//        clone.setTotalPaidAsAgent(this.totalPaidAsAgent);
//        clone.setTotalPaidAsCheque(this.totalPaidAsCheque);
//        clone.setTotalPaidAsSlip(this.totalPaidAsSlip);
//        clone.setTotalPaidAsEwallet(this.totalPaidAsEwallet);
//        clone.setTotalPaidAsPatientDeposit(this.totalPaidAsPatientDeposit);
//        clone.setTotalPaidAsPatientPoints(this.totalPaidAsPatientPoints);
//        clone.setTotalPaidAsOnlineSettlement(this.totalPaidAsOnlineSettlement);
//        clone.setTotalPaidAsOnCall(this.totalPaidAsOnCall);
//        clone.setTotalPaidAsYouOweMe(this.totalPaidAsYouOweMe);
//        clone.setTotalPaidAsNone(this.totalPaidAsNone);
//
//        // Note: skip ID, createdAt, etc. – those should be managed by persistence layer
        return clone;
    }

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @ManyToOne
    private WebUser createdBy;

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
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof BillFinanceDetails)) {
            return false;
        }
        BillFinanceDetails other = (BillFinanceDetails) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.core.entity.BillFinanceDetails[ id=" + id + " ]";
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public BigDecimal getBillDiscount() {
        return billDiscount;
    }

    public void setBillDiscount(BigDecimal billDiscount) {
        this.billDiscount = billDiscount;
    }

    public BigDecimal getLineDiscount() {
        return lineDiscount;
    }

    public void setLineDiscount(BigDecimal lineDiscount) {
        this.lineDiscount = lineDiscount;
    }

    public BigDecimal getTotalDiscount() {
        return totalDiscount;
    }

    public void setTotalDiscount(BigDecimal totalDiscount) {
        this.totalDiscount = totalDiscount;
    }

    public BigDecimal getTotalExpense() {
        return totalExpense;
    }

    public void setTotalExpense(BigDecimal totalExpense) {
        this.totalExpense = totalExpense;
    }

    @Deprecated
    public BigDecimal getTotalOfFreeItemValues() {
        return totalOfFreeItemValues;
    }

    @Deprecated
    public void setTotalOfFreeItemValues(BigDecimal totalOfFreeItemValues) {
        this.totalOfFreeItemValues = totalOfFreeItemValues;
    }

    public BigDecimal getTotalOfFreeItemValuesFree() {
        return totalOfFreeItemValuesFree;
    }

    public void setTotalOfFreeItemValuesFree(BigDecimal totalOfFreeItemValuesFree) {
        this.totalOfFreeItemValuesFree = totalOfFreeItemValuesFree;
    }

    public BigDecimal getTotalOfFreeItemValuesNonFree() {
        return totalOfFreeItemValuesNonFree;
    }

    public void setTotalOfFreeItemValuesNonFree(BigDecimal totalOfFreeItemValuesNonFree) {
        this.totalOfFreeItemValuesNonFree = totalOfFreeItemValuesNonFree;
    }

    public BigDecimal getTotalCostValue() {
        return totalCostValue;
    }

    public void setTotalCostValue(BigDecimal totalCostValue) {
        this.totalCostValue = totalCostValue;
    }

    public BigDecimal getTotalCostValueFree() {
        return totalCostValueFree;
    }

    public void setTotalCostValueFree(BigDecimal totalCostValueFree) {
        this.totalCostValueFree = totalCostValueFree;
    }

    public BigDecimal getTotalCostValueNonFree() {
        return totalCostValueNonFree;
    }

    public void setTotalCostValueNonFree(BigDecimal totalCostValueNonFree) {
        this.totalCostValueNonFree = totalCostValueNonFree;
    }

    public BigDecimal getTotalPurchaseValue() {
        return totalPurchaseValue;
    }

    public void setTotalPurchaseValue(BigDecimal totalPurchaseValue) {
        this.totalPurchaseValue = totalPurchaseValue;
    }

    public BigDecimal getTotalPurchaseValueFree() {
        return totalPurchaseValueFree;
    }

    public void setTotalPurchaseValueFree(BigDecimal totalPurchaseValueFree) {
        this.totalPurchaseValueFree = totalPurchaseValueFree;
    }

    public BigDecimal getTotalPurchaseValueNonFree() {
        return totalPurchaseValueNonFree;
    }

    public void setTotalPurchaseValueNonFree(BigDecimal totalPurchaseValueNonFree) {
        this.totalPurchaseValueNonFree = totalPurchaseValueNonFree;
    }

    public BigDecimal getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(BigDecimal totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public BigDecimal getTotalFreeQuantity() {
        return totalFreeQuantity;
    }

    public void setTotalFreeQuantity(BigDecimal totalFreeQuantity) {
        this.totalFreeQuantity = totalFreeQuantity;
    }

    public BigDecimal getTotalTaxValue() {
        return totalTaxValue;
    }

    public void setTotalTaxValue(BigDecimal totalTaxValue) {
        this.totalTaxValue = totalTaxValue;
    }

    public BigDecimal getTotalRetailSaleValue() {
        return totalRetailSaleValue;
    }

    public void setTotalRetailSaleValue(BigDecimal totalRetailSaleValue) {
        this.totalRetailSaleValue = totalRetailSaleValue;
    }

    public BigDecimal getTotalRetailSaleValueFree() {
        return totalRetailSaleValueFree;
    }

    public void setTotalRetailSaleValueFree(BigDecimal totalRetailSaleValueFree) {
        this.totalRetailSaleValueFree = totalRetailSaleValueFree;
    }

    public BigDecimal getTotalRetailSaleValueNonFree() {
        return totalRetailSaleValueNonFree;
    }

    public void setTotalRetailSaleValueNonFree(BigDecimal totalRetailSaleValueNonFree) {
        this.totalRetailSaleValueNonFree = totalRetailSaleValueNonFree;
    }

    public BigDecimal getTotalWholesaleValue() {
        return totalWholesaleValue;
    }

    public void setTotalWholesaleValue(BigDecimal totalWholesaleValue) {
        this.totalWholesaleValue = totalWholesaleValue;
    }

    public BigDecimal getTotalWholesaleValueFree() {
        return totalWholesaleValueFree;
    }

    public void setTotalWholesaleValueFree(BigDecimal totalWholesaleValueFree) {
        this.totalWholesaleValueFree = totalWholesaleValueFree;
    }

    public BigDecimal getTotalWholesaleValueNonFree() {
        return totalWholesaleValueNonFree;
    }

    public void setTotalWholesaleValueNonFree(BigDecimal totalWholesaleValueNonFree) {
        this.totalWholesaleValueNonFree = totalWholesaleValueNonFree;
    }

    public BigDecimal getTotalBeforeAdjustmentValue() {
        return totalBeforeAdjustmentValue;
    }

    public void setTotalBeforeAdjustmentValue(BigDecimal totalBeforeAdjustmentValue) {
        this.totalBeforeAdjustmentValue = totalBeforeAdjustmentValue;
    }

    public BigDecimal getTotalAfterAdjustmentValue() {
        return totalAfterAdjustmentValue;
    }

    public void setTotalAfterAdjustmentValue(BigDecimal totalAfterAdjustmentValue) {
        this.totalAfterAdjustmentValue = totalAfterAdjustmentValue;
    }

    public BigDecimal getTotalQuantityInAtomicUnitOfMeasurement() {
        return totalQuantityInAtomicUnitOfMeasurement;
    }

    public void setTotalQuantityInAtomicUnitOfMeasurement(BigDecimal totalQuantityInAtomicUnitOfMeasurement) {
        this.totalQuantityInAtomicUnitOfMeasurement = totalQuantityInAtomicUnitOfMeasurement;
    }

    public BigDecimal getTotalFreeQuantityInAtomicUnitOfMeasurement() {
        return totalFreeQuantityInAtomicUnitOfMeasurement;
    }

    public void setTotalFreeQuantityInAtomicUnitOfMeasurement(BigDecimal totalFreeQuantityInAtomicUnitOfMeasurement) {
        this.totalFreeQuantityInAtomicUnitOfMeasurement = totalFreeQuantityInAtomicUnitOfMeasurement;
    }

    public BigDecimal getBillExpense() {
        return billExpense;
    }

    public void setBillExpense(BigDecimal billExpense) {
        this.billExpense = billExpense;
    }

    public BigDecimal getLineExpense() {
        return lineExpense;
    }

    public void setLineExpense(BigDecimal lineExpense) {
        this.lineExpense = lineExpense;
    }

    public BigDecimal getBillCostValue() {
        return billCostValue;
    }

    public void setBillCostValue(BigDecimal billCostValue) {
        this.billCostValue = billCostValue;
    }

    public BigDecimal getLineCostValue() {
        return lineCostValue;
    }

    public void setLineCostValue(BigDecimal lineCostValue) {
        this.lineCostValue = lineCostValue;
    }

    public BigDecimal getBillTaxValue() {
        return billTaxValue;
    }

    public void setBillTaxValue(BigDecimal billTaxValue) {
        this.billTaxValue = billTaxValue;
    }

    public BigDecimal getItemTaxValue() {
        return itemTaxValue;
    }

    public void setItemTaxValue(BigDecimal itemTaxValue) {
        this.itemTaxValue = itemTaxValue;
    }

    public BigDecimal getLineGrossTotal() {
        return lineGrossTotal;
    }

    public void setLineGrossTotal(BigDecimal lineGrossTotal) {
        this.lineGrossTotal = lineGrossTotal;
    }

    public BigDecimal getBillGrossTotal() {
        return billGrossTotal;
    }

    public void setBillGrossTotal(BigDecimal billGrossTotal) {
        this.billGrossTotal = billGrossTotal;
    }

    public BigDecimal getGrossTotal() {
        return grossTotal;
    }

    public void setGrossTotal(BigDecimal grossTotal) {
        this.grossTotal = grossTotal;
    }

    public BigDecimal getLineNetTotal() {
        return lineNetTotal;
    }

    public void setLineNetTotal(BigDecimal lineNetTotal) {
        this.lineNetTotal = lineNetTotal;
    }

    public BigDecimal getBillNetTotal() {
        return billNetTotal;
    }

    public void setBillNetTotal(BigDecimal billNetTotal) {
        this.billNetTotal = billNetTotal;
    }

    public BigDecimal getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(BigDecimal netTotal) {
        this.netTotal = netTotal;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public WebUser getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(WebUser createdBy) {
        this.createdBy = createdBy;
    }

    public BigDecimal getBillExpensesConsideredForCosting() {
        return billExpensesConsideredForCosting;
    }

    public void setBillExpensesConsideredForCosting(BigDecimal billExpensesConsideredForCosting) {
        this.billExpensesConsideredForCosting = billExpensesConsideredForCosting;
    }

    public BigDecimal getBillExpensesNotConsideredForCosting() {
        return billExpensesNotConsideredForCosting;
    }

    public void setBillExpensesNotConsideredForCosting(BigDecimal billExpensesNotConsideredForCosting) {
        this.billExpensesNotConsideredForCosting = billExpensesNotConsideredForCosting;
    }

    public BigDecimal getActualNetValue() {
        return actualNetValue;
    }

    public void setActualNetValue(BigDecimal actualNetValue) {
        this.actualNetValue = actualNetValue;
    }

    public BigDecimal getNetValueAdjustment() {
        return netValueAdjustment;
    }

    public void setNetValueAdjustment(BigDecimal netValueAdjustment) {
        this.netValueAdjustment = netValueAdjustment;
    }

}
