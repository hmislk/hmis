-- ============================================================
-- Migration v2.2.0: Switch entity ID generation to AUTO_INCREMENT
-- Issue: #19941
-- Root cause: Single shared SEQUENCE row causes lock contention
--             under concurrent load (MySQL Error 1205)
-- Impact: All entity tables — adds AUTO_INCREMENT to ID column
-- Requires: Application server STOPPED, full backup taken
-- ============================================================

-- Step 1: Verify current state
SELECT SEQ_NAME, SEQ_COUNT FROM SEQUENCE WHERE SEQ_NAME = 'SEQ_GEN';

-- Step 2: Create stored procedure to add AUTO_INCREMENT dynamically
-- Uses information_schema so it works with both uppercase and lowercase table names
DELIMITER //
CREATE PROCEDURE hmis_add_auto_increment()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE tbl VARCHAR(255);
    DECLARE cur CURSOR FOR
        SELECT TABLE_NAME
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND COLUMN_NAME = 'ID'
          AND DATA_TYPE = 'bigint'
          AND EXTRA NOT LIKE '%auto_increment%';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO tbl;
        IF done THEN LEAVE read_loop; END IF;
        SET @alter_sql = CONCAT('ALTER TABLE `', tbl, '` MODIFY COLUMN ID BIGINT NOT NULL AUTO_INCREMENT');
        PREPARE stmt FROM @alter_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END LOOP;
    CLOSE cur;
END//
DELIMITER ;

-- Step 3: Execute the procedure
CALL hmis_add_auto_increment();

-- Step 4: Drop the procedure (cleanup)
DROP PROCEDURE IF EXISTS hmis_add_auto_increment;

-- Step 5: Verify results — all tables should now have AUTO_INCREMENT set
SELECT TABLE_NAME, AUTO_INCREMENT
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND AUTO_INCREMENT IS NOT NULL
ORDER BY TABLE_NAME;
