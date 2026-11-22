-- Migration v2.24.0: WebUser service account flag
-- Issue: #25755 (two-factor login follow-ups)
--
-- Adds WEBUSER.SERVICEACCOUNT (BOOLEAN, default false). When two-factor login is required for
-- all users, lab and channelling API logins (which cannot give a second-step code) accept only
-- accounts with this flag. Existing users get false.
--
-- Safe to re-run: the column is added only when missing.
--
-- UNIVERSAL: detects actual table-name case via INFORMATION_SCHEMA so this works on both
-- case-sensitive (Linux, lower_case_table_names=0) and case-insensitive (Windows/Azure) MySQL.

SELECT 'Migration v2.24.0 - WebUser service account flag' AS status;

SET @webuser_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'WEBUSER'
    LIMIT 1
);

SELECT CONCAT('WebUser table: ', COALESCE(@webuser_table, 'NOT FOUND')) AS info;

SET @has_col = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @webuser_table
      AND UPPER(COLUMN_NAME) = 'SERVICEACCOUNT'
);
SET @sql = IF(@webuser_table IS NULL,
    'SELECT "SKIPPED: WEBUSER table not found" AS status',
    IF(@has_col > 0,
        'SELECT "SKIPPED: SERVICEACCOUNT already exists" AS status',
        CONCAT('ALTER TABLE ', @webuser_table, ' ADD COLUMN SERVICEACCOUNT TINYINT(1) NOT NULL DEFAULT 0')
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
