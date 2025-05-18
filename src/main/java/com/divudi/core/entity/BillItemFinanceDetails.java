package com.divudi.core.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
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
    private BigDecimal unitsPerPack = BigDecimal.ZERO;

    // ------------------ RATES ------------------
    // Base price before any deductions
    private BigDecimal grossRate = BigDecimal.ZERO;

    // Final rate after all discounts, expenses, and taxes
    private BigDecimal netRate = BigDecimal.ZERO;

    // Discount percentages applied at line level, bill level, and total
    private BigDecimal lineDiscountRate = BigDecimal.ZERO;
    private BigDecimal billDiscountRate = BigDecimal.ZERO;
    private BigDecimal totalDiscountRate = BigDecimal.ZERO;

    // Expense percentages applied at line level, bill level, and total
    private BigDecimal lineExpenseRate = BigDecimal.ZERO;
    private BigDecimal billExpenseRate = BigDecimal.ZERO;
    private BigDecimal totalExpenseRate = BigDecimal.ZERO;

    // Tax percentages applied at bill, line, and total
    private BigDecimal billTaxRate = BigDecimal.ZERO;
    private BigDecimal lineTaxRate = BigDecimal.ZERO;
    private BigDecimal totalTaxRate = BigDecimal.ZERO;

    // Cost percentages applied at bill, line, and total
    private BigDecimal billCostRate = BigDecimal.ZERO;
    private BigDecimal lineCostRate = BigDecimal.ZERO;
    private BigDecimal totalCostRate = BigDecimal.ZERO;

    // ------------------ TOTALS ------------------
    // Value before deductions
    private BigDecimal grossTotal = BigDecimal.ZERO;

    // Value after deductions
    private BigDecimal netTotal = BigDecimal.ZERO;

    // Absolute discount values
    private BigDecimal lineDiscount = BigDecimal.ZERO;
    private BigDecimal billDiscount = BigDecimal.ZERO;
    private BigDecimal totalDiscount = BigDecimal.ZERO;

    // Retail and wholesale rates
    private BigDecimal retailSaleRate = BigDecimal.ZERO;
    private BigDecimal wholesaleRate = BigDecimal.ZERO;

    // Retail and wholesale rates per unit (based on unitsPerPack)
    private BigDecimal retailSaleRatePerUnit = BigDecimal.ZERO;
    private BigDecimal wholesaleRatePerUnit = BigDecimal.ZERO;

    // Absolute tax values
    private BigDecimal billTax = BigDecimal.ZERO;
    private BigDecimal lineTax = BigDecimal.ZERO;
    private BigDecimal totalTax = BigDecimal.ZERO;

    // Absolute expense values
    private BigDecimal billExpense = BigDecimal.ZERO;
    private BigDecimal lineExpense = BigDecimal.ZERO;
    private BigDecimal totalExpense = BigDecimal.ZERO;

    // Absolute cost values
    private BigDecimal billCost = BigDecimal.ZERO;
    private BigDecimal lineCost = BigDecimal.ZERO;
    private BigDecimal totalCost = BigDecimal.ZERO;

    // ------------------ PERCENTAGES ------------------
    // Discounts as percentages from bill, line, and total
    private BigDecimal discountPercentageFromBill = BigDecimal.ZERO;
    private BigDecimal discountPercentageForTheLine = BigDecimal.ZERO;
    private BigDecimal totalDiscountPercentage = BigDecimal.ZERO;

    // Taxes as percentages from bill, line, and total
    private BigDecimal taxPercentageFromBill = BigDecimal.ZERO;
    private BigDecimal taxPercentageForTheLine = BigDecimal.ZERO;
    private BigDecimal totalTaxPercentage = BigDecimal.ZERO;

    // Expenses as percentages from bill, line, and total
    private BigDecimal expensePercentageFromBill = BigDecimal.ZERO;
    private BigDecimal expensePercentageForTheLine = BigDecimal.ZERO;
    private BigDecimal totalExpensePercentage = BigDecimal.ZERO;

    // Costs as percentages from bill, line, and total
    private BigDecimal costPercentageFromBill = BigDecimal.ZERO;
    private BigDecimal costPercentageForTheLine = BigDecimal.ZERO;
    private BigDecimal totalCostPercentage = BigDecimal.ZERO;

    // ------------------ QUANTITIES ------------------
    // Quantities as entered (in packs when item is an AMPP or in units if item is AMP)
    private BigDecimal freeQuantity = BigDecimal.ZERO;
    private BigDecimal quantity = BigDecimal.ZERO;
    private BigDecimal totalQuantity = BigDecimal.ZERO;

    // Quantities converted to atomic units (e.g., tablets)
    private BigDecimal freeQuantityByUnits = BigDecimal.ZERO;
    private BigDecimal quantityByUnits = BigDecimal.ZERO;
    private BigDecimal totalQuantityByUnits = BigDecimal.ZERO;

    // ------------------ VALUE ESTIMATES ------------------
    // Value of free items at different rates
    private BigDecimal freeValueAtCostRate = BigDecimal.ZERO;
    private BigDecimal freeValueAtRetailRate = BigDecimal.ZERO;
    private BigDecimal freeValueAtPurchaseRate = BigDecimal.ZERO;
    private BigDecimal freeValueAtWholesaleRate = BigDecimal.ZERO;

    // Value of total quantity at different rates
    private BigDecimal valueAtRetailRate = BigDecimal.ZERO;
    private BigDecimal valueAtWholesaleRate = BigDecimal.ZERO;
    private BigDecimal valueAtPurchaseRate = BigDecimal.ZERO;
    private BigDecimal valueAtCostRate = BigDecimal.ZERO;

    // Return quantities
    private BigDecimal returnQuantity = BigDecimal.ZERO;
    private BigDecimal returnFreeQuantity = BigDecimal.ZERO;
    private BigDecimal totalReturnQuantity = BigDecimal.ZERO;

