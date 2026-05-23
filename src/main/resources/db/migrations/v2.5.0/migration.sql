-- Migration v2.5.0: RMH production performance indexes — Bill (x4) + StockHistory (x1)
-- Author: Dr M H B Ariyaratne
-- Issue: #20727 — formalise indexes applied directly to RMH prod on 2026-05-14
--
-- Root cause:
--   Five queries caused severe load on RMH production (Azure managed MySQL 10.0.0.105).
--   All were identified via slow-query log analysis and fixed with targeted composite indexes.
--   This migration formalises those indexes so all other deployments benefit.
--
-- Bill — 4 indexes:
--   1. idx_bill_institution_billtype_dtype_retired
--      COUNT(*) ... WHERE RETIRED=? AND INSTITUTION_ID=? AND BILLTYPE=? AND DTYPE=?
--      Was scanning 1.26M rows, 3,600x/day at avg 6.91s (24,892s total DB time).
--   2. idx_bill_institution_billtype_retired_createdat
--      SUM(NETTOTAL) ... WHERE INSTITUTION_ID=? AND BILLTYPE=? AND RETIRED=? AND CREATEDAT BETWEEN ?
--      Was scanning 231k rows, 1,505x/day at avg 3.47s (5,226s total).
--   3. idx_bill_institution_paymentmethod_retired_createdat
--      SUM(NETTOTAL) ... WHERE INSTITUTION_ID=? AND PAYMENTMETHOD=? AND RETIRED=? AND CREATEDAT BETWEEN ?
--      Was scanning full table, 1,120x/day at avg 3.5s (3,917s total).
--   4. idx_bill_dept_billdate_billtype
--      COUNT(*) ... WHERE DEPARTMENT_ID=? AND BILLDATE=? AND (BILLTYPE=? OR BILLTYPE=?)
--      Was examining 1.58B rows, 2,794x/day at avg 2.14s (5,985s total).
--
-- StockHistory — 1 index:
--   5. idx_stockhistory_historytype_dept_createdat
--      Filters on HISTORYTYPE='MonthlyRecord' had no index; 714k MonthlyRecord rows
--      (29% of 2.36M total) were scanned on every monthly report query.
--
-- UNIVERSAL: detects actual table-name case via INFORMATION_SCHEMA so this works on both
-- case-sensitive (Linux, lower_case_table_names=0) and case-insensitive (Windows/Azure) MySQL.
-- All CREATE INDEX statements are guarded with existence checks — safe to re-run.
-- RMH production already has these indexes; all steps will SKIP cleanly there.

SELECT 'Migration v2.5.0 - RMH production performance indexes (Bill x4, StockHistory x1)' AS status;

-- ============================================================
-- STEP 0: DETECT ACTUAL TABLE NAME CASE
-- ============================================================

SET @bill_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'BILL'
    LIMIT 1
);

SET @sh_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'STOCKHISTORY'
    LIMIT 1
);

SELECT CONCAT('Bill table: ',         COALESCE(@bill_table, 'NOT FOUND')) AS info;
SELECT CONCAT('StockHistory table: ', COALESCE(@sh_table,   'NOT FOUND')) AS info;

-- ============================================================
-- STEP 1: PRE-MIGRATION ANALYSIS
-- ============================================================

SELECT 'BEFORE: Table sizes' AS status;

