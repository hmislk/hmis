-- Rollback v2.17.0: Revert DTYPE fix for GRN Payment bills
-- WARNING: Only run if you need to undo migration v2.17.0.
--
-- SCOPED TO RECORDED IDS: only reverts the exact bill IDs that migration.sql recorded
-- into migration_v2170_grn_payment_bill_ids before it ran. This intentionally does NOT
-- match on BILLTYPE/DTYPE alone, because by the time a rollback might be needed, new
-- GRN Payment bills settled after this migration shipped are correctly created as
-- BilledBill by the application code fix (#22717) — a broad DTYPE match would also
-- revert those unrelated, already-correct rows back to the broken DTYPE='Bill' state.
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

SET @backup_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'MIGRATION_V2170_GRN_PAYMENT_BILL_IDS'
);

SELECT CONCAT('Bill table: ', COALESCE(@bill_table, 'NOT FOUND')) AS info;
SELECT IF(@backup_exists > 0, 'Recorded bill ID table found', 'Recorded bill ID table NOT FOUND — nothing to roll back') AS info;

-- ── STEP 1: BilledBill → Bill, restricted to recorded bill IDs ───────────────

SET @sql = IF(@bill_table IS NULL OR @backup_exists = 0,
    'SELECT "SKIPPED: bill table or recorded-ID table not found" AS status',
    CONCAT(
        'UPDATE ', @bill_table, ' ',
        'SET DTYPE = ''Bill'' ',
        'WHERE BILLTYPE = ''GrnPaymentPre'' ',
        '  AND DTYPE    = ''BilledBill'' ',
        '  AND ID IN (SELECT BILL_ID FROM migration_v2170_grn_payment_bill_ids)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
SET @reverted_step1 = ROW_COUNT();
DEALLOCATE PREPARE stmt;

SELECT @reverted_step1 AS reverted_step1;

SELECT 'Rollback v2.17.0 completed' AS final_status;
