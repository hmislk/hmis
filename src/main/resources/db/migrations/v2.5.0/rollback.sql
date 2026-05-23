-- Rollback v2.5.0: Drop RMH performance indexes from Bill and StockHistory
-- NOTE: RMH production already has these indexes applied directly.
-- Rolling back here will NOT remove them from RMH prod — run this script manually on prod if needed.

SET @bill_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'BILL'
    LIMIT 1
);

SET @sh_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'STOCKHISTORY'
    LIMIT 1
);

-- Drop Bill indexes

SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @bill_table
      AND INDEX_NAME = 'idx_bill_institution_billtype_dtype_retired');
SET @sql = IF(@bill_table IS NULL OR @idx_exists = 0, 'SELECT 1',
    CONCAT('ALTER TABLE ', @bill_table, ' DROP INDEX idx_bill_institution_billtype_dtype_retired'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @bill_table
      AND INDEX_NAME = 'idx_bill_institution_billtype_retired_createdat');
SET @sql = IF(@bill_table IS NULL OR @idx_exists = 0, 'SELECT 1',
    CONCAT('ALTER TABLE ', @bill_table, ' DROP INDEX idx_bill_institution_billtype_retired_createdat'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @bill_table
      AND INDEX_NAME = 'idx_bill_institution_paymentmethod_retired_createdat');
SET @sql = IF(@bill_table IS NULL OR @idx_exists = 0, 'SELECT 1',
    CONCAT('ALTER TABLE ', @bill_table, ' DROP INDEX idx_bill_institution_paymentmethod_retired_createdat'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @bill_table
      AND INDEX_NAME = 'idx_bill_dept_billdate_billtype');
SET @sql = IF(@bill_table IS NULL OR @idx_exists = 0, 'SELECT 1',
    CONCAT('ALTER TABLE ', @bill_table, ' DROP INDEX idx_bill_dept_billdate_billtype'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Drop StockHistory index

SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @sh_table
      AND INDEX_NAME = 'idx_stockhistory_historytype_dept_createdat');
SET @sql = IF(@sh_table IS NULL OR @idx_exists = 0, 'SELECT 1',
    CONCAT('ALTER TABLE ', @sh_table, ' DROP INDEX idx_stockhistory_historytype_dept_createdat'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'Rollback v2.5.0 completed — requested index-drop steps processed (missing tables/indexes skipped)' AS status;
