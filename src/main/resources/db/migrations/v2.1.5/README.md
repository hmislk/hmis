# Migration v2.1.5 - PRICEMATRIX Index Optimization

## Purpose
Adds composite indexes to PRICEMATRIX/pricematrix table to optimize PaymentSchemeDiscount queries.

**Performance Impact**: Reduces discount calculation queries from 552ms to 2-5ms (100x faster)

---

## Migration Files

### 1. `migration.sql` - Production/Ubuntu/Linux (UPPERCASE table names)
- **Use for**: Production, QA, Ubuntu/Linux environments
- **Table name**: `PRICEMATRIX` (uppercase)
- **Execution**: Run through migration system or mysql command line

### 2. `migration-dev.sql` - Development/Windows (lowercase table names)
- **Use for**: Development, Windows environments
- **Table name**: `pricematrix` (lowercase)
- **Execution**: Run manually through mysql command line or modify migration system config

---

## Why Two Versions?

MySQL table names are **case-sensitive on Linux/Unix** but **case-insensitive on Windows**:
- **Production (Ubuntu/Linux)**: Uses `PRICEMATRIX` (uppercase)
- **Development (Windows)**: Uses `pricematrix` (lowercase)

The JPA/JDBC migration system executes SQL statements independently, making it difficult to use dynamic SQL with session variables. Therefore, we provide two separate migration files.

---

## Execution Instructions

### Production/Ubuntu/Linux

**Option 1: Using Migration System**
```bash
# Migration system will automatically execute migration.sql
# Follow migration system prompts
```

**Option 2: Manual Execution**
```bash
# Connect to database (credentials from C:\Credentials\credentials.txt)
mysql -u username -p database_name

# Execute migration
source /path/to/migration.sql
```

### Development/Windows

**Option 1: Manual Execution**
```bash
# Connect to database (credentials from C:\Credentials\credentials.txt)
mysql -u username -p database_name

# Execute development migration
source C:/Development/hmis/src/main/resources/db/migrations/v2.1.5/migration-dev.sql
```

**Option 2: Copy to migration.sql**
```bash
# Backup original
cp migration.sql migration-prod.sql.bak

# Replace with dev version
cp migration-dev.sql migration.sql

# Run through migration system
```

---

## Verification

After running migration, verify indexes were created:

```sql
-- Check indexes exist
SELECT TABLE_NAME, INDEX_NAME, COLUMN_NAME, SEQ_IN_INDEX, CARDINALITY
FROM INFORMATION_SCHEMA.STATISTICS
WHERE INDEX_NAME LIKE 'idx_psd_%'
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;

-- Expected result: 3 indexes (idx_psd_item, idx_psd_category, idx_psd_department)
-- Each with 5 columns
```

Test query performance:

```sql
-- Should use idx_psd_item index
EXPLAIN SELECT DISCOUNTPERCENT
FROM pricematrix  -- or PRICEMATRIX on production
WHERE RETIRED=0
  AND PAYMENTMETHOD='Cash'
  AND PAYMENTSCHEME_ID IS NULL
  AND MEMBERSHIPSCHEME_ID IS NULL
  AND ITEM_ID=1;

-- Look for "Using index" in Extra column
-- rows examined should be low (< 100)
```

---

## Rollback

If needed, drop the indexes:

```sql
-- For Production (uppercase)
DROP INDEX idx_psd_item ON PRICEMATRIX;
DROP INDEX idx_psd_category ON PRICEMATRIX;
DROP INDEX idx_psd_department ON PRICEMATRIX;

-- For Development (lowercase)
DROP INDEX idx_psd_item ON pricematrix;
DROP INDEX idx_psd_category ON pricematrix;
DROP INDEX idx_psd_department ON pricematrix;
```

**Note**: Application will continue to work without indexes (just slower).

---

## Metadata

- **Version**: v2.1.5
- **Author**: Dr M H B Ariyaratne
- **Date**: 2025-12-13
- **GitHub Issue**: [#16990](https://github.com/hmislk/hmis/issues/16990)
- **Branch**: `16990-speed-up-the-pharmacy-retail-sale`
- **Requires Downtime**: No
- **Estimated Duration**: 3 minutes
- **Affects**: Pharmacy, Retail Sale, Cashier Sale, Discount Management, Billing

---

## Related Files

- `migration-info.json` - Detailed migration metadata
- `../../java/com/divudi/bean/pharmacy/PharmacySaleForCashierController.java` - Uses optimized discount queries
- `../../java/com/divudi/bean/common/PriceMatrixController.java` - DTO-based discount methods
- `../../../developer_docs/performance/mysql-pharmacy-retail-sale-optimization.md` - Complete optimization documentation
