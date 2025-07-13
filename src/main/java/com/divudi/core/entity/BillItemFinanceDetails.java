package com.divudi.core.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
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
 * @author Dr M H Buddhika Ariyaratne buddhika.ari@gmail.com
 *
 */
@Entity
public class BillItemFinanceDetails implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @OneToOne(mappedBy = "billItemFinanceDetails")
    private BillItem billItem;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @ManyToOne
    private WebUser createdBy;

    // Conversion: number of units per pack (e.g., tablets in a blister)
    @Column(precision = 18, scale = 4)
    private BigDecimal unitsPerPack = BigDecimal.ONE;

    // ------------------ RATES ------------------
    // Base price before any deductions
    @Column(precision = 18, scale = 4)
    private BigDecimal lineGrossRate = BigDecimal.ZERO;
    @Column(precision = 18, scale = 4)
    private BigDecimal billGrossRate = BigDecimal.ZERO;
    @Column(precision = 18, scale = 4)
    private BigDecimal grossRate = BigDecimal.ZERO;

    // Final rate after all discounts, expenses, and taxes
    @Column(precision = 18, scale = 4)
    private BigDecimal lineNetRate = BigDecimal.ZERO;
    @Column(precision = 18, scale = 4)
    private BigDecimal billNetRate = BigDecimal.ZERO;
    @Column(precision = 18, scale = 4)
    private BigDecimal netRate = BigDecimal.ZERO;

    // Discount percentages applied at line level, bill level, and total
    @Column(precision = 18, scale = 4)
    private BigDecimal lineDiscountRate = BigDecimal.ZERO;
    @Column(precision = 18, scale = 4)
    private BigDecimal billDiscountRate = BigDecimal.ZERO;
    @Column(precision = 18, scale = 4)
    private BigDecimal totalDiscountRate = BigDecimal.ZERO;

    // Expense percentages applied at line level, bill level, and total
    @Column(precision = 18, scale = 4)
    private BigDecimal lineExpenseRate = BigDecimal.ZERO;
    @Column(precision = 18, scale = 4)
    private BigDecimal billExpenseRate = BigDecimal.ZERO;
    @Column(precision = 18, scale = 4)
    private BigDecimal totalExpenseRate = BigDecimal.ZERO;

    // Tax percentages applied at bill, line, and total
    @Column(precision = 18, scale = 4)
    private BigDecimal billTaxRate = BigDecimal.ZERO;
    @Column(precision = 18, scale = 4)
    private BigDecimal lineTaxRate = BigDecimal.ZERO;
    @Column(precision = 18, scale = 4)
    private BigDecimal totalTaxRate = BigDecimal.ZERO;

    // Cost percentages applied at bill, line, and total
    @Column(precision = 18, scale = 4)
    private BigDecimal billCostRate = BigDecimal.ZERO;
    @Column(precision = 18, scale = 4)
    private BigDecimal lineCostRate = BigDecimal.ZERO;
    @Column(precision = 18, scale = 4)
    private BigDecimal totalCostRate = BigDecimal.ZERO;

    // ------------------ TOTALS ------------------
    @Column(precision = 18, scale = 4)
    private BigDecimal lineGrossTotal = BigDecimal.ZERO;
    @Column(precision = 18, scale = 4)
    private BigDecimal billGrossTotal = BigDecimal.ZERO;
    @Column(precision = 18, scale = 4)
    private BigDecimal grossTotal = BigDecimal.ZERO;

    @Column(precision = 18, scale = 4)
    private BigDecimal lineNetTotal = BigDecimal.ZERO;
    @Column(precision = 18, scale = 4)
    private BigDecimal billNetTotal = BigDecimal.ZERO;
    @Column(precision = 18, scale = 4)
    private BigDecimal netTotal = BigDecimal.ZERO;

    @Column(precision = 18, scale = 4)
    private BigDecimal lineDiscount = BigDecimal.ZERO;
    @Column(precision = 18, scale = 4)
    private BigDecimal billDiscount = BigDecimal.ZERO;
    @Column(precision = 18, scale = 4)
    private BigDecimal totalDiscount = BigDecimal.ZERO;

    // Retail and wholesale rates
    @Column(precision = 18, scale = 4)
    private BigDecimal retailSaleRate = BigDecimal.ZERO;
    @Column(precision = 18, scale = 4)
    private BigDecimal wholesaleRate = BigDecimal.ZERO;

    // Retail and wholesale rates per unit (based on unitsPerPack)
    @Column(precision = 18, scale = 4)
    private BigDecimal retailSaleRatePerUnit = BigDecimal.ZERO;
    @Column(precision = 18, scale = 4)
    private BigDecimal wholesaleRatePerUnit = BigDecimal.ZERO;

    @Column(precision = 18, scale = 4)
    private BigDecimal valueAtRetailRate = BigDecimal.ZERO;

    @Column(precision = 18, scale = 4)
    private BigDecimal valueAtPurchaseRate = BigDecimal.ZERO;

    // Absolute tax values
    @Column(precision = 18, scale = 4)
    private BigDecimal billTax = BigDecimal.ZERO;
    @Column(precision = 18, scale = 4)
    private BigDecimal lineTax = BigDecimal.ZERO;
    @Column(precision = 18, scale = 4)
    private BigDecimal totalTax = BigDecimal.ZERO;

    // Absolute expense values
    @Column(precision = 18, scale = 4)
    private BigDecimal billExpense = BigDecimal.ZERO;
    @Column(precision = 18, scale = 4)
    private BigDecimal lineExpense = BigDecimal.ZERO;
    @Column(precision = 18, scale = 4)
    private BigDecimal totalExpense = BigDecimal.ZERO;

    // Absolute cost values
    @Column(precision = 18, scale = 4)
    private BigDecimal billCost = BigDecimal.ZERO;
    @Column(precision = 18, scale = 4)
    private BigDecimal lineCost = BigDecimal.ZERO;
    @Column(precision = 18, scale = 4)
    private BigDecimal totalCost = BigDecimal.ZERO;

    // ------------------ PERCENTAGES ------------------
    // Discounts as percentages from bill, line, and total
