-- ============================================================
-- Rollback v2.2.0: Remove AUTO_INCREMENT from all entity tables
-- WARNING: After rollback, redeploy previous app version (GenerationType.AUTO)
-- ============================================================

DELIMITER //
CREATE PROCEDURE hmis_remove_auto_increment()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE tbl VARCHAR(255);
    DECLARE cur CURSOR FOR
        SELECT TABLE_NAME
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND COLUMN_NAME = 'ID'
          AND DATA_TYPE = 'bigint'
          AND EXTRA LIKE '%auto_increment%';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO tbl;
        IF done THEN LEAVE read_loop; END IF;
        SET @alter_sql = CONCAT('ALTER TABLE `', tbl, '` MODIFY COLUMN ID BIGINT NOT NULL');
        PREPARE stmt FROM @alter_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END LOOP;
    CLOSE cur;
END//
DELIMITER ;

CALL hmis_remove_auto_increment();
DROP PROCEDURE IF EXISTS hmis_remove_auto_increment;
