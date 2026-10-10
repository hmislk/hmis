-- Rollback v2.18.0: Revert consumptionAllowed backfill
-- WARNING: Only run if you need to undo migration v2.18.0.
--
-- SCOPED TO RECORDED IDS: only reverts the exact item IDs that migration.sql recorded
-- into migration_v2180_consumption_allowed_item_ids before it ran. This intentionally
-- does NOT match on CONSUMPTIONALLOWED alone, because by the time a rollback might be
-- needed, someone may have deliberately restricted other items to false through the
-- AMP "Consumption Allowed" toggle after this migration shipped — a broad match would
-- also revert those unrelated, deliberate settings.
--
-- UNIVERSAL: detects actual table-name case via INFORMATION_SCHEMA so this works on both
-- case-sensitive (Linux, lower_case_table_names=0) and case-insensitive (Windows/Azure) MySQL.

SELECT 'Rollback v2.18.0 - Reverting consumptionAllowed backfill' AS status;

-- ── STEP 0: DETECT ACTUAL TABLE NAME CASE ────────────────────────────────────

SET @item_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'ITEM'
    LIMIT 1
);

SET @backup_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'MIGRATION_V2180_CONSUMPTION_ALLOWED_ITEM_IDS'
);

SELECT CONCAT('Item table: ', COALESCE(@item_table, 'NOT FOUND')) AS info;
SELECT IF(@backup_exists > 0, 'Recorded item ID table found', 'Recorded item ID table NOT FOUND — nothing to roll back') AS info;

-- ── STEP 1: CONSUMPTIONALLOWED 1 -> 0, restricted to recorded item IDs ───────

SET @sql = IF(@item_table IS NULL OR @backup_exists = 0,
    'SELECT "SKIPPED: item table or recorded-ID table not found" AS status',
    CONCAT(
        'UPDATE ', @item_table, ' ',
        'SET CONSUMPTIONALLOWED = 0 ',
        'WHERE CONSUMPTIONALLOWED = 1 ',
        '  AND ID IN (SELECT ITEM_ID FROM migration_v2180_consumption_allowed_item_ids)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
SET @reverted_step1 = ROW_COUNT();
DEALLOCATE PREPARE stmt;

SELECT @reverted_step1 AS reverted_step1;

SELECT 'Rollback v2.18.0 completed' AS final_status;
