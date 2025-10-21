# BinCard DTO Enhancement for Hotfix Merge Compatibility

## Overview
This document describes the changes made to support merging `southernlanka-prod-bincard-update-hotfix` into the `development` branch, which has converted bincard functionality to use DTOs.

## Problem
- **Development branch**: Uses `PharmacyBinCardDTO` for bincard display (efficient, lightweight)
- **Hotfix branch**: Uses `StockHistory` entities directly with additional fields:
  - `stockQty` for "Balance Qty (Batch)" column
  - `batchNo` for "Batch Number" column

## Solution
Enhanced the existing DTO approach to include the missing fields, providing backward compatibility for future merges.

## Files Modified

### 1. Created: `PharmacyBinCardDTO.java`
**Location**: `src/main/java/com/divudi/core/data/dto/PharmacyBinCardDTO.java`

**New fields added**:
- `Double stockQty` - Stock quantity for batch-level tracking
- `String batchNo` - Batch number for item batch tracking

**Constructor updated** to include these fields in the projection query.

### 2. Enhanced: `StockHistoryController.java`
**Location**: `src/main/java/com/divudi/bean/pharmacy/StockHistoryController.java`

**Changes**:
- Added import for `PharmacyBinCardDTO`
- Added `findBinCardDTOs()` method with enhanced JPQL query including:
  - `s.stockQty` 
  - `s.pbItem.stock.itemBatch.batchNo`
- Added `navigateToViewStockHistoryById(Long id)` method for DTO-based navigation

### 3. Enhanced: `PharmacyErrorChecking.java`
**Location**: `src/main/java/com/divudi/bean/pharmacy/PharmacyErrorChecking.java`

**Changes**:
- Added import for `PharmacyBinCardDTO`
- Added `binCardEntries` field for DTO-based results
- Added `processBinCardWithDTO()` method for DTO-based processing
- Added getter/setter for `binCardEntries`

### 4. Created: `bin_card_dto.xhtml`
**Location**: `src/main/webapp/pharmacy/bin_card_dto.xhtml`

**Features**:
- Uses `#{pharmacyErrorChecking.binCardEntries}` (DTO-based)
- Displays all columns from hotfix including:
  - "Batch Number" → `#{sh.batchNo}`
  - "Balance Qty (Item)" → `#{sh.itemStock}`
  - "Balance Qty (Batch)" → `#{sh.stockQty}`
- Uses `processBinCardWithDTO()` action

## Column Mapping

| Display Name | Hotfix (Entity) | Enhanced DTO |
|--------------|-----------------|--------------|
| Stock Hx ID | `#{sh.id}` | `#{sh.id}` |
| Date & Time | `#{sh.createdAt}` | `#{sh.createdAt}` |
| Bill Type | `#{sh.pbItem.billItem.bill.billType.label}` | `#{sh.billType.label}` |
| Batch Number | `#{sh.pbItem.stock.itemBatch.batchNo}` | `#{sh.batchNo}` |
| Qty In | `#{sh.pbItem.transAbsoluteQtyPlusFreeQty}` | `#{sh.transAbsoluteQtyPlusFreeQty}` |
| Qty Out | `#{sh.pbItem.transAbsoluteQtyPlusFreeQty}` | `#{sh.transAbsoluteQtyPlusFreeQty}` |
| Balance Qty (Item) | `#{sh.itemStock}` | `#{sh.itemStock}` |
| Balance Qty (Batch) | `#{sh.stockQty}` | `#{sh.stockQty}` |

## Benefits

1. **Performance**: DTOs avoid loading full entity graphs
2. **Compatibility**: Supports all fields from hotfix
3. **Future-proof**: Enables smooth merging of hotfix into development
4. **Dual approach**: Both entity and DTO methods available

## Usage

### For Current Hotfix Branch
Use existing approach with `bin_card.xhtml` and `processBinCard()`.

### For Development/Future Branches
Use enhanced approach with `bin_card_dto.xhtml` and `processBinCardWithDTO()`.

### For Merged Branch
Both approaches available, recommend migrating to DTO for performance.

## Testing

- Compilation successful
- All dependencies resolved
- Navigation methods implemented for both approaches

## Merge Conflict Resolution

When merging this hotfix branch with development, the following conflicts were resolved:

### 1. PharmacyBinCardDTO.java
**Conflicts**: Missing `stockQty` and `batchNo` fields in development version
**Resolution**: Kept hotfix additions for both fields and their getters/setters

### 2. StockHistoryController.java  
**Conflicts**: DTO query missing enhanced fields
**Resolution**: Updated JPQL to include `s.stockQty, s.pbItem.stock.itemBatch.batchNo`

### 3. bin_card.xhtml
**Conflicts**: Different column structures between branches
**Resolution**: Used DTO-based approach with all hotfix columns:
- "Batch Number" → `#{sh.batchNo}`
- "Balance Qty (Item)" → `#{sh.itemStock}`  
- "Balance Qty (Batch)" → `#{sh.stockQty}`

### 4. Method Signature Updates
**Issue**: Development branch requires `TemporalType` parameter in facade methods
**Files Fixed**:
- `PharmacyBean.java` - Added `TemporalType.TIMESTAMP` parameter and import
- `PharmacyCalculation.java` - Added `TemporalType.TIMESTAMP` parameter

**Resolution Steps**:
1. Added `import javax.persistence.TemporalType;` to required files
2. Updated method calls to include `TemporalType.TIMESTAMP` parameter

## Related GitHub Issue

**Issue**: [#14938 - Add missing fields to PharmacyBinCardDTO for hotfix merge compatibility](https://github.com/hmislk/hmis/issues/14938)

## Migration Path

1. **Immediate**: Use current hotfix implementation
2. **Development merge**: Use enhanced DTO implementation  
3. **Future**: Migrate all bincard functionality to DTO approach
4. **Cleanup**: Remove entity-based approach after migration complete

---

**Generated with Claude Code**: This enhancement provides a smooth migration path for merging bincard hotfix changes into the DTO-based development branch.