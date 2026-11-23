-- Rollback v2.24.0: drop WEBUSER.SERVICEACCOUNT
-- WARNING: only roll back together with a build that no longer maps this WebUser field,
-- or every login will fail.

SELECT 'Rollback v2.24.0 - Drop WebUser service account flag' AS status;

SET @webuser_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'WEBUSER'
    LIMIT 1
);

SET @col = (
    SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @webuser_table
      AND UPPER(COLUMN_NAME) = 'SERVICEACCOUNT'
    LIMIT 1
);
SET @sql = IF(@webuser_table IS NULL OR @col IS NULL,
    'SELECT "SKIPPED: SERVICEACCOUNT not found" AS status',
    CONCAT('ALTER TABLE ', @webuser_table, ' DROP COLUMN ', @col)
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
