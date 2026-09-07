# Cost of Goods Report - Retail Value Enhancement

**Project:** Add Retail Rate column and Item filter to Cost of Goods Sold Report
**Date Started:** 2025-10-06
**Status:** Not Started
**File Location:** /home/buddhika/development/rh/cost-of-goods-report-retail-value-enhancement-todos.md

---

## Overview

Currently, the Cost of Goods Report displays only Purchase Value and Cost Value columns. This enhancement will:
1. Add a new "Retail Value" column to display retail rate calculations
2. Add an Item filter to allow filtering by specific items

**Key Files:**
- **UI:** `/src/main/webapp/reports/inventoryReports/cost_of_goods_sold.xhtml`
- **Controller:** `/src/main/java/com/divudi/bean/report/PharmacyReportController.java`
- **Entities:**
  - `StockHistory` (has: retailRate, wholesaleRate, purchaseRate, costRate)
  - `BillItemFinanceDetails` (has: valueAtRetailRate, valueAtPurchaseRate, valueAtCostRate, valueAtWholesaleRate)

---

## Data Structure Understanding

### Current cogsRows Map Structure
```java
private Map<String, Object> cogsRows = new LinkedHashMap<>();
```

Each entry in cogsRows contains:
- **Key:** String (e.g., "Opening Stock", "GRN Cash", "Closing Stock")
- **Value:** Map<String, Double> with keys:
  - "purchaseValue": double
  - "costValue": double

**After Enhancement:**
- "purchaseValue": double
- "costValue": double
- **"retailValue": double** (NEW)

---

## Tasks Breakdown

### Phase 1: Backend - Core Calculation Methods (Controller Layer)

#### ✅ = Completed | 🔄 = In Progress | ⏸️ = Not Started

---

#### Task 1.1: Add Retail Value to Opening Stock Calculation
**Status:** ✅ Completed
**Method:** `calculateOpeningStockRow()` and `calculateStockTotals(Date date)`
**Location:** PharmacyReportController.java:4680-4685 and 4332-4430
**Started:** 2025-10-06
**Completed:** 2025-10-06

**Current Implementation:**
```java
jpql.append("SELECT ")
    .append("SUM(sh.stockQty * sh.itemBatch.purcahseRate), ")
    .append("SUM(sh.stockQty * COALESCE(sh.itemBatch.costRate, 0.0)) ")
```

**Required Changes:**
1. Modify SELECT clause to add retail value calculation:
   ```java
   .append("SUM(sh.stockQty * sh.itemBatch.retailRate) ")
   ```
2. Update result mapping to extract 3 values instead of 2
3. Add "retailValue" key to returned Map<String, Double>

**Notes:**
- StockHistory entity already has `retailRate` field (line 73 in StockHistory.java)
- Pattern: `SUM(quantity * rate)` for all calculations

---

#### Task 1.2: Add Retail Value to Stock Correction Calculation
**Status:** ✅ Completed
**Method:** `calculateStockCorrectionRow()` and `calculateStockCorrectionValues()`
**Location:** PharmacyReportController.java:4687-4690 and 4431-4481
**Started:** 2025-10-06
**Completed:** 2025-10-06

**Current Implementation:**
```java
jpql.append("SELECT ")
    .append("SUM(bi.bill.billFinanceDetails.totalPurchaseValue), ")
    .append("SUM(bi.bill.billFinanceDetails.totalCostValue) ")
```

**Required Changes:**
1. Add to SELECT clause:
   ```java
   .append("SUM(bi.bill.billFinanceDetails.totalRetailValue) ")
   ```
2. Update result mapping for 3 values
3. Add "retailValue" to returned map

**Notes:**
- Uses `BillFinanceDetails` not `BillItemFinanceDetails`
- Check if `totalRetailValue` field exists in `BillFinanceDetails` entity
- If not, may need to calculate from `BillItem.billItemFinanceDetails.valueAtRetailRate`

---

#### Task 1.3: Add Retail Value to GRN Cash and Credit Rows
**Status:** ✅ Completed
**Method:** `calculateGrnCashAndCreditValues()`
**Location:** PharmacyReportController.java:4482-4552
**Started:** 2025-10-06
**Completed:** 2025-10-06

