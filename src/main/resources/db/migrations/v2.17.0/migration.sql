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

-- ── STEP 1: Bill → BilledBill for GrnPaymentPre rows ─────────────────────────

SELECT 'Step 1: DTYPE Bill -> BilledBill for BILLTYPE=GrnPaymentPre' AS status;

SET @sql = IF(@bill_table IS NULL,
    'SELECT "SKIPPED: bill table not found" AS status',
    CONCAT(
        'UPDATE ', @bill_table, ' ',
        'SET DTYPE = ''BilledBill'' ',
        'WHERE BILLTYPE = ''GrnPaymentPre'' ',
        '  AND DTYPE    = ''Bill'''
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT ROW_COUNT() AS updated_step1;

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