//    private BigDecimal discountPercentageFromBill = BigDecimal.ZERO;
//    private BigDecimal discountPercentageForTheLine = BigDecimal.ZERO;
//    private BigDecimal totalDiscountPercentage = BigDecimal.ZERO;
//
//    // Taxes as percentages from bill, line, and total
//    private BigDecimal taxPercentageFromBill = BigDecimal.ZERO;
//    private BigDecimal taxPercentageForTheLine = BigDecimal.ZERO;
//    private BigDecimal totalTaxPercentage = BigDecimal.ZERO;
//
//    // Expenses as percentages from bill, line, and total
//    private BigDecimal expensePercentageFromBill = BigDecimal.ZERO;
//    private BigDecimal expensePercentageForTheLine = BigDecimal.ZERO;
//    private BigDecimal totalExpensePercentage = BigDecimal.ZERO;
//
//    // Costs as percentages from bill, line, and total
//    private BigDecimal costPercentageFromBill = BigDecimal.ZERO;
//    private BigDecimal costPercentageForTheLine = BigDecimal.ZERO;
//    private BigDecimal totalCostPercentage = BigDecimal.ZERO;
    // ------------------ QUANTITIES ------------------
    // Quantities as entered (in packs when item is an AMPP or in units if item is AMP)
    @Column(precision = 18, scale = 4)
    private BigDecimal freeQuantity = BigDecimal.ZERO;
    @Column(precision = 18, scale = 4)
    private BigDecimal quantity = BigDecimal.ZERO;
    @Column(precision = 18, scale = 4)
    private BigDecimal totalQuantity = BigDecimal.ZERO;

    // Quantities converted to atomic units (e.g., tablets)
    @Column(precision = 18, scale = 4)
    private BigDecimal freeQuantityByUnits = BigDecimal.ZERO;
    @Column(precision = 18, scale = 4)
    private BigDecimal quantityByUnits = BigDecimal.ZERO;
    @Column(precision = 18, scale = 4)
    private BigDecimal totalQuantityByUnits = BigDecimal.ZERO;

    // ------------------ VALUE ESTIMATES ------------------
    // Value of free items at different rates
//    private BigDecimal freeValueAtCostRate = BigDecimal.ZERO;
//    private BigDecimal freeValueAtRetailRate = BigDecimal.ZERO;
//    private BigDecimal freeValueAtPurchaseRate = BigDecimal.ZERO;
//    private BigDecimal freeValueAtWholesaleRate = BigDecimal.ZERO;
    // Value of total quantity at different rates
