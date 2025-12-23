# Migration v2.1.4 - Database Index Creation

## Overview
This migration adds composite indexes for performance optimization on:
- **USER_STOCK**: Optimizes stock availability queries
- **USERSTOCKCONTAINER**: Optimizes join filtering and Add button queries
- **PRICEMATRIX**: Optimizes Settle button discount calculations

**Expected Performance Improvement**: 50-150ms → 5-15ms (10x faster)

## Files

### Main Migration Script
- **migration.sql** - Complete migration (all steps combined)
  - Use this when running through the migration admin page
  - Fixed to avoid `PREPARE` statement errors with NULL variables

### Individual Scripts (Can be run separately)
1. **01-pre-migration-checks.sql** - Pre-migration verification (read-only)
2. **02-create-userstock-indexes.sql** - Create USER_STOCK indexes
3. **03-create-userstockcontainer-indexes.sql** - Create USERSTOCKCONTAINER indexes
4. **04-create-pricematrix-indexes.sql** - Create PRICEMATRIX indexes
5. **05-post-migration-verification.sql** - Post-migration verification and performance testing

### Other Files
- **rollback.sql** - Rollback script to drop all created indexes
- **migration-info.json** - Migration metadata
- **README.md** - This file

## Running the Migration

### Option 1: Through Admin Page (Recommended)
1. Navigate to `/rhLocal/faces/admin/database_migration.xhtml`
2. Click "Execute All Migrations" button
3. The fixed `migration.sql` will run all steps automatically

### Option 2: Run Individual Scripts (Manual)
Useful for testing or troubleshooting specific parts:

```bash
# 1. Pre-migration checks (optional, read-only)
mysql -u[user] -p[password] [database] < 01-pre-migration-checks.sql

# 2. Create USER_STOCK indexes
mysql -u[user] -p[password] [database] < 02-create-userstock-indexes.sql

# 3. Create USERSTOCKCONTAINER indexes
mysql -u[user] -p[password] [database] < 03-create-userstockcontainer-indexes.sql

# 4. Create PRICEMATRIX indexes
mysql -u[user] -p[password] [database] < 04-create-pricematrix-indexes.sql

# 5. Post-migration verification (optional)
mysql -u[user] -p[password] [database] < 05-post-migration-verification.sql
```

### Option 3: Run Using Credentials File
```bash
# Load credentials
source C:/Credentials/credentials.txt  # Windows
source ~/.config/hmis/credentials.txt  # Linux/Mac

# Run scripts
mysql -u$DB_USER -p$DB_PASS $DB_NAME < 02-create-userstock-indexes.sql
```

## What Was Fixed

### Original Issue
The migration failed with:
```
SQLSyntaxErrorException: You have an error in your SQL syntax;
check the manual that corresponds to your MySQL server version
for the right syntax to use near 'NULL' at line 1
Error Code: 1064
Call: PREPARE count_stmt FROM @record_count_sql
```

### Root Cause
When the migration runner executes SQL statements one-by-one, MySQL user variables set in one statement (like `@record_count_sql`) were not properly available for subsequent `PREPARE` statements, resulting in NULL values.

### Solution
Replaced all dynamic `PREPARE` statements with:
1. **Direct SQL queries with UNION** - For record counting
2. **Direct EXPLAIN statements** - For performance verification

The fixed migration is:
- ✅ **Idempotent** - Safe to run multiple times (uses `IF NOT EXISTS`)
- ✅ **No dynamic SQL** - Avoids PREPARE statement issues
- ✅ **Statement-by-statement safe** - Works with any execution method
- ✅ **Fail-safe** - EXPLAIN queries may fail gracefully if tables don't exist

## Safety Features

### Idempotent Operations
All index creation uses `CREATE INDEX IF NOT EXISTS`, so:
- Safe to re-run if migration fails partway through
- Won't fail if indexes already exist
- Won't create duplicate indexes

### No Data Modifications
This migration only adds indexes:
- No data is modified
- No data is deleted
- Schema changes only
- Safe to rollback at any time

### Table Name Case Handling
Handles both uppercase and lowercase table names:
- `USER_STOCK` / `userstock`
- `USERSTOCKCONTAINER` / `userstockcontainer`
- `PRICEMATRIX` / `pricematrix`

