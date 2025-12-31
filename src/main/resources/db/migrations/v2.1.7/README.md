# Migration v2.1.7: Fix TOTALCOSTVALUE Decimal Precision

## Overview
This migration fixes a decimal precision issue in Stock Transfer Reports where the Summary report type displays cost values as whole numbers while the Breakdown Summary displays them with decimal places.

## Issue Description
- **Problem**: In `reports/inventoryReports/stock_transfer_report.xhtml`, decimal places are not displayed for Cost Rate and Cost Value when Report Type is "Summary"
- **Root Cause**: `BILLFINANCEDETAILS.TOTALCOSTVALUE` column is defined as `decimal(38,0)` (no decimal places) while individual item records in `BILLITEMFINANCEDETAILS.VALUEATCOSTRATE` are `decimal(18,4)` (4 decimal places)
- **Impact**: Inconsistent display between Summary (shows "150") and Breakdown Summary (shows "150.0000")

## Database Investigation Results
```sql
-- Current column definitions
BILLFINANCEDETAILS.TOTALCOSTVALUE: decimal(38,0)     -- Problem: 0 decimal places
BILLITEMFINANCEDETAILS.VALUEATCOSTRATE: decimal(18,4)  -- Correct: 4 decimal places

-- Sample data showing the issue
SELECT TOTALCOSTVALUE FROM BILLFINANCEDETAILS WHERE TOTALCOSTVALUE > 0 LIMIT 5;
-- Results: 75, 575, 30, 80, 180 (no decimals)

SELECT VALUEATCOSTRATE FROM BILLITEMFINANCEDETAILS WHERE VALUEATCOSTRATE > 0 LIMIT 5;
-- Results: 75.0000, 575.0000, 30.0003, 79.9997, 179.9996 (with decimals)
```

## Solution
Change `BILLFINANCEDETAILS.TOTALCOSTVALUE` from `decimal(38,0)` to `decimal(38,4)` to preserve decimal precision during aggregation.

## Files in this Migration
- `migration.sql` - The actual ALTER TABLE statement
- `rollback.sql` - Rollback script (WARNING: loses decimal precision)
- `migration-info.json` - Detailed metadata for the migration system
- `README.md` - This documentation

## Testing Instructions

### Before Migration
1. Navigate to: Reports > Inventory Reports > Stock Transfer Report
2. Set Report Type to "Summary"
3. Generate report - observe Cost Value shows whole numbers (e.g., "150")
4. Change Report Type to "Breakdown Summary"
5. Generate report - observe values show decimals (e.g., "150.0000")

### After Migration
1. Repeat above steps
2. Both Summary and Breakdown Summary should show consistent decimal format
3. Summary report Cost Value should display as "150.0000" instead of "150"

### Verification Queries
```sql
-- Check column definition after migration
SELECT COLUMN_NAME, COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'BILLFINANCEDETAILS' AND COLUMN_NAME = 'TOTALCOSTVALUE';
-- Expected: TOTALCOSTVALUE | decimal(38,4)

-- Sample data after migration
SELECT TOTALCOSTVALUE FROM BILLFINANCEDETAILS WHERE TOTALCOSTVALUE > 0 LIMIT 5;
-- Expected: 75.0000, 575.0000, 30.0000, 80.0000, 180.0000
```

## Risk Assessment
- **Risk Level**: LOW
- **Data Loss**: None (integers converted to .0000 format)
- **Downtime**: None required
- **Rollback**: Available but loses decimal precision

## Related Files
- `/src/main/webapp/reports/inventoryReports/stock_transfer_report.xhtml` - Report template
- `/src/main/java/com/divudi/bean/pharmacy/PharmacyController.java` - Report generation logic
- `/src/main/java/com/divudi/core/data/dataStructure/PharmacySummery.java` - Summary data structure

## Author
Claude Code Assistant (2025-12-22)

## Branch
17452-decimal-places-are-not-displayed