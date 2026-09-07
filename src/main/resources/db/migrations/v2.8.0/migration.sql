-- Migration v2.8.0: Migrate stale BILLTYPEATOMIC for pharmacy unit/disposal issue bills
-- Author: Dr M H B Ariyaratne
-- Issue: #20996
--
-- Background:
--   The Unit Issue report (pharmacy_report_unit_issue_bill.xhtml) filters on
--   BILLTYPEATOMIC IN ('PHARMACY_DISPOSAL_ISSUE', 'PHARMACY_DISPOSAL_ISSUE_CANCELLED',
--                      'PHARMACY_DISPOSAL_ISSUE_RETURN').
--   Bills created before the PHARMACY_DISPOSAL_ISSUE enum value was introduced were
--   saved with BILLTYPEATOMIC = 'PHARMACY_ISSUE' / 'PHARMACY_ISSUE_RETURN' /
--   'PHARMACY_ISSUE_CANCELLED'. This migration updates those rows so they appear
--   in the report.
--
--   IMPORTANT: Bills with BILLTYPE = 'PharmacyTransferIssue' also carry
--   BILLTYPEATOMIC = 'PHARMACY_ISSUE' — those are inter-pharmacy transfers and
--   must NOT be updated. The WHERE clause always includes BILLTYPE = 'PharmacyIssue'.
--
-- Safe to re-run: UPDATE ... WHERE is idempotent.
--
-- UNIVERSAL: detects actual table-name case via INFORMATION_SCHEMA so this works on both
-- case-sensitive (Linux, lower_case_table_names=0) and case-insensitive (Windows/Azure) MySQL.

SELECT 'Migration v2.8.0 - Migrate stale BILLTYPEATOMIC for pharmacy unit/disposal issue bills' AS status;

-- ── STEP 0: DETECT ACTUAL TABLE NAME CASE ────────────────────────────────────

SET @bill_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'BILL'
    LIMIT 1
);

SELECT CONCAT('Bill table: ', COALESCE(@bill_table, 'NOT FOUND')) AS info;

-- ── BEFORE counts ────────────────────────────────────────────────────────────

SELECT 'BEFORE: stale BILLTYPEATOMIC counts' AS status;

SET @before_sql = IF(@bill_table IS NULL,
    'SELECT "SKIPPED: bill table not found" AS status',
    CONCAT(
        'SELECT BILLTYPEATOMIC, COUNT(*) AS cnt ',
        'FROM ', @bill_table, ' ',
        'WHERE BILLTYPE = ''PharmacyIssue'' ',
        '  AND BILLTYPEATOMIC IN (''PHARMACY_ISSUE'', ''PHARMACY_ISSUE_RETURN'', ''PHARMACY_ISSUE_CANCELLED'') ',
        '  AND RETIRED = 0 ',
        'GROUP BY BILLTYPEATOMIC'
    )
);
PREPARE stmt FROM @before_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ── STEP 1: PHARMACY_ISSUE → PHARMACY_DISPOSAL_ISSUE ─────────────────────────

SELECT 'Step 1: PHARMACY_ISSUE -> PHARMACY_DISPOSAL_ISSUE' AS status;

SET @sql = IF(@bill_table IS NULL,
    'SELECT "SKIPPED: bill table not found" AS status',
    CONCAT(
        'UPDATE ', @bill_table, ' ',
        'SET BILLTYPEATOMIC = ''PHARMACY_DISPOSAL_ISSUE'' ',
        'WHERE BILLTYPE     = ''PharmacyIssue'' ',
        '  AND BILLTYPEATOMIC = ''PHARMACY_ISSUE'' ',
        '  AND RETIRED      = 0'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT ROW_COUNT() AS updated_step1;

-- ── STEP 2: PHARMACY_ISSUE_RETURN → PHARMACY_DISPOSAL_ISSUE_RETURN ───────────

SELECT 'Step 2: PHARMACY_ISSUE_RETURN -> PHARMACY_DISPOSAL_ISSUE_RETURN' AS status;

SET @sql = IF(@bill_table IS NULL,
    'SELECT "SKIPPED: bill table not found" AS status',
    CONCAT(
        'UPDATE ', @bill_table, ' ',
        'SET BILLTYPEATOMIC = ''PHARMACY_DISPOSAL_ISSUE_RETURN'' ',
        'WHERE BILLTYPE     = ''PharmacyIssue'' ',
        '  AND BILLTYPEATOMIC = ''PHARMACY_ISSUE_RETURN'' ',
        '  AND RETIRED      = 0'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT ROW_COUNT() AS updated_step2;

-- ── STEP 3: PHARMACY_ISSUE_CANCELLED → PHARMACY_DISPOSAL_ISSUE_CANCELLED ─────

SELECT 'Step 3: PHARMACY_ISSUE_CANCELLED -> PHARMACY_DISPOSAL_ISSUE_CANCELLED' AS status;

SET @sql = IF(@bill_table IS NULL,
    'SELECT "SKIPPED: bill table not found" AS status',
    CONCAT(
        'UPDATE ', @bill_table, ' ',
        'SET BILLTYPEATOMIC = ''PHARMACY_DISPOSAL_ISSUE_CANCELLED'' ',
        'WHERE BILLTYPE     = ''PharmacyIssue'' ',
        '  AND BILLTYPEATOMIC = ''PHARMACY_ISSUE_CANCELLED'' ',
        '  AND RETIRED      = 0'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT ROW_COUNT() AS updated_step3;

-- ── VERIFY ───────────────────────────────────────────────────────────────────

SELECT 'AFTER: remaining stale rows (should all be 0)' AS status;

SET @after_sql = IF(@bill_table IS NULL,
    'SELECT "SKIPPED: bill table not found" AS status',
    CONCAT(
        'SELECT BILLTYPEATOMIC, COUNT(*) AS cnt ',
        'FROM ', @bill_table, ' ',
        'WHERE BILLTYPE = ''PharmacyIssue'' ',
        '  AND BILLTYPEATOMIC IN (''PHARMACY_ISSUE'', ''PHARMACY_ISSUE_RETURN'', ''PHARMACY_ISSUE_CANCELLED'') ',
        '  AND RETIRED = 0 ',
        'GROUP BY BILLTYPEATOMIC'
    )
);
PREPARE stmt FROM @after_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'Migration v2.8.0 completed' AS final_status;
