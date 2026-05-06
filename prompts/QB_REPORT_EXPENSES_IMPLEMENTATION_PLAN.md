# QuickBooks Report Enhancement Plan: GRN & Purchase Expenses Integration

## Executive Summary

This document outlines the implementation plan to enhance the existing QuickBooks (QB) report to include expense tracking for GRN (Goods Received Note) and Purchase transactions. The current system already processes expenses but requires refinement to match the expected QB format for proper financial reporting.

## Current State Analysis

### Database Analysis
- **Recent GRN with Expenses:** Bill ID 1994328
- **Details:**
  - Type: PharmacyGrnBill
  - Department: RHD
  - Net Total: 7,145
  - Invoice: dfg
  - Supplier: Ama Trading (Pvt) Ltd
  - Expense: Transport (100.00)

### Current Implementation
- **File:** `src/main/java/com/divudi/bean/common/QuickBookReportController.java`
- **Method:** `createQBFormatPharmacyGRNAndPurchaseBills()` (lines 533-799)
- **Status:** ✅ Expense processing exists (lines 600-605) but needs formatting fixes

### Current Expense Processing Logic
```java
for (BillItem bi : b.getBillExpenses()) {
    qbf = new QuickBookFormat("SPL", "Bill", sdf.format(b.getCreatedAt()),
        bi.getItem().getPrintName(),  // Issue: Direct printName usage
        "", "", "", bi.getNetValue(),
        b.getInvoiceNumber(), b.getDeptId(),
        bi.getItem().getName(),       // Issue: Wrong QB Class
        bi.getDescreption(),          // Issue: Wrong memo format
        "", "", "", "", "");
    grantTot += bi.getNetValue();
    qbfs.add(qbf);
}
```

## Gap Analysis

### Current Output vs Expected Format

| Aspect | Current Implementation | Expected Format |
|--------|----------------------|-----------------|
| **Date Format** | `2025-11-29 20:18:59` | `11/29/2025` |
| **Expense Account** | `bi.getItem().getPrintName()` | `OTHER MATERIAL & SERVICE COST:Transport` |
| **QB Class** | `bi.getItem().getName()` | `RHD` (department name) |
| **Memo** | `bi.getDescreption()` | `GRN/RHD/RHDDC006/25/000001` (GRN number) |

### Expected QB Format Example
```
TRNS,Bill,11/29/2025,Accounts Payable:Trade Creditor-RHD,Ama Trading (Pvt) Ltd,-7245.00,dfg,RHD,GRN/RHD/RHDDC006/25/000001
SPL,Bill,11/29/2025,OTHER MATERIAL & SERVICE COST:Transport,,100.00,dfg,RHD,GRN/RHD/RHDDC006/25/000001
SPL,Bill,11/29/2025,INVENTORIES:RHD,,7145.00,dfg,RHD,Credit / 11/29/25 / Ama Trading (Pvt) Ltd
ENDTRNS
```

## Implementation Plan

### Phase 1: Date Format Fix (Priority: HIGH, Effort: 1 hour)
**Objective:** Change date format from `yyyy-MM-dd HH:mm:ss` to `M/d/yyyy`

**Changes Required:**
- Line 568: `SimpleDateFormat sdf = new SimpleDateFormat("M/d/yyyy");`
- Line 628: Same change for canceled bills
- Line 687: Same change for returns
- Line 747: Same change for return cancellations

**Testing:** Verify output shows `11/29/2025` instead of `2025-11-29 20:18:59`

### Phase 2: Expense Account Mapping (Priority: HIGH, Effort: 4-6 hours)
**Objective:** Map expense types to proper QB Chart of Accounts

**New Method Required:**
```java
private String getExpenseQBAccount(String expenseType, boolean consideredForCosting, String deptName) {
    if (consideredForCosting) {
        return "INVENTORIES:" + deptName;
    }

    switch (expenseType.toLowerCase()) {
        case "transport":
        case "freight":
            return "OTHER MATERIAL & SERVICE COST:Transport";
        case "handling":
        case "handling fees":
            return "OTHER MATERIAL & SERVICE COST:Handling Fees";
        case "maintenance":
        case "installation":
            return "MAINTANCE COST:Hospital Maintenance:Maintenance - General";
        default:
            return "OTHER MATERIAL & SERVICE COST:Other";
    }
}
```

**Changes to Line 602:**
```java
// Before
qbf = new QuickBookFormat("SPL", "Bill", sdf.format(b.getCreatedAt()),
    bi.getItem().getPrintName(), ...);

// After
String expenseAccount = getExpenseQBAccount(bi.getItem().getName(),
    bi.isConsideredForCosting(), b.getDepartment().getName());
qbf = new QuickBookFormat("SPL", "Bill", sdf.format(b.getCreatedAt()),
    expenseAccount, ...);
```