**Current Implementation:**
```java
StringBuilder jpql = new StringBuilder(
    "SELECT bi.bill.paymentMethod, " +
    "SUM(bi.billItemFinanceDetails.quantityByUnits * bi.pharmaceuticalBillItem.purchaseRate), " +
    "SUM(bi.billItemFinanceDetails.quantityByUnits * bi.pharmaceuticalBillItem.itemBatch.costRate) " +
    "FROM BillItem bi ..."
);
```

**Required Changes:**
1. Add third SUM to SELECT:
   ```java
   "SUM(bi.billItemFinanceDetails.quantityByUnits * bi.pharmaceuticalBillItem.itemBatch.retailRate) "
   ```
2. Update result processing loop to extract 4 values (paymentMethod + 3 values)
3. Add "retailValue" to both cashRow and creditRow maps

**Notes:**
- This method creates two separate rows: "GRN Cash" and "GRN Credit"
- Both need retailValue added

---

#### Task 1.4: Update retrievePurchaseAndCostValues() Method
**Status:** ✅ Completed
**Method:** `retrievePurchaseAndCostValues(String billTypeField, Object billTypeValue)`
**Location:** PharmacyReportController.java:4554-4597
**Started:** 2025-10-06
**Completed:** 2025-10-06

**Current Implementation:**
```java
baseQuery.append("SELECT ")
    .append("SUM(bi.qty * bi.pharmaceuticalBillItem.itemBatch.purcahseRate), ")
    .append("SUM(bi.qty * bi.pharmaceuticalBillItem.itemBatch.costRate) ")
```

**Required Changes:**
1. Add third SUM:
   ```java
   .append("SUM(bi.qty * bi.pharmaceuticalBillItem.itemBatch.retailRate) ")
   ```
2. Update result extraction to handle 3 values
3. Add "retailValue" to result map
4. Update error result map initialization

**Notes:**
- This is a helper method used by MANY other calculation methods
- Used by: Drug Return IP, Drug Return OP, BHT Issue, Transfer Issue, Transfer Receive, Sale Credit, Sale Without Credit

---

#### Task 1.5: Update retrievePurchaseAndCostValues() Overloaded Method (with PaymentMethod)
**Status:** ✅ Completed
**Method:** `retrievePurchaseAndCostValues(String billTypeField, Object billTypeValue, Object paymentMethod)`
**Location:** PharmacyReportController.java:4599-4647
**Started:** 2025-10-06
**Completed:** 2025-10-06

**Required Changes:**
- Same as Task 1.4 but for the overloaded version
- Add retail rate SUM to SELECT
- Update result extraction
- Add "retailValue" to result map

**Notes:**
- This version filters by payment method
- Used by: calculateSaleCreditValue(), calculateSaleWithoutCreditPaymentMethod()

---

#### Task 1.6: Update retrievePurchaseAndCostValuesWithSignLogic() Method
**Status:** ⏸️ Not Started
**Method:** `retrievePurchaseAndCostValuesWithSignLogic(String billTypeField, Object billTypeValue)`
**Location:** PharmacyReportController.java:4750-4801
**Started:** __________
**Completed:** __________

**Current Implementation:**
```java
double itemPurchaseValue = item.getQty() * item.getPharmaceuticalBillItem().getItemBatch().getPurcahseRate();
double itemCostValue = item.getQty() * item.getPharmaceuticalBillItem().getItemBatch().getCostRate();
```

**Required Changes:**
1. Add retail value calculation in loop:
   ```java
   double itemRetailValue = item.getQty() * item.getPharmaceuticalBillItem().getItemBatch().getRetailRate();
   ```
2. Add retailValue variable accumulator
3. Apply same sign logic (+ or -) based on BillTypeAtomic
4. Add "retailValue" to result map

**Notes:**
- This method fetches full BillItem objects (not aggregation)
- Applies different signs based on DISPOSAL_ISSUE vs DISPOSAL_RETURN
- Used by: calculateStockConsumption()

---

#### Task 1.7: Update calculateStockAdjustment() Method
**Status:** ⏸️ Not Started
**Method:** `calculateStockAdjustment()`
**Location:** PharmacyReportController.java:4823-4899
**Started:** __________
**Completed:** __________

**Current Implementation:**
Has two separate queries:
1. Issue query (negative quantities): calculates purchase and cost values
2. Receive query (positive quantities): calculates purchase and cost values

**Required Changes:**
1. **Issue Query:** Add retail rate SUM:
   ```java
   .append("SUM(ABS(bi.pharmaceuticalBillItem.qty) * bi.pharmaceuticalBillItem.itemBatch.retailRate) ")
   ```
