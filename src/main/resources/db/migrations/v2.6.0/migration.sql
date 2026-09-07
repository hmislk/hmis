-- Migration v2.6.0: ItemBatch and Stock archival tables + PBI archive pointer
-- Author: Dr M H B Ariyaratne
-- Issue: #20724 — archive expired zero-stock ItemBatch records to reduce table bloat
--
-- Creates:
--   1. ITEMBATCHARCHIVE  — same schema as ITEMBATCH + ARCHIVEDAT timestamp;
--                          AUTO_INCREMENT removed so rows keep their original IDs
--   2. STOCKARCHIVE      — same schema as STOCK + ARCHIVEDAT timestamp
--   3. PHARMACEUTICALBILLITEM.ARCHIVEDITEMBATCH_ID — FK pointer to ITEMBATCHARCHIVE;
--      set by the archival job before live ITEMBATCH_ID is nullified, so bill
--      viewing can fall back to the archive via pbi.getArchivedItemBatch()
--
-- UNIVERSAL: detects actual table-name case via INFORMATION_SCHEMA so this
-- works on both case-sensitive (Linux) and case-insensitive (Windows/Azure) MySQL.
-- Each CREATE TABLE / ALTER TABLE uses its own PREPARE/EXECUTE block (MySQL
-- does not allow multiple statements in a single PREPARE string).
-- All steps are guarded with existence checks — safe to re-run.

SELECT 'Migration v2.6.0 - ItemBatch and Stock archival tables' AS status;

-- ============================================================
-- STEP 0: DETECT ACTUAL TABLE NAME CASE
-- ============================================================

SET @ib_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'ITEMBATCH'
    LIMIT 1
);

SET @st_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'STOCK'
    LIMIT 1
);

SET @pbi_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'PHARMACEUTICALBILLITEM'
    LIMIT 1
);

SELECT CONCAT('ItemBatch table: ',               COALESCE(@ib_table,  'NOT FOUND')) AS info;
SELECT CONCAT('Stock table: ',                   COALESCE(@st_table,  'NOT FOUND')) AS info;
SELECT CONCAT('PharmaceuticalBillItem table: ',  COALESCE(@pbi_table, 'NOT FOUND')) AS info;

-- ============================================================
-- STEP 1a: CREATE ITEMBATCHARCHIVE (CREATE TABLE ... LIKE)
-- ============================================================

SELECT 'Step 1a: Creating ITEMBATCHARCHIVE (CREATE TABLE LIKE)' AS status;

SET @archive_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'ITEMBATCHARCHIVE'
);
SET @sql = IF(@ib_table IS NULL OR @archive_exists > 0,
    'SELECT "SKIPPED: source table not found or ITEMBATCHARCHIVE already exists" AS status',
    CONCAT('CREATE TABLE ITEMBATCHARCHIVE LIKE ', @ib_table)
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================
-- STEP 1b: ALTER ITEMBATCHARCHIVE — remove AUTO_INCREMENT, add ARCHIVEDAT
-- ============================================================

SELECT 'Step 1b: Altering ITEMBATCHARCHIVE (remove AUTO_INCREMENT, add ARCHIVEDAT)' AS status;

SET @ibarch_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'ITEMBATCHARCHIVE'
    LIMIT 1
);
SET @col_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND UPPER(TABLE_NAME) = 'ITEMBATCHARCHIVE'
      AND UPPER(COLUMN_NAME) = 'ARCHIVEDAT'
);
SET @sql = IF(@ibarch_table IS NULL OR @col_exists > 0,
    'SELECT "SKIPPED: ITEMBATCHARCHIVE not found or ARCHIVEDAT already present" AS status',
    CONCAT('ALTER TABLE ', @ibarch_table, ' MODIFY COLUMN ID BIGINT NOT NULL, ADD COLUMN ARCHIVEDAT DATETIME NULL')
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @archive_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'ITEMBATCHARCHIVE'
);
SELECT IF(@archive_exists > 0,
    'OK: ITEMBATCHARCHIVE present',
    'FAILED: ITEMBATCHARCHIVE missing') AS verify_1;

-- ============================================================
-- STEP 2a: CREATE STOCKARCHIVE (CREATE TABLE ... LIKE)
-- ============================================================

SELECT 'Step 2a: Creating STOCKARCHIVE (CREATE TABLE LIKE)' AS status;

SET @starch_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'STOCKARCHIVE'
);
SET @sql = IF(@st_table IS NULL OR @starch_exists > 0,
    'SELECT "SKIPPED: source table not found or STOCKARCHIVE already exists" AS status',
    CONCAT('CREATE TABLE STOCKARCHIVE LIKE ', @st_table)
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================
-- STEP 2b: ALTER STOCKARCHIVE — remove AUTO_INCREMENT, add ARCHIVEDAT
-- ============================================================

SELECT 'Step 2b: Altering STOCKARCHIVE (remove AUTO_INCREMENT, add ARCHIVEDAT)' AS status;

SET @starch_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'STOCKARCHIVE'
    LIMIT 1
);
SET @col_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND UPPER(TABLE_NAME) = 'STOCKARCHIVE'
      AND UPPER(COLUMN_NAME) = 'ARCHIVEDAT'
);
SET @sql = IF(@starch_table IS NULL OR @col_exists > 0,
    'SELECT "SKIPPED: STOCKARCHIVE not found or ARCHIVEDAT already present" AS status',
    CONCAT('ALTER TABLE ', @starch_table, ' MODIFY COLUMN ID BIGINT NOT NULL, ADD COLUMN ARCHIVEDAT DATETIME NULL')
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @starch_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'STOCKARCHIVE'
);
SELECT IF(@starch_exists > 0,
    'OK: STOCKARCHIVE present',
    'FAILED: STOCKARCHIVE missing') AS verify_2;

-- ============================================================
-- STEP 3: ADD ARCHIVEDITEMBATCH_ID TO PHARMACEUTICALBILLITEM
-- ============================================================

SELECT 'Step 3: Adding ARCHIVEDITEMBATCH_ID to PHARMACEUTICALBILLITEM' AS status;

SET @col_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND UPPER(TABLE_NAME) = 'PHARMACEUTICALBILLITEM'
      AND UPPER(COLUMN_NAME) = 'ARCHIVEDITEMBATCH_ID'
);
SET @sql = IF(@pbi_table IS NULL OR @col_exists > 0,
    'SELECT "SKIPPED: table not found or ARCHIVEDITEMBATCH_ID already exists" AS status',
    CONCAT('ALTER TABLE ', @pbi_table,
           ' ADD COLUMN ARCHIVEDITEMBATCH_ID BIGINT NULL')
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND UPPER(TABLE_NAME) = 'PHARMACEUTICALBILLITEM'
      AND UPPER(COLUMN_NAME) = 'ARCHIVEDITEMBATCH_ID'
);
SELECT IF(@col_exists > 0,
    'OK: ARCHIVEDITEMBATCH_ID present on PHARMACEUTICALBILLITEM',
    'FAILED: ARCHIVEDITEMBATCH_ID missing') AS verify_3;

-- ============================================================
-- STEP 4: POST-MIGRATION SUMMARY
-- ============================================================

SELECT 'AFTER: New archive tables' AS status;

SELECT TABLE_NAME,
       TABLE_ROWS AS est_rows,
       ROUND(DATA_LENGTH/1024/1024,1) AS data_mb
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND UPPER(TABLE_NAME) IN ('ITEMBATCHARCHIVE','STOCKARCHIVE')
ORDER BY TABLE_NAME;

SELECT 'Migration v2.6.0 completed' AS final_status;
