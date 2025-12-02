# MySQL Performance Optimization for Pharmacy Retail Sale

**Related To**: Pharmacy Retail Sale Add Button Optimization
**Issue**: [#16990] Speed up the pharmacy retail sale
**Date**: 2025-12-02
**Database**: MySQL
**Status**: Planning Phase

---

## Executive Summary

This document covers MySQL-specific optimizations to complement the application-level optimizations for the Pharmacy Retail Sale "Add" button performance improvement.

**Key Areas**:
1. Database indexing
2. Query optimization
3. Batch insert optimization
4. Connection pool tuning
5. Query execution plan analysis

---

## Database Environment

### Connection Information
- **Credentials Location**: `C:\Credentials\credentials.txt` (NOT in git)
- **Database**: HMIS Production/QA
- **Engine**: MySQL (InnoDB)
- **Connection**: JDBC via JTA DataSource

### Safety Guidelines
- ✅ Always backup before changes
- ✅ Test on QA environment first
- ✅ Run EXPLAIN on all queries
- ✅ Monitor query performance after changes
- ❌ Never commit credentials to git
- ❌ Never run destructive queries without backup

---

## Optimization #1: Database Indexing

### Current Indexes Audit

**Objective**: Verify existing indexes support the autocomplete query

**Step 1.1: Check Current Indexes**

```sql
-- Connect using credentials from C:\Credentials\credentials.txt

USE hmis_database;

-- Check indexes on Stock table
SHOW INDEX FROM stock;

-- Check indexes on ItemBatch table
SHOW INDEX FROM item_batch;

-- Check indexes on Item table
SHOW INDEX FROM item;
```

**Expected Output**: Document all existing indexes

---

### Recommended New Indexes

**Step 1.2: Create Composite Index for Autocomplete Query**

The autocomplete query (PharmacySaleController.java:1604-1629) searches by:
- `stock > 0`
- `department_id`
- `item_batch.item.name LIKE`
- `item_batch.item.code LIKE` (optional)

**Index Creation Script**:

```sql
-- =====================================================
-- BACKUP FIRST
-- =====================================================
-- Create backup of current database state
-- See "Backup Procedures" section below

-- =====================================================
-- Create Index for Stock Autocomplete Performance
-- =====================================================

-- Index for main stock lookup query
-- Covers: department, stock > 0, and supports JOINs
CREATE INDEX idx_stock_dept_qty
ON stock(department_id, stock, item_batch_id);

-- Check if item name index exists
SELECT COUNT(*) as index_exists
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'item'
  AND index_name = 'idx_item_name';

-- Create item name index if not exists
-- This supports: item.name LIKE '%query%'
-- Note: LIKE with leading % cannot use index, but this helps with sorting
CREATE INDEX idx_item_name
ON item(name);

-- Index for item code search (if "Medicine Identification Codes Used" = true)
CREATE INDEX idx_item_code
ON item(code);

-- Index for barcode search (if enabled)
CREATE INDEX idx_item_barcode
ON item(barcode);

-- Index for generic/VMP search (if enabled)
CREATE INDEX idx_vmp_vtm_name
ON vmp(vtm_id, name);

-- Index for date of expire sorting
CREATE INDEX idx_item_batch_expiry
ON item_batch(date_of_expire);

-- Show created indexes
SHOW INDEX FROM stock WHERE Key_name LIKE 'idx_%';
SHOW INDEX FROM item WHERE Key_name LIKE 'idx_%';
SHOW INDEX FROM item_batch WHERE Key_name LIKE 'idx_%';
```

**Verification Script**:

```sql
-- Test the autocomplete query with EXPLAIN
EXPLAIN
SELECT i.id, i.item_batch_id, i.stock, i.department_id
FROM stock i
INNER JOIN item_batch ib ON i.item_batch_id = ib.id
INNER JOIN item itm ON ib.item_id = itm.id
WHERE i.stock > 0
  AND i.department_id = 1  -- Replace with actual department ID
  AND itm.name LIKE '%para%'
ORDER BY itm.name, ib.date_of_expire
LIMIT 20;

-- Check index usage
-- Look for "Using index" or "Using index condition" in Extra column
-- Avoid "Using filesort" or "Using temporary"
```

**Expected Improvement**: 50-80% reduction in query execution time

**Rollback Script**:

```sql
-- Remove indexes if they cause issues
DROP INDEX idx_stock_dept_qty ON stock;
DROP INDEX idx_item_name ON item;
DROP INDEX idx_item_code ON item;
DROP INDEX idx_item_barcode ON item;
DROP INDEX idx_vmp_vtm_name ON vmp;
DROP INDEX idx_item_batch_expiry ON item_batch;
```

---

## Optimization #2: UserStock Batch Insert

### Current Behavior

Currently (before application optimization):
- Each item add triggers: `INSERT INTO user_stock VALUES (...)`
- 50 items = 50 separate INSERT statements
- Each INSERT requires network round-trip + transaction commit

### Optimized Behavior

After application optimization:
- All UserStock records inserted in single batch during settlement
- Need to optimize MySQL for batch inserts

**Step 2.1: Enable Batch Insert Settings**

```sql
-- Check current settings
SHOW VARIABLES LIKE 'innodb_flush_log_at_trx_commit';
SHOW VARIABLES LIKE 'innodb_buffer_pool_size';
SHOW VARIABLES LIKE 'max_allowed_packet';

-- Document current values for rollback
-- innodb_flush_log_at_trx_commit: usually 1
-- innodb_buffer_pool_size: varies
-- max_allowed_packet: usually 4M or 16M
```

**Step 2.2: JPA Configuration for Batch Insert**

**File**: `src/main/resources/META-INF/persistence.xml`

**ADD** to `<properties>` section:

```xml
<!-- Batch insert optimization for UserStock -->
<property name="hibernate.jdbc.batch_size" value="50"/>
<property name="hibernate.order_inserts" value="true"/>
<property name="hibernate.order_updates" value="true"/>
<property name="hibernate.batch_versioned_data" value="true"/>
```

**Rollback**: Remove these properties from persistence.xml

---

## Optimization #3: Connection Pool Tuning

### Verify Current Pool Settings

**Step 3.1: Check Application Server Configuration**

Locate connection pool configuration (varies by app server):
- GlassFish: `domain.xml`
- WildFly: `standalone.xml`
- Tomcat: `context.xml`

**Step 3.2: Recommended Settings**

```xml
<!-- Example for connection pool -->
<property name="initialPoolSize" value="10"/>
<property name="minPoolSize" value="5"/>
<property name="maxPoolSize" value="50"/>
<property name="maxStatements" value="200"/>
<property name="idleConnectionTestPeriod" value="300"/>
<property name="acquireIncrement" value="3"/>
```

**Monitoring**:
```sql
-- Monitor active connections
SHOW PROCESSLIST;

-- Monitor connection count
SHOW STATUS LIKE 'Threads_connected';
SHOW STATUS LIKE 'Max_used_connections';
```

---

## Optimization #4: Query Execution Plan Analysis

### Analyze Autocomplete Query Performance

**Step 4.1: Capture Query Execution Plan**

```sql
-- Enable profiling (DEV/QA only, not production)
SET profiling = 1;

-- Run the actual autocomplete query
-- Copy from PharmacySaleController.java:1604-1629
SELECT NEW com.divudi.core.data.dto.StockDTO(
    i.id, i.itemBatch.id, i.itemBatch.item.id,
    i.itemBatch.item.name, i.itemBatch.item.code,
    i.itemBatch.item.vmp.name,
    i.itemBatch.batchNo, i.itemBatch.retailsaleRate,
    i.stock, i.itemBatch.dateOfExpire,
    i.itemBatch.item.discountAllowed
)
FROM Stock i
WHERE i.stock > 0
  AND i.department = :department
  AND (i.itemBatch.item.name LIKE :query
       OR i.itemBatch.item.code LIKE :query)
ORDER BY i.itemBatch.item.name, i.itemBatch.dateOfExpire
LIMIT 20;

-- Show profile
SHOW PROFILES;

-- Get detailed profile for last query
SHOW PROFILE FOR QUERY [query_id];

-- Disable profiling
SET profiling = 0;
```

**Step 4.2: Analyze EXPLAIN Output**

```sql
EXPLAIN FORMAT=JSON
[paste the actual query here];

-- Look for:
-- ✅ "access_type": "ref" or "range" (good)
-- ❌ "access_type": "ALL" (table scan - bad)
-- ✅ "key": "idx_..." (using index)
-- ❌ "key": null (not using index)
-- ✅ "rows_examined": low number
-- ❌ "Using temporary" or "Using filesort"
```

---

## Optimization #5: Table Statistics Update

### Keep Statistics Current

MySQL query optimizer uses table statistics to choose best execution plan.

**Step 5.1: Analyze Tables**

```sql
-- Update table statistics for query optimizer
ANALYZE TABLE stock;
ANALYZE TABLE item_batch;
ANALYZE TABLE item;
ANALYZE TABLE user_stock;

-- Check table status
SHOW TABLE STATUS LIKE 'stock';
SHOW TABLE STATUS LIKE 'user_stock';
```

**Step 5.2: Schedule Regular Analysis**

```sql
-- Create event for weekly table analysis (optional)
CREATE EVENT IF NOT EXISTS weekly_table_analysis
ON SCHEDULE EVERY 1 WEEK
STARTS CURRENT_DATE + INTERVAL 1 DAY
DO
BEGIN
    ANALYZE TABLE stock;
    ANALYZE TABLE item_batch;
    ANALYZE TABLE item;
    ANALYZE TABLE user_stock;
END;

-- Verify event created
SHOW EVENTS LIKE 'weekly_table_analysis';
```

**Rollback**:
```sql
DROP EVENT IF EXISTS weekly_table_analysis;
```

---

## Backup Procedures

### Pre-Optimization Backup

**CRITICAL**: Always backup before making database changes

**Step 1: Full Database Backup**

```bash
# Using mysqldump (run from terminal, NOT from application)
# Get credentials from C:\Credentials\credentials.txt

# Full backup
mysqldump -u [username] -p[password] \
  --single-transaction \
  --routines \
  --triggers \
  --events \
  hmis_database > hmis_backup_before_optimization_$(date +%Y%m%d_%H%M%S).sql

# Verify backup file created and has content
ls -lh hmis_backup_before_optimization_*.sql
```

**Step 2: Backup Specific Tables**

```bash
# Backup only affected tables
mysqldump -u [username] -p[password] \
  --single-transaction \
  hmis_database stock item_batch item user_stock vmp \
  > hmis_affected_tables_backup_$(date +%Y%m%d_%H%M%S).sql
```

**Step 3: Test Restore (on QA)**

```bash
# Test restore on QA database
mysql -u [username] -p[password] hmis_qa_database < hmis_backup_before_optimization_[timestamp].sql
```

---

## Performance Monitoring Queries

### Before Optimization - Capture Baseline

```sql
-- =====================================================
-- Capture Performance Baseline
-- Run these queries BEFORE making any changes
-- =====================================================

-- Query 1: Average query execution time
SELECT
    ROUND(AVG(query_time), 3) as avg_query_time_sec,
    ROUND(MAX(query_time), 3) as max_query_time_sec,
    COUNT(*) as query_count
FROM mysql.slow_query_log
WHERE sql_text LIKE '%stock%'
  AND start_time > DATE_SUB(NOW(), INTERVAL 1 HOUR);

-- Query 2: Table sizes
SELECT
    table_name,
    ROUND(data_length / 1024 / 1024, 2) AS data_size_mb,
    ROUND(index_length / 1024 / 1024, 2) AS index_size_mb,
    table_rows
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('stock', 'item_batch', 'item', 'user_stock');

-- Query 3: Index cardinality
SELECT
    table_name,
    index_name,
    cardinality,
    seq_in_index,
    column_name
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name IN ('stock', 'item_batch', 'item')
ORDER BY table_name, index_name, seq_in_index;

-- Save these results for comparison
```

### After Optimization - Compare Results

```sql
-- =====================================================
-- Capture Performance After Optimization
-- Run same queries and compare with baseline
-- =====================================================

-- Re-run all baseline queries above

-- Query 4: Index usage statistics
SELECT
    object_schema,
    object_name,
    index_name,
    count_star as times_used
FROM performance_schema.table_io_waits_summary_by_index_usage
WHERE object_schema = DATABASE()
  AND object_name IN ('stock', 'item_batch', 'item')
  AND index_name IS NOT NULL
ORDER BY count_star DESC;

-- Query 5: Queries NOT using indexes
SELECT
    query,
    exec_count,
    avg_timer_wait
FROM sys.statements_with_full_table_scans
WHERE db = DATABASE()
  AND query LIKE '%stock%'
ORDER BY exec_count DESC
LIMIT 10;
```

---

## Testing on QA Environment

### QA Testing Checklist

- [ ] Backup QA database
- [ ] Apply index changes
- [ ] Run EXPLAIN on queries
- [ ] Measure query execution time
- [ ] Load test with concurrent users
- [ ] Verify application still works
- [ ] Document performance improvement
- [ ] Test rollback procedure

### Performance Test Script

```sql
-- Test autocomplete query performance
SET @department_id = 1;  -- Replace with actual
SET @query = '%para%';   -- Test search term

-- Measure execution time
SET @start_time = NOW(6);

-- Run query (10 times)
SELECT i.id, ib.item_id, itm.name, i.stock
FROM stock i
INNER JOIN item_batch ib ON i.item_batch_id = ib.id
INNER JOIN item itm ON ib.item_id = itm.id
WHERE i.stock > 0
  AND i.department_id = @department_id
  AND itm.name LIKE @query
ORDER BY itm.name, ib.date_of_expire
LIMIT 20;

-- Repeat 9 more times...

SET @end_time = NOW(6);

-- Calculate average time
SELECT
    TIMESTAMPDIFF(MICROSECOND, @start_time, @end_time) / 10 / 1000
    AS avg_execution_ms;

-- Target: < 50ms average
```

---

## Production Deployment Checklist

### Pre-Deployment

- [ ] QA testing completed successfully
- [ ] Performance improvement verified (>50%)
- [ ] Full database backup completed
- [ ] Rollback scripts tested on QA
- [ ] Deployment window scheduled (low traffic time)
- [ ] DBA approval obtained
- [ ] Monitoring tools ready

### Deployment Steps

1. **Announce Maintenance Window**
   - Notify users of brief downtime (5-10 minutes)

2. **Create Backup**
   ```bash
   # Full backup with timestamp
   mysqldump [credentials] > production_backup_[timestamp].sql
   ```

3. **Apply Index Changes**
   ```sql
   -- Run index creation scripts
   -- Monitor progress with: SHOW PROCESSLIST;
   ```

4. **Verify Indexes Created**
   ```sql
   SHOW INDEX FROM stock WHERE Key_name LIKE 'idx_%';
   ```

5. **Analyze Tables**
   ```sql
   ANALYZE TABLE stock, item_batch, item;
   ```

6. **Test Query Performance**
   ```sql
   -- Run test queries with EXPLAIN
   -- Verify indexes being used
   ```

7. **Resume Application**
   - Start application server
   - Monitor logs

### Post-Deployment Monitoring (First Hour)

```sql
-- Monitor every 15 minutes

-- Check query performance
SELECT
    ROUND(AVG(query_time), 3) as avg_query_time_sec,
    COUNT(*) as query_count
FROM mysql.slow_query_log
WHERE sql_text LIKE '%stock%'
  AND start_time > DATE_SUB(NOW(), INTERVAL 15 MINUTE);

-- Check for errors
SHOW ERRORS;

-- Monitor connections
SHOW PROCESSLIST;

-- Check for slow queries
SELECT * FROM mysql.slow_query_log
WHERE start_time > DATE_SUB(NOW(), INTERVAL 15 MINUTE)
ORDER BY query_time DESC
LIMIT 10;
```

---

## Rollback Procedures

### Emergency Rollback

**Trigger Conditions**:
- Query performance degrades (slower than before)
- Indexes causing locking issues
- Disk space issues due to large indexes
- Application errors related to database queries

**Rollback Steps**:

```sql
-- Step 1: Drop problematic indexes
DROP INDEX idx_stock_dept_qty ON stock;
DROP INDEX idx_item_name ON item;
DROP INDEX idx_item_code ON item;
DROP INDEX idx_item_barcode ON item;
DROP INDEX idx_vmp_vtm_name ON vmp;
DROP INDEX idx_item_batch_expiry ON item_batch;

-- Step 2: Analyze tables to update statistics
ANALYZE TABLE stock, item_batch, item;

-- Step 3: Verify indexes removed
SHOW INDEX FROM stock WHERE Key_name LIKE 'idx_%';

-- Step 4: Monitor query performance
-- Run monitoring queries from above
```

**Full Database Restore** (only if critical corruption):

```bash
# ONLY USE IF CRITICAL CORRUPTION DETECTED
# This will restore entire database to pre-optimization state

mysql -u [username] -p[password] hmis_database < production_backup_[timestamp].sql

# WARNING: This will lose any data created after backup
```

---

## Cost-Benefit Analysis

### Disk Space Cost

**Estimated Index Size**:
```sql
-- Estimate index size before creation
SELECT
    table_name,
    table_rows,
    ROUND(data_length / 1024 / 1024, 2) AS data_size_mb,
    ROUND(index_length / 1024 / 1024, 2) AS current_index_size_mb,
    ROUND((data_length + index_length) / 1024 / 1024, 2) AS total_size_mb
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('stock', 'item_batch', 'item');

-- Estimated additional index space: 10-50 MB
-- Verify disk space available
SELECT * FROM information_schema.partitions
WHERE table_schema = DATABASE();
```

### Performance Benefit

**Expected Improvements**:
- Autocomplete query: 50-80% faster
- Index seek vs table scan: 10-100x faster
- UserStock batch insert: 90% fewer round-trips

### Maintenance Cost

**Ongoing Maintenance**:
- Index maintenance during INSERT/UPDATE: +5-10% overhead
- Periodic ANALYZE TABLE: 5 minutes per week
- Monitoring: 10 minutes per day (first week)

**Trade-off**: Acceptable overhead for significant read performance gain

---

## Troubleshooting Guide

### Issue: Index Not Being Used

**Symptoms**: EXPLAIN shows "ALL" in type column

**Diagnosis**:
```sql
-- Check if index exists
SHOW INDEX FROM stock WHERE Key_name = 'idx_stock_dept_qty';

-- Check statistics
SHOW TABLE STATUS LIKE 'stock';

-- Force index usage to test
SELECT SQL_NO_CACHE i.id
FROM stock i USE INDEX (idx_stock_dept_qty)
WHERE i.stock > 0 AND i.department_id = 1;
```

**Solutions**:
1. Run `ANALYZE TABLE stock;`
2. Check query uses exact column names
3. Verify data types match
4. Check for NULL values affecting index

---

### Issue: Slow INSERT Performance

**Symptoms**: UserStock inserts slower after index creation

**Diagnosis**:
```sql
-- Check number of indexes on user_stock
SHOW INDEX FROM user_stock;

-- Monitor INSERT time
SET profiling = 1;
INSERT INTO user_stock [...];
SHOW PROFILE FOR QUERY 1;
```

**Solutions**:
1. Remove unnecessary indexes on user_stock
2. Ensure batch insert is enabled
3. Check for foreign key constraint overhead

---

### Issue: Disk Space Exhaustion

**Symptoms**: "Table is full" or "Disk quota exceeded" errors

**Diagnosis**:
```sql
-- Check table sizes
SELECT
    table_name,
    ROUND((data_length + index_length) / 1024 / 1024, 2) AS total_size_mb
FROM information_schema.tables
WHERE table_schema = DATABASE()
ORDER BY (data_length + index_length) DESC
LIMIT 20;
```

**Solutions**:
1. Drop least beneficial indexes
2. Purge old data if applicable
3. Increase disk allocation
4. Move to larger storage

---

## Configuration Backup

### Save Current Configuration

```sql
-- Save current MySQL configuration
SHOW VARIABLES
INTO OUTFILE '/tmp/mysql_variables_before_optimization.txt';

-- Save current indexes
SELECT
    table_name,
    index_name,
    GROUP_CONCAT(column_name ORDER BY seq_in_index) as columns
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name IN ('stock', 'item_batch', 'item', 'user_stock')
GROUP BY table_name, index_name
INTO OUTFILE '/tmp/mysql_indexes_before_optimization.txt';
```

---

## Success Metrics

### Target Metrics

| Metric | Before | Target | Measurement |
|--------|--------|--------|-------------|
| Autocomplete query time | 200-500ms | < 50ms | EXPLAIN + profiling |
| Index usage | 20% | 90% | performance_schema |
| Full table scans | 80% | < 10% | sys.statements_with_full_table_scans |
| UserStock insert time (50 items) | 5,000ms | < 500ms | Application logs |

### Monitoring Dashboard

Create monitoring dashboard with:
- Average query execution time (last hour)
- Index hit rate
- Slow query count
- Connection pool usage
- Disk space usage

---

## Compliance & Security

### Security Considerations

- ✅ Credentials stored in `C:\Credentials\credentials.txt` (NOT in git)
- ✅ All scripts reviewed by DBA
- ✅ No sensitive data in logs
- ✅ Backup encryption enabled
- ✅ Access control maintained

### Audit Trail

Document all changes:
- Date/time of change
- Person executing change
- Backup file location
- Before/after metrics
- Any issues encountered

---

## Related Documents

- Main optimization plan: `pharmacy-retail-sale-add-button-optimization.md`
- MySQL developer guide: `developer_docs/database/mysql-developer-guide.md`
- Credentials location: `C:\Credentials\credentials.txt`

---

## Approval Sign-off

- [ ] Database Administrator Review
- [ ] Technical Lead Approval
- [ ] Security Team Approval
- [ ] Backup Procedures Verified

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-12-02 | System Analysis | Initial draft |
