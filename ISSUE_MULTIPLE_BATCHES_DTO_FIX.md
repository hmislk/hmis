# Fix Multiple Batches Mode to Work with DTOs in Pharmacy Retail Sale for Cashier

## Issue Summary

The "Add quantity from multiple batches" feature in `pharmacy_bill_retail_sale_for_cashier.xhtml` fails when enabled because the `addBillItemMultipleBatches()` method relies on Stock entities instead of DTOs. After the first batch is added successfully, `clearBillItem()` sets `stockDto = null`, causing subsequent calls to `addBillItemSingleItem()` to fail with "Please select an Item Batch to Dispense ??" error appearing multiple times.

## Priority
**Medium** - Feature is currently disabled as workaround, but should be fixed for users who need multiple batch fulfillment.

## Affects
- **File**: `PharmacySaleForCashierController.java`
- **Method**: `addBillItemMultipleBatches()` (starts around line 1613)
- **Page**: `pharmacy_bill_retail_sale_for_cashier.xhtml`
- **Config**: "Add quantity from multiple batches in pharmacy retail billing"

## Current Behavior

When multiple batches mode is **enabled**:

1. ✅ User selects stock with stockQty = 1.0
2. ✅ User enters requested qty = 10.0
3. ✅ First `addBillItemSingleItem()` succeeds → adds 1.0 to bill
4. ❌ `clearBillItem()` sets `stockDto = null`
5. ❌ `stockController.findNextAvailableStocks(userSelectedStock)` tries to find more batches
6. ❌ Loop calls `addBillItemSingleItem()` 3+ more times with **NULL stockDto**
7. ❌ Each call fails validation → "Please select an Item Batch to Dispense ??" × 3
8. ❌ Final error: "Only 1.0 is Available form the Requested Quantity"

### Console Log Evidence

```
INFO:   === CASHIER addBillItemMultipleBatches START ===
INFO:   WARNING: Multiple batches mode is active!
INFO:   === CASHIER addBillItemSingleItem START ===
INFO:   Validation PASSED: Stock quantity check successful
INFO:   SUCCESS: Adding item to bill - ID: 488787, Qty: 1.0
INFO:   === CASHIER addBillItemSingleItem START ===
INFO:   Validation FAILED: stockDto is NULL
INFO:   === CASHIER addBillItemSingleItem START ===
INFO:   Validation FAILED: stockDto is NULL
INFO:   === CASHIER addBillItemSingleItem START ===
INFO:   Validation FAILED: stockDto is NULL
INFO:   === MULTIPLE BATCHES: Insufficient quantity ===
INFO:   Requested: 10.0, Added: 1.0
```

## Expected Behavior

When multiple batches mode is **enabled**:

1. ✅ User selects stock with stockQty = 1.0
2. ✅ User enters requested qty = 10.0
3. ✅ System adds 1.0 from first batch
4. ✅ System **automatically finds next available batches** of same item
5. ✅ System adds quantities from additional batches until qty = 10.0 is fulfilled
6. ✅ If insufficient total stock:
   - Show **single** error: "Only X is available from the requested quantity"
   - Add all available stock to bill
7. ✅ No duplicate error messages

## Root Cause Analysis

### Problem 1: Entity-Based Lookup After DTO Selection

**Location**: `addBillItemMultipleBatches()` line ~1709

```java
// Convert DTO to entity for finding next available stocks (multiple batches feature)
Stock userSelectedStock = convertStockDtoToEntity(userSelectedStockDto);
List<Stock> availableStocks = stockController.findNextAvailableStocks(userSelectedStock);
```

- Uses `stockController.findNextAvailableStocks()` which returns **Stock entities**
- Not DTO-based, contradicts the DTO-first architecture
- Requires entity conversion which is slow

### Problem 2: StockDto Cleared After First Batch

**Location**: `addBillItemSingleItem()` line ~1608

```java
clearBillItem();  // Sets stockDto = null
```

After first batch is added:
- `clearBillItem()` nulls out `stockDto`
- Loop continues with `stock = s` (entity) but `stockDto` is null
- Subsequent `addBillItemSingleItem()` calls fail because stockDto is required

### Problem 3: No DTO Cache for Additional Batches