### Phase 3: Memo Field Enhancement (Priority: MEDIUM, Effort: 2 hours)
**Objective:** Use GRN number in expense memo instead of description

**Changes Required:**
- Line 602: Change memo from `bi.getDescreption()` to `b.getDeptId()` (GRN number)
- Apply same change to lines 660, 720, 780 (other bill types)

### Phase 4: QB Class Correction (Priority: MEDIUM, Effort: 1 hour)
**Objective:** Use department name instead of item name for QB Class

**Changes Required:**
- Line 602: Change QB Class from `bi.getItem().getName()` to `b.getDepartment().getName()`
- Apply same change to lines 660, 720, 780

### Phase 5: Validation & Error Handling (Priority: MEDIUM, Effort: 4 hours)
**Objective:** Add robust error handling and validation

**New Validation Methods:**
```java
private void validateBillForQBExport(Bill bill) throws ValidationException {
    if (bill.getNetTotal() == null || bill.getNetTotal() == 0) {
        throw new ValidationException("Bill has zero total: " + bill.getId());
    }
    if (bill.getDepartment() == null) {
        throw new ValidationException("Bill missing department: " + bill.getId());
    }
    // Additional validations...
}

private void validateTransactionBalance(double trnsAmount, List<QuickBookFormat> splLines) {
    double splTotal = splLines.stream().mapToDouble(QuickBookFormat::getAmount).sum();
    if (Math.abs(Math.abs(trnsAmount) - splTotal) > 0.01) {
        throw new RuntimeException("Transaction out of balance: TRNS=" + trnsAmount + " SPL=" + splTotal);
    }
}
```

## Testing Strategy

### Test Data: Bill ID 1994328
- **Type:** PharmacyGrnBill
- **Department:** RHD
- **Supplier:** Ama Trading (Pvt) Ltd
- **Base Amount:** 7,145.00
- **Transport Expense:** 100.00
- **Total:** 7,245.00

### Test Scenarios

#### Test 1: Current System Output
**Action:** Run QB report for date range including 2025-11-29
**Expected Issues:**
- Date format: `2025-11-29 20:18:59`
- Expense account: `Transport` (instead of proper QB account)
- Wrong memo format

#### Test 2: After Phase 1 (Date Fix)
**Expected Output:**
```
TRNS,Bill,11/29/2025,Accounts Payable:Trade Creditor-RHD,Ama Trading (Pvt) Ltd,-7245.00
SPL,Bill,11/29/2025,Transport,,100.00
SPL,Bill,11/29/2025,INVENTORIES:RHD,,7145.00
ENDTRNS
```

#### Test 3: After Phase 2 (Account Mapping)
**Expected Output:**
```
TRNS,Bill,11/29/2025,Accounts Payable:Trade Creditor-RHD,Ama Trading (Pvt) Ltd,-7245.00
SPL,Bill,11/29/2025,OTHER MATERIAL & SERVICE COST:Transport,,100.00
SPL,Bill,11/29/2025,INVENTORIES:RHD,,7145.00
ENDTRNS
```

#### Test 4: Final Output (All Phases)
**Expected Output:**
```
TRNS,Bill,11/29/2025,Accounts Payable:Trade Creditor-RHD,Ama Trading (Pvt) Ltd,-7245.00,dfg,RHD,GRN/RHD/RHDDC006/25/000001
SPL,Bill,11/29/2025,OTHER MATERIAL & SERVICE COST:Transport,,100.00,dfg,RHD,GRN/RHD/RHDDC006/25/000001
SPL,Bill,11/29/2025,INVENTORIES:RHD,,7145.00,dfg,RHD,Credit / 11/29/25 / Ama Trading (Pvt) Ltd
ENDTRNS
```

## Database Schema Requirements

### Required Fields Analysis
Based on code analysis, these fields are essential:

**BILL Table:**
- `id` ✅
- `billType` ✅
- `billClassType` ✅
- `netTotal` ✅
- `invoiceNumber` ✅
- `createdAt` ✅
- `paymentMethod` ✅
- `department_id` ✅
- `fromInstitution_id` ✅
- `deptId` ✅ (GRN number)

**BILLITEM Table:**
- `id` ✅
- `expenseBill_id` ✅
- `netValue` ✅
- `descreption` ✅ (note: typo in DB)
- `item_id` ✅
- `consideredForCosting` (⚠️ Need to verify this exists)

