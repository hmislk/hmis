-- Rollback v2.19.0: drop idx_bill_onlinebooking
-- Only drops the index this migration created (by name); an index that already
-- existed under another name is left alone.

SELECT 'Rollback v2.19.0 - Drop idx_bill_onlinebooking' AS status;

SET @bill_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'BILL'
    LIMIT 1
);

SET @has_index = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @bill_table
      AND INDEX_NAME = 'idx_bill_onlinebooking'
);

SET @drop_sql = IF(@bill_table IS NULL OR @has_index = 0,
    'SELECT "SKIPPED: idx_bill_onlinebooking not found" AS status',
    CONCAT('DROP INDEX idx_bill_onlinebooking ON ', @bill_table)
);
PREPARE stmt FROM @drop_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
