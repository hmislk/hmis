-- Rollback v2.15.0
-- Drops the FK and THEATREROOM_ID column added to patienttransferrequest in v2.15.0.
-- Idempotent and case-insensitive: only drops what exists.

SET @transferrequest_table = (
    SELECT TABLE_NAME
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND UPPER(TABLE_NAME) = 'PATIENTTRANSFERREQUEST'
    LIMIT 1
);

-- STEP 1: DROP FOREIGN KEY (if present)
SET @fk_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = @transferrequest_table
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
      AND CONSTRAINT_NAME = 'FK_patienttransferrequest_theatreroom'
);

SET @sql_drop_fk = IF(@transferrequest_table IS NOT NULL AND @fk_exists > 0,
    CONCAT('ALTER TABLE ', @transferrequest_table, ' DROP FOREIGN KEY FK_patienttransferrequest_theatreroom'),
    'SELECT ''FK_patienttransferrequest_theatreroom not found — nothing to drop'' AS info'
);
PREPARE stmt FROM @sql_drop_fk;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- STEP 2: DROP COLUMN THEATREROOM_ID (if present)
SET @col = (
    SELECT COLUMN_NAME
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = @transferrequest_table
      AND UPPER(COLUMN_NAME) = 'THEATREROOM_ID'
    LIMIT 1
);

SET @sql_drop_col = IF(@transferrequest_table IS NOT NULL AND @col IS NOT NULL,
    CONCAT('ALTER TABLE ', @transferrequest_table, ' DROP COLUMN ', @col),
    'SELECT ''patienttransferrequest.THEATREROOM_ID not found — nothing to drop'' AS info'
);
PREPARE stmt FROM @sql_drop_col;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'Rollback v2.15.0 complete' AS status;