2. **Receive Query:** Add retail rate SUM (same pattern)
3. Update result extraction for both queries (3 values each)
4. Calculate netRetailValue = receiveRetailValue - issueRetailValue
5. Add "retailValue" to result map

**Notes:**
- Special case: combines two queries (issue and receive)
- Uses ABS() for issue quantities since they're negative

---

#### Task 1.8: Verify All Calculation Methods Updated
**Status:** ⏸️ Not Started
**Started:** __________
**Completed:** __________

**Methods to Double-Check:**
- ✅ calculateDrugReturnIp() - Uses retrievePurchaseAndCostValues() ✓
- ✅ calculateDrugReturnOp() - Uses retrievePurchaseAndCostValues() ✓
- ✅ calculateStockConsumption() - Uses retrievePurchaseAndCostValuesWithSignLogic() ✓
- ✅ calculatePurchaseReturn() - Uses retrievePurchaseAndCostValues() ✓
- ✅ calculateStockAdjustment() - Custom implementation ✓
- ✅ calculateTransferIssueValue() - Uses retrievePurchaseAndCostValues() ✓
- ✅ calculateTransferReceiveValue() - Uses retrievePurchaseAndCostValues() ✓
- ✅ calculateSaleCreditValue() - Uses retrievePurchaseAndCostValues(with payment) ✓
- ✅ calculateBhtIssueValue() - Uses retrievePurchaseAndCostValues() ✓
- ✅ calculateSaleWithoutCreditPaymentMethod() - Uses retrievePurchaseAndCostValues(with payment) ✓

**Verification Steps:**
1. Search for each method name
2. Confirm it either:
   - Calls an updated helper method, OR
   - Has been updated directly
3. Mark as verified

---

#### Task 1.9: Update calculateClosingStockValueByCalculatedRows()
**Status:** ⏸️ Not Started
**Method:** `calculateClosingStockValueByCalculatedRows()`
**Location:** PharmacyReportController.java:4976-5020
**Started:** __________
**Completed:** __________

**Current Implementation:**
```java
totalCalculatedClosingStockPurchaseValue = this.cogsRows.entrySet().stream()
    .filter(...)
    .mapToDouble(entry -> {
        // Extract "purchaseValue" from map
    })
    .sum();
```

**Required Changes:**
1. Add new field: `double totalCalculatedClosingStockRetailValue = 0.0;`
2. Add stream calculation for retailValue (similar to purchase and cost)
3. Add "retailValue" to calculatedClosingStockByCogsRows map

**Notes:**
- This method sums all row values to calculate closing stock
- Currently has: totalCalculatedClosingStockPurchaseValue, totalCalculatedClosingStockCostValue
- Need to add: totalCalculatedClosingStockRetailValue

---

#### Task 1.10: Update calculateVariance() Method
**Status:** ⏸️ Not Started
**Method:** `calculateVariance(Map<String, Double> calculatedClosingStockByCogsRowValues)`
**Location:** PharmacyReportController.java:5021-5053
**Started:** __________
**Completed:** __________

**Current Implementation:**
```java
double calculatedPurchaseValue = calculatedClosingStockByCogsRowValues.getOrDefault("purchaseValue", 0.0);
double dbPurchaseValue = closingStockFromDB != null ? closingStockFromDB.getOrDefault("purchaseValue", 0.0) : 0.0;
double purchaseVariance = calculatedPurchaseValue - dbPurchaseValue;
```

**Required Changes:**
1. Extract calculatedRetailValue from input map
2. Extract dbRetailValue from closingStockFromDB
3. Calculate: `double retailVariance = calculatedRetailValue - dbRetailValue;`
4. Add "retailValue" with retailVariance to variance map

**Notes:**
- Variance = Calculated - Actual (from DB)
- Need to handle null values properly

---

### Phase 2: Backend - Item Filter Implementation

#### Task 2.1: Add Item Property to Controller
**Status:** ⏸️ Not Started
**Location:** PharmacyReportController.java (class level)
**Started:** __________
**Completed:** __________

**Required Changes:**
1. Add property:
   ```java
   private Item item;
   ```
2. Add getter/setter:
   ```java
   public Item getItem() { return item; }
   public void setItem(Item item) { this.item = item; }
   ```