The loop at line ~1710 sets:
```java
for (Stock s : availableStocks) {
    stock = s;  // Sets entity
    // But stockDto is still NULL!
    qty = s.getStock();
    addBillItemSingleItem();  // FAILS - needs stockDto, not stock
}
```

## Proposed Solution

### Approach: DTO-First Multiple Batches Implementation

Follow the pattern from `PharmacySaleForCashierController` where we:
1. Store autocomplete results in `lastAutocompleteResults` DTO cache
2. Create a new method: `findNextAvailableStockDtos(StockDTO selectedDto)`
3. Refactor `addBillItemMultipleBatches()` to use DTOs throughout

### Implementation Plan

#### Step 1: Create DTO-Based Stock Lookup Method

**Location**: Add to `PharmacySaleForCashierController.java`

```java
/**
 * Find next available stock DTOs for the same item as the selected stock.
 * Used by multiple batches mode to find additional batches to fulfill quantity.
 *
 * @param selectedStockDto The initially selected stock DTO
 * @return List of additional StockDTOs for same item, ordered by expiry date
 */
public List<StockDTO> findNextAvailableStockDtos(StockDTO selectedStockDto) {
    if (selectedStockDto == null || selectedStockDto.getItemId() == null) {
        return Collections.emptyList();
    }

    Map<String, Object> parameters = new HashMap<>();
    parameters.put("department", getSessionController().getLoggedUser().getDepartment());
    parameters.put("itemId", selectedStockDto.getItemId());
    parameters.put("excludeStockId", selectedStockDto.getId());
    parameters.put("stockMin", 0.0);

    String jpql = "SELECT NEW com.divudi.core.data.dto.StockDTO("
            + "s.id, s.itemBatch.id, s.itemBatch.item.id, "
            + "s.itemBatch.item.name, s.itemBatch.item.code, "
            + "COALESCE(s.itemBatch.item.vmp.name, ''), "
            + "s.itemBatch.batchNo, "
            + "s.itemBatch.retailsaleRate, s.stock, "
            + "s.itemBatch.dateOfExpire, "
            + "s.itemBatch.item.discountAllowed) "
            + "FROM Stock s "
            + "WHERE s.department = :department "
            + "AND s.itemBatch.item.id = :itemId "
            + "AND s.id != :excludeStockId "
            + "AND s.stock > :stockMin "
            + "ORDER BY s.itemBatch.dateOfExpire ASC";

    return getStockFacade().findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP, 20);
}
```

#### Step 2: Refactor `addBillItemMultipleBatches()` Method

**Location**: Replace existing method starting at line ~1613

