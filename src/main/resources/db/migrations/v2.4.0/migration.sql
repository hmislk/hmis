-- Migration v2.4.0: Performance indexes for PharmaceuticalBillItem + drop dead index on BillItem
-- Author: Dr M H B Ariyaratne
-- Issue: RMH database slowness — pharmacy retail sale and batch/stock transaction queries
--
-- Root cause:
--   PHARMACEUTICALBILLITEM (7.15M rows) had no date-based index. Any date-range pharmacy
--   report triggered a full 7.15M-row table scan. Stock-specific date queries also scanned
--   all rows for that stock (up to 12K+ rows) without a composite index.
--
--   BILLITEM.BILLITEMSTATUS_idx had cardinality=1 (100% NULL across 4.86M rows) — a
--   completely useless index adding write overhead to every pharmacy sale INSERT/UPDATE.
--
-- Before/after EXPLAIN (tested on qa4):
--   Date-range report query: type=ALL  rows=7,153,035  → type=range rows=3
--   Stock+date query:        type=ref  rows=12,744     → type=range rows=1
--   Batch+date query:        type=ref  rows=scan       → type=range rows=1
--
-- UNIVERSAL: detects actual table name case via INFORMATION_SCHEMA so this works on both
-- case-sensitive (Linux, lower_case_table_names=0) and case-insensitive (Windows/Azure) MySQL.
-- All CREATE INDEX statements are guarded with existence checks — safe to re-run.

SELECT 'Migration v2.4.0 - PharmaceuticalBillItem performance indexes' AS status;

-- ============================================================
-- STEP 0: DETECT ACTUAL TABLE NAME CASE
-- ============================================================

SET @pbi_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'PHARMACEUTICALBILLITEM'
    LIMIT 1
);

SET @bi_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'BILLITEM'
    LIMIT 1
);

SELECT CONCAT('PHARMACEUTICALBILLITEM table: ', COALESCE(@pbi_table, 'NOT FOUND')) AS info;
SELECT CONCAT('BILLITEM table: ',              COALESCE(@bi_table,  'NOT FOUND')) AS info;

-- ============================================================
-- STEP 1: PRE-MIGRATION ANALYSIS
-- ============================================================

SELECT 'BEFORE: Table sizes and existing indexes' AS status;

SET @size_sql = CONCAT(
    'SELECT TABLE_NAME, TABLE_ROWS AS est_rows, ',
    'ROUND(DATA_LENGTH/1024/1024,1) AS data_mb, ',
    'ROUND(INDEX_LENGTH/1024/1024,1) AS index_mb ',
    'FROM INFORMATION_SCHEMA.TABLES ',
    'WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN (''', @pbi_table, ''',''', @bi_table, ''')'
);
PREPARE stmt FROM @size_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT COUNT(*) AS pbi_existing_idx FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @pbi_table;

-- ============================================================
-- STEP 2: CREATE idx_pbi_createdat_retired
--   Fixes full-table-scan on date-range pharmacy reports
--   (7.15M → targeted row range)
-- ============================================================

SELECT 'Step 2: Creating idx_pbi_createdat_retired (CREATEDAT, RETIRED)' AS status;

SET @idx_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @pbi_table
      AND INDEX_NAME = 'idx_pbi_createdat_retired'
);
SET @sql = IF(@pbi_table IS NULL OR @idx_exists > 0,
    'SELECT "SKIPPED: table not found or index already exists" AS status',
    CONCAT('CREATE INDEX idx_pbi_createdat_retired ON ', @pbi_table, '(CREATEDAT, RETIRED)')
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @pbi_table AND INDEX_NAME = 'idx_pbi_createdat_retired');
SELECT IF(@idx_exists > 0, 'OK: idx_pbi_createdat_retired present', 'FAILED: idx_pbi_createdat_retired missing') AS verify_1;

-- ============================================================
-- STEP 3: CREATE idx_pbi_stock_createdat
--   Fixes stock + date-range queries (e.g. stock movement history for one item)
-- ============================================================

SELECT 'Step 3: Creating idx_pbi_stock_createdat (STOCK_ID, CREATEDAT, RETIRED)' AS status;

SET @idx_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @pbi_table
      AND INDEX_NAME = 'idx_pbi_stock_createdat'
);
SET @sql = IF(@pbi_table IS NULL OR @idx_exists > 0,
    'SELECT "SKIPPED: table not found or index already exists" AS status',
    CONCAT('CREATE INDEX idx_pbi_stock_createdat ON ', @pbi_table, '(STOCK_ID, CREATEDAT, RETIRED)')
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @pbi_table AND INDEX_NAME = 'idx_pbi_stock_createdat');
SELECT IF(@idx_exists > 0, 'OK: idx_pbi_stock_createdat present', 'FAILED: idx_pbi_stock_createdat missing') AS verify_2;

-- ============================================================
-- STEP 4: CREATE idx_pbi_batch_createdat
--   Fixes batch + date-range queries (e.g. GRN batch movement reports)
-- ============================================================

SELECT 'Step 4: Creating idx_pbi_batch_createdat (ITEMBATCH_ID, CREATEDAT, RETIRED)' AS status;

SET @idx_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @pbi_table
      AND INDEX_NAME = 'idx_pbi_batch_createdat'
);
SET @sql = IF(@pbi_table IS NULL OR @idx_exists > 0,
    'SELECT "SKIPPED: table not found or index already exists" AS status',
    CONCAT('CREATE INDEX idx_pbi_batch_createdat ON ', @pbi_table, '(ITEMBATCH_ID, CREATEDAT, RETIRED)')
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @pbi_table AND INDEX_NAME = 'idx_pbi_batch_createdat');
SELECT IF(@idx_exists > 0, 'OK: idx_pbi_batch_createdat present', 'FAILED: idx_pbi_batch_createdat missing') AS verify_3;

-- ============================================================
-- STEP 5: DROP BILLITEM_BILLITEMSTATUS_idx
--   100% NULL across 4.86M rows — cardinality=1, never used for any query.
--   Removing it reduces write overhead on every pharmacy sale INSERT/UPDATE.
-- ============================================================

SELECT 'Step 5: Dropping dead index BILLITEM_BILLITEMSTATUS_idx from billitem' AS status;

SET @idx_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @bi_table
      AND INDEX_NAME = 'BILLITEM_BILLITEMSTATUS_idx'
);
SET @sql = IF(@bi_table IS NULL OR @idx_exists = 0,
    'SELECT "SKIPPED: table not found or index already absent" AS status',
    CONCAT('ALTER TABLE ', @bi_table, ' DROP INDEX BILLITEM_BILLITEMSTATUS_idx')
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_gone = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @bi_table AND INDEX_NAME = 'BILLITEM_BILLITEMSTATUS_idx');
SELECT IF(@idx_gone = 0, 'OK: BILLITEM_BILLITEMSTATUS_idx absent', 'FAILED: BILLITEM_BILLITEMSTATUS_idx still present') AS verify_4;

-- ============================================================
-- STEP 6: POST-MIGRATION VERIFICATION
-- ============================================================

SELECT 'AFTER: New indexes on PHARMACEUTICALBILLITEM' AS status;

SELECT INDEX_NAME,
       GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ', ') AS cols,
       MAX(CARDINALITY) AS cardinality
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @pbi_table
  AND INDEX_NAME LIKE 'idx_pbi_%'
GROUP BY INDEX_NAME
ORDER BY INDEX_NAME;

SELECT 'Migration v2.4.0 completed' AS final_status;