//    private BigDecimal valueAtRetailRate = BigDecimal.ZERO;
//    private BigDecimal valueAtWholesaleRate = BigDecimal.ZERO;
//    private BigDecimal valueAtPurchaseRate = BigDecimal.ZERO;
//    private BigDecimal valueAtCostRate = BigDecimal.ZERO;
    // Return quantities
    @Column(precision = 18, scale = 4)
    private BigDecimal returnQuantity = BigDecimal.ZERO;
    @Column(precision = 18, scale = 4)
    private BigDecimal returnFreeQuantity = BigDecimal.ZERO;
    @Column(precision = 18, scale = 4)
    private BigDecimal totalReturnQuantity = BigDecimal.ZERO;

// Return Totals
    @Column(precision = 18, scale = 4)
    private BigDecimal returnGrossTotal = BigDecimal.ZERO;
    @Column(precision = 18, scale = 4)
    private BigDecimal returnNetTotal = BigDecimal.ZERO;

// Return Quantity-based Totals (for clarity if needed separately)
//    @Column(precision = 18, scale = 4)
//    private BigDecimal returnQuantityTotal = BigDecimal.ZERO;
//    @Column(precision = 18, scale = 4)
//    private BigDecimal returnFreeQuantityTotal = BigDecimal.ZERO;
    // Taxes
    // Payment method values
