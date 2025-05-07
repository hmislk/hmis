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

    // Gross and Net Totals
    private BigDecimal grossTotal = BigDecimal.ZERO;
    private BigDecimal netTotal = BigDecimal.ZERO;

// Return Totals
    private BigDecimal returnGrossTotal = BigDecimal.ZERO;
    private BigDecimal returnNetTotal = BigDecimal.ZERO;

// Return Quantity-based Totals (for clarity if needed separately)
    private BigDecimal returnQuantityTotal = BigDecimal.ZERO;
    private BigDecimal returnFreeQuantityTotal = BigDecimal.ZERO;

    // Discounts
    private BigDecimal billItemDiscountFromBill = BigDecimal.ZERO;
    private BigDecimal billItemDiscountForTheLine = BigDecimal.ZERO;
    private BigDecimal totalBillItemDiscount = BigDecimal.ZERO;

    // Taxes
    private BigDecimal billItemTaxFromBill = BigDecimal.ZERO;
    private BigDecimal billItemTaxForTheLine = BigDecimal.ZERO;
    private BigDecimal totalBillItemTax = BigDecimal.ZERO;

    // Expenses
    private BigDecimal billItemExpenseFromBill = BigDecimal.ZERO;
    private BigDecimal billItemExpenseForTheLine = BigDecimal.ZERO;
    private BigDecimal totalBillItemExpense = BigDecimal.ZERO;

    // Costs
    private BigDecimal billItemCostFromBill = BigDecimal.ZERO;
    private BigDecimal billItemCostForTheLine = BigDecimal.ZERO;
    private BigDecimal totalBillItemCost = BigDecimal.ZERO;

    // Percentages
    private BigDecimal discountPercentageFromBill = BigDecimal.ZERO;
    private BigDecimal discountPercentageForTheLine = BigDecimal.ZERO;
    private BigDecimal totalDiscountPercentage = BigDecimal.ZERO;

    private BigDecimal taxPercentageFromBill = BigDecimal.ZERO;
    private BigDecimal taxPercentageForTheLine = BigDecimal.ZERO;
    private BigDecimal totalTaxPercentage = BigDecimal.ZERO;

    private BigDecimal expensePercentageFromBill = BigDecimal.ZERO;
    private BigDecimal expensePercentageForTheLine = BigDecimal.ZERO;
    private BigDecimal totalExpensePercentage = BigDecimal.ZERO;

    private BigDecimal costPercentageFromBill = BigDecimal.ZERO;
    private BigDecimal costPercentageForTheLine = BigDecimal.ZERO;
    private BigDecimal totalCostPercentage = BigDecimal.ZERO;

    // Quantities and values
    private BigDecimal freeQuantity = BigDecimal.ZERO;
    private BigDecimal freeValue = BigDecimal.ZERO;
    private BigDecimal retailSaleValue = BigDecimal.ZERO;
    private BigDecimal wholesaleValue = BigDecimal.ZERO;
    private BigDecimal quantity = BigDecimal.ZERO;
    private BigDecimal totalQuantity = BigDecimal.ZERO;

    // Return quantities
    private BigDecimal returnQuantity = BigDecimal.ZERO;
    private BigDecimal returnFreeQuantity = BigDecimal.ZERO;
    private BigDecimal totalReturnQuantity = BigDecimal.ZERO;

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

    public BillItemFinanceDetails clone() {
        BillItemFinanceDetails cloned = new BillItemFinanceDetails();

        cloned.grossTotal = this.grossTotal;
        cloned.netTotal = this.netTotal;

        cloned.returnGrossTotal = this.returnGrossTotal;
        cloned.returnNetTotal = this.returnNetTotal;
        cloned.returnQuantityTotal = this.returnQuantityTotal;
        cloned.returnFreeQuantityTotal = this.returnFreeQuantityTotal;

        cloned.billItemDiscountFromBill = this.billItemDiscountFromBill;
        cloned.billItemDiscountForTheLine = this.billItemDiscountForTheLine;
        cloned.totalBillItemDiscount = this.totalBillItemDiscount;

        cloned.billItemTaxFromBill = this.billItemTaxFromBill;
        cloned.billItemTaxForTheLine = this.billItemTaxForTheLine;
        cloned.totalBillItemTax = this.totalBillItemTax;

        cloned.billItemExpenseFromBill = this.billItemExpenseFromBill;
        cloned.billItemExpenseForTheLine = this.billItemExpenseForTheLine;
        cloned.totalBillItemExpense = this.totalBillItemExpense;

        cloned.billItemCostFromBill = this.billItemCostFromBill;
        cloned.billItemCostForTheLine = this.billItemCostForTheLine;
        cloned.totalBillItemCost = this.totalBillItemCost;

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
        cloned.freeValue = this.freeValue;
        cloned.retailSaleValue = this.retailSaleValue;
        cloned.wholesaleValue = this.wholesaleValue;
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

        return cloned;
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

    public BigDecimal getBillItemDiscountFromBill() {
        return billItemDiscountFromBill;
    }

    public void setBillItemDiscountFromBill(BigDecimal billItemDiscountFromBill) {
        this.billItemDiscountFromBill = billItemDiscountFromBill;
    }

    public BigDecimal getBillItemDiscountForTheLine() {
        return billItemDiscountForTheLine;
    }

    public void setBillItemDiscountForTheLine(BigDecimal billItemDiscountForTheLine) {
        this.billItemDiscountForTheLine = billItemDiscountForTheLine;
    }

    public BigDecimal getTotalBillItemDiscount() {
        return totalBillItemDiscount;
    }

    public void setTotalBillItemDiscount(BigDecimal totalBillItemDiscount) {
        this.totalBillItemDiscount = totalBillItemDiscount;
    }

    public BigDecimal getBillItemTaxFromBill() {
        return billItemTaxFromBill;
    }

    public void setBillItemTaxFromBill(BigDecimal billItemTaxFromBill) {
        this.billItemTaxFromBill = billItemTaxFromBill;
    }

    public BigDecimal getBillItemTaxForTheLine() {
        return billItemTaxForTheLine;
    }

    public void setBillItemTaxForTheLine(BigDecimal billItemTaxForTheLine) {
        this.billItemTaxForTheLine = billItemTaxForTheLine;
    }

    public BigDecimal getTotalBillItemTax() {
        return totalBillItemTax;
    }

    public void setTotalBillItemTax(BigDecimal totalBillItemTax) {
        this.totalBillItemTax = totalBillItemTax;
    }

    public BigDecimal getBillItemExpenseFromBill() {
        return billItemExpenseFromBill;
    }

    public void setBillItemExpenseFromBill(BigDecimal billItemExpenseFromBill) {
        this.billItemExpenseFromBill = billItemExpenseFromBill;
    }

    public BigDecimal getBillItemExpenseForTheLine() {
        return billItemExpenseForTheLine;
    }

    public void setBillItemExpenseForTheLine(BigDecimal billItemExpenseForTheLine) {
        this.billItemExpenseForTheLine = billItemExpenseForTheLine;
    }

    public BigDecimal getTotalBillItemExpense() {
        return totalBillItemExpense;
    }

    public void setTotalBillItemExpense(BigDecimal totalBillItemExpense) {
        this.totalBillItemExpense = totalBillItemExpense;
    }

    public BigDecimal getBillItemCostFromBill() {
        return billItemCostFromBill;
    }

    public void setBillItemCostFromBill(BigDecimal billItemCostFromBill) {
        this.billItemCostFromBill = billItemCostFromBill;
    }

    public BigDecimal getBillItemCostForTheLine() {
        return billItemCostForTheLine;
    }

    public void setBillItemCostForTheLine(BigDecimal billItemCostForTheLine) {
        this.billItemCostForTheLine = billItemCostForTheLine;
    }

    public BigDecimal getTotalBillItemCost() {
        return totalBillItemCost;
    }

    public void setTotalBillItemCost(BigDecimal totalBillItemCost) {
        this.totalBillItemCost = totalBillItemCost;
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

    public BigDecimal getFreeValue() {
        return freeValue;
    }

    public void setFreeValue(BigDecimal freeValue) {
        this.freeValue = freeValue;
    }

    public BigDecimal getRetailSaleValue() {
        return retailSaleValue;
    }

    public void setRetailSaleValue(BigDecimal retailSaleValue) {
        this.retailSaleValue = retailSaleValue;
    }

    public BigDecimal getWholesaleValue() {
        return wholesaleValue;
    }

    public void setWholesaleValue(BigDecimal wholesaleValue) {
        this.wholesaleValue = wholesaleValue;
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
    
    

}