**ITEM Table:**
- `name` ✅ (expense type)
- `printName` ✅ (current QB account - may need mapping)

### Missing Field Investigation
Need to verify if `consideredForCosting` exists in BILLITEM table:
```sql
DESCRIBE BILLITEM;
```

If missing, expense classification will use default rules based on expense type.

## Risk Assessment

### Technical Risks
1. **Date Format Change Impact:** LOW - Only affects QB export, no database changes
2. **Account Mapping Logic:** MEDIUM - Need to handle unmapped expense types
3. **Balance Validation:** HIGH - Critical for QB import success

### Mitigation Strategies
1. **Backward Compatibility:** Keep original format as fallback option
2. **Unmapped Expenses:** Use generic "OTHER MATERIAL & SERVICE COST:Other" account
3. **Transaction Validation:** Implement pre-flight balance check before export

## Success Criteria

### Technical Success
- [ ] All transactions balance (TRNS = sum of SPL lines)
- [ ] Date format matches QB requirements (`M/d/yyyy`)
- [ ] Expense accounts map to valid QB Chart of Accounts
- [ ] All expense types have proper classification

### Business Success
- [ ] Finance team can import QB file without errors
- [ ] Expense allocation matches accounting requirements
- [ ] Audit trail maintained from QB back to HMIS
- [ ] Monthly reconciliation between QB and HMIS balances

## Implementation Schedule

| Phase | Tasks | Duration | Dependencies |
|-------|--------|----------|--------------|
| **Phase 1** | Date format fix | 1 hour | None |
| **Phase 2** | Account mapping logic | 4-6 hours | Phase 1 complete |
| **Phase 3** | Memo field enhancement | 2 hours | Phase 1 complete |
| **Phase 4** | QB Class correction | 1 hour | Phase 1 complete |
| **Phase 5** | Validation & testing | 4 hours | All phases complete |
| **Total** | **Complete implementation** | **12-14 hours** | **Sequential execution** |

## Files to Modify

### Primary Changes
1. **QuickBookReportController.java** - Main implementation
   - Lines 568, 628, 687, 747: Date format
   - Lines 602, 660, 720, 780: Account mapping, memo, QB class
   - Add new validation methods

### Supporting Files (if needed)
2. **ExpenseAccountMapper.java** - New utility class for account mapping
3. **QBExportValidator.java** - New validation utility

### Configuration (future enhancement)
4. **expense_qb_mapping** table - Database table for flexible mapping
5. **application.properties** - Configuration for QB account defaults

## Post-Implementation

### Monitoring
- Daily QB export success rate
- Monthly reconciliation reports
- Quarterly expense distribution analysis

### Documentation Updates
- User guide for running enhanced QB report
- Finance procedure for setting up expense items
- Troubleshooting guide for common QB import errors

### Training Required
- Finance team: Enhanced report functionality
- Pharmacy staff: Proper expense item setup
- IT team: Troubleshooting and maintenance

---

## Appendix

### Sample Data Used in Testing
```sql
-- Test GRN with Transport Expense
SELECT * FROM BILL WHERE id = 1994328;
-- Result: PharmacyGrnBill, RHD dept, 7145.00, Cash payment

SELECT * FROM BILLITEM WHERE expenseBill_id = 1994328;
-- Result: Transport expense, 100.00
```

### Current vs Expected Output Comparison
**Current Format:**
```
TRNS,Bill,2025-11-29 20:18:59,Accounts Payable:Trade Creditor-RHD,Ama Trading (Pvt) Ltd,-7245.00
SPL,Bill,2025-11-29 20:18:59,Transport,,100.00
SPL,Bill,2025-11-29 20:18:59,INVENTORIES:RHD,,7145.00
ENDTRNS
```

**Expected Format:**
```
TRNS,Bill,11/29/2025,Accounts Payable:Trade Creditor-RHD,Ama Trading (Pvt) Ltd,-7245.00,dfg,RHD,GRN/RHD/RHDDC006/25/000001
SPL,Bill,11/29/2025,OTHER MATERIAL & SERVICE COST:Transport,,100.00,dfg,RHD,GRN/RHD/RHDDC006/25/000001
SPL,Bill,11/29/2025,INVENTORIES:RHD,,7145.00,dfg,RHD,Credit / 11/29/25 / Ama Trading (Pvt) Ltd
ENDTRNS
```

### Key Implementation Notes
- ⚠️ Database field `DESCREPTION` has typo - use as-is for compatibility
- ✅ Expense processing loop already exists - just needs formatting fixes
- 🔍 Need to investigate `consideredForCosting` field existence
- 📋 Total implementation effort: 12-14 hours across 5 phases