SET @size_sql = CONCAT(
    'SELECT TABLE_NAME, TABLE_ROWS AS est_rows, ',
    'ROUND(DATA_LENGTH/1024/1024,1) AS data_mb, ',
    'ROUND(INDEX_LENGTH/1024/1024,1) AS index_mb ',
    'FROM INFORMATION_SCHEMA.TABLES ',
    'WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN (''', @bill_table, ''',''', @sh_table, ''')'
);
PREPARE stmt FROM @size_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================
-- STEP 2: idx_bill_institution_billtype_dtype_retired
--   Fixes COUNT queries: WHERE RETIRED=? AND INSTITUTION_ID=? AND BILLTYPE=? AND DTYPE=?
--   Before: full 1.26M-row scan via low-cardinality FK_bill_INSTITUTION_ID index
--   After:  pure 'Using index' covering scan
-- ============================================================

SELECT 'Step 2: Creating idx_bill_institution_billtype_dtype_retired' AS status;

SET @idx_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @bill_table
      AND INDEX_NAME = 'idx_bill_institution_billtype_dtype_retired'
);
SET @sql = IF(@bill_table IS NULL OR @idx_exists > 0,
    'SELECT "SKIPPED: table not found or index already exists" AS status',
    CONCAT('CREATE INDEX idx_bill_institution_billtype_dtype_retired ON ', @bill_table,
           '(INSTITUTION_ID, BILLTYPE, DTYPE, RETIRED)')
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @bill_table
      AND INDEX_NAME = 'idx_bill_institution_billtype_dtype_retired');
SELECT IF(@idx_exists > 0,
    'OK: idx_bill_institution_billtype_dtype_retired present',
    'FAILED: idx_bill_institution_billtype_dtype_retired missing') AS verify_2;

-- ============================================================
-- STEP 3: idx_bill_institution_billtype_retired_createdat
--   Fixes SUM(NETTOTAL) date-range queries: WHERE INSTITUTION_ID=? AND BILLTYPE=? AND RETIRED=?
--     AND CREATEDAT BETWEEN ? AND ?
--   Before: 231k rows scanned (wrong leading column on existing CREATEDAT index)
--   After:  64k rows, 'Using index condition'
-- ============================================================

SELECT 'Step 3: Creating idx_bill_institution_billtype_retired_createdat' AS status;

SET @idx_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @bill_table
      AND INDEX_NAME = 'idx_bill_institution_billtype_retired_createdat'
);
SET @sql = IF(@bill_table IS NULL OR @idx_exists > 0,
    'SELECT "SKIPPED: table not found or index already exists" AS status',
    CONCAT('CREATE INDEX idx_bill_institution_billtype_retired_createdat ON ', @bill_table,
           '(INSTITUTION_ID, BILLTYPE, RETIRED, CREATEDAT)')
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @bill_table
      AND INDEX_NAME = 'idx_bill_institution_billtype_retired_createdat');
SELECT IF(@idx_exists > 0,
    'OK: idx_bill_institution_billtype_retired_createdat present',
    'FAILED: idx_bill_institution_billtype_retired_createdat missing') AS verify_3;

-- ============================================================
-- STEP 4: idx_bill_institution_paymentmethod_retired_createdat
--   Fixes SUM(NETTOTAL) by payment method: WHERE INSTITUTION_ID=? AND PAYMENTMETHOD=?
--     AND RETIRED=? AND CREATEDAT BETWEEN ? AND ?
--   Before: full table scan, 1,120x/day at avg 3.5s
--   After:  191k rows, 'Using index condition'
-- ============================================================

SELECT 'Step 4: Creating idx_bill_institution_paymentmethod_retired_createdat' AS status;

SET @idx_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @bill_table
      AND INDEX_NAME = 'idx_bill_institution_paymentmethod_retired_createdat'
);
SET @sql = IF(@bill_table IS NULL OR @idx_exists > 0,
    'SELECT "SKIPPED: table not found or index already exists" AS status',
    CONCAT('CREATE INDEX idx_bill_institution_paymentmethod_retired_createdat ON ', @bill_table,
           '(INSTITUTION_ID, PAYMENTMETHOD, RETIRED, CREATEDAT)')
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @bill_table
      AND INDEX_NAME = 'idx_bill_institution_paymentmethod_retired_createdat');
SELECT IF(@idx_exists > 0,
    'OK: idx_bill_institution_paymentmethod_retired_createdat present',
    'FAILED: idx_bill_institution_paymentmethod_retired_createdat missing') AS verify_4;

-- ============================================================
-- STEP 5: idx_bill_dept_billdate_billtype
--   Fixes COUNT by dept+date+billtype: WHERE DEPARTMENT_ID=? AND BILLDATE=?
--     AND (BILLTYPE=? OR BILLTYPE=?) AND (DTYPE=? OR DTYPE=?)
--   Before: 1.58B rows examined, only FK on DEPARTMENT_ID used
--   After:  targeted range scan by dept+date+billtype
-- ============================================================

SELECT 'Step 5: Creating idx_bill_dept_billdate_billtype' AS status;

SET @idx_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @bill_table
      AND INDEX_NAME = 'idx_bill_dept_billdate_billtype'
);
SET @sql = IF(@bill_table IS NULL OR @idx_exists > 0,
    'SELECT "SKIPPED: table not found or index already exists" AS status',
    CONCAT('CREATE INDEX idx_bill_dept_billdate_billtype ON ', @bill_table,
           '(DEPARTMENT_ID, BILLDATE, BILLTYPE)')
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @bill_table
      AND INDEX_NAME = 'idx_bill_dept_billdate_billtype');
SELECT IF(@idx_exists > 0,
    'OK: idx_bill_dept_billdate_billtype present',
    'FAILED: idx_bill_dept_billdate_billtype missing') AS verify_5;

-- ============================================================
-- STEP 6: idx_stockhistory_historytype_dept_createdat
--   Fixes MonthlyRecord report queries: WHERE HISTORYTYPE='MonthlyRecord' AND DEPARTMENT_ID=?
--     AND CREATEDAT BETWEEN ? AND ?
--   Before: no index on HISTORYTYPE; 714k MonthlyRecord rows (29% of 2.36M) scanned
--   After:  direct seek to MonthlyRecord rows by dept+date
-- ============================================================

SELECT 'Step 6: Creating idx_stockhistory_historytype_dept_createdat' AS status;

SET @idx_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @sh_table
      AND INDEX_NAME = 'idx_stockhistory_historytype_dept_createdat'
);
SET @sql = IF(@sh_table IS NULL OR @idx_exists > 0,
    'SELECT "SKIPPED: table not found or index already exists" AS status',
    CONCAT('CREATE INDEX idx_stockhistory_historytype_dept_createdat ON ', @sh_table,
           '(HISTORYTYPE, DEPARTMENT_ID, CREATEDAT)')
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @sh_table
      AND INDEX_NAME = 'idx_stockhistory_historytype_dept_createdat');
SELECT IF(@idx_exists > 0,
    'OK: idx_stockhistory_historytype_dept_createdat present',
    'FAILED: idx_stockhistory_historytype_dept_createdat missing') AS verify_6;

-- ============================================================
-- STEP 7: POST-MIGRATION VERIFICATION
-- ============================================================

SELECT 'AFTER: New indexes on Bill' AS status;

SELECT INDEX_NAME,
       GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ', ') AS cols,
       MAX(CARDINALITY) AS cardinality
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @bill_table
  AND INDEX_NAME LIKE 'idx_bill_%'
GROUP BY INDEX_NAME
ORDER BY INDEX_NAME;

SELECT 'AFTER: New indexes on StockHistory' AS status;

SELECT INDEX_NAME,
       GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ', ') AS cols,
       MAX(CARDINALITY) AS cardinality
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @sh_table
  AND INDEX_NAME LIKE 'idx_stockhistory_%'
GROUP BY INDEX_NAME
ORDER BY INDEX_NAME;

SELECT 'Migration v2.5.0 completed' AS final_status;