//    @Column(precision = 18, scale = 4)
//    private BigDecimal totalPaidAsCash = BigDecimal.ZERO;
//    @Column(precision = 18, scale = 4)
//    private BigDecimal totalPaidAsCard = BigDecimal.ZERO;
//    @Column(precision = 18, scale = 4)
//    private BigDecimal totalPaidAsMultiplePaymentMethods = BigDecimal.ZERO;
//    @Column(precision = 18, scale = 4)
//    private BigDecimal totalPaidAsStaff = BigDecimal.ZERO;
//    @Column(precision = 18, scale = 4)
//    private BigDecimal totalPaidAsCredit = BigDecimal.ZERO;
//    @Column(precision = 18, scale = 4)
//    private BigDecimal totalPaidAsStaffWelfare = BigDecimal.ZERO;
//    @Column(precision = 18, scale = 4)
//    private BigDecimal totalPaidAsVoucher = BigDecimal.ZERO;
//    @Column(precision = 18, scale = 4)
//    private BigDecimal totalPaidAsIOU = BigDecimal.ZERO;
//    @Column(precision = 18, scale = 4)
//    private BigDecimal totalPaidAsAgent = BigDecimal.ZERO;
//    @Column(precision = 18, scale = 4)
//    private BigDecimal totalPaidAsCheque = BigDecimal.ZERO;
//    @Column(precision = 18, scale = 4)
//    private BigDecimal totalPaidAsSlip = BigDecimal.ZERO;
//    @Column(precision = 18, scale = 4)
//    private BigDecimal totalPaidAsEwallet = BigDecimal.ZERO;
//    @Column(precision = 18, scale = 4)
//    private BigDecimal totalPaidAsPatientDeposit = BigDecimal.ZERO;
//    @Column(precision = 18, scale = 4)
//    private BigDecimal totalPaidAsPatientPoints = BigDecimal.ZERO;
//    @Column(precision = 18, scale = 4)
//    private BigDecimal totalPaidAsOnlineSettlement = BigDecimal.ZERO;
//    @Column(precision = 18, scale = 4)
//    private BigDecimal totalPaidAsOnCall = BigDecimal.ZERO;
//    private BigDecimal totalPaidAsYouOweMe = BigDecimal.ZERO;
//    private BigDecimal totalPaidAsNone = BigDecimal.ZERO;
    @Column(precision = 18, scale = 4)
    private BigDecimal profitMargin = BigDecimal.ZERO;

    @Override
    public BillItemFinanceDetails clone() {
        BillItemFinanceDetails cloned = new BillItemFinanceDetails();

        // Metadata
        cloned.unitsPerPack = this.unitsPerPack;

        // ------------------ RATES ------------------
        cloned.lineGrossRate = this.lineGrossRate;
        cloned.billGrossRate = this.billGrossRate;
        cloned.grossRate = this.grossRate;

        cloned.lineNetRate = this.lineNetRate;
        cloned.billNetRate = this.billNetRate;
        cloned.netRate = this.netRate;
        cloned.lineDiscountRate = this.lineDiscountRate;
        cloned.billDiscountRate = this.billDiscountRate;
        cloned.totalDiscountRate = this.totalDiscountRate;
        cloned.lineExpenseRate = this.lineExpenseRate;
        cloned.billExpenseRate = this.billExpenseRate;
        cloned.totalExpenseRate = this.totalExpenseRate;
        cloned.billTaxRate = this.billTaxRate;
        cloned.lineTaxRate = this.lineTaxRate;
        cloned.totalTaxRate = this.totalTaxRate;
        cloned.billCostRate = this.billCostRate;
        cloned.lineCostRate = this.lineCostRate;
        cloned.totalCostRate = this.totalCostRate;

        // ------------------ TOTALS ------------------
        cloned.lineGrossTotal = this.lineGrossTotal;
        cloned.billGrossTotal = this.billGrossTotal;
        cloned.grossTotal = this.grossTotal;
        cloned.lineNetTotal = this.lineNetTotal;
        cloned.billNetTotal = this.billNetTotal;
        cloned.netTotal = this.netTotal;

        // ------------------ DISCOUNTS ------------------
        cloned.lineDiscount = this.lineDiscount;
        cloned.billDiscount = this.billDiscount;
        cloned.totalDiscount = this.totalDiscount;

        // ------------------ RETAIL/WHOLESALE RATES ------------------
        cloned.retailSaleRate = this.retailSaleRate;
        cloned.wholesaleRate = this.wholesaleRate;
        cloned.retailSaleRatePerUnit = this.retailSaleRatePerUnit;
        cloned.wholesaleRatePerUnit = this.wholesaleRatePerUnit;

        // ------------------ TAXES ------------------
        cloned.billTax = this.billTax;
        cloned.lineTax = this.lineTax;
        cloned.totalTax = this.totalTax;

        // ------------------ EXPENSES ------------------
        cloned.billExpense = this.billExpense;
        cloned.lineExpense = this.lineExpense;
        cloned.totalExpense = this.totalExpense;

        // ------------------ COSTS ------------------
        cloned.billCost = this.billCost;
        cloned.lineCost = this.lineCost;
        cloned.totalCost = this.totalCost;

        // ------------------ PERCENTAGES ------------------
//        cloned.discountPercentageFromBill = this.discountPercentageFromBill;
//        cloned.discountPercentageForTheLine = this.discountPercentageForTheLine;
//        cloned.totalDiscountPercentage = this.totalDiscountPercentage;
//        cloned.taxPercentageFromBill = this.taxPercentageFromBill;
//        cloned.taxPercentageForTheLine = this.taxPercentageForTheLine;
//        cloned.totalTaxPercentage = this.totalTaxPercentage;
//        cloned.expensePercentageFromBill = this.expensePercentageFromBill;
//        cloned.expensePercentageForTheLine = this.expensePercentageForTheLine;
//        cloned.totalExpensePercentage = this.totalExpensePercentage;
//        cloned.costPercentageFromBill = this.costPercentageFromBill;
//        cloned.costPercentageForTheLine = this.costPercentageForTheLine;
//        cloned.totalCostPercentage = this.totalCostPercentage;
        // ------------------ QUANTITIES ------------------
        cloned.freeQuantity = this.freeQuantity;
        cloned.quantity = this.quantity;
        cloned.totalQuantity = this.totalQuantity;
        cloned.freeQuantityByUnits = this.freeQuantityByUnits;
        cloned.quantityByUnits = this.quantityByUnits;
        cloned.totalQuantityByUnits = this.totalQuantityByUnits;

        // ------------------ VALUE ESTIMATES ------------------
//        cloned.freeValueAtCostRate = this.freeValueAtCostRate;
//        cloned.freeValueAtRetailRate = this.freeValueAtRetailRate;
//        cloned.freeValueAtPurchaseRate = this.freeValueAtPurchaseRate;
//        cloned.freeValueAtWholesaleRate = this.freeValueAtWholesaleRate;
//        cloned.valueAtRetailRate = this.valueAtRetailRate;
//        cloned.valueAtWholesaleRate = this.valueAtWholesaleRate;
//        cloned.valueAtPurchaseRate = this.valueAtPurchaseRate;
//        cloned.valueAtCostRate = this.valueAtCostRate;
        // ------------------ RETURN QUANTITIES ------------------
        cloned.returnQuantity = this.returnQuantity;
        cloned.returnFreeQuantity = this.returnFreeQuantity;
        cloned.totalReturnQuantity = this.totalReturnQuantity;
//        cloned.returnQuantityTotal = this.returnQuantityTotal;
//        cloned.returnFreeQuantityTotal = this.returnFreeQuantityTotal;

        // ------------------ RETURN TOTALS ------------------
        cloned.returnGrossTotal = this.returnGrossTotal;
        cloned.returnNetTotal = this.returnNetTotal;

        //Profit
        cloned.profitMargin = this.profitMargin;

        // ------------------ PAYMENT METHODS ------------------
//        cloned.totalPaidAsCash = this.totalPaidAsCash;
//        cloned.totalPaidAsCard = this.totalPaidAsCard;
//        cloned.totalPaidAsMultiplePaymentMethods = this.totalPaidAsMultiplePaymentMethods;
//        cloned.totalPaidAsStaff = this.totalPaidAsStaff;
//        cloned.totalPaidAsCredit = this.totalPaidAsCredit;
//        cloned.totalPaidAsStaffWelfare = this.totalPaidAsStaffWelfare;
//        cloned.totalPaidAsVoucher = this.totalPaidAsVoucher;
//        cloned.totalPaidAsIOU = this.totalPaidAsIOU;
//        cloned.totalPaidAsAgent = this.totalPaidAsAgent;
//        cloned.totalPaidAsCheque = this.totalPaidAsCheque;
//        cloned.totalPaidAsSlip = this.totalPaidAsSlip;
//        cloned.totalPaidAsEwallet = this.totalPaidAsEwallet;
//        cloned.totalPaidAsPatientDeposit = this.totalPaidAsPatientDeposit;
//        cloned.totalPaidAsPatientPoints = this.totalPaidAsPatientPoints;
//        cloned.totalPaidAsOnlineSettlement = this.totalPaidAsOnlineSettlement;
//        cloned.totalPaidAsOnCall = this.totalPaidAsOnCall;
//        cloned.totalPaidAsYouOweMe = this.totalPaidAsYouOweMe;
//        cloned.totalPaidAsNone = this.totalPaidAsNone;
        return cloned;
    }

    public BillItemFinanceDetails() {
        createdAt = new Date();
    }

    public BillItemFinanceDetails(BillItem billItem) {
        this.billItem = billItem;
        createdAt = new Date();
    }

    public BigDecimal getGrossRate() {
        return grossRate;
    }

    public void setGrossRate(BigDecimal grossRate) {
        this.grossRate = grossRate;
    }

    public BigDecimal getNetRate() {
        return netRate;
    }

    public void setNetRate(BigDecimal netRate) {
        this.netRate = netRate;
    }

    public BigDecimal getLineDiscountRate() {
        return lineDiscountRate;
    }

    public void setLineDiscountRate(BigDecimal lineDiscountRate) {
        this.lineDiscountRate = lineDiscountRate;
    }

    public BigDecimal getBillDiscountRate() {
        return billDiscountRate;
    }

    public void setBillDiscountRate(BigDecimal billDiscountRate) {
        this.billDiscountRate = billDiscountRate;
    }

    public BigDecimal getTotalDiscountRate() {
        return totalDiscountRate;
    }

    public void setTotalDiscountRate(BigDecimal totalDiscountRate) {
        this.totalDiscountRate = totalDiscountRate;
    }

    public BigDecimal getLineExpenseRate() {
        return lineExpenseRate;
    }

    public void setLineExpenseRate(BigDecimal lineExpenseRate) {
        this.lineExpenseRate = lineExpenseRate;
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
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof BillItemFinanceDetails)) {
            return false;
        }
        BillItemFinanceDetails other = (BillItemFinanceDetails) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.core.entity.BillItemFinanceDetails[ id=" + id + " ]";
    }

    public BigDecimal getGrossTotal() {
        return grossTotal;
    }

    public void setGrossTotal(BigDecimal grossTotal) {
        this.grossTotal = grossTotal;
    }

    public BigDecimal getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(BigDecimal netTotal) {
        this.netTotal = netTotal;
    }

    public BigDecimal getReturnGrossTotal() {
        return returnGrossTotal;
    }

    public void setReturnGrossTotal(BigDecimal returnGrossTotal) {
        this.returnGrossTotal = returnGrossTotal;
    }

    public BigDecimal getReturnNetTotal() {
        return returnNetTotal;
    }

    public void setReturnNetTotal(BigDecimal returnNetTotal) {
        this.returnNetTotal = returnNetTotal;
    }