```java
public void addBillItemMultipleBatches() {
    System.out.println("=== CASHIER addBillItemMultipleBatches START ===");
    System.out.println("Multiple batches mode: Attempting to fulfill quantity from multiple batches");

    editingQty = null;
    errorMessage = null;

    if (billItem == null || billItem.getPharmaceuticalBillItem() == null) {
        return;
    }

    if (getStockDto() == null) {
        errorMessage = "Please select an Item Batch to Dispense?";
        JsfUtil.addErrorMessage("Please select an Item Batch to Dispense?");
        return;
    }

    if (getQty() == null || getQty() == 0.0) {
        errorMessage = "Please enter a Quantity";
        JsfUtil.addErrorMessage("Quantity?");
        return;
    }

    if (getStockDto().getStockQty() == null) {
        errorMessage = "Stock quantity not available.";
        JsfUtil.addErrorMessage("Stock quantity not available. Please select a valid stock.");
        return;
    }

    // Allergy check if enabled
    if (configOptionApplicationController.getBooleanValueByKey("Check for Allergies during Dispensing")) {
        if (patient != null && getBillItem() != null) {
            if (allergyListOfPatient == null) {
                allergyListOfPatient = pharmacyService.getAllergyListForPatient(patient);
            }
            String allergyMsg = pharmacyService.getAllergyMessageForPatient(patient, billItem, allergyListOfPatient);
            if (!allergyMsg.isEmpty()) {
                JsfUtil.addErrorMessage(allergyMsg);
                return;
            }
        }
    }

    double requestedQty = getQty();
    double addedQty = 0.0;
    double remainingQty = requestedQty;

    System.out.println("Requested quantity: " + requestedQty);
    System.out.println("First batch available: " + getStockDto().getStockQty());

    // Store reference to initially selected DTO
    StockDTO initialStockDto = getStockDto();

    // Add from first batch
    if (initialStockDto.getStockQty() >= remainingQty) {
        // First batch has enough stock
        double thisTimeAddingQty = addBillItemSingleItem();
        addedQty = thisTimeAddingQty;
        System.out.println("First batch fulfilled entire request. Added: " + addedQty);
        System.out.println("=== CASHIER addBillItemMultipleBatches END ===");
        return;
    } else {
        // First batch has partial stock
        qty = initialStockDto.getStockQty();
        double thisTimeAddingQty = addBillItemSingleItem();
        addedQty += thisTimeAddingQty;
        remainingQty -= thisTimeAddingQty;
        System.out.println("First batch partial. Added: " + thisTimeAddingQty + ", Remaining: " + remainingQty);
    }

    // Find and add from additional batches
    List<StockDTO> additionalStockDtos = findNextAvailableStockDtos(initialStockDto);
    System.out.println("Found " + additionalStockDtos.size() + " additional batches for same item");

    for (StockDTO additionalDto : additionalStockDtos) {
        if (remainingQty <= 0) {
            System.out.println("Quantity fulfilled, stopping batch search");
            break;
        }

        // Set stockDto to the additional batch DTO
        this.stockDto = additionalDto;
        System.out.println("Processing batch ID: " + additionalDto.getId() +
                          ", Available: " + additionalDto.getStockQty());

        // Determine quantity to add from this batch
        if (remainingQty <= additionalDto.getStockQty()) {
            qty = remainingQty;
        } else {
            qty = additionalDto.getStockQty();
        }

        // Re-create billItem for this batch (since previous one was cleared)
        getBillItem();

        double thisTimeAddingQty = addBillItemSingleItem();
        addedQty += thisTimeAddingQty;
        remainingQty -= thisTimeAddingQty;

        System.out.println("Added " + thisTimeAddingQty + " from batch. Total added: " +
                          addedQty + ", Remaining: " + remainingQty);
    }

    // Final result
    if (addedQty < requestedQty) {
        errorMessage = "Quantity is not Enough...!";
        System.out.println("=== MULTIPLE BATCHES: Insufficient quantity ===");
        System.out.println("Requested: " + requestedQty + ", Total Added: " + addedQty);
        JsfUtil.addErrorMessage("Only " + String.format("%.0f", addedQty) +
                               " is available from the requested quantity");
    } else {
        System.out.println("SUCCESS: Fulfilled entire request of " + requestedQty +
                          " from " + (additionalStockDtos.size() + 1) + " batches");
    }

    System.out.println("=== CASHIER addBillItemMultipleBatches END ===");
}
```

#### Step 3: Update StockDTO Constructor (if needed)

Check if `StockDTO` has a constructor that includes `itemId`. If not, add:

**Location**: `StockDTO.java`

```java
// Constructor for multiple batches lookup (includes itemId for filtering)
public StockDTO(Long id, Long itemBatchId, Long itemId, String itemName, String code,
                String genericName, String batchNo, Double retailRate, Double stockQty,
                Date dateOfExpire, Boolean discountAllowed) {
    this.id = id;
    this.itemBatchId = itemBatchId;
    this.itemId = itemId;
    this.itemName = itemName;
    this.code = code;
    this.genericName = genericName;
    this.batchNo = batchNo;
    this.retailRate = retailRate;
    this.stockQty = stockQty;
    this.dateOfExpire = dateOfExpire;
    this.discountAllowed = discountAllowed;
}
```

And add field if missing:
```java
private Long itemId;

public Long getItemId() {
    return itemId;
}

public void setItemId(Long itemId) {
    this.itemId = itemId;
}
```

#### Step 4: Update `completeAvailableStockOptimizedDto()` to Include itemId

**Location**: `PharmacySaleForCashierController.java` line ~1173

Update the DTO constructor in the JPQL to include `itemId`:

