-- Rollback v2.4.0: Remove PharmaceuticalBillItem performance indexes
-- Re-creates the dropped BILLITEM_BILLITEMSTATUS_idx only if needed (it was useless — generally safe to leave absent)

SET @pbi_table = (SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'PHARMACEUTICALBILLITEM' LIMIT 1);

SET @bi_table = (SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'BILLITEM' LIMIT 1);

-- Drop the three new indexes (checked individually via INFORMATION_SCHEMA — avoids
-- ALTER TABLE DROP INDEX IF EXISTS which requires MySQL 8.0.29+)

SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @pbi_table AND INDEX_NAME = 'idx_pbi_createdat_retired');
SET @sql = IF(@pbi_table IS NULL OR @idx_exists = 0, 'SELECT 1',
    CONCAT('ALTER TABLE ', @pbi_table, ' DROP INDEX idx_pbi_createdat_retired'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @pbi_table AND INDEX_NAME = 'idx_pbi_stock_createdat');
SET @sql = IF(@pbi_table IS NULL OR @idx_exists = 0, 'SELECT 1',
    CONCAT('ALTER TABLE ', @pbi_table, ' DROP INDEX idx_pbi_stock_createdat'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @pbi_table AND INDEX_NAME = 'idx_pbi_batch_createdat');
SET @sql = IF(@pbi_table IS NULL OR @idx_exists = 0, 'SELECT 1',
    CONCAT('ALTER TABLE ', @pbi_table, ' DROP INDEX idx_pbi_batch_createdat'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT 'Rollback v2.4.0 completed — three idx_pbi_* indexes dropped' AS status;
SELECT 'NOTE: BILLITEM_BILLITEMSTATUS_idx was 100% NULL and intentionally not restored' AS note;
