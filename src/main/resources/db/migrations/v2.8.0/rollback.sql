-- Rollback v2.8.0: Revert BILLTYPEATOMIC migration for pharmacy unit/disposal issue bills
-- WARNING: Only run if you need to undo migration v2.8.0 entirely.
--
-- UNIVERSAL: detects actual table-name case via INFORMATION_SCHEMA so this works on both
-- case-sensitive (Linux, lower_case_table_names=0) and case-insensitive (Windows/Azure) MySQL.

SELECT 'Rollback v2.7.0 - Reverting BILLTYPEATOMIC for pharmacy disposal issue bills' AS status;

-- ── STEP 0: DETECT ACTUAL TABLE NAME CASE ────────────────────────────────────

SET @bill_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'BILL'
    LIMIT 1
);

SELECT CONCAT('Bill table: ', COALESCE(@bill_table, 'NOT FOUND')) AS info;

-- ── STEP 1: PHARMACY_DISPOSAL_ISSUE → PHARMACY_ISSUE ─────────────────────────

SET @sql = IF(@bill_table IS NULL,
    'SELECT "SKIPPED: bill table not found" AS status',
    CONCAT(
        'UPDATE ', @bill_table, ' ',
        'SET BILLTYPEATOMIC = ''PHARMACY_ISSUE'' ',
        'WHERE BILLTYPE     = ''PharmacyIssue'' ',
        '  AND BILLTYPEATOMIC = ''PHARMACY_DISPOSAL_ISSUE'' ',
        '  AND RETIRED      = 0'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT ROW_COUNT() AS reverted_step1;

-- ── STEP 2: PHARMACY_DISPOSAL_ISSUE_RETURN → PHARMACY_ISSUE_RETURN ───────────

SET @sql = IF(@bill_table IS NULL,
    'SELECT "SKIPPED: bill table not found" AS status',
    CONCAT(
        'UPDATE ', @bill_table, ' ',
        'SET BILLTYPEATOMIC = ''PHARMACY_ISSUE_RETURN'' ',
        'WHERE BILLTYPE     = ''PharmacyIssue'' ',
        '  AND BILLTYPEATOMIC = ''PHARMACY_DISPOSAL_ISSUE_RETURN'' ',
        '  AND RETIRED      = 0'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT ROW_COUNT() AS reverted_step2;

-- ── STEP 3: PHARMACY_DISPOSAL_ISSUE_CANCELLED → PHARMACY_ISSUE_CANCELLED ─────

SET @sql = IF(@bill_table IS NULL,
    'SELECT "SKIPPED: bill table not found" AS status',
    CONCAT(
        'UPDATE ', @bill_table, ' ',
        'SET BILLTYPEATOMIC = ''PHARMACY_ISSUE_CANCELLED'' ',
        'WHERE BILLTYPE     = ''PharmacyIssue'' ',
        '  AND BILLTYPEATOMIC = ''PHARMACY_DISPOSAL_ISSUE_CANCELLED'' ',
        '  AND RETIRED      = 0'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT ROW_COUNT() AS reverted_step3;

SELECT 'Rollback v2.8.0 completed' AS final_status;
