-- Rollback v2.17.0: Revert DTYPE fix for GRN Payment bills
-- WARNING: Only run if you need to undo migration v2.17.0 entirely. This restores the
-- pre-fix (broken) state where these bills are excluded from the GRN Payment listing again.
--
-- UNIVERSAL: detects actual table-name case via INFORMATION_SCHEMA so this works on both
-- case-sensitive (Linux, lower_case_table_names=0) and case-insensitive (Windows/Azure) MySQL.

SELECT 'Rollback v2.17.0 - Reverting DTYPE fix for GRN Payment bills' AS status;

-- ── STEP 0: DETECT ACTUAL TABLE NAME CASE ────────────────────────────────────

SET @bill_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'BILL'
    LIMIT 1
);

SELECT CONCAT('Bill table: ', COALESCE(@bill_table, 'NOT FOUND')) AS info;

-- ── STEP 1: BilledBill → Bill for GrnPaymentPre rows fixed by this migration ─
--
--   NOTE: this only reverts rows that were previously updated by migration.sql.
--   It cannot distinguish those from GrnPaymentPre bills that were legitimately
--   created as BilledBill (e.g. by the code fix in #22717) after this migration
--   ran, so this rollback is only safe immediately after applying the migration
--   and before any new GRN Payment bills have been settled.

SET @sql = IF(@bill_table IS NULL,
    'SELECT "SKIPPED: bill table not found" AS status',
    CONCAT(
        'UPDATE ', @bill_table, ' ',
        'SET DTYPE = ''Bill'' ',
        'WHERE BILLTYPE = ''GrnPaymentPre'' ',
        '  AND DTYPE    = ''BilledBill'''
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT ROW_COUNT() AS reverted_step1;

SELECT 'Rollback v2.17.0 completed' AS final_status;
