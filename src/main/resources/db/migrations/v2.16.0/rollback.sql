-- Rollback v2.16.0
-- Drops the FK and the FINALBILLVERSIONSERIAL / CONFIRMEDFINALBILL / PREVIOUSVERSION_ID
-- columns added to bill in v2.16.0. Idempotent and case-insensitive: only drops what exists.
-- The confirmedFinalBill/version-serial backfill is data, not schema, and is discarded
-- along with the columns — nothing further to reverse.

SET @bill_table = (
    SELECT TABLE_NAME
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND UPPER(TABLE_NAME) = 'BILL'
    LIMIT 1
);

-- STEP 1: DROP FOREIGN KEY (if present)
SET @fk_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = @bill_table
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
      AND CONSTRAINT_NAME = 'FK_bill_previousversion'
);

SET @sql_drop_fk = IF(@bill_table IS NOT NULL AND @fk_exists > 0,
    CONCAT('ALTER TABLE ', @bill_table, ' DROP FOREIGN KEY FK_bill_previousversion'),
    'SELECT ''FK_bill_previousversion not found — nothing to drop'' AS info'
);
PREPARE stmt FROM @sql_drop_fk;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- STEP 2: DROP COLUMN PREVIOUSVERSION_ID (if present)
SET @col_prevver = (
    SELECT COLUMN_NAME
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = @bill_table
      AND UPPER(COLUMN_NAME) = 'PREVIOUSVERSION_ID'
    LIMIT 1
);

SET @sql_drop_prevver = IF(@bill_table IS NOT NULL AND @col_prevver IS NOT NULL,
    CONCAT('ALTER TABLE ', @bill_table, ' DROP COLUMN ', @col_prevver),
    'SELECT ''bill.PREVIOUSVERSION_ID not found — nothing to drop'' AS info'
);
PREPARE stmt FROM @sql_drop_prevver;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- STEP 3: DROP COLUMN CONFIRMEDFINALBILL (if present)
SET @col_confirmed = (
    SELECT COLUMN_NAME
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = @bill_table
      AND UPPER(COLUMN_NAME) = 'CONFIRMEDFINALBILL'
    LIMIT 1
);

SET @sql_drop_confirmed = IF(@bill_table IS NOT NULL AND @col_confirmed IS NOT NULL,
    CONCAT('ALTER TABLE ', @bill_table, ' DROP COLUMN ', @col_confirmed),
    'SELECT ''bill.CONFIRMEDFINALBILL not found — nothing to drop'' AS info'
);
PREPARE stmt FROM @sql_drop_confirmed;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- STEP 4: DROP COLUMN FINALBILLVERSIONSERIAL (if present)
SET @col_serial = (
    SELECT COLUMN_NAME
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = @bill_table
      AND UPPER(COLUMN_NAME) = 'FINALBILLVERSIONSERIAL'
    LIMIT 1
);

SET @sql_drop_serial = IF(@bill_table IS NOT NULL AND @col_serial IS NOT NULL,
    CONCAT('ALTER TABLE ', @bill_table, ' DROP COLUMN ', @col_serial),
    'SELECT ''bill.FINALBILLVERSIONSERIAL not found — nothing to drop'' AS info'
);
PREPARE stmt FROM @sql_drop_serial;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'Rollback v2.16.0 complete' AS status;
