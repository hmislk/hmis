-- Rollback v2.13.0
-- Drops MEDICATIONADMINISTRATIONRECORD table.

SET @mar_table = (
    SELECT TABLE_NAME
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND UPPER(TABLE_NAME) = 'MEDICATIONADMINISTRATIONRECORD'
    LIMIT 1
);

SET @sql_drop = IF(
    @mar_table IS NOT NULL,
    CONCAT('DROP TABLE ', @mar_table),
    'SELECT ''MEDICATIONADMINISTRATIONRECORD not found — nothing to drop'' AS info'
);

PREPARE stmt FROM @sql_drop;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'Rollback v2.13.0 complete' AS status;
