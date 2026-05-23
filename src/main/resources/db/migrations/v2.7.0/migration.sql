-- Migration v2.7.0: Performance indexes for STOCKHISTORYARCHIVE (x3)
-- Author: Dr M H B Ariyaratne
-- Issue: #20729 — StockHistory archive read paths
--
-- Background:
--   Migration v2.1.18 created STOCKHISTORYARCHIVE with only a single-column index
--   on CREATEDAT. The same composite indexes that exist on STOCKHISTORY are needed
--   here so that archive queries (bin card, stock ledger, closing stock) can use
--   index scans instead of full table scans once archived rows accumulate.
--
-- Indexes added (mirrors the STOCKHISTORY composite indexes from v2.1.10 / v2.5.0):
--   1. idx_stockhistoryarchive_batch_date_retired   (ITEMBATCH_ID, CREATEDAT, RETIRED)
--      Used by: batch-level bin card queries, stock ledger filtered by batch
--   2. idx_stockhistoryarchive_date_dept_retired    (CREATEDAT, DEPARTMENT_ID, RETIRED)
--      Used by: stock value calculation at a point in time, department stock history
--   3. idx_stockhistoryarchive_historytype_dept_createdat (HISTORYTYPE, DEPARTMENT_ID, CREATEDAT)
--      Used by: bin card DTO report, monthly record queries
--
-- UNIVERSAL: detects actual table-name case via INFORMATION_SCHEMA — works on both
-- case-sensitive (Linux, lower_case_table_names=0) and case-insensitive (Windows/Azure).
-- All CREATE INDEX statements are guarded with existence checks — safe to re-run.
-- Deployments that already applied this migration manually will SKIP all steps cleanly.

SELECT 'Migration v2.7.0 - STOCKHISTORYARCHIVE performance indexes (x3)' AS status;

-- ============================================================
-- STEP 0: DETECT ACTUAL TABLE NAME CASE
-- ============================================================

SET @sha_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'STOCKHISTORYARCHIVE'
    LIMIT 1
);

SELECT CONCAT('STOCKHISTORYARCHIVE table: ', COALESCE(@sha_table, 'NOT FOUND — migration will skip')) AS info;

-- ============================================================
-- STEP 1: PRE-MIGRATION STATE
-- ============================================================

SELECT 'BEFORE: STOCKHISTORYARCHIVE indexes' AS status;
SELECT INDEX_NAME, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS columns
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'STOCKHISTORYARCHIVE'
GROUP BY INDEX_NAME
ORDER BY INDEX_NAME;

-- ============================================================
-- STEP 2: idx_stockhistoryarchive_batch_date_retired
--         (ITEMBATCH_ID, CREATEDAT, RETIRED)
-- ============================================================

SELECT 'Step 2: Creating idx_stockhistoryarchive_batch_date_retired' AS status;

SET @idx1_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND UPPER(TABLE_NAME) = 'STOCKHISTORYARCHIVE'
      AND INDEX_NAME = 'idx_stockhistoryarchive_batch_date_retired'
);

SET @sql = IF(@sha_table IS NOT NULL AND @idx1_exists = 0,
    CONCAT('CREATE INDEX idx_stockhistoryarchive_batch_date_retired ON ', @sha_table,
           ' (ITEMBATCH_ID, CREATEDAT, RETIRED)'),
    'SELECT "SKIPPED: idx_stockhistoryarchive_batch_date_retired already exists or table not found" AS status'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE()
       AND UPPER(TABLE_NAME) = 'STOCKHISTORYARCHIVE'
       AND INDEX_NAME = 'idx_stockhistoryarchive_batch_date_retired') > 0,
    'OK: idx_stockhistoryarchive_batch_date_retired present',
    'FAILED: idx_stockhistoryarchive_batch_date_retired missing') AS verify_2;

-- ============================================================
-- STEP 3: idx_stockhistoryarchive_date_dept_retired
--         (CREATEDAT, DEPARTMENT_ID, RETIRED)
-- ============================================================

SELECT 'Step 3: Creating idx_stockhistoryarchive_date_dept_retired' AS status;

SET @idx2_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND UPPER(TABLE_NAME) = 'STOCKHISTORYARCHIVE'
      AND INDEX_NAME = 'idx_stockhistoryarchive_date_dept_retired'
);

SET @sql = IF(@sha_table IS NOT NULL AND @idx2_exists = 0,
    CONCAT('CREATE INDEX idx_stockhistoryarchive_date_dept_retired ON ', @sha_table,
           ' (CREATEDAT, DEPARTMENT_ID, RETIRED)'),
    'SELECT "SKIPPED: idx_stockhistoryarchive_date_dept_retired already exists or table not found" AS status'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE()
       AND UPPER(TABLE_NAME) = 'STOCKHISTORYARCHIVE'
       AND INDEX_NAME = 'idx_stockhistoryarchive_date_dept_retired') > 0,
    'OK: idx_stockhistoryarchive_date_dept_retired present',
    'FAILED: idx_stockhistoryarchive_date_dept_retired missing') AS verify_3;

-- ============================================================
-- STEP 4: idx_stockhistoryarchive_historytype_dept_createdat
--         (HISTORYTYPE, DEPARTMENT_ID, CREATEDAT)
-- ============================================================

SELECT 'Step 4: Creating idx_stockhistoryarchive_historytype_dept_createdat' AS status;

SET @idx3_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND UPPER(TABLE_NAME) = 'STOCKHISTORYARCHIVE'
      AND INDEX_NAME = 'idx_stockhistoryarchive_historytype_dept_createdat'
);

SET @sql = IF(@sha_table IS NOT NULL AND @idx3_exists = 0,
    CONCAT('CREATE INDEX idx_stockhistoryarchive_historytype_dept_createdat ON ', @sha_table,
           ' (HISTORYTYPE, DEPARTMENT_ID, CREATEDAT)'),
    'SELECT "SKIPPED: idx_stockhistoryarchive_historytype_dept_createdat already exists or table not found" AS status'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE()
       AND UPPER(TABLE_NAME) = 'STOCKHISTORYARCHIVE'
       AND INDEX_NAME = 'idx_stockhistoryarchive_historytype_dept_createdat') > 0,
    'OK: idx_stockhistoryarchive_historytype_dept_createdat present',
    'FAILED: idx_stockhistoryarchive_historytype_dept_createdat missing') AS verify_4;

-- ============================================================
-- STEP 5: POST-MIGRATION VERIFICATION
-- ============================================================

SELECT 'AFTER: STOCKHISTORYARCHIVE indexes' AS status;
SELECT INDEX_NAME, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS columns
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'STOCKHISTORYARCHIVE'
GROUP BY INDEX_NAME
ORDER BY INDEX_NAME;

SELECT 'Migration v2.7.0 completed - STOCKHISTORYARCHIVE has all 4 performance indexes' AS final_status;
