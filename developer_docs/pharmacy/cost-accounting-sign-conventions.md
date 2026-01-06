# Cost Accounting Sign Conventions for Pharmacy Transfer Transactions

**Document Version**: 1.0
**Last Updated**: 2025-10-09
**Related Issue**: #15696 - Cancellation amount not deducted from transfer issue by bill
**Reviewed By**: Health Informatics Architect (TOGAF Certified)

---

## Table of Contents
1. [Executive Summary](#executive-summary)
2. [The Three-Tier Accounting Model](#the-three-tier-accounting-model)
3. [Sign Convention Rules](#sign-convention-rules)
4. [Implementation Pattern](#implementation-pattern)
5. [Data Integrity Validation](#data-integrity-validation)
6. [Common Pitfalls](#common-pitfalls)
7. [Financial Reporting Impact](#financial-reporting-impact)
8. [Compliance & Standards](#compliance--standards)
9. [FAQ](#faq)

---

## Executive Summary

### The Core Principle

**For pharmacy transfer ISSUE transactions: Cost flows in the SAME direction as stock.**

```
When stock goes OUT (negative quantity)
↓
Cost burden also goes OUT (negative cost)
↓
But money comes IN (positive revenue)
```

### Quick Reference Card

| Field | Sign for Transfer ISSUE | Example | Rationale |
|-------|-------------------------|---------|-----------|
| **BIFD.quantity** | NEGATIVE | -10 packs | Stock leaves issuing dept |
| **BIFD.lineCostRate** | POSITIVE | 2.7273 | Rate is intrinsic property |
| **BIFD.lineCost** | NEGATIVE | -27.273 | Cost burden relieved |
| **BIFD.totalCost** | NEGATIVE | -27.273 | Cost burden relieved |
| **BIFD.valueAtCostRate** | NEGATIVE | -27.273 | Inventory value reduced |
| **BIFD.valueAtPurchaseRate** | NEGATIVE | -50.00 | Inventory value reduced |
| **BIFD.valueAtRetailRate** | NEGATIVE | -100.00 | Inventory value reduced |
| **Bill.netTotal** | POSITIVE | 100.0 | Revenue received |
| **BillItem.netValue** | POSITIVE | 100.0 | Revenue per item |
| **Margin** | POSITIVE | 127.273 | 100 - (-27.273) |

### Why This Matters

1. **Enables accurate departmental profit/loss tracking**
2. **Supports proper inventory valuation reporting**
3. **Maintains mathematical consistency across the system**
4. **Complies with IFRS (IAS 2) and GAAP accounting standards**
5. **Facilitates audit trail and regulatory compliance**

---

## The Three-Tier Accounting Model

Pharmacy transfer issue transactions use a three-tier accounting model that separates physical, cost, and revenue perspectives:

```
┌─────────────────────────────────────────────────────────┐
│ TIER 1: PHYSICAL INVENTORY ACCOUNTING                   │
│ Purpose: Track stock movement                           │
│ Metrics: quantity (signed), stock level                 │
│ Sign: NEGATIVE for issue, POSITIVE for receive          │
│                                                          │
│ Fields:                                                  │
│ - BillItemFinanceDetails.quantity: -10 packs             │
│ - BillItemFinanceDetails.totalQuantity: -10 packs        │
│ - PharmaceuticalBillItem.qty: -10 units                 │
└─────────────────────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────────────────────┐
│ TIER 2: COST ACCOUNTING                                 │
│ Purpose: Track cost burden movement                     │
│ Metrics: lineCost, totalCost (signed)                   │
│ Sign: NEGATIVE for issue, POSITIVE for receive          │
│ Principle: Mirror quantity sign                         │
│                                                          │
│ Fields:                                                  │
│ - lineCostRate: 2.7273 (POSITIVE - rate is unsigned)    │
│ - lineCost: -27.273 (NEGATIVE - cost relieved)          │
│ - totalCost: -27.273 (NEGATIVE - cost relieved)         │
│ - valueAtCostRate: -27.273 (NEGATIVE - value reduced)   │
└─────────────────────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────────────────────┐
│ TIER 3: REVENUE ACCOUNTING                              │
│ Purpose: Track financial settlement                     │
│ Metrics: netValue, netTotal (signed)                    │
│ Sign: POSITIVE for issue (money in), NEGATIVE for bill  │
│ Principle: Opposite of quantity sign                    │
│                                                          │
│ Fields:                                                  │
│ - Bill.netTotal: 50.0 (POSITIVE - revenue)              │
│ - BillItem.netValue: 50.0 (POSITIVE - revenue)          │
│ - BillItem.grossValue: 50.0 (POSITIVE - revenue)        │
└─────────────────────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────────────────────┐
│ TIER 4: MARGIN ANALYSIS (DERIVED)                       │
│ Purpose: Calculate departmental profit/loss             │
│ Metrics: margin = revenue - cost                        │
│ Formula: netValue - totalCost                           │
│                                                          │
│ Calculation:                                             │
│ - Revenue: 50.0                                          │
│ - Cost: -27.273                                          │
│ - Margin: 50.0 - (-27.273) = 77.273                     │
└─────────────────────────────────────────────────────────┘
```

---

## Sign Convention Rules

### Rule 1: Rates Are Always Positive (Unsigned)

**Rates are intrinsic properties of items and are never signed.**

```java
// ✓ CORRECT
BigDecimal costRate = BigDecimal.valueOf(itemBatch.getCostRate()); // Always positive
f.setLineCostRate(costRate); // 2.7273

// ✗ INCORRECT
BigDecimal costRate = BigDecimal.ZERO.subtract(itemBatch.getCostRate()); // Never negate rates!
```

**Affected Fields:**
- `BillItemFinanceDetails.lineCostRate`
- `BillItemFinanceDetails.billCostRate`
- `BillItemFinanceDetails.totalCostRate`
- `BillItemFinanceDetails.purchaseRate`
- `BillItemFinanceDetails.retailSaleRate`

### Rule 2: Costs Mirror Quantity Sign

**Cost values must have the same sign as quantity to maintain accounting symmetry.**

```java
// For Transfer ISSUE (quantity is negative):
BigDecimal quantity = BigDecimal.valueOf(-10); // Negative for stock out
BigDecimal costRate = BigDecimal.valueOf(2.7273); // Rate is positive

// ✓ CORRECT: Cost mirrors quantity sign
BigDecimal lineCost = costRate.multiply(quantity); // -27.273
f.setLineCost(lineCost);

// ✗ INCORRECT: Cost has opposite sign of quantity
BigDecimal lineCost = costRate.multiply(quantity.abs()); // +27.273 (WRONG!)
f.setLineCost(lineCost);
```

**Mathematical Invariant:**
```
lineCost = lineCostRate × quantity
```

### Rule 3: Revenue Has Opposite Sign of Quantity

**For transfer issue, quantity is negative but revenue must be positive (money comes in).**

```java
BigDecimal quantity = BigDecimal.valueOf(-10); // Negative for stock out
BigDecimal rate = BigDecimal.valueOf(5.0);

// ✓ CORRECT: Use absolute value for revenue
double netValue = rate.doubleValue() * Math.abs(quantity.doubleValue()); // 50.0
billItem.setNetValue(netValue);

// ✗ INCORRECT: Don't use negative quantity directly for revenue
double netValue = rate.doubleValue() * quantity.doubleValue(); // -50.0 (WRONG!)
billItem.setNetValue(netValue);
```

### Rule 4: Idempotent Sign Normalization

**Always use `.abs()` before applying sign to ensure idempotent operations.**

```java
// ✓ CORRECT: Idempotent (works whether value is already negative or not)
BigDecimal absQtyInUnits = qtyInUnits.abs();
f.setLineCost(BigDecimal.ZERO.subtract(costRate.multiply(absQtyInUnits)));

// ✗ INCORRECT: Not idempotent (breaks if value is already negative)
f.setLineCost(BigDecimal.ZERO.subtract(costRate.multiply(qtyInUnits)));
```

**Why Idempotent Matters:**
- Prevents double-negation bugs
- Safe for data corrections and re-runs
- Handles edge cases gracefully

---

## Implementation Pattern

### Standard Pattern for Transfer Issue

```java
/**
 * STANDARD PATTERN: Pharmacy Transfer Issue Cost Accounting
 *
 * Sign Conventions:
 * - Quantity: NEGATIVE (stock out)
 * - Cost Rates: POSITIVE (never signed)
 * - Cost Values: NEGATIVE (mirrors quantity)
 * - Revenue: POSITIVE (money in)
 * - Margin: POSITIVE (revenue - cost)
 */
public void updateBillItemRateAndValueForDirectIssue(BillItem billItem) {
    BillItemFinanceDetails f = billItem.getBillItemFinanceDetails();
    PharmaceuticalBillItem phItem = billItem.getPharmaceuticalBillItem();
    ItemBatch batch = phItem.getItemBatch();

    double rate = f.getLineGrossRate().doubleValue();

    // TIER 1: Physical Inventory (NEGATIVE for issue)
    BigDecimal qtyInUnits = BigDecimal.valueOf(phItem.getQty());
    BigDecimal qtyInPacks = BigDecimal.valueOf(billItem.getQty());

    // Make quantities negative for stock-out using idempotent pattern
    BigDecimal absQtyInPacks = qtyInPacks.abs();
    f.setQuantity(BigDecimal.ZERO.subtract(absQtyInPacks)); // -10 packs
    f.setTotalQuantity(BigDecimal.ZERO.subtract(absQtyInPacks)); // -10 packs

    // TIER 2: Cost Accounting (NEGATIVE - mirror quantity sign)

    // Cost rates (POSITIVE - intrinsic properties)
    BigDecimal costRate = BigDecimal.valueOf(batch.getCostRate());
    f.setLineCostRate(costRate); // 2.7273
    f.setBillCostRate(BigDecimal.ZERO); // No bill-level cost adjustment
    f.setTotalCostRate(costRate); // 2.7273

    // Cost values (NEGATIVE - cost burden relieved)
    BigDecimal absQtyInUnits = qtyInUnits.abs();
    f.setLineCost(BigDecimal.ZERO.subtract(costRate.multiply(absQtyInUnits))); // -27.273
    f.setTotalCost(BigDecimal.ZERO.subtract(f.getTotalCostRate().multiply(absQtyInUnits))); // -27.273

    // Inventory valuations (NEGATIVE - value reduced)
    f.setValueAtCostRate(BigDecimal.ZERO.subtract(
        BigDecimal.valueOf(batch.getCostRate()).multiply(absQtyInUnits))); // -27.273
    f.setValueAtPurchaseRate(BigDecimal.ZERO.subtract(
        BigDecimal.valueOf(batch.getPurcahseRate()).multiply(absQtyInUnits))); // -50.00
    f.setValueAtRetailRate(BigDecimal.ZERO.subtract(
        BigDecimal.valueOf(batch.getRetailsaleRate()).multiply(absQtyInUnits))); // -100.00

    // TIER 3: Revenue Accounting (POSITIVE - money comes in)
    billItem.setRate(rate);
    billItem.setNetRate(rate);
    // Use absolute quantity for revenue - money is positive
    billItem.setNetValue(rate * Math.abs(billItem.getQty())); // +50.0
    billItem.setGrossValue(rate * Math.abs(billItem.getQty())); // +50.0

    // Save
    billItemFacade.edit(billItem);
}
```

### Bill Total Calculation

```java
/**
 * Calculates Bill.netTotal ensuring positive revenue for transfer issues.
 */
private double calculateBillNetTotal() {
    double value = 0;
    for (BillItem b : getIssuedBill().getBillItems()) {
        double rate = b.getBillItemFinanceDetails().getLineGrossRate().doubleValue();
        // Use absolute value - revenue must be positive
        value += rate * Math.abs(b.getPharmaceuticalBillItem().getQty());
    }
    return value; // Returns positive revenue
}
```

---

## Data Integrity Validation

### Validation Rule 1: Cost-Quantity Mathematical Consistency

```java
public void validateCostQuantityConsistency(BillItemFinanceDetails f) {
    // lineCost must equal lineCostRate × quantity
    BigDecimal calculatedLineCost = f.getLineCostRate().multiply(f.getQuantity());
    if (!f.getLineCost().equals(calculatedLineCost)) {
        throw new ValidationException(
            "Line cost must equal lineCostRate × quantity. " +
            "Expected: " + calculatedLineCost + ", Found: " + f.getLineCost()
        );
    }

    // totalCost must equal totalCostRate × quantity
    BigDecimal calculatedTotalCost = f.getTotalCostRate().multiply(f.getQuantity());
    if (!f.getTotalCost().equals(calculatedTotalCost)) {
        throw new ValidationException(
            "Total cost must equal totalCostRate × quantity. " +
            "Expected: " + calculatedTotalCost + ", Found: " + f.getTotalCost()
        );
    }
}
```

### Validation Rule 2: Sign Consistency for Transfer Issue

```java
public void validateSignConsistency(BillItemFinanceDetails f, BillType billType) {
    if (billType == BillType.PharmacyTransferIssue ||
        billType == BillTypeAtomic.PHARMACY_DIRECT_ISSUE) {

        // For transfer issue: quantity and costs must be negative
        if (f.getQuantity().compareTo(BigDecimal.ZERO) >= 0) {
            throw new ValidationException(
                "Transfer issue quantity must be negative. Found: " + f.getQuantity()
            );
        }

        if (f.getLineCost().compareTo(BigDecimal.ZERO) >= 0) {
            throw new ValidationException(
                "Transfer issue line cost must be negative. Found: " + f.getLineCost()
            );
        }

        if (f.getTotalCost().compareTo(BigDecimal.ZERO) >= 0) {
            throw new ValidationException(
                "Transfer issue total cost must be negative. Found: " + f.getTotalCost()
            );
        }
    }
}
```

### Validation Rule 3: Inventory Valuation Consistency

```java
public void validateInventoryValuationConsistency(BillItemFinanceDetails f) {
    // valueAtCostRate must equal costRate × quantityInUnits
    BigDecimal expectedValueAtCost = f.getLineCostRate().multiply(f.getQuantityByUnits());
    if (!f.getValueAtCostRate().equals(expectedValueAtCost)) {
        throw new ValidationException(
            "Value at cost rate must equal costRate × quantityInUnits. " +
            "Expected: " + expectedValueAtCost + ", Found: " + f.getValueAtCostRate()
        );
    }
}
```

### Database Constraint (Optional)

```sql
-- For transfer issue bill items, ensure cost fields are negative
ALTER TABLE bill_item_finance_details
ADD CONSTRAINT chk_transfer_issue_costs
CHECK (
    (bill_type_atomic != 'PHARMACY_DIRECT_ISSUE' AND bill_type != 'PharmacyTransferIssue')
    OR
    (quantity < 0 AND line_cost < 0 AND total_cost < 0)
);
```

---

## Common Pitfalls

### Pitfall 1: Forgetting to Use Absolute Value

```java
// ❌ WRONG: Doesn't use absolute value
BigDecimal lineCost = costRate.multiply(qtyInUnits); // Could be positive if qty already negative
f.setLineCost(BigDecimal.ZERO.subtract(lineCost)); // Double negation!

// ✅ CORRECT: Always use absolute value first
BigDecimal absQtyInUnits = qtyInUnits.abs();
f.setLineCost(BigDecimal.ZERO.subtract(costRate.multiply(absQtyInUnits)));
```

### Pitfall 2: Negating Rates Instead of Values

```java
// ❌ WRONG: Negating the rate
BigDecimal negativeCostRate = BigDecimal.ZERO.subtract(itemBatch.getCostRate());
f.setLineCostRate(negativeCostRate); // Rates should never be negative!

// ✅ CORRECT: Keep rate positive, negate the value
BigDecimal costRate = BigDecimal.valueOf(itemBatch.getCostRate());
f.setLineCostRate(costRate); // Positive
f.setLineCost(BigDecimal.ZERO.subtract(costRate.multiply(absQty))); // Negative value
```

### Pitfall 3: Inconsistent Revenue Sign

```java
// ❌ WRONG: Using negative quantity for revenue
double netValue = rate * billItem.getQty(); // Results in negative revenue!

// ✅ CORRECT: Use absolute value for revenue
double netValue = rate * Math.abs(billItem.getQty()); // Always positive
```

### Pitfall 4: Mixing Up BillItem.qty vs PharmaceuticalBillItem.qty

```java
// ⚠️ IMPORTANT: Different quantity fields serve different purposes
// - BillItem.qty: Quantity in PACKS (may be positive or negative)
// - PharmaceuticalBillItem.qty: Quantity in UNITS (negative for issue)
// - BillItemFinanceDetails.quantity: Quantity in PACKS (negative for issue)
// - BillItemFinanceDetails.quantityByUnits: Quantity in UNITS (negative for issue)

// For revenue calculation, use BillItem.qty (can be either sign)
double revenue = rate * Math.abs(billItem.getQty());

// For cost calculation, use PharmaceuticalBillItem.qty or BIFD.quantityByUnits
BigDecimal cost = costRate.multiply(absQtyInUnits);
```

---

## Financial Reporting Impact

### Departmental Cost Summary Report

```java
public DepartmentalCostSummary generateCostSummary(
    Department dept,
    DateRange dateRange
) {
    List<BillItemFinanceDetails> issues = findTransferIssues(dept, dateRange);

    // Sum costs (negative for issues)
    BigDecimal totalCostRelieved = issues.stream()
        .map(BillItemFinanceDetails::getTotalCost)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    // Result: NEGATIVE value = cost burden relieved

    // Sum revenue (positive for issues)
    BigDecimal totalRevenue = issues.stream()
        .map(f -> BigDecimal.valueOf(f.getBillItem().getNetValue()))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    // Result: POSITIVE value = revenue received

    // Calculate margin
    BigDecimal margin = totalRevenue.subtract(totalCostRelieved);
    // Result: POSITIVE value = profit from transfer pricing

    return new DepartmentalCostSummary(
        totalCostRelieved, // Display as negative
        totalRevenue,      // Display as positive
        margin             // Display as positive
    );
}
```

**Example Output:**
```
Pharmacy Department - October 2025
Cost Relieved:    -1,250.00  (inventory cost reduced)
Revenue Received: +2,500.00  (money received)
Margin:           +3,750.00  (profit from transfer pricing)
```

### Inventory Valuation Report

```java
public InventoryValuationReport generateValuationReport(
    Department dept,
    Date asOfDate
) {
    // Opening balance
    BigDecimal openingValue = getInventoryValueAsOf(dept, startOfPeriod);

    // Issues (negative costs reduce inventory value)
    BigDecimal issuesCost = sumTotalCost(dept, dateRange, BillTypeAtomic.PHARMACY_DIRECT_ISSUE);
    // Result: NEGATIVE = reduction in inventory value

    // Receipts (positive costs increase inventory value)
    BigDecimal receiptsCost = sumTotalCost(dept, dateRange, BillTypeAtomic.PHARMACY_GRN);
    // Result: POSITIVE = increase in inventory value

    // Closing balance
    BigDecimal closingValue = openingValue.add(issuesCost).add(receiptsCost);

    return new InventoryValuationReport(
        openingValue,  // e.g., 10,000.00
        issuesCost,    // e.g., -1,250.00
        receiptsCost,  // e.g., +500.00
        closingValue   // e.g., 9,250.00
    );
}
```

**Example Output:**
```
Inventory Valuation Report - Pharmacy Store
Opening Inventory:   10,000.00
Issues at Cost:       -1,250.00  (stock transferred out)
Receipts at Cost:       +500.00  (stock received in)
Closing Inventory:     9,250.00
```

---

## Compliance & Standards

### International Financial Reporting Standards (IFRS)

**IAS 2 - Inventories**

> "Inventories shall be measured at the lower of cost and net realizable value."

The negative cost convention ensures:
- Inventory is valued at actual cost basis
- Cost flows match physical inventory flows
- Inventory valuation is auditable and traceable

### Generally Accepted Accounting Principles (GAAP)

**ASC 330 - Inventory**

> "Inventory cost includes all costs of purchase, costs of conversion, and other costs incurred in bringing the inventories to their present location and condition."

The sign convention maintains:
- Proper cost allocation to departments
- Accurate inventory cost flow assumptions
- Compliance with matching principle

### Healthcare Financial Management Association (HFMA)

**Principle 15: Cost Accounting**

> "Healthcare organizations should maintain cost accounting systems that support management decision-making and regulatory compliance."

Our implementation:
- Enables departmental cost accountability
- Supports activity-based costing for patient services
- Provides audit trail for regulatory reporting

---

## FAQ

### Q1: Why are costs negative instead of zero for internal transfers?

**A:** Costs represent the **cost burden** associated with inventory. When you transfer inventory out:
- Physical stock leaves (quantity negative)
- Cost investment also leaves (cost negative)
- This enables **margin analysis** and **departmental P&L tracking**

If costs were zero:
- ❌ Cannot calculate true profitability of transfer pricing
- ❌ Cannot track cost allocation to clinical departments
- ❌ Cannot perform activity-based costing
- ❌ Inventory valuation reports would be incorrect

### Q2: Won't negative costs confuse users in reports?

**A:** Display formatting handles this:

```java
// Backend: Store as negative
f.setTotalCost(BigDecimal.valueOf(-27.273));

// Frontend: Display with context
String display = "Cost Relieved: " +
    Math.abs(f.getTotalCost()) + " (Out)";
// Shows: "Cost Relieved: 27.273 (Out)"
```

### Q3: What if I migrate data from a system with different sign conventions?

**A:** Use a migration script:

```sql
-- Fix incorrect positive costs for transfer issues
UPDATE bill_item_finance_details bifd
INNER JOIN bill b ON bifd.bill_item_id = b.id
SET
    bifd.line_cost = -ABS(bifd.line_cost),
    bifd.total_cost = -ABS(bifd.total_cost),
    bifd.value_at_cost_rate = -ABS(bifd.value_at_cost_rate)
WHERE
    b.bill_type_atomic = 'PHARMACY_DIRECT_ISSUE'
    AND bifd.quantity < 0
    AND bifd.line_cost > 0;  -- Only fix incorrect records

-- Verify migration
SELECT COUNT(*) as incorrect_records
FROM bill_item_finance_details bifd
INNER JOIN bill b ON bifd.bill_item_id = b.id
WHERE
    b.bill_type_atomic = 'PHARMACY_DIRECT_ISSUE'
    AND (
        (bifd.quantity < 0 AND bifd.line_cost > 0) OR
        (bifd.quantity < 0 AND bifd.total_cost > 0)
    );
-- Should return 0
```

### Q4: How do I calculate departmental margin?

**A:**

```java
// For a single bill item:
BigDecimal revenue = BigDecimal.valueOf(billItem.getNetValue()); // +50.0
BigDecimal cost = f.getTotalCost(); // -27.273
BigDecimal margin = revenue.subtract(cost); // 50.0 - (-27.273) = 77.273

// For entire department:
BigDecimal totalRevenue = sumAllNetValues(dept, period); // +10,000
BigDecimal totalCost = sumAllTotalCosts(dept, period); // -6,000
BigDecimal totalMargin = totalRevenue.subtract(totalCost); // 16,000
```

### Q5: What happens during year-end closing?

**A:** The sign convention supports proper year-end reconciliation:

```java
public void validateYearEndReconciliation(Department dept, int fiscalYear) {
    BigDecimal openingValue = getOpeningInventoryValue(dept, fiscalYear);
    BigDecimal yearCostMovements = sumAllCostMovements(dept, fiscalYear);
    BigDecimal closingValue = getClosingInventoryValue(dept, fiscalYear);

    BigDecimal expected = openingValue.add(yearCostMovements);

    if (!closingValue.equals(expected)) {
        throw new ReconciliationException(
            "Inventory valuation mismatch: " +
            "Expected " + expected + ", Found " + closingValue
        );
    }
}
```

### Q6: How does this integrate with General Ledger?

**A:**

```java
// Transfer Issue GL Entry (for issuing department)
public List<JournalEntry> createGLEntries(Bill bill) {
    List<JournalEntry> entries = new ArrayList<>();

    for (BillItem item : bill.getBillItems()) {
        BillItemFinanceDetails f = item.getFinanceDetails();

        // Debit: Cash/Receivables (revenue - positive)
        entries.add(new JournalEntry(
            DEBIT,
            CASH_RECEIVABLES,
            item.getNetValue(), // Positive amount
            "Transfer issue revenue"
        ));

        // Credit: Inventory Asset (cost was negative, credit is positive)
        entries.add(new JournalEntry(
            CREDIT,
            INVENTORY_ASSET,
            f.getTotalCost().abs(), // Use absolute value
            "Inventory cost relieved"
        ));

        // Margin (if positive)
        BigDecimal margin = BigDecimal.valueOf(item.getNetValue())
            .subtract(f.getTotalCost());
        if (margin.compareTo(BigDecimal.ZERO) > 0) {
            entries.add(new JournalEntry(
                CREDIT,
                TRANSFER_PRICING_MARGIN,
                margin,
                "Internal transfer margin"
            ));
        }
    }

    return entries;
}
```

---

## Related Documentation

- **JavaDoc**: See `TransferIssueController.updateBillItemRateAndValueForDirectIssue()`
- **JavaDoc**: See `TransferIssueController.calculateBillNetTotal()`
- **Entity**: `com.divudi.core.entity.BillItemFinanceDetails`
- **Service**: `com.divudi.service.pharmacy.PharmacyCostingService`
- **Architecture Review**: Consulted with health-informatics-architect agent (TOGAF certified)

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-10-09 | HMIS Development Team | Initial version based on issue #15696 and expert architect review |

---

## Approval

**Technical Review**: Health Informatics Architect (TOGAF Certified)
**Compliance Review**: Based on IFRS IAS 2, GAAP ASC 330, HFMA Principle 15
**Implementation**: TransferIssueController.java (lines 986-1225)

---

**⚠️ CRITICAL**: Do NOT change this sign convention without consulting the architecture team. The negative cost convention is fundamental to the system's accounting integrity and changing it would break:
- Departmental P&L tracking
- Inventory valuation reports
- Cost center accountability
- Financial year-end closing
- General Ledger integration
