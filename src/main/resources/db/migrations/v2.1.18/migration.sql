-- Migration v2.1.18: Create STOCKHISTORYARCHIVE table for old StockHistory records
-- Issue: #20726 - Archive old StockHistory records to reduce 850MB table bloat
-- Author: Dr M H B Ariyaratne
-- Date: 2026-05-14
-- Database: main HMIS database (hmisPU)
--
-- Purpose: Create a sibling archive table with the same schema as STOCKHISTORY,
-- where old rows (default: older than 2 years by CREATEDAT) will be moved by
-- the archival job (StockHistoryArchivalService).
--
-- Strategy:
-- 1. CREATE TABLE ... LIKE clones columns + indexes (NOT foreign keys, NOT auto-increment behaviour we need to keep, NOT triggers).
-- 2. Remove AUTO_INCREMENT from the ID column so archived rows keep their original IDs.
-- 3. Add ARCHIVEDAT timestamp column to record when each row was archived.
-- 4. Add an index on CREATEDAT for the archive job's range queries and for any future archive lookups.
--
-- UNIVERSAL: Detects actual table name case (STOCKHISTORY vs stockhistory) so this
-- script works on both case-sensitive (Linux, lower_case_table_names=0) and
-- case-insensitive (Windows) MySQL instances. All DDL is built with prepared
-- statements against the real table name from INFORMATION_SCHEMA.
--
-- IDEMPOTENT: All statements check for existence first, so this migration can
-- be safely re-run without error.

SELECT 'Migration v2.1.18 - Create STOCKHISTORYARCHIVE table' AS status;

-- ==========================================
-- STEP 0: DETECT ACTUAL TABLE NAME CASE
-- ==========================================

SET @sh_table = (
    SELECT TABLE_NAME
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND UPPER(TABLE_NAME) = 'STOCKHISTORY'
    LIMIT 1
);

SET @archive_table = (
    SELECT TABLE_NAME
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND UPPER(TABLE_NAME) = 'STOCKHISTORYARCHIVE'
    LIMIT 1
);

SELECT CONCAT('Detected stockhistory table as: ', IFNULL(@sh_table, '(not found)')) AS info;
SELECT CONCAT('Detected stockhistoryarchive table as: ', IFNULL(@archive_table, '(not found - will be created)')) AS info;

SELECT IF(@sh_table IS NULL,
          'STOCKHISTORY table not found; cannot create archive. Migration aborted.',
          'STOCKHISTORY table found; proceeding with archive table creation') AS applicability;

-- ==========================================
-- STEP 1: CREATE STOCKHISTORYARCHIVE TABLE
-- ==========================================

-- Only create if it does not already exist. CREATE TABLE LIKE clones the full
-- schema (columns, primary key, KEY indexes) but not foreign keys.
SET @sql = IF(@archive_table IS NULL AND @sh_table IS NOT NULL,
              CONCAT('CREATE TABLE STOCKHISTORYARCHIVE LIKE ', @sh_table),
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Re-detect after creation (the cloned table will be in the same case as the source)
SET @archive_table = (
    SELECT TABLE_NAME
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND UPPER(TABLE_NAME) = 'STOCKHISTORYARCHIVE'
    LIMIT 1
);

SELECT CONCAT('Archive table is now: ', IFNULL(@archive_table, '(creation failed)')) AS info;

-- ==========================================
-- STEP 2: REMOVE AUTO_INCREMENT FROM ID COLUMN
-- ==========================================
-- We preserve the original StockHistory.id when archiving so historical
-- references stay valid. AUTO_INCREMENT on the archive would clash with that.

SET @has_auto_inc = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = @archive_table
      AND COLUMN_NAME = 'ID'
      AND EXTRA LIKE '%auto_increment%'
);

SET @sql = IF(@archive_table IS NOT NULL AND @has_auto_inc > 0,
              CONCAT('ALTER TABLE ', @archive_table, ' MODIFY ID BIGINT NOT NULL'),
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ==========================================
-- STEP 3: ADD ARCHIVEDAT COLUMN
-- ==========================================
-- Records the timestamp at which each row was moved to the archive.

SET @has_archivedat = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = @archive_table
      AND UPPER(COLUMN_NAME) = 'ARCHIVEDAT'
);

SET @sql = IF(@archive_table IS NOT NULL AND @has_archivedat = 0,
              CONCAT('ALTER TABLE ', @archive_table, ' ADD COLUMN ARCHIVEDAT DATETIME NULL'),
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ==========================================
-- STEP 4: ADD INDEX ON CREATEDAT
-- ==========================================
-- The archive job's cutoff query and any future archive lookups both filter
-- on CREATEDAT, so we want it indexed independently of any cloned indexes.

SET @has_createdat_idx = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = @archive_table
      AND INDEX_NAME = 'idx_stockhistoryarchive_createdat'
);

SET @sql = IF(@archive_table IS NOT NULL AND @has_createdat_idx = 0,
              CONCAT('ALTER TABLE ', @archive_table, ' ADD INDEX idx_stockhistoryarchive_createdat (CREATEDAT)'),
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ==========================================
-- STEP 5: POST-MIGRATION VERIFICATION
-- ==========================================

SELECT 'STOCKHISTORYARCHIVE table schema' AS status;

SELECT
    ROUND(DATA_LENGTH/1024/1024, 1) AS data_mb,
    ROUND(INDEX_LENGTH/1024/1024, 1) AS index_mb,
    TABLE_ROWS
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @archive_table;

SELECT COUNT(*) AS column_count
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @archive_table;

SELECT
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    EXTRA
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @archive_table
  AND COLUMN_NAME IN ('ID', 'CREATEDAT', 'ARCHIVEDAT')
ORDER BY ORDINAL_POSITION;

SELECT 'Migration v2.1.18 completed - STOCKHISTORYARCHIVE table ready' AS final_status;
