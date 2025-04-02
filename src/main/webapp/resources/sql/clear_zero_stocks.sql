-- Disable foreign key checks (if required, improves delete speed)
SET FOREIGN_KEY_CHECKS = 0;

-- Step 1: Delete stock records in batches
DELIMITER $$

CREATE PROCEDURE DeleteOldStock()
BEGIN
    DECLARE row_count INT DEFAULT 1;
    
    -- Delete in batches to prevent table locks
    WHILE row_count > 0 DO
        DELETE FROM stock 
        WHERE id < 47968146 
        AND stock < 1 
        LIMIT 100000;
        
        -- Get the number of rows affected
        SET row_count = ROW_COUNT();
    END WHILE;
END $$

DELIMITER ;

CALL DeleteOldStock();

DROP PROCEDURE DeleteOldStock;

-- Step 2: Delete orphaned itembatch records in batches
DELIMITER $$

CREATE PROCEDURE DeleteOrphanedItemBatch()
BEGIN
    DECLARE row_count INT DEFAULT 1;
    
    -- Delete in batches to prevent table locks
    WHILE row_count > 0 DO
        DELETE FROM itembatch 
        WHERE id < 47968146 
        AND NOT EXISTS (
            SELECT 1 FROM stock WHERE stock.itembatch_id = itembatch.id LIMIT 1
        )
        LIMIT 100000;
        
        -- Get the number of rows affected
        SET row_count = ROW_COUNT();
    END WHILE;
END $$

DELIMITER ;

CALL DeleteOrphanedItemBatch();

DROP PROCEDURE DeleteOrphanedItemBatch;

-- Re-enable foreign key checks (if needed)
SET FOREIGN_KEY_CHECKS = 1;