```java
StringBuilder sql = new StringBuilder("SELECT NEW com.divudi.core.data.dto.StockDTO(")
    .append("i.id, i.itemBatch.id, i.itemBatch.item.id, ")  // Added itemBatchId and itemId
    .append("i.itemBatch.item.name, i.itemBatch.item.code, ")
    .append("COALESCE(i.itemBatch.item.vmp.name, ''), ")
    .append("i.itemBatch.batchNo, ")  // Added batchNo
    .append("i.itemBatch.retailsaleRate, i.stock, i.itemBatch.dateOfExpire, ")
    .append("i.itemBatch.item.discountAllowed) ")  // Added discountAllowed
    .append("FROM Stock i ")
    // ... rest of query
```

## Files to Modify

### Primary Changes

1. **`PharmacySaleForCashierController.java`**
   - Add `findNextAvailableStockDtos()` method (~30 lines)
   - Refactor `addBillItemMultipleBatches()` method (~80 lines replacement)
   - Update `completeAvailableStockOptimizedDto()` JPQL query (1 line)

2. **`StockDTO.java`** (if needed)
   - Add `itemId` field + getter/setter (~10 lines)
   - Add/update constructor to include itemId and other fields (~15 lines)

### Testing Files

3. **Manual testing checklist** (create separate test document)

## Testing Plan

### Test Case 1: Single Batch Fulfillment
**Scenario**: Requested qty ≤ first batch stockQty

**Steps**:
1. Enable config: "Add quantity from multiple batches in pharmacy retail billing" = true
2. Search and select item with stockQty = 100
3. Enter qty = 50
4. Click Add

**Expected**:
- ✅ Item added successfully with qty = 50
- ✅ No error messages
- ✅ Console: "First batch fulfilled entire request. Added: 50.0"

### Test Case 2: Multiple Batch Fulfillment - Full
**Scenario**: Requested qty > first batch, but total stock sufficient

**Steps**:
1. Enable multiple batches mode
2. Select item Batch A with stockQty = 30
3. Ensure Batch B exists for same item with stockQty = 50
4. Enter qty = 70
5. Click Add

**Expected**:
- ✅ 2 bill items added (30 from Batch A, 40 from Batch B)
- ✅ No error messages
- ✅ Console: "SUCCESS: Fulfilled entire request of 70.0 from 2 batches"

### Test Case 3: Multiple Batch Fulfillment - Partial
**Scenario**: Requested qty > total available stock

**Steps**:
1. Enable multiple batches mode
2. Select item Batch A with stockQty = 30
3. Ensure Batch B exists for same item with stockQty = 20 (total = 50)
4. Enter qty = 100
5. Click Add

**Expected**:
- ✅ 2 bill items added (30 + 20 = 50 total)
- ✅ **Single** error message: "Only 50 is available from the requested quantity"
- ✅ No duplicate error messages
- ✅ Console: "Requested: 100.0, Total Added: 50.0"

### Test Case 4: Expired Batch Skipping
**Scenario**: Additional batches are expired

**Steps**:
1. Enable multiple batches mode
2. Select item Batch A (not expired) with stockQty = 10
3. Ensure Batch B exists but is expired with stockQty = 50
4. Enter qty = 50
5. Click Add

**Expected**:
- ✅ Only Batch A added (qty = 10)
- ✅ Error: "Only 10 is available from the requested quantity"
- ❌ Expired batch should not be added
- ✅ Console: "Found 0 additional batches" (expired filtered by query)

### Test Case 5: Disable Multiple Batches Mode
**Scenario**: Config disabled - should work as single batch

**Steps**:
1. **Disable** config: "Add quantity from multiple batches in pharmacy retail billing" = false
2. Select item with stockQty = 30
3. Enter qty = 50
4. Click Add

**Expected**:
- ❌ Error: "Insufficient stock. Available: 30, Requested: 50"
- ❌ Item NOT added to bill
- ✅ Single batch validation works correctly

## Acceptance Criteria

### Functional Requirements
- [ ] Multiple batches mode successfully adds items from multiple batches to fulfill quantity
- [ ] First batch (FIFO by expiry date) is used first
- [ ] Subsequent batches are added in expiry date order until qty fulfilled
- [ ] If total stock insufficient, add all available and show **single** error message
- [ ] No duplicate error messages
- [ ] No NULL stockDto errors in console
- [ ] Works with existing discount schemes and payment methods
- [ ] Works with allergy checking if enabled