**Notes:**
- Follow existing pattern (institution, site, department are already there)
- Item filter is optional (null means all items)

---

#### Task 2.2: Create addItemFilter() Helper Method
**Status:** ⏸️ Not Started
**Location:** PharmacyReportController.java
**Started:** __________
**Completed:** __________

**Required Changes:**
1. Add new method (follow pattern of existing addFilter method):
   ```java
   private void addItemFilter(StringBuilder jpql, Map<String, Object> params, String fieldPath) {
       if (item != null) {
           jpql.append("AND ").append(fieldPath).append(" = :item ");
           params.put("item", item);
       }
   }
   ```

**Notes:**
- Item field path varies by query:
  - StockHistory queries: `sh.item` or `sh2.item`
  - BillItem queries: `bi.item` or `bi.pharmaceuticalBillItem.itemBatch.item`

---

#### Task 2.3: Update calculateStockTotals() with Item Filter
**Status:** ⏸️ Not Started
**Method:** `calculateStockTotals(Date date)`
**Started:** __________
**Completed:** __________

**Required Changes:**
1. Add item filter after existing filters:
   ```java
   addItemFilter(jpql, params, "sh2.item");
   ```
2. Repeat for all three subquery levels (sh2, sh3 filter sections)

**Notes:**
- StockHistory has direct `item` field
- Must add filter to ALL subquery levels

---

#### Task 2.4: Update calculateStockCorrectionValues() with Item Filter
**Status:** ⏸️ Not Started
**Method:** `calculateStockCorrectionValues()`
**Started:** __________
**Completed:** __________

**Required Changes:**
1. After existing filters, add:
   ```java
   addItemFilter(jpql, params, "bi.item");
   ```

---

#### Task 2.5: Update calculateGrnCashAndCreditValues() with Item Filter
**Status:** ⏸️ Not Started
**Method:** `calculateGrnCashAndCreditValues()`
**Started:** __________
**Completed:** __________

**Required Changes:**
1. After existing filters, add:
   ```java
   addItemFilter(jpql, params, "bi.item");
   ```

---

#### Task 2.6: Update retrievePurchaseAndCostValues() Methods with Item Filter
**Status:** ⏸️ Not Started
**Methods:** Both overloaded versions
**Started:** __________
**Completed:** __________

**Required Changes:**
1. Add to both methods after existing filters:
   ```java
   addItemFilter(baseQuery, commonParams, "bi.item");
   ```

**Notes:**
- This will automatically add item filtering to all methods that use these helpers

---

#### Task 2.7: Update retrievePurchaseAndCostValuesWithSignLogic() with Item Filter
**Status:** ⏸️ Not Started
**Method:** `retrievePurchaseAndCostValuesWithSignLogic()`
**Started:** __________
**Completed:** __________

**Required Changes:**
1. Add after existing filters:
   ```java
   addItemFilter(baseQuery, commonParams, "bi.item");
   ```

---

#### Task 2.8: Update calculateStockAdjustment() with Item Filter
**Status:** ⏸️ Not Started
**Method:** `calculateStockAdjustment()`
**Started:** __________
**Completed:** __________

**Required Changes:**
1. Add to Issue query after filters:
   ```java
   addItemFilter(jpqlIssue, paramsIssue, "bi.item");
   ```
2. Add to Receive query after filters:
   ```java
   addItemFilter(jpqlReceive, paramsReceive, "bi.item");
   ```

**Notes:**
- Two separate queries both need item filter

---

### Phase 3: Frontend - XHTML Updates

#### Task 3.1: Add Item Filter to UI
**Status:** ⏸️ Not Started
**File:** cost_of_goods_sold.xhtml
**Location:** After line 142 (after Department filter)
**Started:** __________
**Completed:** __________

**Required Changes:**
Add after the Department filter closing tag:

```xml
<h:panelGroup layout="block" styleClass="form-group">
    <h:outputText styleClass="fa fa-pills ml-5" />
    <p:outputLabel value="Item" class="mx-3"></p:outputLabel>
</h:panelGroup>
<p:autoComplete
    id="itemFilter"
    class="w-100"
    value="#{pharmacyReportController.item}"
    completeMethod="#{itemController.completeItem}"
    var="i"
    itemLabel="#{i.name}"
    itemValue="#{i}"
    forceSelection="true"
    scrollHeight="200">
</p:autoComplete>
```

