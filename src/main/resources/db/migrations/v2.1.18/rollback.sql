-- Rollback v2.1.18: Drop STOCKHISTORYARCHIVE table
-- Issue: #20726
--
-- WARNING: This is destructive. If the archive has been populated, ALL
-- archived rows will be lost. Before rolling back, ensure either:
--   (a) the archive is still empty, OR
--   (b) you have a backup of the archive data you can restore later.
--
-- UNIVERSAL: handles both case-sensitive and case-insensitive MySQL.

SELECT 'Rollback v2.1.18 - Drop STOCKHISTORYARCHIVE table' AS status;

SET @archive_table = (
    SELECT TABLE_NAME
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND UPPER(TABLE_NAME) = 'STOCKHISTORYARCHIVE'
    LIMIT 1
);

SELECT CONCAT('Detected stockhistoryarchive table as: ', IFNULL(@archive_table, '(not found - nothing to drop)')) AS info;

SET @row_count = COALESCE((
    SELECT TABLE_ROWS
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @archive_table
    LIMIT 1
), 0);

SELECT CONCAT('Approximate rows in archive (will be lost): ', @row_count) AS warning;

SET @sql = IF(@archive_table IS NOT NULL,
              CONCAT('DROP TABLE ', @archive_table),
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'Rollback v2.1.18 completed' AS final_status;