### Technical Requirements
- [ ] Uses DTO-based approach (no entity queries in main flow)
- [ ] `findNextAvailableStockDtos()` method implemented and tested
- [ ] `lastAutocompleteResults` cache preserved throughout operation
- [ ] Console logging shows clear flow of batch additions
- [ ] No performance degradation (DTO queries should be fast)
- [ ] Code follows existing patterns in `PharmacySaleForCashierController`

### Code Quality
- [ ] All debug `System.out.println()` statements can be removed or converted to proper logging
- [ ] JavaDoc comments added to new methods
- [ ] No commented-out code blocks
- [ ] Follows project coding standards

## Non-Goals (Out of Scope)

- Batch selection UI (user cannot manually choose batches - system auto-selects)
- Cross-department stock lookup (stays within user's department)
- Pre-commit stock reservation (existing behavior maintained)
- User stock tracking (already commented out for performance)

## References

### Related Code Patterns

**PharmacyFastRetailSaleController.java** - Already implements working DTO cache pattern:
```java
private List<StockDTO> cachedStockDtos;  // Line ~200
// Used by converter to find DTOs
```

**StockController.java** - `findNextAvailableStocks()` entity-based method:
```java
public List<Stock> findNextAvailableStocks(Stock s) {
    // This is what we're replacing with DTO version
}
```

### Configuration

**Config Key**: `"Add quantity from multiple batches in pharmacy retail billing"`
- **Type**: Boolean
- **Default**: false (recommended until this issue is fixed)
- **Location**: Application configuration table

## Implementation Notes

### Why DTOs Instead of Entities?

1. **Performance**: DTO queries are optimized, fetch only needed fields
2. **Consistency**: Rest of the page uses DTOs (stockDto, lastAutocompleteResults)
3. **Converter Compatibility**: StockDtoConverter expects DTOs, not entities
4. **Architecture**: Project is moving toward DTO-first approach

### Migration Path

1. **Phase 1** (This Issue): Implement DTO-based multiple batches
2. **Phase 2** (Future): Remove entity conversion methods if no longer needed
3. **Phase 3** (Future): Apply same pattern to other pharmacy sale pages

## Questions for Developer

Before starting implementation:

1. Should `findNextAvailableStockDtos()` filter by expiry date or some other criteria?
2. Do we need to check for "allow negative stock" configuration?
3. Should reserved quantities be considered (if stock reservation is re-enabled)?
4. Do we need to log batch additions for audit trail?

## Estimated Effort

- **Research & Planning**: 2 hours (reading existing code, understanding flow)
- **Implementation**: 4-6 hours (new method + refactor + DTO updates)
- **Testing**: 2-3 hours (manual test cases + edge cases)
- **Code Review & Fixes**: 1-2 hours
- **Total**: ~10-12 hours

## Branch Naming Convention

```
feature/fix-multiple-batches-dto-pharmacy-cashier-sale
```

Or use issue number if creating GitHub issue:
```
feature/issue-XXXX-multiple-batches-dto-fix
```

---

## Additional Context from Investigation

### Original Error Investigation Log

User reported duplicate error messages when multiple batches mode was enabled:
- "Please select an Item Batch to Dispense ??" × 4
- "Already added this item batch" × 2
- "Only 0 is Available form the Requested Quantity"

Debug logging revealed:
```
INFO:   === CASHIER addBillItemMultipleBatches START ===
INFO:   === CASHIER addBillItemSingleItem START ===
INFO:   SUCCESS: Adding item to bill - ID: 488787, Qty: 1.0
INFO:   === CASHIER addBillItemSingleItem START ===
INFO:   Validation FAILED: stockDto is NULL
[Repeated 3 more times]
```

This led to discovering that:
1. Config "Add quantity from multiple batches" was enabled
2. First call succeeded, subsequent calls failed due to NULL stockDto
3. Root cause: `clearBillItem()` nulling stockDto after first batch
4. Loop using entity-based `findNextAvailableStocks()` incompatible with DTO approach

### Workaround Applied

Temporary fix: Disabled multiple batches mode
- Config set to `false`
- Single batch mode works correctly
- This issue tracks permanent fix for multiple batches mode

---

**Created by**: Investigation on 2025-01-16
**For**: HMIS Pharmacy Module
**Component**: Retail Sale for Cashier
**Issue Type**: Bug Fix + Feature Enhancement
