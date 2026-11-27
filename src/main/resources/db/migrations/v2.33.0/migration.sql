-- Migration v2.33.0: cancellation columns on PATIENTENCOUNTER
-- Issue: #25745 (cancelled admissions should track required information)
--
-- Adds CANCELLATIONREASON_ID (the coded reason, a row of CATEGORY) and CANCELLATIONDECIDEDBY_ID
-- (a STAFF row) to the admission table. PatientEncounter maps them, so without them every admission
-- load fails. Run this by hand BEFORE deploying a build that maps these fields.
-- The reason list itself reuses the existing CATEGORY table and needs no change.
-- Old cancelled admissions keep both columns empty.
--
-- Also adds an index and a foreign key on each column (CATEGORY and STAFF), like v2.31.0.
-- Safe to re-run: each column, index and foreign key is added only when missing.
--
-- UNIVERSAL: detects actual table-name case via INFORMATION_SCHEMA so this works on both
-- case-sensitive (Linux, lower_case_table_names=0) and case-insensitive (Windows/Azure) MySQL.

SELECT 'Migration v2.33.0 - cancellation columns on PATIENTENCOUNTER' AS status;

SET @enc_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'PATIENTENCOUNTER' LIMIT 1
);

SELECT CONCAT('PATIENTENCOUNTER: ', COALESCE(@enc_table, 'NOT FOUND')) AS info;

-- STEP 1: CANCELLATIONREASON_ID
SET @exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'PATIENTENCOUNTER' AND UPPER(COLUMN_NAME) = 'CANCELLATIONREASON_ID'
);
SET @sql = IF(@enc_table IS NULL,
    'SELECT "SKIPPED: PATIENTENCOUNTER table not found" AS status',
    IF(@exists > 0,
        'SELECT "SKIPPED: CANCELLATIONREASON_ID already exists" AS status',
        CONCAT('ALTER TABLE ', @enc_table, ' ADD COLUMN CANCELLATIONREASON_ID BIGINT DEFAULT NULL')
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- STEP 2: CANCELLATIONDECIDEDBY_ID
SET @exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'PATIENTENCOUNTER' AND UPPER(COLUMN_NAME) = 'CANCELLATIONDECIDEDBY_ID'
);
SET @sql = IF(@enc_table IS NULL,
    'SELECT "SKIPPED: PATIENTENCOUNTER table not found" AS status',
    IF(@exists > 0,
        'SELECT "SKIPPED: CANCELLATIONDECIDEDBY_ID already exists" AS status',
        CONCAT('ALTER TABLE ', @enc_table, ' ADD COLUMN CANCELLATIONDECIDEDBY_ID BIGINT DEFAULT NULL')
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- STEP 3: index and foreign key on CANCELLATIONREASON_ID (index is skipped when any index already starts on the column)
SET @ref_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'CATEGORY' LIMIT 1
);
SET @idx_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'PATIENTENCOUNTER'
      AND UPPER(COLUMN_NAME) = 'CANCELLATIONREASON_ID' AND SEQ_IN_INDEX = 1
);
SET @sql = IF(@enc_table IS NULL,
    'SELECT "SKIPPED: PATIENTENCOUNTER table not found" AS status',
    IF(@idx_exists > 0,
        'SELECT "SKIPPED: index on CANCELLATIONREASON_ID already exists" AS status',
        CONCAT('CREATE INDEX IDX_PATIENTENCOUNTER_CANCELLATIONREASON_ID ON ', @enc_table, ' (CANCELLATIONREASON_ID)')
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'PATIENTENCOUNTER'
      AND UPPER(CONSTRAINT_NAME) = 'FK_PATIENTENCOUNTER_CANCELLATIONREASON_ID'
);
SET @sql = IF(@enc_table IS NULL OR @ref_table IS NULL,
    'SELECT "SKIPPED: PATIENTENCOUNTER or CATEGORY table not found" AS status',
    IF(@fk_exists > 0,
        'SELECT "SKIPPED: FK_PATIENTENCOUNTER_CANCELLATIONREASON_ID already exists" AS status',
        CONCAT('ALTER TABLE ', @enc_table, ' ADD CONSTRAINT FK_PATIENTENCOUNTER_CANCELLATIONREASON_ID FOREIGN KEY (CANCELLATIONREASON_ID) REFERENCES ', @ref_table, ' (ID)')
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- STEP 4: index and foreign key on CANCELLATIONDECIDEDBY_ID (index is skipped when any index already starts on the column)
SET @ref_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'STAFF' LIMIT 1
);
SET @idx_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'PATIENTENCOUNTER'
      AND UPPER(COLUMN_NAME) = 'CANCELLATIONDECIDEDBY_ID' AND SEQ_IN_INDEX = 1
);
SET @sql = IF(@enc_table IS NULL,
    'SELECT "SKIPPED: PATIENTENCOUNTER table not found" AS status',
    IF(@idx_exists > 0,
        'SELECT "SKIPPED: index on CANCELLATIONDECIDEDBY_ID already exists" AS status',
        CONCAT('CREATE INDEX IDX_PATIENTENCOUNTER_CANCELLATIONDECIDEDBY_ID ON ', @enc_table, ' (CANCELLATIONDECIDEDBY_ID)')
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @fk_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'PATIENTENCOUNTER'
      AND UPPER(CONSTRAINT_NAME) = 'FK_PATIENTENCOUNTER_CANCELLATIONDECIDEDBY_ID'
);
SET @sql = IF(@enc_table IS NULL OR @ref_table IS NULL,
    'SELECT "SKIPPED: PATIENTENCOUNTER or STAFF table not found" AS status',
    IF(@fk_exists > 0,
        'SELECT "SKIPPED: FK_PATIENTENCOUNTER_CANCELLATIONDECIDEDBY_ID already exists" AS status',
        CONCAT('ALTER TABLE ', @enc_table, ' ADD CONSTRAINT FK_PATIENTENCOUNTER_CANCELLATIONDECIDEDBY_ID FOREIGN KEY (CANCELLATIONDECIDEDBY_ID) REFERENCES ', @ref_table, ' (ID)')
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'Migration v2.33.0 complete' AS status;
