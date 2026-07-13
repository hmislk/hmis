-- Migration v2.3.0: Backfill per-unit rate fields in BillFee
-- Author: Dr M H B Ariyaratne
-- Issue: #20413
--
-- Context:
--   BillFee now has four persisted per-unit fields:
--     feeUnitGrossValue, feeUnitValue, feeUnitMargin, feeUnitDiscount
--   The corresponding total fields (feeGrossValue, feeValue, feeMargin, feeDiscount)
--   already hold  unit × qty  values.  This migration derives unit = total / qty
--   for all existing rows so that historical bills display correct per-unit rates.
--
-- Pre-requisite:
--   Run missing_database_fields.xhtml BEFORE this migration to ensure the four
--   new columns have been added to the BILLFEE table by EclipseLink.
--
-- Idempotent: the WHERE clause limits updates to rows where feeUnitGrossValue IS NULL,
--   so re-running this script is safe.

SELECT 'Migration v2.3.0 - BillFee per-unit rate backfill starting' AS status;

-- ============================================================
-- STEP 1: Detect actual table names (cross-deployment safety)
-- ============================================================

SET @billfee_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'BILLFEE'
    LIMIT 1
);

SET @billitem_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'BILLITEM'
    LIMIT 1
);

SELECT CONCAT('BILLFEE table: ', COALESCE(@billfee_table, 'NOT FOUND')) AS status;
SELECT CONCAT('BILLITEM table: ', COALESCE(@billitem_table, 'NOT FOUND')) AS status;

-- ============================================================
-- STEP 2: Detect actual column names in BILLFEE
-- ============================================================

-- Source total columns
SET @col_feegross = (
    SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @billfee_table
    AND UPPER(COLUMN_NAME) = 'FEEGROSSVALUE' LIMIT 1
);
SET @col_feevalue = (
    SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @billfee_table
    AND UPPER(COLUMN_NAME) = 'FEEVALUE' LIMIT 1
);
SET @col_feemargin = (
    SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @billfee_table
    AND UPPER(COLUMN_NAME) = 'FEEMARGIN' LIMIT 1
);
SET @col_feediscount = (
    SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @billfee_table
    AND UPPER(COLUMN_NAME) = 'FEEDISCOUNT' LIMIT 1
);

-- Target unit columns
SET @col_unitgross = (
    SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @billfee_table
    AND UPPER(COLUMN_NAME) = 'FEEUNITGROSSVALUE' LIMIT 1
);
SET @col_unitvalue = (
    SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @billfee_table
    AND UPPER(COLUMN_NAME) = 'FEEUNITVALUE' LIMIT 1
);
SET @col_unitmargin = (
    SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @billfee_table
    AND UPPER(COLUMN_NAME) = 'FEEUNITMARGIN' LIMIT 1
);
SET @col_unitdiscount = (
    SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @billfee_table
    AND UPPER(COLUMN_NAME) = 'FEEUNITDISCOUNT' LIMIT 1
);

-- FK column linking BILLFEE → BILLITEM
SET @col_billitem_fk = (
    SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @billfee_table
    AND UPPER(COLUMN_NAME) = 'BILLITEM_ID' LIMIT 1
);

-- QTY column in BILLITEM
SET @col_qty = (
    SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @billitem_table
    AND UPPER(COLUMN_NAME) = 'QTY' LIMIT 1
);

SELECT CONCAT('Columns detected — feegross:', COALESCE(@col_feegross,'?'),
    ' feevalue:', COALESCE(@col_feevalue,'?'),
    ' feemargin:', COALESCE(@col_feemargin,'?'),
    ' feediscount:', COALESCE(@col_feediscount,'?')) AS status;
SELECT CONCAT('Unit columns — unitgross:', COALESCE(@col_unitgross,'MISSING'),
    ' unitvalue:', COALESCE(@col_unitvalue,'MISSING'),
    ' unitmargin:', COALESCE(@col_unitmargin,'MISSING'),
    ' unitdiscount:', COALESCE(@col_unitdiscount,'MISSING')) AS status;

-- ============================================================
-- STEP 3: Guard — abort if any required column is missing
-- ============================================================
-- If missing_database_fields.xhtml has not been run yet the unit columns will be
-- NULL here and we should not proceed (the UPDATE would reference a non-existent column).

SET @abort = (
    @col_unitgross IS NULL OR @col_unitvalue IS NULL
    OR @col_unitmargin IS NULL OR @col_unitdiscount IS NULL
);

-- ============================================================
-- STEP 4: Backfill unit fields  (idempotent: only NULL rows)
-- ============================================================
-- Uses LEFT JOIN so that BillFee rows with no BillItem (BILLITEM_ID IS NULL)
-- are treated as qty = 1  (unit = total).

SET @sql = IF(@abort,
    'SELECT "SKIPPED: one or more feeUnit* columns are missing — run missing_database_fields.xhtml first" AS status',
    CONCAT(
        'UPDATE ', @billfee_table, ' bf ',
        'LEFT JOIN ', @billitem_table, ' bi ON bf.', @col_billitem_fk, ' = bi.ID ',
        'SET ',
          'bf.', @col_unitgross,    ' = CASE WHEN bi.', @col_qty, ' > 0 THEN bf.', @col_feegross,    ' / bi.', @col_qty, ' ELSE bf.', @col_feegross,    ' END, ',
          'bf.', @col_unitvalue,    ' = CASE WHEN bi.', @col_qty, ' > 0 THEN bf.', @col_feevalue,    ' / bi.', @col_qty, ' ELSE bf.', @col_feevalue,    ' END, ',
          'bf.', @col_unitmargin,   ' = CASE WHEN bi.', @col_qty, ' > 0 THEN bf.', @col_feemargin,   ' / bi.', @col_qty, ' ELSE bf.', @col_feemargin,   ' END, ',
          'bf.', @col_unitdiscount, ' = CASE WHEN bi.', @col_qty, ' > 0 THEN bf.', @col_feediscount, ' / bi.', @col_qty, ' ELSE bf.', @col_feediscount, ' END ',
        'WHERE bf.', @col_unitgross, ' IS NULL'
    )
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================
-- STEP 5: Verification (skipped when aborted — @col_unitgross is NULL in that path)
-- ============================================================
SET @verify_sql = IF(@abort,
    'SELECT "Verification skipped — migration was aborted (missing columns)" AS status',
    CONCAT(
        'SELECT ',
        'COUNT(*) AS total_rows, ',
        'SUM(CASE WHEN bf.', @col_unitgross, ' IS NULL THEN 1 ELSE 0 END) AS still_null ',
        'FROM ', @billfee_table, ' bf'
    )
);
PREPARE stmt FROM @verify_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'Migration v2.3.0 complete' AS status;
