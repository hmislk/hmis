package com.divudi.core.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import javax.persistence.CascadeType;
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

    @OneToOne(mappedBy = "billFinanceDetails", cascade = CascadeType.ALL)
    private Bill bill;

    // ------------------ DISCOUNTS ------------------
    // Discount applied directly to the Bill (not tied to specific lines)
    private BigDecimal billDiscount = BigDecimal.ZERO;

    // Total of all line-level discounts (sum of discounts on individual BillItems)
    private BigDecimal totalOfBillLineDiscounts = BigDecimal.ZERO;

    // Total discount (bill-level + all line-level)
    private BigDecimal totalDiscount = BigDecimal.ZERO;

    // ------------------ EXPENSES ------------------
    // Expense applied to the Bill itself (e.g., delivery fee, service charge)
    private BigDecimal billExpense = BigDecimal.ZERO;

    // Total of all expenses from individual BillItems
    private BigDecimal totalOfBillLineExpense = BigDecimal.ZERO;

    // Total expense (bill-level + all line-level)
    private BigDecimal totalExpense = BigDecimal.ZERO;

    // ------------------ COST ------------------
    // Cost incurred for the Bill as a whole (not specific to lines)
    private BigDecimal billCostValue = BigDecimal.ZERO;

    // Sum of cost values from each BillItem
    private BigDecimal billOfBillLineCostValue = BigDecimal.ZERO;

    // Total cost (bill-level + all line-level)
    private BigDecimal totalCostValue = BigDecimal.ZERO;

    // ------------------ TAXES ------------------
    // Tax applied to the whole Bill (e.g., VAT)
    private BigDecimal billTaxValue = BigDecimal.ZERO;

    // Total of tax amounts from all BillItems
    private BigDecimal totalOfBillLineTaxValue = BigDecimal.ZERO;

    // Total tax (bill-level + all line-level)
    private BigDecimal totalTaxValue = BigDecimal.ZERO;

    // ------------------ VALUES ------------------
    // Total purchase value for all BillItems (excluding discounts/taxes)
    private BigDecimal totalPurchaseValue = BigDecimal.ZERO;

    // Estimated value of items given free of charge
    private BigDecimal totalOfFreeItemValues = BigDecimal.ZERO;

    // Expected total if all items sold at retail rate
    private BigDecimal totalRetailSaleValue = BigDecimal.ZERO;

    // Expected total if all items sold at wholesale rate
    private BigDecimal totalWholesaleValue = BigDecimal.ZERO;

    // ------------------ QUANTITIES ------------------
    // Total quantity of all BillItems (excluding free)
    private BigDecimal totalQuantity = BigDecimal.ZERO;

    // Total of free quantities across BillItems
    private BigDecimal totalFreeQuantity = BigDecimal.ZERO;

    // Quantity in atomic units (e.g., tablets instead of boxes)
    private BigDecimal totalQuantityInAtomicUnitOfMeasurement = BigDecimal.ZERO;

    // Free quantity in atomic units
    private BigDecimal totalFreeQuantityInAtomicUnitOfMeasurement = BigDecimal.ZERO;

    private BigDecimal lineGrossTotal = BigDecimal.ZERO;
    private BigDecimal billGrossTotal = BigDecimal.ZERO;
    private BigDecimal grossTotal = BigDecimal.ZERO;

    // Value after deductions
    private BigDecimal lineNetTotal = BigDecimal.ZERO;
    private BigDecimal billNetTotal = BigDecimal.ZERO;
    private BigDecimal netTotal = BigDecimal.ZERO;

    // Getters and setters omitted for brevity
    // Payment method totals
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
        clone.setTotalOfBillLineDiscounts(this.totalOfBillLineDiscounts);
        clone.setTotalDiscount(this.totalDiscount);

        // ------------------ EXPENSES ------------------
        clone.setBillExpense(this.billExpense);
        clone.setTotalOfBillLineExpense(this.totalOfBillLineExpense);
        clone.setTotalExpense(this.totalExpense);

        // ------------------ COST ------------------
        clone.setBillCostValue(this.billCostValue);
        clone.setBillOfBillLineCostValue(this.billOfBillLineCostValue);
        clone.setTotalCostValue(this.totalCostValue);

        // ------------------ TAXES ------------------
        clone.setBillTaxValue(this.billTaxValue);
        clone.setTotalOfBillLineTaxValue(this.totalOfBillLineTaxValue);
        clone.setTotalTaxValue(this.totalTaxValue);

        // ------------------ VALUES ------------------
        clone.setTotalPurchaseValue(this.totalPurchaseValue);
        clone.setTotalOfFreeItemValues(this.totalOfFreeItemValues);
        clone.setTotalRetailSaleValue(this.totalRetailSaleValue);
        clone.setTotalWholesaleValue(this.totalWholesaleValue);

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

        // ------------------ PAYMENT METHODS ------------------
        clone.setTotalPaidAsCash(this.totalPaidAsCash);
        clone.setTotalPaidAsCard(this.totalPaidAsCard);
        clone.setTotalPaidAsMultiplePaymentMethods(this.totalPaidAsMultiplePaymentMethods);
        clone.setTotalPaidAsStaff(this.totalPaidAsStaff);
        clone.setTotalPaidAsCredit(this.totalPaidAsCredit);
        clone.setTotalPaidAsStaffWelfare(this.totalPaidAsStaffWelfare);
        clone.setTotalPaidAsVoucher(this.totalPaidAsVoucher);
        clone.setTotalPaidAsIOU(this.totalPaidAsIOU);
        clone.setTotalPaidAsAgent(this.totalPaidAsAgent);
        clone.setTotalPaidAsCheque(this.totalPaidAsCheque);
        clone.setTotalPaidAsSlip(this.totalPaidAsSlip);
        clone.setTotalPaidAsEwallet(this.totalPaidAsEwallet);
        clone.setTotalPaidAsPatientDeposit(this.totalPaidAsPatientDeposit);
        clone.setTotalPaidAsPatientPoints(this.totalPaidAsPatientPoints);
        clone.setTotalPaidAsOnlineSettlement(this.totalPaidAsOnlineSettlement);
        clone.setTotalPaidAsOnCall(this.totalPaidAsOnCall);
        clone.setTotalPaidAsYouOweMe(this.totalPaidAsYouOweMe);
        clone.setTotalPaidAsNone(this.totalPaidAsNone);

        // Note: skip ID, createdAt, etc. – those should be managed by persistence layer
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

    public BigDecimal getTotalOfBillLineDiscounts() {
        return totalOfBillLineDiscounts;
    }

    public void setTotalOfBillLineDiscounts(BigDecimal totalOfBillLineDiscounts) {
        this.totalOfBillLineDiscounts = totalOfBillLineDiscounts;
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

    public BigDecimal getTotalOfFreeItemValues() {
        return totalOfFreeItemValues;
    }

    public void setTotalOfFreeItemValues(BigDecimal totalOfFreeItemValues) {
        this.totalOfFreeItemValues = totalOfFreeItemValues;
    }

    public BigDecimal getTotalCostValue() {
        return totalCostValue;
    }

    public void setTotalCostValue(BigDecimal totalCostValue) {
        this.totalCostValue = totalCostValue;
    }

    public BigDecimal getTotalPurchaseValue() {
        return totalPurchaseValue;
    }

    public void setTotalPurchaseValue(BigDecimal totalPurchaseValue) {
        this.totalPurchaseValue = totalPurchaseValue;
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

    public BigDecimal getTotalWholesaleValue() {
        return totalWholesaleValue;
    }

    public void setTotalWholesaleValue(BigDecimal totalWholesaleValue) {
        this.totalWholesaleValue = totalWholesaleValue;
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

    public BigDecimal getBillExpense() {
        return billExpense;
    }

    public void setBillExpense(BigDecimal billExpense) {
        this.billExpense = billExpense;
    }

    public BigDecimal getTotalOfBillLineExpense() {
        return totalOfBillLineExpense;
    }

    public void setTotalOfBillLineExpense(BigDecimal totalOfBillLineExpense) {
        this.totalOfBillLineExpense = totalOfBillLineExpense;
    }

    public BigDecimal getBillCostValue() {
        return billCostValue;
    }

    public void setBillCostValue(BigDecimal billCostValue) {
        this.billCostValue = billCostValue;
    }

    public BigDecimal getBillOfBillLineCostValue() {
        return billOfBillLineCostValue;
    }

    public void setBillOfBillLineCostValue(BigDecimal billOfBillLineCostValue) {
        this.billOfBillLineCostValue = billOfBillLineCostValue;
    }

    public BigDecimal getBillTaxValue() {
        return billTaxValue;
    }

    public void setBillTaxValue(BigDecimal billTaxValue) {
        this.billTaxValue = billTaxValue;
    }

    public BigDecimal getTotalOfBillLineTaxValue() {
        return totalOfBillLineTaxValue;
    }

    public void setTotalOfBillLineTaxValue(BigDecimal totalOfBillLineTaxValue) {
        this.totalOfBillLineTaxValue = totalOfBillLineTaxValue;
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

}
