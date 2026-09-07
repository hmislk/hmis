# StockHistory Archive Read Paths

**Issue**: [#20729](https://github.com/hmislk/hmis/issues/20729)  
**Branch**: `20729-stockhistory-archive-read-paths`  
**Related**: [#20726](https://github.com/hmislk/hmis/issues/20726) (StockHistory archival), [#20978](https://github.com/hmislk/hmis/issues/20978) (REST API includeArchived — deferred)

## Background

Issue #20726 introduced archival of `StockHistory` rows older than the configured retention period (default 2 years) into a separate `StockHistoryArchive` table. This document audits all read paths that query `StockHistory` and records which ones were updated to optionally include archived data.

MySQL partitioning was considered but ruled out: `StockHistory` has 7 foreign key constraints, and MySQL cannot partition tables with FK constraints.

## Architecture: Two-Entity Strategy

`StockHistoryArchive` is a JPA entity with the same schema as `StockHistory` (same columns, same FK relationships). All JPQL queries that work on `StockHistory` work identically on `StockHistoryArchive` by substituting the entity name.

Two merge strategies are used depending on query type:

### 1. Two-Query Merge (for JPQL date-range queries)
Run the same JPQL against both entities, merge results in Java, sort by `createdAt`. Used by:
- Bin card (DTO)
- Stock ledger (DTO)
- `fillStockHistories()` in the department stock history page

### 2. Native SQL UNION (for MAX(id) subquery patterns)
The inner `MAX(id)` subquery cannot be expressed in JPQL with two entities. A private helper `shUnionSrc(columns)` in `StockHistoryFacade` builds:
```sql
(SELECT <columns> FROM STOCKHISTORY UNION ALL SELECT <columns> FROM STOCKHISTORYARCHIVE)
```
Used by the three `calculateStockValueAt*RateOptimized` methods.

### 3. ID-Based Deduplication (for closing stock reports)
Closing stock reports compute the latest QOH per item or batch. Since STOCKHISTORY always holds records newer than STOCKHISTORYARCHIVE (archival moves old rows out), live results always take precedence. Archive rows are added only if their item/batch ID is not already present in live results.

## Affected Read Paths

### A. Updated — "Include Archived" Toggle Added

| Report Page | Backing Bean | Method(s) Updated | Toggle Field |
|---|---|---|---|
| `pharmacy/bin_card_dto.xhtml` | `PharmacyErrorChecking` | `processBinCardWithDTO()` → `findBinCardDTOs(6-arg)` | `includeArchived` |
| `reports/inventoryReports/stock_ledger_dto.xhtml` | `PharmacyReportController` | `processStockLedgerDtoReport()` | `includeArchived` |
| `reports/inventoryReports/closing_stock_report.xhtml` | `PharmacyReportController` | `processClosingStockForItemReport()`, `processClosingStockForBatchReport()` | `includeArchived` |
| `pharmacy/pharmacy_department_stock_history.xhtml` | `StockHistoryController` | `fillHistoryAvailableDays()`, `fillStockHistories(boolean)` | `includeArchived` |
| Daily Stock Balance Report | `PharmacySummaryReportController` | `calculateStockValueAtRetailRate(Date, Department)` (private) | `includeArchived` |

### B. Not Updated — Archive Not Required

| Location | Reason |
|---|---|
| `DataAdministrationController.repairStockHistoryRates()` | Operates on recent data only; not a user-facing report |
| REST API `GET /stock_history` (`StockHistoryResource`) | Deferred to issue #20978 |

## Implementation Details

### StockHistoryFacade

**New method overloads** (3-arg, `includeArchived` flag):
- `calculateStockValueAtRetailRateOptimized(Date, Long departmentId, boolean includeArchived)`
- `calculateStockValueAtCostRateOptimized(Date, Long departmentId, boolean includeArchived)`
- `calculateStockValueAtPurchaseRateOptimized(Date, Long departmentId, boolean includeArchived)`

When `includeArchived=false`, each delegates to the existing 2-arg overload (no change in behavior). When `true`, the inner `MAX(id)` subquery is rewritten as a native SQL UNION query using the `shUnionSrc()` helper.

**Private helper**:
```java
private String shUnionSrc(String columns) {
    return "(SELECT " + columns + " FROM STOCKHISTORY UNION ALL SELECT " + columns
           + " FROM STOCKHISTORYARCHIVE)";
}
```

### StockHistoryController

Added:
- `@EJB StockHistoryArchiveFacade archiveFacade`
- `boolean includeArchived = false` (with getter/setter)
- `List<StockHistoryArchive> pharmacyStockHistoriesArchive` (read-only getter; populated when `includeArchived=true`)

`fillHistoryAvailableDays()`: when `includeArchived=true`, queries archive entity for date list and merges (sorted descending).

`fillStockHistories(boolean withoutZeroStock)`: when `includeArchived=true`, also queries archive entity via `archiveFacade.findByJpql(...)`, stores results in `pharmacyStockHistoriesArchive`, and adds archive stock values to controller totals.

`findBinCardDTOs(6-arg)`: runs live JPQL then, if `withArchive=true`, runs same JPQL against `StockHistoryArchive` (entity name substitution), merges and sorts by `PharmacyBinCardDTO.getCreatedAt()`.

### PharmacyErrorChecking

Added `boolean includeArchived = false`. `processBinCardWithDTO()` passes `includeArchived` to `findBinCardDTOs(6-arg)`.

### PharmacyReportController

Added `boolean includeArchived = false` (with `isIncludeArchived()` / `setIncludeArchived()`).

`processStockLedgerDtoReport()`: after live JPQL, when `includeArchived=true` runs archive query (entity name substitution), appends results, sorts by `StockLedgerDTO.getCreatedAt()`.

`processClosingStockForItemReport()`: after live rows processed, when `includeArchived=true` runs archive JPQL, deduplicates by `row.getItem().getId()` (live takes precedence), applies same scope/consignment logic as live.

`processClosingStockForBatchReport()`: same pattern, deduplicates by `row.getItemBatch().getId()`.

### PharmacySummaryReportController

Added `boolean includeArchived = false`. `calculateStockValueAtRetailRate(Date, Department)`: when `includeArchived=true`, immediately delegates to `stockHistoryFacade.calculateStockValueAtRetailRateOptimized(date, dept != null ? dept.getId() : null, true)` which uses the native SQL UNION path.

## XHTML Changes

Each report page gained a `<p:selectBooleanCheckbox>` bound to the backing bean's `includeArchived` property:

- `bin_card_dto.xhtml`: checkbox before the Generate Report button
- `stock_ledger_dto.xhtml`: checkbox above the Process button row
- `closing_stock_report.xhtml`: checkbox below the Consignment Item checkbox (matching existing style)
- `pharmacy_department_stock_history.xhtml`: checkbox before Display Available Days + separate `tblArchiveHistories` data table rendered when `pharmacyStockHistoriesArchive` is non-empty

## Deduplication Key Reference

| Report | Dedup Key | Rationale |
|---|---|---|
| Closing stock (item-wise) | `row.getItem().getId()` | `PharmacyRow` 18-arg constructor: first arg sets `item.id` |
| Closing stock (batch-wise) | `row.getItemBatch().getId()` | `PharmacyRow` 24-arg constructor: 8th arg sets `itemBatch.id` |
| Bin card / Stock ledger | No dedup — time-series rows | All rows from both tables are shown in chronological order |
| Stock history page | No dedup — snapshot rows | Archive rows shown in separate table section |
