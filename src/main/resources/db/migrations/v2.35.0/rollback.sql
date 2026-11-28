-- Rollback v2.35.0: drop IDX_BILLFEE_REFERENCEBILLITEM
-- Changes no data. Only the index added under this name is dropped. An index that already existed
-- under another name is left alone.

SELECT 'Rollback v2.35.0 - drop IDX_BILLFEE_REFERENCEBILLITEM' AS status;

SET @billfee_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'BILLFEE' LIMIT 1
);

SET @idx_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'BILLFEE'
      AND UPPER(INDEX_NAME) = 'IDX_BILLFEE_REFERENCEBILLITEM'
);
SET @sql = IF(@billfee_table IS NULL OR @idx_exists = 0,
    'SELECT "SKIPPED: IDX_BILLFEE_REFERENCEBILLITEM not found" AS status',
    CONCAT('ALTER TABLE ', @billfee_table, ' DROP INDEX IDX_BILLFEE_REFERENCEBILLITEM')
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'Rollback v2.35.0 complete' AS status;
