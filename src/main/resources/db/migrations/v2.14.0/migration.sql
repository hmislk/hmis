-- Migration v2.14.0
-- Adds the SVG bed-board columns (issue #21580, PR #21591).
--
-- The graphical bed board stores two SVG fragments per entity:
--   * svgParentView  — the entity's own floor-plan canvas
--   * svgChildView   — how the entity looks as a tile inside its parent
-- plus a bed status enum on Room.
--
-- Column placement (EclipseLink default naming: UPPERCASE, no underscores):
--   * Room extends Category (single-table inheritance) -> columns on CATEGORY:
--       BED_STATUS    VARCHAR(255)   (explicit @Column(name="BED_STATUS"))
--       SVGCHILDVIEW  LONGTEXT       (@Lob)
--   * Department -> DEPARTMENT: SVGPARENTVIEW, SVGCHILDVIEW (LONGTEXT)
--   * Institution -> INSTITUTION: SVGPARENTVIEW, SVGCHILDVIEW (LONGTEXT)
--
-- Idempotent and case-insensitive: detects the actual table-name case via
-- INFORMATION_SCHEMA and only adds a column when it does not already exist,
-- so it is safe to run on any customer DB regardless of table-name casing.

-- ---------------------------------------------------------------------------
-- CATEGORY.BED_STATUS
-- ---------------------------------------------------------------------------
SET @tbl = (SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
            WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'CATEGORY' LIMIT 1);
SET @col = (SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'CATEGORY'
              AND UPPER(COLUMN_NAME) = 'BED_STATUS' LIMIT 1);
SET @sql = IF(@tbl IS NOT NULL AND @col IS NULL,
    CONCAT('ALTER TABLE ', @tbl, ' ADD COLUMN BED_STATUS VARCHAR(255)'),
    'SELECT ''CATEGORY.BED_STATUS already present or table missing — skipping'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- CATEGORY.SVGCHILDVIEW
-- ---------------------------------------------------------------------------
SET @tbl = (SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
            WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'CATEGORY' LIMIT 1);
SET @col = (SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'CATEGORY'
              AND UPPER(COLUMN_NAME) = 'SVGCHILDVIEW' LIMIT 1);
SET @sql = IF(@tbl IS NOT NULL AND @col IS NULL,
    CONCAT('ALTER TABLE ', @tbl, ' ADD COLUMN SVGCHILDVIEW LONGTEXT'),
    'SELECT ''CATEGORY.SVGCHILDVIEW already present or table missing — skipping'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- DEPARTMENT.SVGPARENTVIEW
-- ---------------------------------------------------------------------------
SET @tbl = (SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
            WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'DEPARTMENT' LIMIT 1);
SET @col = (SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'DEPARTMENT'
              AND UPPER(COLUMN_NAME) = 'SVGPARENTVIEW' LIMIT 1);
SET @sql = IF(@tbl IS NOT NULL AND @col IS NULL,
    CONCAT('ALTER TABLE ', @tbl, ' ADD COLUMN SVGPARENTVIEW LONGTEXT'),
    'SELECT ''DEPARTMENT.SVGPARENTVIEW already present or table missing — skipping'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- DEPARTMENT.SVGCHILDVIEW
-- ---------------------------------------------------------------------------
SET @tbl = (SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
            WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'DEPARTMENT' LIMIT 1);
SET @col = (SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'DEPARTMENT'
              AND UPPER(COLUMN_NAME) = 'SVGCHILDVIEW' LIMIT 1);
SET @sql = IF(@tbl IS NOT NULL AND @col IS NULL,
    CONCAT('ALTER TABLE ', @tbl, ' ADD COLUMN SVGCHILDVIEW LONGTEXT'),
    'SELECT ''DEPARTMENT.SVGCHILDVIEW already present or table missing — skipping'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- INSTITUTION.SVGPARENTVIEW
-- ---------------------------------------------------------------------------
SET @tbl = (SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
            WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'INSTITUTION' LIMIT 1);
SET @col = (SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'INSTITUTION'
              AND UPPER(COLUMN_NAME) = 'SVGPARENTVIEW' LIMIT 1);
SET @sql = IF(@tbl IS NOT NULL AND @col IS NULL,
    CONCAT('ALTER TABLE ', @tbl, ' ADD COLUMN SVGPARENTVIEW LONGTEXT'),
    'SELECT ''INSTITUTION.SVGPARENTVIEW already present or table missing — skipping'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- INSTITUTION.SVGCHILDVIEW
-- ---------------------------------------------------------------------------
SET @tbl = (SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
            WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'INSTITUTION' LIMIT 1);
SET @col = (SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'INSTITUTION'
              AND UPPER(COLUMN_NAME) = 'SVGCHILDVIEW' LIMIT 1);
SET @sql = IF(@tbl IS NOT NULL AND @col IS NULL,
    CONCAT('ALTER TABLE ', @tbl, ' ADD COLUMN SVGCHILDVIEW LONGTEXT'),
    'SELECT ''INSTITUTION.SVGCHILDVIEW already present or table missing — skipping'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT 'Migration v2.14.0 complete' AS status;
