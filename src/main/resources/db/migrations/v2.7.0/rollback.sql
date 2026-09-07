-- Rollback v2.7.0: Drop STOCKHISTORYARCHIVE performance indexes

SET @sha_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'STOCKHISTORYARCHIVE'
    LIMIT 1
);

SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'STOCKHISTORYARCHIVE'
      AND INDEX_NAME = 'idx_stockhistoryarchive_batch_date_retired');
SET @sql = IF(@sha_table IS NULL OR @idx_exists = 0, 'SELECT 1',
    CONCAT('ALTER TABLE ', @sha_table, ' DROP INDEX idx_stockhistoryarchive_batch_date_retired'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'STOCKHISTORYARCHIVE'
      AND INDEX_NAME = 'idx_stockhistoryarchive_date_dept_retired');
SET @sql = IF(@sha_table IS NULL OR @idx_exists = 0, 'SELECT 1',
    CONCAT('ALTER TABLE ', @sha_table, ' DROP INDEX idx_stockhistoryarchive_date_dept_retired'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'STOCKHISTORYARCHIVE'
      AND INDEX_NAME = 'idx_stockhistoryarchive_historytype_dept_createdat');
SET @sql = IF(@sha_table IS NULL OR @idx_exists = 0, 'SELECT 1',
    CONCAT('ALTER TABLE ', @sha_table, ' DROP INDEX idx_stockhistoryarchive_historytype_dept_createdat'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT 'Rollback v2.7.0 completed — STOCKHISTORYARCHIVE performance indexes dropped' AS status;
