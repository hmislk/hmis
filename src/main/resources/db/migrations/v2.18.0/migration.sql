-- Migration v2.18.0: Backfill Item.consumptionAllowed default for pre-existing items
-- Author: Dr M H B Ariyaratne
-- Issue: #23995 — Consumption/Disposal Issue item search hides ~92% of pharmacy stock
--
-- Root cause:
--   Item.consumptionAllowed was added (commit edcde8f9, Nov 2025) as a Java primitive
--   `boolean consumptionAllowed = true`. That initializer only applies to objects
--   created fresh in the JVM — it has no effect on the SQL column default. The column
--   was added via generate-ddl to a table with many pre-existing rows; with no
--   explicit DEFAULT clause, MySQL backfilled every existing row to 0 (false), the
--   opposite of the intended default, instead of leaving them NULL. The consuming
--   query (PharmacyIssueController.completeAvailableStocksWithConsumptionFilter)
--   already has an `OR consumptionAllowed IS NULL` fallback for exactly this legacy-
--   data case, but it never fires because nothing is actually NULL — every affected
--   item is silently excluded from Consumption/Disposal Issue item search.
--
--   This migration backfills CONSUMPTIONALLOWED=1 for items that predate the column
--   (i.e. still sit at the wrong backfilled default of 0), restoring the intended
--   "allowed unless explicitly restricted" default. No application code change is
--   needed — Item.consumptionAllowed's Java default is already correct.
--
-- Safe to re-run: UPDATE ... WHERE is idempotent (rows already fixed no longer match).
--
-- ROLLBACK SAFETY: the affected item IDs are recorded into
-- migration_v2180_consumption_allowed_item_ids before the UPDATE runs, and
-- rollback.sql only reverts those exact IDs — so an item someone deliberately
-- restricts (sets to false) *after* this migration runs is never touched by rollback.
--
-- UNIVERSAL: detects actual table-name case via INFORMATION_SCHEMA so this works on both
-- case-sensitive (Linux, lower_case_table_names=0) and case-insensitive (Windows/Azure) MySQL.

SELECT 'Migration v2.18.0 - Backfill Item.consumptionAllowed default for pre-existing items' AS status;

-- ── STEP 0: DETECT ACTUAL TABLE NAME CASE ────────────────────────────────────

SET @item_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'ITEM'
    LIMIT 1
);

SELECT CONCAT('Item table: ', COALESCE(@item_table, 'NOT FOUND')) AS info;

-- ── BEFORE counts ────────────────────────────────────────────────────────────

SELECT 'BEFORE: CONSUMPTIONALLOWED distribution on active items' AS status;

SET @before_sql = IF(@item_table IS NULL,
    'SELECT "SKIPPED: item table not found" AS status',
    CONCAT(
        'SELECT CONSUMPTIONALLOWED, COUNT(*) AS cnt ',
        'FROM ', @item_table, ' ',
        'WHERE RETIRED = 0 AND INACTIVE = 0 ',
        'GROUP BY CONSUMPTIONALLOWED'
    )
);
PREPARE stmt FROM @before_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ── STEP 1: RECORD AFFECTED ITEM IDS FOR SAFE ROLLBACK ────────────────────────

SELECT 'Step 1: Recording affected item IDs' AS status;

SET @create_backup_sql = 'CREATE TABLE IF NOT EXISTS migration_v2180_consumption_allowed_item_ids (
    ITEM_ID BIGINT NOT NULL PRIMARY KEY,
    RECORDED_AT DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4';
PREPARE stmt FROM @create_backup_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @record_sql = IF(@item_table IS NULL,
    'SELECT "SKIPPED: item table not found" AS status',
    CONCAT(
        'INSERT IGNORE INTO migration_v2180_consumption_allowed_item_ids (ITEM_ID) ',
        'SELECT ID FROM ', @item_table, ' ',
        'WHERE CONSUMPTIONALLOWED = 0'
    )
);
PREPARE stmt FROM @record_sql;
EXECUTE stmt;
SET @recorded_step1 = ROW_COUNT();
DEALLOCATE PREPARE stmt;

SELECT @recorded_step1 AS recorded_step1;

-- ── STEP 2: Backfill CONSUMPTIONALLOWED 0 -> 1 for the recorded rows ─────────

SELECT 'Step 2: CONSUMPTIONALLOWED 0 -> 1 for recorded item IDs' AS status;

SET @sql = IF(@item_table IS NULL,
    'SELECT "SKIPPED: item table not found" AS status',
    CONCAT(
        'UPDATE ', @item_table, ' ',
        'SET CONSUMPTIONALLOWED = 1 ',
        'WHERE CONSUMPTIONALLOWED = 0 ',
        '  AND ID IN (SELECT ITEM_ID FROM migration_v2180_consumption_allowed_item_ids)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
SET @updated_step2 = ROW_COUNT();
DEALLOCATE PREPARE stmt;

SELECT @updated_step2 AS updated_step2;

-- ── VERIFY ───────────────────────────────────────────────────────────────────

SELECT 'AFTER: CONSUMPTIONALLOWED distribution on active items (should be all 1, or explicit later restrictions)' AS status;

SET @after_sql = IF(@item_table IS NULL,
    'SELECT "SKIPPED: item table not found" AS status',
    CONCAT(
        'SELECT CONSUMPTIONALLOWED, COUNT(*) AS cnt ',
        'FROM ', @item_table, ' ',
        'WHERE RETIRED = 0 AND INACTIVE = 0 ',
        'GROUP BY CONSUMPTIONALLOWED'
    )
);
PREPARE stmt FROM @after_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'Migration v2.18.0 completed' AS final_status;
