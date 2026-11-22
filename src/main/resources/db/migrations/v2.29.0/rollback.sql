-- Rollback v2.29.0: drop the OPD credit write-off table and BILL.WRITTENOFFAMOUNT
-- WARNING: all write-off requests and the written-off amounts on bills are lost, so every written-off
-- bill is due again. Only roll back together with a build that no longer maps CreditCompanyWriteOff
-- and Bill.writtenOffAmount.

SELECT 'Rollback v2.29.0 - Drop OPD credit write-off table and column' AS status;

SET @writeoff_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'CREDITCOMPANYWRITEOFF' LIMIT 1
);
SET @sql = IF(@writeoff_table IS NULL,
    'SELECT "SKIPPED: CREDITCOMPANYWRITEOFF not found" AS status',
    CONCAT('DROP TABLE ', @writeoff_table)
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @bill_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'BILL' LIMIT 1
);
SET @col_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'BILL' AND UPPER(COLUMN_NAME) = 'WRITTENOFFAMOUNT'
);
SET @sql = IF(@bill_table IS NULL OR @col_exists = 0,
    'SELECT "SKIPPED: BILL.WRITTENOFFAMOUNT not found" AS status',
    CONCAT('ALTER TABLE ', @bill_table, ' DROP COLUMN WRITTENOFFAMOUNT')
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