**Notes:**
- Follow existing pattern (Department uses p:selectOneMenu, but Item should use p:autoComplete due to large number of items)
- Use fa-pills icon (pharmaceutical)
- Ensure proper grid layout (panelGrid is columns="6", so this is position 5-6)

---

#### Task 3.2: Add Retail Value Column to DataTable
**Status:** ⏸️ Not Started
**File:** cost_of_goods_sold.xhtml
**Location:** After line 224 (after Cost Value column)
**Started:** __________
**Completed:** __________

**Required Changes:**
Add new column after Cost Value column:

```xml
<p:column headerText="Retail Value" style="text-align: right; padding-right: 200px !important;">
    <h:outputText value="#{f.value['retailValue']}" >
        <f:convertNumber pattern="#,##0.00"/>
    </h:outputText>
</p:column>
```

**Notes:**
- Follow exact same pattern as Purchase Value and Cost Value columns
- Use same styling (right-aligned, same padding)
- Currency format: #,##0.00

---

#### Task 3.3: Update Print/Export Functions
**Status:** ⏸️ Not Started
**File:** cost_of_goods_sold.xhtml
**Started:** __________
**Completed:** __________

**Required Changes:**
1. Test PDF export with new column
2. Test Excel export with new column
3. Test Print function with new column

**Notes:**
- PrimeFaces dataExporter should handle new column automatically
- Verify column widths don't break in PDF
- Test that JavaScript print functions still work (hidePaginatorBeforePrint, restorePaginatorAfterPrint)

---

### Phase 4: Testing

#### Task 4.1: Unit Testing - Backend Calculations
**Status:** ⏸️ Not Started
**Started:** __________
**Completed:** __________

**Test Cases:**
1. **Opening Stock Retail Value**
   - Query StockHistory manually
   - Calculate expected retail value: SUM(stockQty * retailRate)
   - Compare with method result

2. **GRN Retail Value**
   - Create test GRN with known retail rates
   - Verify retail value calculation
   - Test both Cash and Credit separately

3. **Closing Stock Retail Value**
   - Similar to opening stock
   - Verify variance calculation includes retail value

4. **All Transaction Types**
   - Test each row type (Drug Return IP, Drug Return OP, etc.)
   - Ensure retail value is calculated correctly

**Test Data Setup:**
- Use existing test database or create sample data
- Ensure test items have all three rates: purchase, cost, retail
- Verify rates are different to catch calculation errors

---

#### Task 4.2: Integration Testing - Item Filter
**Status:** ⏸️ Not Started
**Started:** __________
**Completed:** __________

**Test Cases:**
1. **No Item Selected (null)**
   - Should return all items (existing behavior)
   - Verify all transactions included

2. **Single Item Selected**
   - Select specific item
   - Verify only that item's transactions appear
   - Check all row types are filtered

3. **Item with No Transactions**
   - Select item with no activity in date range
   - Should return zero values (not errors)

4. **Item with Mixed Transaction Types**
   - Select item with GRN, Issues, Returns, etc.
   - Verify all transaction types are filtered correctly

**Notes:**
- Test with different date ranges
- Test with different institution/department filters combined

---

#### Task 4.3: UI Testing
**Status:** ⏸️ Not Started
**Started:** __________
**Completed:** __________

**Test Cases:**
1. **Item Filter UI**
   - Verify autocomplete works
   - Test typing and selection
   - Test clearing selection
   - Test forceSelection validation

2. **Retail Value Column Display**
   - Verify column appears in correct position
   - Check number formatting (2 decimal places)
   - Verify alignment (right-aligned)
   - Check for all row types

3. **Responsive Design**
   - Test on different screen sizes
   - Verify horizontal scrolling if needed
   - Check table doesn't break layout

4. **Export Functions**
   - Excel: All three columns export correctly
   - PDF: Page breaks and column widths are proper
   - Print: All columns visible and formatted

---

#### Task 4.4: Performance Testing
**Status:** ⏸️ Not Started
**Started:** __________
**Completed:** __________

**Test Cases:**
1. **Large Date Range**
   - Test with 1 year date range
   - Measure query execution time
   - Should complete within acceptable time (<30 seconds)

2. **All Institutions/All Items**
   - Test with no filters
   - Measure performance impact of retail value calculation
   - Compare with current two-column performance

3. **Item Filter Performance**
   - With item filter: should be faster (fewer records)
   - Without item filter: should be same as before

