-- Migration v2.23.0: OTPCODE table and WebUser two-factor set-up window column
-- Issues: #25755-#25762 (two-factor login hardening)
--
-- 1. Creates the OTPCODE table (entity OtpCode) when it does not exist. SMS login codes,
--    mobile checks and set-up checks are stored there; without the table every SMS code
--    request fails. Columns match the entity fields exactly.
-- 2. Adds WEBUSER.TWOFACTORSETUPWINDOWSTARTEDAT (nullable): when an admin required two-factor
--    login for a user or reset the authenticator; the 24-hour set-up period counts from it.
--    NULL for existing users means the period has not been opened.
--
-- Safe to re-run: the table and the column are created only when missing. No data is changed.
--
-- UNIVERSAL: detects actual table-name case via INFORMATION_SCHEMA so this works on both
-- case-sensitive (Linux, lower_case_table_names=0) and case-insensitive (Windows/Azure) MySQL.

SELECT 'Migration v2.23.0 - OTPCODE table and WebUser set-up window column' AS status;

SET @otp_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'OTPCODE'
    LIMIT 1
);

SET @sql = IF(@otp_table IS NOT NULL,
    'SELECT "SKIPPED: OTPCODE already exists" AS status',
    'CREATE TABLE OTPCODE (
        ID BIGINT NOT NULL AUTO_INCREMENT,
        ATTEMPTS INT DEFAULT NULL,
        CHANNEL VARCHAR(255) DEFAULT NULL,
        CODEHASH VARCHAR(64) DEFAULT NULL,
        CONSUMEDAT DATETIME DEFAULT NULL,
        CREATEDAT DATETIME DEFAULT NULL,
        EXPIRESAT DATETIME DEFAULT NULL,
        IDENTIFIER VARCHAR(255) DEFAULT NULL,
        INVALIDATEDAT DATETIME DEFAULT NULL,
        PURPOSE VARCHAR(255) DEFAULT NULL,
        REQUESTIP VARCHAR(255) DEFAULT NULL,
        SENTSUCCESSFULLY TINYINT(1) DEFAULT 0,
        USERAGENT VARCHAR(500) DEFAULT NULL,
        SMS_ID BIGINT DEFAULT NULL,
        WEBUSER_ID BIGINT DEFAULT NULL,
        PRIMARY KEY (ID)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @webuser_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'WEBUSER'
    LIMIT 1
);

SELECT CONCAT('WebUser table: ', COALESCE(@webuser_table, 'NOT FOUND')) AS info;

SET @has_col = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @webuser_table
      AND UPPER(COLUMN_NAME) = 'TWOFACTORSETUPWINDOWSTARTEDAT'
);
SET @sql = IF(@webuser_table IS NULL,
    'SELECT "SKIPPED: WEBUSER table not found" AS status',
    IF(@has_col > 0,
        'SELECT "SKIPPED: TWOFACTORSETUPWINDOWSTARTEDAT already exists" AS status',
        CONCAT('ALTER TABLE ', @webuser_table, ' ADD COLUMN TWOFACTORSETUPWINDOWSTARTEDAT DATETIME NULL')
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
