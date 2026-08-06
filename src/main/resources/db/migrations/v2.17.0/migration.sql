-- Migration v2.17.0: Fix DTYPE for GRN Payment bills saved as base Bill class
-- Author: Dr M H B Ariyaratne
-- Issue: #22717 — GRN Payment listing never shows data
--
-- Root cause:
--   SupplierPaymentController.settleApprovedSupplierPayment() persisted GrnPaymentPre
--   bills via `new Bill()` instead of `new BilledBill()`, so they were saved with
--   DTYPE='Bill' (the base class). Every GRN Payment listing query — the entity-based
--   fallback (type(b) IN (BilledBill, PreBill)) and the DTO query added in #20523
--   (FROM BilledBill b) — requires DTYPE to be 'BilledBill' or 'PreBill', so these
--   bills were silently excluded from every list.
--
--   The application code has been fixed to persist new GrnPaymentPre bills as
--   BilledBill going forward. This migration backfills historical rows so they
--   become visible in the GRN Payment search list retroactively.
--
-- Safe to re-run: UPDATE ... WHERE is idempotent (rows already fixed no longer match).
--
-- ROLLBACK SAFETY: the affected bill IDs are recorded into
-- migration_v2170_grn_payment_bill_ids before the UPDATE runs, and rollback.sql only
-- reverts those exact IDs. This matters because, after this migration ships, newly
-- settled GRN Payment bills are correctly created as BilledBill by the application
-- code fix (#22717) — a rollback that matched on BILLTYPE/DTYPE alone would also
-- revert those unrelated, already-correct rows.
--
-- UNIVERSAL: detects actual table-name case via INFORMATION_SCHEMA so this works on both
-- case-sensitive (Linux, lower_case_table_names=0) and case-insensitive (Windows/Azure) MySQL.

SELECT 'Migration v2.17.0 - Fix DTYPE for GRN Payment bills saved as base Bill class' AS status;

-- ── STEP 0: DETECT ACTUAL TABLE NAME CASE ────────────────────────────────────

SET @bill_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'BILL'
    LIMIT 1
);

SELECT CONCAT('Bill table: ', COALESCE(@bill_table, 'NOT FOUND')) AS info;

-- ── BEFORE counts ────────────────────────────────────────────────────────────

SELECT 'BEFORE: GrnPaymentPre bills mis-saved as base Bill class' AS status;

SET @before_sql = IF(@bill_table IS NULL,
    'SELECT "SKIPPED: bill table not found" AS status',
    CONCAT(
        'SELECT DTYPE, COUNT(*) AS cnt ',
        'FROM ', @bill_table, ' ',
        'WHERE BILLTYPE = ''GrnPaymentPre'' ',
        'GROUP BY DTYPE'
    )
);
PREPARE stmt FROM @before_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ── STEP 1: RECORD AFFECTED BILL IDS FOR SAFE ROLLBACK ────────────────────────

SELECT 'Step 1: Recording affected bill IDs' AS status;

SET @create_backup_sql = 'CREATE TABLE IF NOT EXISTS migration_v2170_grn_payment_bill_ids (
    BILL_ID BIGINT NOT NULL PRIMARY KEY,
    RECORDED_AT DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4';
PREPARE stmt FROM @create_backup_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @record_sql = IF(@bill_table IS NULL,
    'SELECT "SKIPPED: bill table not found" AS status',
    CONCAT(
        'INSERT IGNORE INTO migration_v2170_grn_payment_bill_ids (BILL_ID) ',
        'SELECT ID FROM ', @bill_table, ' ',
        'WHERE BILLTYPE = ''GrnPaymentPre'' ',
        '  AND DTYPE    = ''Bill'''
    )
);
PREPARE stmt FROM @record_sql;
EXECUTE stmt;
SET @recorded_step1 = ROW_COUNT();
DEALLOCATE PREPARE stmt;

SELECT @recorded_step1 AS recorded_step1;

-- ── STEP 2: Bill → BilledBill for the recorded GrnPaymentPre rows ────────────

SELECT 'Step 2: DTYPE Bill -> BilledBill for recorded BILLTYPE=GrnPaymentPre rows' AS status;

SET @sql = IF(@bill_table IS NULL,
    'SELECT "SKIPPED: bill table not found" AS status',
    CONCAT(
        'UPDATE ', @bill_table, ' ',
        'SET DTYPE = ''BilledBill'' ',
        'WHERE BILLTYPE = ''GrnPaymentPre'' ',
        '  AND DTYPE    = ''Bill'' ',
        '  AND ID IN (SELECT BILL_ID FROM migration_v2170_grn_payment_bill_ids)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
SET @updated_step2 = ROW_COUNT();
DEALLOCATE PREPARE stmt;

SELECT @updated_step2 AS updated_step2;

-- ── VERIFY ───────────────────────────────────────────────────────────────────

SELECT 'AFTER: remaining GrnPaymentPre rows with DTYPE=Bill (should be 0)' AS status;

SET @after_sql = IF(@bill_table IS NULL,
    'SELECT "SKIPPED: bill table not found" AS status',
    CONCAT(
        'SELECT DTYPE, COUNT(*) AS cnt ',
        'FROM ', @bill_table, ' ',
        'WHERE BILLTYPE = ''GrnPaymentPre'' ',
        'GROUP BY DTYPE'
    )
);
PREPARE stmt FROM @after_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'Migration v2.17.0 completed' AS final_status;