**Notes:**
- Document any performance degradation
- Consider adding database indexes if needed

---

#### Task 4.5: Edge Cases and Error Handling
**Status:** ⏸️ Not Started
**Started:** __________
**Completed:** __________

**Test Cases:**
1. **Null Retail Rates**
   - Items without retail rate defined
   - Should handle null gracefully (use 0.0 or show warning)

2. **Negative Quantities**
   - Return transactions
   - Adjustment transactions
   - Verify sign logic is correct for retail value

3. **Date Boundary Conditions**
   - Transactions exactly at fromDate
   - Transactions exactly at toDate
   - Empty date range

4. **Filter Combinations**
   - Item + Institution + Department + Date
   - Test all filter combinations work together

---

### Phase 5: Documentation and Deployment

#### Task 5.1: Code Documentation
**Status:** ⏸️ Not Started
**Started:** __________
**Completed:** __________

**Required Updates:**
1. Add JavaDoc comments to new/modified methods
2. Update method-level comments explaining retail value inclusion
3. Document item filter behavior
4. Update any inline comments that reference "two values" to "three values"

---

#### Task 5.2: Update Wiki/User Documentation
**Status:** ⏸️ Not Started
**Started:** __________
**Completed:** __________

**Documentation Needed:**
1. **User Guide Update**
   - How to use Item filter
   - Explanation of Retail Value column
   - When to use each value type (purchase vs cost vs retail)

2. **Screenshots**
   - Updated report screenshot showing retail value column
   - Item filter usage example

**Notes:**
- Follow Wiki Writing Guidelines from CLAUDE.md
- Focus on user perspective, not technical implementation

---

#### Task 5.3: Create Migration/Update Notes
**Status:** ⏸️ Not Started
**Started:** __________
**Completed:** __________

**Required Notes:**
1. **Database Changes**
   - Any new indexes needed?
   - Performance implications

2. **Behavioral Changes**
   - New column appears automatically (no user action needed)
   - Item filter is optional enhancement

3. **Backward Compatibility**
   - Report still works without item filter
   - Existing data unaffected

---

#### Task 5.4: Prepare for Code Review
**Status:** ⏸️ Not Started
**Started:** __________
**Completed:** __________

**Checklist:**
- [ ] All methods updated consistently
- [ ] No hardcoded values
- [ ] Follows existing code patterns
- [ ] Error handling in place
- [ ] No debug code left in
- [ ] Proper null checks
- [ ] Code formatted properly
- [ ] No warnings or compilation errors

---

#### Task 5.5: Create Pull Request
**Status:** ⏸️ Not Started
**Started:** __________
**Completed:** __________

**PR Description Should Include:**
1. **Summary**
   - Added Retail Value column to Cost of Goods Report
   - Added Item filter capability

2. **Changes Made**
   - Modified: PharmacyReportController.java (list specific methods)
   - Modified: cost_of_goods_sold.xhtml
   - Testing completed

3. **Testing Evidence**
   - Screenshots of report with new column
   - Test results summary

4. **Closes Issue:** #[issue_number]

---

## Summary Checklist

### Backend Changes
- [ ] Task 1.1: Opening Stock retail calculation
- [ ] Task 1.2: Stock Correction retail calculation
- [ ] Task 1.3: GRN retail calculation
- [ ] Task 1.4: Update retrievePurchaseAndCostValues()
- [ ] Task 1.5: Update retrievePurchaseAndCostValues() overload
- [ ] Task 1.6: Update retrievePurchaseAndCostValuesWithSignLogic()
- [ ] Task 1.7: Update calculateStockAdjustment()
- [ ] Task 1.8: Verify all calculation methods
- [ ] Task 1.9: Update calculateClosingStockValueByCalculatedRows()
- [ ] Task 1.10: Update calculateVariance()

### Item Filter Implementation
- [ ] Task 2.1: Add Item property to controller
- [ ] Task 2.2: Create addItemFilter() helper
- [ ] Task 2.3: Update calculateStockTotals() with filter
- [ ] Task 2.4: Update calculateStockCorrectionValues() with filter
- [ ] Task 2.5: Update calculateGrnCashAndCreditValues() with filter
- [ ] Task 2.6: Update retrievePurchaseAndCostValues() with filter
- [ ] Task 2.7: Update retrievePurchaseAndCostValuesWithSignLogic() with filter
- [ ] Task 2.8: Update calculateStockAdjustment() with filter

