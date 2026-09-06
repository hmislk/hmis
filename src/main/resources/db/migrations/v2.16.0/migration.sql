-- Migration v2.16.0: Add bill columns for multiple final bill versions per admission
-- Issue: hmislk/hmis#22282
--
-- Background:
--   Inward final bills can now have multiple versions per admission (/1, /2, /3...),
--   with exactly one marked as the Confirmed Final Bill that counts financially.
--   Bill gains three new fields: FINALBILLVERSIONSERIAL (the version number),
--   CONFIRMEDFINALBILL (denormalized flag mirroring patientencounter.finalBill),
--   and PREVIOUSVERSION_ID (display-only link to the bill a version was created from).
--   Production persistence.xml has no DDL generation, so the columns and FK must be
--   created explicitly here, per the v2.15.0 precedent.
--
--   CONFIRMEDFINALBILL defaults to false for every existing row, which would break
--   credit-company dues / refund / payment-total queries (now filtered on
--   confirmedFinalBill=true) for every admission that was already finalized before
--   this migration runs. This script backfills CONFIRMEDFINALBILL=true and
--   FINALBILLVERSIONSERIAL=1 on each admission's existing patientencounter.finalBill,
--   since every pre-existing admission has at most one non-cancelled final bill today
--   (multiple versions were not possible before this feature).
--
-- UNIVERSAL: Detects actual table name case via INFORMATION_SCHEMA so this script
--   works on both case-sensitive (Linux) and case-insensitive (Windows) MySQL.
--
-- IDEMPOTENT: The ADD COLUMN / ADD FOREIGN KEY / backfill statements are gated on
--   INFORMATION_SCHEMA existence checks and WHERE-clause guards so the script can
--   be safely re-run.

SELECT 'Migration v2.16.0 - Add bill columns for multiple final bill versions' AS status;

-- ==========================================
-- STEP 0: RESOLVE ACTUAL TABLE NAMES
-- ==========================================

SET @bill_table = (
    SELECT TABLE_NAME
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND UPPER(TABLE_NAME) = 'BILL'
    LIMIT 1
);

SET @patientencounter_table = (
    SELECT TABLE_NAME
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND UPPER(TABLE_NAME) = 'PATIENTENCOUNTER'
    LIMIT 1
);

SELECT CONCAT('Resolved bill table as: ', IFNULL(@bill_table, '(not found)')) AS info;
SELECT CONCAT('Resolved patientencounter table as: ', IFNULL(@patientencounter_table, '(not found)')) AS info;

-- ==========================================
-- STEP 1: ADD COLUMN FINALBILLVERSIONSERIAL (if missing)
-- ==========================================

SET @col_serial_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = @bill_table
      AND UPPER(COLUMN_NAME) = 'FINALBILLVERSIONSERIAL'
);

SET @sql_add_serial = IF(@bill_table IS NOT NULL AND @col_serial_exists = 0,
    CONCAT('ALTER TABLE ', @bill_table, ' ADD COLUMN FINALBILLVERSIONSERIAL INT NULL'),
    'SELECT ''bill.FINALBILLVERSIONSERIAL already exists or table missing — skipping column add'' AS info'
);
PREPARE stmt FROM @sql_add_serial;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ==========================================
-- STEP 2: ADD COLUMN CONFIRMEDFINALBILL (if missing)
-- ==========================================

SET @col_confirmed_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = @bill_table
      AND UPPER(COLUMN_NAME) = 'CONFIRMEDFINALBILL'
);

SET @sql_add_confirmed = IF(@bill_table IS NOT NULL AND @col_confirmed_exists = 0,
    CONCAT('ALTER TABLE ', @bill_table, ' ADD COLUMN CONFIRMEDFINALBILL TINYINT(1) NOT NULL DEFAULT 0'),
    'SELECT ''bill.CONFIRMEDFINALBILL already exists or table missing — skipping column add'' AS info'
);
PREPARE stmt FROM @sql_add_confirmed;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ==========================================
-- STEP 3: ADD COLUMN PREVIOUSVERSION_ID (if missing)
-- ==========================================

SET @col_prevver_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = @bill_table
      AND UPPER(COLUMN_NAME) = 'PREVIOUSVERSION_ID'
);

SET @sql_add_prevver = IF(@bill_table IS NOT NULL AND @col_prevver_exists = 0,
    CONCAT('ALTER TABLE ', @bill_table, ' ADD COLUMN PREVIOUSVERSION_ID BIGINT NULL'),
    'SELECT ''bill.PREVIOUSVERSION_ID already exists or table missing — skipping column add'' AS info'
);
PREPARE stmt FROM @sql_add_prevver;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ==========================================
-- STEP 4: ADD FOREIGN KEY PREVIOUSVERSION_ID -> bill(ID) (if missing)
-- ==========================================

SET @fk_prevver_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = @bill_table
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
      AND CONSTRAINT_NAME = 'FK_bill_previousversion'
);

SET @sql_add_fk = IF(@bill_table IS NOT NULL AND @fk_prevver_exists = 0,
    CONCAT('ALTER TABLE ', @bill_table,
           ' ADD CONSTRAINT FK_bill_previousversion',
           ' FOREIGN KEY (PREVIOUSVERSION_ID) REFERENCES ', @bill_table, '(ID)'),
    'SELECT ''FK_bill_previousversion already exists or table missing — skipping FK add'' AS info'
);
PREPARE stmt FROM @sql_add_fk;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ==========================================
-- STEP 5: BACKFILL CONFIRMEDFINALBILL + FINALBILLVERSIONSERIAL
-- ==========================================
-- Every admission finalized before this migration has at most one
-- non-cancelled final bill (multiple versions were impossible until now),
-- namely patientencounter.finalBill. Mark that bill as confirmed and /1,
-- so existing financial queries (now scoped to confirmedFinalBill=true)
-- keep returning the same results they did before this migration.

SET @sql_backfill = IF(@bill_table IS NOT NULL AND @patientencounter_table IS NOT NULL,
    CONCAT(
        'UPDATE ', @bill_table, ' b ',
        'JOIN ', @patientencounter_table, ' pe ON pe.FINALBILL_ID = b.ID ',
        'SET b.CONFIRMEDFINALBILL = 1, ',
        '    b.FINALBILLVERSIONSERIAL = IFNULL(b.FINALBILLVERSIONSERIAL, 1) ',
        'WHERE b.CONFIRMEDFINALBILL = 0'
    ),
    'SELECT ''bill or patientencounter table missing — skipping confirmedFinalBill backfill'' AS info'
);
PREPARE stmt FROM @sql_backfill;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'Migration v2.16.0 complete' AS status;