//    public BigDecimal getReturnQuantityTotal() {
//        return returnQuantityTotal;
//    }
//
//    public void setReturnQuantityTotal(BigDecimal returnQuantityTotal) {
//        this.returnQuantityTotal = returnQuantityTotal;
//    }
//
//    public BigDecimal getReturnFreeQuantityTotal() {
//        return returnFreeQuantityTotal;
//    }
//
//    public void setReturnFreeQuantityTotal(BigDecimal returnFreeQuantityTotal) {
//        this.returnFreeQuantityTotal = returnFreeQuantityTotal;
//    }
    public BigDecimal getBillTaxRate() {
        return billTaxRate;
    }

    public void setBillTaxRate(BigDecimal billTaxRate) {
        this.billTaxRate = billTaxRate;
    }

    public BigDecimal getLineTaxRate() {
        return lineTaxRate;
    }

    public void setLineTaxRate(BigDecimal lineTaxRate) {
        this.lineTaxRate = lineTaxRate;
    }

    public BigDecimal getTotalTaxRate() {
        return totalTaxRate;
    }

    public void setTotalTaxRate(BigDecimal totalTaxRate) {
        this.totalTaxRate = totalTaxRate;
    }

    public BigDecimal getBillTax() {
        return billTax;
    }

    public void setBillTax(BigDecimal billTax) {
        this.billTax = billTax;
    }

    public BigDecimal getLineTax() {
        return lineTax;
    }

    public void setLineTax(BigDecimal lineTax) {
        this.lineTax = lineTax;
    }

    public BigDecimal getTotalTax() {
        return totalTax;
    }

    public void setTotalTax(BigDecimal totalTax) {
        this.totalTax = totalTax;
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

    public BigDecimal getTotalExpense() {
        return totalExpense;
    }

    public void setTotalExpense(BigDecimal totalExpense) {
        this.totalExpense = totalExpense;
    }

    public BigDecimal getBillCost() {
        return billCost;
    }

    public void setBillCost(BigDecimal billCost) {
        this.billCost = billCost;
    }

    public BigDecimal getLineCost() {
        return lineCost;
    }

    public void setLineCost(BigDecimal lineCost) {
        this.lineCost = lineCost;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public BillItem getBillItem() {
        return billItem;
    }

    public void setBillItem(BillItem billItem) {
        this.billItem = billItem;
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

    public BigDecimal getUnitsPerPack() {
        return unitsPerPack;
    }

    public void setUnitsPerPack(BigDecimal unitsPerPack) {
        this.unitsPerPack = unitsPerPack;
    }

    public BigDecimal getLineGrossRate() {
        return lineGrossRate;
    }

    public void setLineGrossRate(BigDecimal lineGrossRate) {
        this.lineGrossRate = lineGrossRate;
    }

    public BigDecimal getBillGrossRate() {
        return billGrossRate;
    }

    public void setBillGrossRate(BigDecimal billGrossRate) {
        this.billGrossRate = billGrossRate;
    }

    public BigDecimal getLineNetRate() {
        return lineNetRate;
    }

    public void setLineNetRate(BigDecimal lineNetRate) {
        this.lineNetRate = lineNetRate;
    }

    public BigDecimal getBillNetRate() {
        return billNetRate;
    }

    public void setBillNetRate(BigDecimal billNetRate) {
        this.billNetRate = billNetRate;
    }

    public BigDecimal getBillExpenseRate() {
        return billExpenseRate;
    }

    public void setBillExpenseRate(BigDecimal billExpenseRate) {
        this.billExpenseRate = billExpenseRate;
    }

    public BigDecimal getTotalExpenseRate() {
        return totalExpenseRate;
    }

    public void setTotalExpenseRate(BigDecimal totalExpenseRate) {
        this.totalExpenseRate = totalExpenseRate;
    }

    public BigDecimal getBillCostRate() {
        return billCostRate;
    }

    public void setBillCostRate(BigDecimal billCostRate) {
        this.billCostRate = billCostRate;
    }

    public BigDecimal getLineCostRate() {
        return lineCostRate;
    }

    public void setLineCostRate(BigDecimal lineCostRate) {
        this.lineCostRate = lineCostRate;
    }

    public BigDecimal getTotalCostRate() {
        return totalCostRate;
    }

    public void setTotalCostRate(BigDecimal totalCostRate) {
        this.totalCostRate = totalCostRate;
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

    public BigDecimal getLineDiscount() {
        return lineDiscount;
    }

    public void setLineDiscount(BigDecimal lineDiscount) {
        this.lineDiscount = lineDiscount;
    }

    public BigDecimal getBillDiscount() {
        return billDiscount;
    }

    public void setBillDiscount(BigDecimal billDiscount) {
        this.billDiscount = billDiscount;
    }

    public BigDecimal getTotalDiscount() {
        return totalDiscount;
    }

    public void setTotalDiscount(BigDecimal totalDiscount) {
        this.totalDiscount = totalDiscount;
    }

    public BigDecimal getRetailSaleRate() {
        return retailSaleRate;
    }

    public void setRetailSaleRate(BigDecimal retailSaleRate) {
        this.retailSaleRate = retailSaleRate;
    }

    public BigDecimal getWholesaleRate() {
        return wholesaleRate;
    }

    public void setWholesaleRate(BigDecimal wholesaleRate) {
        this.wholesaleRate = wholesaleRate;
    }

    public BigDecimal getRetailSaleRatePerUnit() {
        return retailSaleRatePerUnit;
    }

    public void setRetailSaleRatePerUnit(BigDecimal retailSaleRatePerUnit) {
        this.retailSaleRatePerUnit = retailSaleRatePerUnit;
    }

    public BigDecimal getWholesaleRatePerUnit() {
        return wholesaleRatePerUnit;
    }

    public void setWholesaleRatePerUnit(BigDecimal wholesaleRatePerUnit) {
        this.wholesaleRatePerUnit = wholesaleRatePerUnit;
    }

    public BigDecimal getFreeQuantity() {
        return freeQuantity;
    }

    public void setFreeQuantity(BigDecimal freeQuantity) {
        this.freeQuantity = freeQuantity;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(BigDecimal totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public BigDecimal getFreeQuantityByUnits() {
        return freeQuantityByUnits;
    }

    public void setFreeQuantityByUnits(BigDecimal freeQuantityByUnits) {
        this.freeQuantityByUnits = freeQuantityByUnits;
    }

    public BigDecimal getQuantityByUnits() {
        return quantityByUnits;
    }

    public void setQuantityByUnits(BigDecimal quantityByUnits) {
        this.quantityByUnits = quantityByUnits;
    }

    public BigDecimal getTotalQuantityByUnits() {
        return totalQuantityByUnits;
    }

    public void setTotalQuantityByUnits(BigDecimal totalQuantityByUnits) {
        this.totalQuantityByUnits = totalQuantityByUnits;
    }

    public BigDecimal getReturnQuantity() {
        return returnQuantity;
    }

    public void setReturnQuantity(BigDecimal returnQuantity) {
        this.returnQuantity = returnQuantity;
    }

    public BigDecimal getReturnFreeQuantity() {
        return returnFreeQuantity;
    }

    public void setReturnFreeQuantity(BigDecimal returnFreeQuantity) {
        this.returnFreeQuantity = returnFreeQuantity;
    }

    public BigDecimal getTotalReturnQuantity() {
        return totalReturnQuantity;
    }

    public void setTotalReturnQuantity(BigDecimal totalReturnQuantity) {
        this.totalReturnQuantity = totalReturnQuantity;
    }

    public BigDecimal getProfitMargin() {
        return profitMargin;
    }

    public void setProfitMargin(BigDecimal profitMargin) {
        this.profitMargin = profitMargin;
    }

    public BigDecimal getValueAtRetailRate() {
        return valueAtRetailRate;
    }

    public void setValueAtRetailRate(BigDecimal valueAtRetailRate) {
        this.valueAtRetailRate = valueAtRetailRate;
    }

    public BigDecimal getValueAtPurchaseRate() {
        return valueAtPurchaseRate;
    }

    public void setValueAtPurchaseRate(BigDecimal valueAtPurchaseRate) {
        this.valueAtPurchaseRate = valueAtPurchaseRate;
    }
    
    

}