// Return Totals
    private BigDecimal returnGrossTotal = BigDecimal.ZERO;
    private BigDecimal returnNetTotal = BigDecimal.ZERO;

// Return Quantity-based Totals (for clarity if needed separately)
    private BigDecimal returnQuantityTotal = BigDecimal.ZERO;
    private BigDecimal returnFreeQuantityTotal = BigDecimal.ZERO;

    // Taxes
    // Payment method values
    private BigDecimal totalPaidAsCash = BigDecimal.ZERO;
    private BigDecimal totalPaidAsCard = BigDecimal.ZERO;
    private BigDecimal totalPaidAsMultiplePaymentMethods = BigDecimal.ZERO;
    private BigDecimal totalPaidAsStaff = BigDecimal.ZERO;
    private BigDecimal totalPaidAsCredit = BigDecimal.ZERO;
    private BigDecimal totalPaidAsStaffWelfare = BigDecimal.ZERO;
    private BigDecimal totalPaidAsVoucher = BigDecimal.ZERO;
    private BigDecimal totalPaidAsIOU = BigDecimal.ZERO;
    private BigDecimal totalPaidAsAgent = BigDecimal.ZERO;
    private BigDecimal totalPaidAsCheque = BigDecimal.ZERO;
    private BigDecimal totalPaidAsSlip = BigDecimal.ZERO;
    private BigDecimal totalPaidAsEwallet = BigDecimal.ZERO;
    private BigDecimal totalPaidAsPatientDeposit = BigDecimal.ZERO;
    private BigDecimal totalPaidAsPatientPoints = BigDecimal.ZERO;
    private BigDecimal totalPaidAsOnlineSettlement = BigDecimal.ZERO;
    private BigDecimal totalPaidAsOnCall = BigDecimal.ZERO;
    private BigDecimal totalPaidAsYouOweMe = BigDecimal.ZERO;
    private BigDecimal totalPaidAsNone = BigDecimal.ZERO;
    
    

    public BillItemFinanceDetails() {
        createdAt = new Date();
    }

    public BillItemFinanceDetails(BillItem billItem) {
        this.billItem = billItem;
        createdAt = new Date();
    }

    @Override
    public BillItemFinanceDetails clone() {
        BillItemFinanceDetails cloned = new BillItemFinanceDetails();

        cloned.grossTotal = this.grossTotal;
        cloned.netTotal = this.netTotal;

        cloned.returnGrossTotal = this.returnGrossTotal;
        cloned.returnNetTotal = this.returnNetTotal;
        cloned.returnQuantityTotal = this.returnQuantityTotal;
        cloned.returnFreeQuantityTotal = this.returnFreeQuantityTotal;

        cloned.billTaxRate = this.billTaxRate;
        cloned.lineTaxRate = this.lineTaxRate;
        cloned.totalTaxRate = this.totalTaxRate;

        cloned.billTax = this.billTax;
        cloned.lineTax = this.lineTax;
        cloned.totalTax = this.totalTax;

        cloned.billExpense = this.billExpense;
        cloned.lineExpense = this.lineExpense;
        cloned.totalExpense = this.totalExpense;

        cloned.billCost = this.billCost;
        cloned.lineCost = this.lineCost;
        cloned.totalCost = this.totalCost;

        cloned.discountPercentageFromBill = this.discountPercentageFromBill;
        cloned.discountPercentageForTheLine = this.discountPercentageForTheLine;
        cloned.totalDiscountPercentage = this.totalDiscountPercentage;

        cloned.taxPercentageFromBill = this.taxPercentageFromBill;
        cloned.taxPercentageForTheLine = this.taxPercentageForTheLine;
        cloned.totalTaxPercentage = this.totalTaxPercentage;

        cloned.expensePercentageFromBill = this.expensePercentageFromBill;
        cloned.expensePercentageForTheLine = this.expensePercentageForTheLine;
        cloned.totalExpensePercentage = this.totalExpensePercentage;

        cloned.costPercentageFromBill = this.costPercentageFromBill;
        cloned.costPercentageForTheLine = this.costPercentageForTheLine;
        cloned.totalCostPercentage = this.totalCostPercentage;

        cloned.freeQuantity = this.freeQuantity;
        cloned.freeValueAtCostRate = this.freeValueAtCostRate;
        cloned.valueAtRetailRate = this.valueAtRetailRate;
        cloned.valueAtWholesaleRate = this.valueAtWholesaleRate;
        cloned.quantity = this.quantity;
        cloned.totalQuantity = this.totalQuantity;

        cloned.returnQuantity = this.returnQuantity;
        cloned.returnFreeQuantity = this.returnFreeQuantity;
        cloned.totalReturnQuantity = this.totalReturnQuantity;

        cloned.totalPaidAsCash = this.totalPaidAsCash;
        cloned.totalPaidAsCard = this.totalPaidAsCard;
        cloned.totalPaidAsMultiplePaymentMethods = this.totalPaidAsMultiplePaymentMethods;
        cloned.totalPaidAsStaff = this.totalPaidAsStaff;
        cloned.totalPaidAsCredit = this.totalPaidAsCredit;
        cloned.totalPaidAsStaffWelfare = this.totalPaidAsStaffWelfare;
        cloned.totalPaidAsVoucher = this.totalPaidAsVoucher;
        cloned.totalPaidAsIOU = this.totalPaidAsIOU;
        cloned.totalPaidAsAgent = this.totalPaidAsAgent;
        cloned.totalPaidAsCheque = this.totalPaidAsCheque;
        cloned.totalPaidAsSlip = this.totalPaidAsSlip;
        cloned.totalPaidAsEwallet = this.totalPaidAsEwallet;
        cloned.totalPaidAsPatientDeposit = this.totalPaidAsPatientDeposit;
        cloned.totalPaidAsPatientPoints = this.totalPaidAsPatientPoints;
        cloned.totalPaidAsOnlineSettlement = this.totalPaidAsOnlineSettlement;
        cloned.totalPaidAsOnCall = this.totalPaidAsOnCall;
        cloned.totalPaidAsYouOweMe = this.totalPaidAsYouOweMe;
        cloned.totalPaidAsNone = this.totalPaidAsNone;
        cloned.grossRate = this.grossRate;
        cloned.netRate = this.netRate;
        cloned.lineDiscountRate = this.lineDiscountRate;
        cloned.billDiscountRate = this.billDiscountRate;
        cloned.totalDiscountRate = this.totalDiscountRate;
        cloned.lineExpenseRate = this.lineExpenseRate;

        cloned.lineDiscount = this.lineDiscount;
        cloned.billDiscount = this.billDiscount;
        cloned.totalDiscount = this.totalDiscount;
        cloned.billTax = this.billTax;

        cloned.retailSaleRate = this.retailSaleRate;
        cloned.wholesaleRate = this.wholesaleRate;
        cloned.retailSaleRatePerUnit = this.retailSaleRatePerUnit;
        cloned.wholesaleRatePerUnit = this.wholesaleRatePerUnit;

        cloned.valueAtPurchaseRate = this.valueAtPurchaseRate;
        cloned.valueAtCostRate = this.valueAtCostRate;

        cloned.billCostRate = this.billCostRate;
        cloned.lineCostRate = this.lineCostRate;
        cloned.totalCostRate = this.totalCostRate;

        cloned.unitsPerPack = this.unitsPerPack;

        return cloned;
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

    public BigDecimal getReturnQuantityTotal() {
        return returnQuantityTotal;
    }

    public void setReturnQuantityTotal(BigDecimal returnQuantityTotal) {
        this.returnQuantityTotal = returnQuantityTotal;
    }

    public BigDecimal getReturnFreeQuantityTotal() {
        return returnFreeQuantityTotal;
    }

    public void setReturnFreeQuantityTotal(BigDecimal returnFreeQuantityTotal) {
        this.returnFreeQuantityTotal = returnFreeQuantityTotal;
    }

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

    public BigDecimal getDiscountPercentageFromBill() {
        return discountPercentageFromBill;
    }

    public void setDiscountPercentageFromBill(BigDecimal discountPercentageFromBill) {
        this.discountPercentageFromBill = discountPercentageFromBill;
    }

    public BigDecimal getDiscountPercentageForTheLine() {
        return discountPercentageForTheLine;
    }

    public void setDiscountPercentageForTheLine(BigDecimal discountPercentageForTheLine) {
        this.discountPercentageForTheLine = discountPercentageForTheLine;
    }

    public BigDecimal getTotalDiscountPercentage() {
        return totalDiscountPercentage;
    }

    public void setTotalDiscountPercentage(BigDecimal totalDiscountPercentage) {
        this.totalDiscountPercentage = totalDiscountPercentage;
    }

    public BigDecimal getTaxPercentageFromBill() {
        return taxPercentageFromBill;
    }

    public void setTaxPercentageFromBill(BigDecimal taxPercentageFromBill) {
        this.taxPercentageFromBill = taxPercentageFromBill;
    }

    public BigDecimal getTaxPercentageForTheLine() {
        return taxPercentageForTheLine;
    }

    public void setTaxPercentageForTheLine(BigDecimal taxPercentageForTheLine) {
        this.taxPercentageForTheLine = taxPercentageForTheLine;
    }

    public BigDecimal getTotalTaxPercentage() {
        return totalTaxPercentage;
    }

    public void setTotalTaxPercentage(BigDecimal totalTaxPercentage) {
        this.totalTaxPercentage = totalTaxPercentage;
    }

    public BigDecimal getExpensePercentageFromBill() {
        return expensePercentageFromBill;
    }

    public void setExpensePercentageFromBill(BigDecimal expensePercentageFromBill) {
        this.expensePercentageFromBill = expensePercentageFromBill;
    }

    public BigDecimal getExpensePercentageForTheLine() {
        return expensePercentageForTheLine;
    }

    public void setExpensePercentageForTheLine(BigDecimal expensePercentageForTheLine) {
        this.expensePercentageForTheLine = expensePercentageForTheLine;
    }

    public BigDecimal getTotalExpensePercentage() {
        return totalExpensePercentage;
    }

    public void setTotalExpensePercentage(BigDecimal totalExpensePercentage) {
        this.totalExpensePercentage = totalExpensePercentage;
    }

    public BigDecimal getCostPercentageFromBill() {
        return costPercentageFromBill;
    }

    public void setCostPercentageFromBill(BigDecimal costPercentageFromBill) {
        this.costPercentageFromBill = costPercentageFromBill;
    }

    public BigDecimal getCostPercentageForTheLine() {
        return costPercentageForTheLine;
    }

    public void setCostPercentageForTheLine(BigDecimal costPercentageForTheLine) {
        this.costPercentageForTheLine = costPercentageForTheLine;
    }

    public BigDecimal getTotalCostPercentage() {
        return totalCostPercentage;
    }

    public void setTotalCostPercentage(BigDecimal totalCostPercentage) {
        this.totalCostPercentage = totalCostPercentage;
    }

    public BigDecimal getFreeQuantity() {
        return freeQuantity;
    }

    public void setFreeQuantity(BigDecimal freeQuantity) {
        this.freeQuantity = freeQuantity;
    }

    public BigDecimal getFreeValueAtCostRate() {
        return freeValueAtCostRate;
    }

    public void setFreeValueAtCostRate(BigDecimal freeValueAtCostRate) {
        this.freeValueAtCostRate = freeValueAtCostRate;
    }

    public BigDecimal getValueAtRetailRate() {
        return valueAtRetailRate;
    }

    public void setValueAtRetailRate(BigDecimal valueAtRetailRate) {
        this.valueAtRetailRate = valueAtRetailRate;
    }

    public BigDecimal getValueAtWholesaleRate() {
        return valueAtWholesaleRate;
    }

    public void setValueAtWholesaleRate(BigDecimal valueAtWholesaleRate) {
        this.valueAtWholesaleRate = valueAtWholesaleRate;
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

    public BigDecimal getTotalPaidAsCash() {
        return totalPaidAsCash;
    }

    public void setTotalPaidAsCash(BigDecimal totalPaidAsCash) {
        this.totalPaidAsCash = totalPaidAsCash;
    }

    public BigDecimal getTotalPaidAsCard() {
        return totalPaidAsCard;
    }

    public void setTotalPaidAsCard(BigDecimal totalPaidAsCard) {
        this.totalPaidAsCard = totalPaidAsCard;
    }

    public BigDecimal getTotalPaidAsMultiplePaymentMethods() {
        return totalPaidAsMultiplePaymentMethods;
    }

    public void setTotalPaidAsMultiplePaymentMethods(BigDecimal totalPaidAsMultiplePaymentMethods) {
        this.totalPaidAsMultiplePaymentMethods = totalPaidAsMultiplePaymentMethods;
    }

    public BigDecimal getTotalPaidAsStaff() {
        return totalPaidAsStaff;
    }

    public void setTotalPaidAsStaff(BigDecimal totalPaidAsStaff) {
        this.totalPaidAsStaff = totalPaidAsStaff;
    }

    public BigDecimal getTotalPaidAsCredit() {
        return totalPaidAsCredit;
    }

    public void setTotalPaidAsCredit(BigDecimal totalPaidAsCredit) {
        this.totalPaidAsCredit = totalPaidAsCredit;
    }

    public BigDecimal getTotalPaidAsStaffWelfare() {
        return totalPaidAsStaffWelfare;
    }

    public void setTotalPaidAsStaffWelfare(BigDecimal totalPaidAsStaffWelfare) {
        this.totalPaidAsStaffWelfare = totalPaidAsStaffWelfare;
    }

    public BigDecimal getTotalPaidAsVoucher() {
        return totalPaidAsVoucher;
    }

    public void setTotalPaidAsVoucher(BigDecimal totalPaidAsVoucher) {
        this.totalPaidAsVoucher = totalPaidAsVoucher;
    }

    public BigDecimal getTotalPaidAsIOU() {
        return totalPaidAsIOU;
    }

    public void setTotalPaidAsIOU(BigDecimal totalPaidAsIOU) {
        this.totalPaidAsIOU = totalPaidAsIOU;
    }

    public BigDecimal getTotalPaidAsAgent() {
        return totalPaidAsAgent;
    }

    public void setTotalPaidAsAgent(BigDecimal totalPaidAsAgent) {
        this.totalPaidAsAgent = totalPaidAsAgent;
    }

    public BigDecimal getTotalPaidAsCheque() {
        return totalPaidAsCheque;
    }

    public void setTotalPaidAsCheque(BigDecimal totalPaidAsCheque) {
        this.totalPaidAsCheque = totalPaidAsCheque;
    }

    public BigDecimal getTotalPaidAsSlip() {
        return totalPaidAsSlip;
    }

    public void setTotalPaidAsSlip(BigDecimal totalPaidAsSlip) {
        this.totalPaidAsSlip = totalPaidAsSlip;
    }

    public BigDecimal getTotalPaidAsEwallet() {
        return totalPaidAsEwallet;
    }

    public void setTotalPaidAsEwallet(BigDecimal totalPaidAsEwallet) {
        this.totalPaidAsEwallet = totalPaidAsEwallet;
    }

    public BigDecimal getTotalPaidAsPatientDeposit() {
        return totalPaidAsPatientDeposit;
    }

    public void setTotalPaidAsPatientDeposit(BigDecimal totalPaidAsPatientDeposit) {
        this.totalPaidAsPatientDeposit = totalPaidAsPatientDeposit;
    }

    public BigDecimal getTotalPaidAsPatientPoints() {
        return totalPaidAsPatientPoints;
    }

    public void setTotalPaidAsPatientPoints(BigDecimal totalPaidAsPatientPoints) {
        this.totalPaidAsPatientPoints = totalPaidAsPatientPoints;
    }

    public BigDecimal getTotalPaidAsOnlineSettlement() {
        return totalPaidAsOnlineSettlement;
    }

    public void setTotalPaidAsOnlineSettlement(BigDecimal totalPaidAsOnlineSettlement) {
        this.totalPaidAsOnlineSettlement = totalPaidAsOnlineSettlement;
    }

    public BigDecimal getTotalPaidAsOnCall() {
        return totalPaidAsOnCall;
    }

    public void setTotalPaidAsOnCall(BigDecimal totalPaidAsOnCall) {
        this.totalPaidAsOnCall = totalPaidAsOnCall;
    }

    public BigDecimal getTotalPaidAsYouOweMe() {
        return totalPaidAsYouOweMe;
    }

    public void setTotalPaidAsYouOweMe(BigDecimal totalPaidAsYouOweMe) {
        this.totalPaidAsYouOweMe = totalPaidAsYouOweMe;
    }

    public BigDecimal getTotalPaidAsNone() {
        return totalPaidAsNone;
    }

    public void setTotalPaidAsNone(BigDecimal totalPaidAsNone) {
        this.totalPaidAsNone = totalPaidAsNone;
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

    public BigDecimal getValueAtPurchaseRate() {
        return valueAtPurchaseRate;
    }

    public void setValueAtPurchaseRate(BigDecimal valueAtPurchaseRate) {
        this.valueAtPurchaseRate = valueAtPurchaseRate;
    }

    public BigDecimal getValueAtCostRate() {
        return valueAtCostRate;
    }

    public void setValueAtCostRate(BigDecimal valueAtCostRate) {
        this.valueAtCostRate = valueAtCostRate;
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

    public BigDecimal getUnitsPerPack() {
        return unitsPerPack;
    }

    public void setUnitsPerPack(BigDecimal unitsPerPack) {
        this.unitsPerPack = unitsPerPack;
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

    public BigDecimal getFreeValueAtRetailRate() {
        return freeValueAtRetailRate;
    }

    public void setFreeValueAtRetailRate(BigDecimal freeValueAtRetailRate) {
        this.freeValueAtRetailRate = freeValueAtRetailRate;
    }

    public BigDecimal getFreeValueAtPurchaseRate() {
        return freeValueAtPurchaseRate;
    }

    public void setFreeValueAtPurchaseRate(BigDecimal freeValueAtPurchaseRate) {
        this.freeValueAtPurchaseRate = freeValueAtPurchaseRate;
    }

    public BigDecimal getFreeValueAtWholesaleRate() {
        return freeValueAtWholesaleRate;
    }

    public void setFreeValueAtWholesaleRate(BigDecimal freeValueAtWholesaleRate) {
        this.freeValueAtWholesaleRate = freeValueAtWholesaleRate;
    }

}
