-- Rollback v2.23.0: drop WEBUSER.TWOFACTORSETUPWINDOWSTARTEDAT
-- The OTPCODE table is NOT dropped: it holds SMS login code records that a build with the
-- OtpCode entity needs, and v2.23.0 only creates it when missing.
-- WARNING: only roll back together with a build that no longer maps this WebUser field,
-- or every login will fail.

SELECT 'Rollback v2.23.0 - Drop WebUser set-up window column' AS status;

SET @webuser_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'WEBUSER'
    LIMIT 1
);

SET @col = (
    SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @webuser_table
      AND UPPER(COLUMN_NAME) = 'TWOFACTORSETUPWINDOWSTARTEDAT'
    LIMIT 1
);
SET @sql = IF(@webuser_table IS NULL OR @col IS NULL,
    'SELECT "SKIPPED: TWOFACTORSETUPWINDOWSTARTEDAT not found" AS status',
    CONCAT('ALTER TABLE ', @webuser_table, ' DROP COLUMN ', @col)
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
