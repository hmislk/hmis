-- Migration v2.29.0: write-off of OPD credit bill dues
-- Issue: #25847 (OPD credit company claims, part 3)
--
-- 1. Adds BILL.WRITTENOFFAMOUNT (DOUBLE NULL), mapped by Bill.writtenOffAmount, when missing.
-- 2. Creates CREDITCOMPANYWRITEOFF, mapped by the CreditCompanyWriteOff entity, when missing.
--
-- Production has no DDL generation, so run this BEFORE deploying a build that contains these entities.
-- Requires v2.26.0 (CREDITCOMPANYCLAIM).
--
-- Safe to re-run: each step is skipped when its column or table already exists.
--
-- UNIVERSAL: detects actual table-name case via INFORMATION_SCHEMA so this works on both
-- case-sensitive (Linux, lower_case_table_names=0) and case-insensitive (Windows/Azure) MySQL.
-- The new table is created in the same case as the existing WEBUSER table.

SELECT 'Migration v2.29.0 - OPD credit write-off' AS status;

SET @webuser_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'WEBUSER' LIMIT 1
);
SET @bill_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'BILL' LIMIT 1
);
SET @claim_actual = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'CREDITCOMPANYCLAIM' LIMIT 1
);

SELECT CONCAT('WEBUSER: ', COALESCE(@webuser_table, 'NOT FOUND'),
              ', BILL: ', COALESCE(@bill_table, 'NOT FOUND'),
              ', CREDITCOMPANYCLAIM: ', COALESCE(@claim_actual, 'NOT FOUND')) AS info;

-- Step 1: BILL.WRITTENOFFAMOUNT
-- ALTER on BILL: instant on MySQL 8.0.12+; on older MySQL it rebuilds the table (run in a quiet window).
SET @col_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'BILL' AND UPPER(COLUMN_NAME) = 'WRITTENOFFAMOUNT'
);
SET @sql = IF(@bill_table IS NULL,
    'SELECT "SKIPPED: BILL table not found" AS status',
    IF(@col_exists > 0,
        'SELECT "SKIPPED: BILL.WRITTENOFFAMOUNT already exists" AS status',
        CONCAT('ALTER TABLE ', @bill_table, ' ADD COLUMN WRITTENOFFAMOUNT DOUBLE NULL')
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 2: CREDITCOMPANYWRITEOFF
SET @writeoff_table = IF(BINARY @webuser_table = BINARY UPPER(@webuser_table), 'CREDITCOMPANYWRITEOFF', 'creditcompanywriteoff');

SET @exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'CREDITCOMPANYWRITEOFF'
);
SET @sql = IF(@webuser_table IS NULL OR @bill_table IS NULL OR @claim_actual IS NULL,
    'SELECT "SKIPPED: WEBUSER, BILL or CREDITCOMPANYCLAIM table not found (run v2.26.0 first)" AS status',
    IF(@exists > 0,
        'SELECT "SKIPPED: CREDITCOMPANYWRITEOFF already exists" AS status',
        CONCAT('CREATE TABLE ', @writeoff_table, ' (
            ID BIGINT NOT NULL AUTO_INCREMENT,
            AMOUNT DOUBLE DEFAULT NULL,
            REASON VARCHAR(500) DEFAULT NULL,
            STATUS VARCHAR(255) DEFAULT NULL,
            REQUESTEDAT DATETIME DEFAULT NULL,
            DECIDEDAT DATETIME DEFAULT NULL,
            DECISIONCOMMENT VARCHAR(500) DEFAULT NULL,
            CANCELLEDAT DATETIME DEFAULT NULL,
            CANCELREASON VARCHAR(500) DEFAULT NULL,
            RETIRED TINYINT(1) DEFAULT 0,
            RETIREDAT DATETIME DEFAULT NULL,
            BILL_ID BIGINT DEFAULT NULL,
            CLAIM_ID BIGINT DEFAULT NULL,
            REQUESTEDBY_ID BIGINT DEFAULT NULL,
            DECIDEDBY_ID BIGINT DEFAULT NULL,
            CANCELLEDBY_ID BIGINT DEFAULT NULL,
            RETIRER_ID BIGINT DEFAULT NULL,
            PRIMARY KEY (ID),
            KEY FK_CREDITCOMPANYWRITEOFF_BILL_ID (BILL_ID),
            KEY FK_CREDITCOMPANYWRITEOFF_CLAIM_ID (CLAIM_ID),
            KEY FK_CREDITCOMPANYWRITEOFF_REQUESTEDBY_ID (REQUESTEDBY_ID),
            KEY FK_CREDITCOMPANYWRITEOFF_DECIDEDBY_ID (DECIDEDBY_ID),
            KEY FK_CREDITCOMPANYWRITEOFF_CANCELLEDBY_ID (CANCELLEDBY_ID),
            KEY FK_CREDITCOMPANYWRITEOFF_RETIRER_ID (RETIRER_ID),
            CONSTRAINT FK_CREDITCOMPANYWRITEOFF_BILL_ID FOREIGN KEY (BILL_ID) REFERENCES ', @bill_table, ' (ID),
            CONSTRAINT FK_CREDITCOMPANYWRITEOFF_CLAIM_ID FOREIGN KEY (CLAIM_ID) REFERENCES ', @claim_actual, ' (ID),
            CONSTRAINT FK_CREDITCOMPANYWRITEOFF_REQUESTEDBY_ID FOREIGN KEY (REQUESTEDBY_ID) REFERENCES ', @webuser_table, ' (ID),
            CONSTRAINT FK_CREDITCOMPANYWRITEOFF_DECIDEDBY_ID FOREIGN KEY (DECIDEDBY_ID) REFERENCES ', @webuser_table, ' (ID),
            CONSTRAINT FK_CREDITCOMPANYWRITEOFF_CANCELLEDBY_ID FOREIGN KEY (CANCELLEDBY_ID) REFERENCES ', @webuser_table, ' (ID),
            CONSTRAINT FK_CREDITCOMPANYWRITEOFF_RETIRER_ID FOREIGN KEY (RETIRER_ID) REFERENCES ', @webuser_table, ' (ID)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4')
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