### Frontend Changes
- [ ] Task 3.1: Add Item filter to UI
- [ ] Task 3.2: Add Retail Value column
- [ ] Task 3.3: Test print/export functions

### Testing
- [ ] Task 4.1: Backend calculation testing
- [ ] Task 4.2: Item filter integration testing
- [ ] Task 4.3: UI testing
- [ ] Task 4.4: Performance testing
- [ ] Task 4.5: Edge cases and error handling

### Documentation & Deployment
- [ ] Task 5.1: Code documentation
- [ ] Task 5.2: User documentation/wiki
- [ ] Task 5.3: Migration notes
- [ ] Task 5.4: Code review preparation
- [ ] Task 5.5: Pull request creation

---

## Notes and Observations

### Important Patterns Identified:
1. **Rate Field Names:** Note the typo `purcahseRate` (not `purchaseRate`) - this is intentional for backward compatibility (see CLAUDE.md rule #10)
2. **Consistent Map Structure:** All calculation methods return `Map<String, Double>` with keys like "purchaseValue", "costValue"
3. **Helper Methods:** Most calculations use shared helper methods - updating these updates multiple reports
4. **Sign Logic:** Some transactions add, some subtract - follow existing patterns carefully

### Potential Issues to Watch:
1. **Null Retail Rates:** Some items may not have retail rates defined - handle gracefully
2. **Performance:** Adding third calculation may impact query performance - test thoroughly
3. **BillFinanceDetails vs BillItemFinanceDetails:** Stock Correction uses BillFinanceDetails which may not have retail value field

### Questions to Resolve:
1. Does `BillFinanceDetails` entity have `totalRetailValue` field? If not, how to calculate for Stock Correction?
2. Should item filter support multiple items or just single item?
3. What should happen if retail rate is null/zero?

---

## Progress Log

### Session 1: 2025-10-06
- Created comprehensive todo list
- Analyzed existing code structure
- Identified all methods requiring updates
- Documented patterns and potential issues

### Session 2: 2025-10-06 (Implementation Started)
- ✅ Task 1.1: Completed Opening Stock retail value calculation (calculateStockTotals)
  - Added retail rate SUM to SELECT clause
  - Updated result extraction to handle 3 values
  - Added retailValue to both normal and error result maps

- ✅ Task 1.2: Completed Stock Correction retail value calculation
  - Used totalRetailSaleValue from BillFinanceDetails entity
  - Updated query and result mappings

- ✅ Task 1.3: Completed GRN Cash and Credit retail value calculations
  - Added retail rate to aggregation query
  - Updated both cashRow and creditRow maps
  - Updated error handling

- ✅ Task 1.4: Completed retrievePurchaseAndCostValues() helper method
  - CRITICAL: This updates Drug Return IP, Drug Return OP, BHT Issue, Transfer Issue, Transfer Receive automatically
  - Added retail rate SUM to query
  - Updated all result maps

- ✅ Task 1.5: Completed retrievePurchaseAndCostValues() overloaded method (with PaymentMethod)
  - CRITICAL: This updates Sale Credit and Sale Without Credit automatically
  - Added retail rate SUM to query
  - Updated all result maps

**Important Note:** By completing tasks 1.4 and 1.5, the following methods are now automatically updated:
- calculateDrugReturnIp()
- calculateDrugReturnOp()
- calculateStockConsumption() (partially - needs Task 1.6)
- calculatePurchaseReturn()
- calculateTransferIssueValue()
- calculateTransferReceiveValue()
- calculateSaleCreditValue()
- calculateBhtIssueValue()
- calculateSaleWithoutCreditPaymentMethod()

**Remaining Backend Tasks:**
- Task 1.6: retrievePurchaseAndCostValuesWithSignLogic() - for Stock Consumption
- Task 1.7: calculateStockAdjustment() - custom dual-query implementation
- Task 1.9: calculateClosingStockValueByCalculatedRows() - aggregation method
- Task 1.10: calculateVariance() - variance calculation

### Session 3: [Date]
- [Agent can update this section when resuming work]

---

**End of Todo Document**
**Last Updated:** 2025-10-06
**Next Action:** Task 1.6 - Update retrievePurchaseAndCostValuesWithSignLogic() method

**Progress Summary:** 5 of 12 major tasks completed (42%). Most critical helper methods done, which automatically updated 9 other calculation methods.