Works on both:
- Production (Ubuntu) - typically uppercase
- Development (Windows) - typically lowercase

## Verification

### Check if indexes were created
```sql
-- USER_STOCK index
SELECT INDEX_NAME, COLUMN_NAME, SEQ_IN_INDEX
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('USER_STOCK', 'userstock')
  AND INDEX_NAME = 'idx_user_stock_fast_lookup'
ORDER BY SEQ_IN_INDEX;

-- USERSTOCKCONTAINER indexes
SELECT DISTINCT INDEX_NAME
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('USERSTOCKCONTAINER', 'userstockcontainer')
  AND INDEX_NAME LIKE 'idx_userstockcontainer%';

-- PRICEMATRIX indexes
SELECT DISTINCT INDEX_NAME
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('PRICEMATRIX', 'pricematrix')
  AND INDEX_NAME LIKE 'idx_pricematrix%';
```

### Test query performance
```sql
-- Should use idx_user_stock_fast_lookup index
EXPLAIN SELECT SUM(us.UPDATIONQTY)
FROM userstock us
WHERE us.RETIRED = 0
  AND us.STOCK_ID = 1
  AND us.CREATER_ID != 1
  AND us.CREATEDAT BETWEEN DATE_SUB(NOW(), INTERVAL 30 MINUTE) AND NOW();
```

## Rollback

To remove all created indexes:
```bash
mysql -u[user] -p[password] [database] < rollback.sql
```

Or through the admin page:
1. Navigate to database migration page
2. Select v2.1.4
3. Click "Rollback" button

## Expected Results

After successful migration:

### Indexes Created
- **USER_STOCK**: 1 composite index (4 columns)
- **USERSTOCKCONTAINER**: 3 indexes (1 single-column, 2 composite)
- **PRICEMATRIX**: 2 composite indexes

### Performance Improvements
- **Stock selection**: 10x faster (50-150ms → 5-15ms)
- **Add button**: Faster user stock container queries
- **Settle button**: Faster discount calculations

### User Experience
- Noticeable reduction in pharmacy retail sale lag
- Faster item addition
- Faster settlement

## Troubleshooting

### Migration still fails?
1. Check MySQL error log for details
2. Run individual scripts to isolate the problem
3. Run `01-pre-migration-checks.sql` to verify tables exist
4. Ensure user has INDEX privilege: `GRANT INDEX ON database.* TO 'user'@'host';`

### EXPLAIN queries fail?
This is normal if:
- Tables don't exist (development environment)
- Tables have different names than expected
- EXPLAIN errors don't affect index creation

### Index not being used?
1. Check query structure matches expected pattern
2. Analyze table statistics: `ANALYZE TABLE userstock;`
3. Check index cardinality in INFORMATION_SCHEMA.STATISTICS

## Technical Notes

### Index Column Order
**USER_STOCK** index optimized for query selectivity:
1. `STOCK_ID` - Most selective (specific stock lookup)
2. `RETIRED` - Boolean filter (early elimination)
3. `CREATEDAT` - Date range filter (30-minute window)
4. `CREATER_ID` - User exclusion (NOT equal condition)

### Query Pattern
```sql
SELECT sum(us.UPDATIONQTY) FROM UserStock us
WHERE us.retired = false
  AND us.userStockContainer.retired = false
  AND us.stock = :stk
  AND us.creater != :wb
  AND us.createdAt BETWEEN :frm AND :to
```

### USERSTOCKCONTAINER Indexes
- `idx_userstockcontainer_retired` - Critical for JOIN filtering (587K retired vs 10 active!)
- `idx_userstockcontainer_retired_created` - Future optimization potential
- `idx_userstockcontainer_creater_retired` - Optimizes Add button query

### PRICEMATRIX Indexes
- `idx_pricematrix_payment_dept_category` - Payment + Department + Category lookups
- `idx_pricematrix_payment_category` - Payment + Category lookups (no department)

## GitHub Issue
Closes #16990 - Speed up the pharmacy retail sale

## Author
Dr M H B Ariyaratne

## Date
2025-12-03 (Updated: 2025-12-22 - Fixed PREPARE statement issues)
