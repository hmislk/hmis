-- Rollback v2.20.0: drop the WebUser two-factor columns
-- WARNING: this deletes every user's two-factor enrolment (authenticator secrets,
-- lock state and verified SMS numbers). Only roll back together with a build that
-- no longer maps these WebUser fields, or every login will fail again.

SELECT 'Rollback v2.20.0 - Drop WebUser two-factor columns' AS status;

SET @webuser_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'WEBUSER'
    LIMIT 1
);

SET @col = (
    SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @webuser_table
      AND UPPER(COLUMN_NAME) = 'TWOFACTORREQUIRED'
    LIMIT 1
);
SET @sql = IF(@webuser_table IS NULL OR @col IS NULL,
    'SELECT "SKIPPED: TWOFACTORREQUIRED not found" AS status',
    CONCAT('ALTER TABLE ', @webuser_table, ' DROP COLUMN ', @col)
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col = (
    SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @webuser_table
      AND UPPER(COLUMN_NAME) = 'TWOFACTORENROLLED'
    LIMIT 1
);
SET @sql = IF(@webuser_table IS NULL OR @col IS NULL,
    'SELECT "SKIPPED: TWOFACTORENROLLED not found" AS status',
    CONCAT('ALTER TABLE ', @webuser_table, ' DROP COLUMN ', @col)
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col = (
    SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @webuser_table
      AND UPPER(COLUMN_NAME) = 'TWOFACTORSECRET'
    LIMIT 1
);
SET @sql = IF(@webuser_table IS NULL OR @col IS NULL,
    'SELECT "SKIPPED: TWOFACTORSECRET not found" AS status',
    CONCAT('ALTER TABLE ', @webuser_table, ' DROP COLUMN ', @col)
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col = (
    SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @webuser_table
      AND UPPER(COLUMN_NAME) = 'TWOFACTORENROLLEDAT'
    LIMIT 1
);
SET @sql = IF(@webuser_table IS NULL OR @col IS NULL,
    'SELECT "SKIPPED: TWOFACTORENROLLEDAT not found" AS status',
    CONCAT('ALTER TABLE ', @webuser_table, ' DROP COLUMN ', @col)
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col = (
    SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @webuser_table
      AND UPPER(COLUMN_NAME) = 'TWOFACTORLASTUSEDSTEP'
    LIMIT 1
);
SET @sql = IF(@webuser_table IS NULL OR @col IS NULL,
    'SELECT "SKIPPED: TWOFACTORLASTUSEDSTEP not found" AS status',
    CONCAT('ALTER TABLE ', @webuser_table, ' DROP COLUMN ', @col)
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col = (
    SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @webuser_table
      AND UPPER(COLUMN_NAME) = 'TWOFACTORFAILEDATTEMPTS'
    LIMIT 1
);
SET @sql = IF(@webuser_table IS NULL OR @col IS NULL,
    'SELECT "SKIPPED: TWOFACTORFAILEDATTEMPTS not found" AS status',
    CONCAT('ALTER TABLE ', @webuser_table, ' DROP COLUMN ', @col)
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col = (
    SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @webuser_table
      AND UPPER(COLUMN_NAME) = 'TWOFACTORLOCKED'
    LIMIT 1
);
SET @sql = IF(@webuser_table IS NULL OR @col IS NULL,
    'SELECT "SKIPPED: TWOFACTORLOCKED not found" AS status',
    CONCAT('ALTER TABLE ', @webuser_table, ' DROP COLUMN ', @col)
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col = (
    SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @webuser_table
      AND UPPER(COLUMN_NAME) = 'TWOFACTORLOCKEDAT'
    LIMIT 1
);
SET @sql = IF(@webuser_table IS NULL OR @col IS NULL,
    'SELECT "SKIPPED: TWOFACTORLOCKEDAT not found" AS status',
    CONCAT('ALTER TABLE ', @webuser_table, ' DROP COLUMN ', @col)
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col = (
    SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @webuser_table
      AND UPPER(COLUMN_NAME) = 'TWOFACTORSMSALLOWED'
    LIMIT 1
);
SET @sql = IF(@webuser_table IS NULL OR @col IS NULL,
    'SELECT "SKIPPED: TWOFACTORSMSALLOWED not found" AS status',
    CONCAT('ALTER TABLE ', @webuser_table, ' DROP COLUMN ', @col)
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col = (
    SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @webuser_table
      AND UPPER(COLUMN_NAME) = 'TWOFACTORMOBILE'
    LIMIT 1
);
SET @sql = IF(@webuser_table IS NULL OR @col IS NULL,
    'SELECT "SKIPPED: TWOFACTORMOBILE not found" AS status',
    CONCAT('ALTER TABLE ', @webuser_table, ' DROP COLUMN ', @col)
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col = (
    SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @webuser_table
      AND UPPER(COLUMN_NAME) = 'TWOFACTORMOBILEVERIFIEDAT'
    LIMIT 1
);
SET @sql = IF(@webuser_table IS NULL OR @col IS NULL,
    'SELECT "SKIPPED: TWOFACTORMOBILEVERIFIEDAT not found" AS status',
    CONCAT('ALTER TABLE ', @webuser_table, ' DROP COLUMN ', @col)
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
