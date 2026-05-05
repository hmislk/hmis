-- Rollback v2.3.0: Clear backfilled per-unit rate fields in BillFee
-- Author: Dr M H B Ariyaratne
--
-- Sets feeUnit* columns back to NULL so the migration can be re-run if needed.
-- NOTE: This does NOT remove the columns themselves — those were added by
-- missing_database_fields.xhtml and are owned by the schema management system.

SELECT 'Rollback v2.3.0 - clearing BillFee per-unit rate backfill' AS status;

SET @billfee_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'BILLFEE'
    LIMIT 1
);

SET @col_unitgross = (
    SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @billfee_table
    AND UPPER(COLUMN_NAME) = 'FEEUNITGROSSVALUE' LIMIT 1
);
SET @col_unitvalue = (
    SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @billfee_table
    AND UPPER(COLUMN_NAME) = 'FEEUNITVALUE' LIMIT 1
);
SET @col_unitmargin = (
    SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @billfee_table
    AND UPPER(COLUMN_NAME) = 'FEEUNITMARGIN' LIMIT 1
);
SET @col_unitdiscount = (
    SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @billfee_table
    AND UPPER(COLUMN_NAME) = 'FEEUNITDISCOUNT' LIMIT 1
);

SET @sql = CONCAT(
    'UPDATE ', @billfee_table,
    ' SET ',
    @col_unitgross,    ' = NULL, ',
    @col_unitvalue,    ' = NULL, ',
    @col_unitmargin,   ' = NULL, ',
    @col_unitdiscount, ' = NULL'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'Rollback v2.3.0 complete' AS status;
