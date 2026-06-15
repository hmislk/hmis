-- Migration v2.13.0
-- Creates MEDICATIONADMINISTRATIONRECORD table for ward medicine administration
-- tracking (issue #21469, part of #21466).
--
-- Each row records one dose event (given/refused/held) at a point in time.
-- Stock is NOT deducted here; stockDeducted=false until the Stage-2 batch
-- settlement screen processes the record.

SET @mar_table = (
    SELECT TABLE_NAME
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND UPPER(TABLE_NAME) = 'MEDICATIONADMINISTRATIONRECORD'
    LIMIT 1
);

SET @sql_create = IF(
    @mar_table IS NULL,
    '
    CREATE TABLE MEDICATIONADMINISTRATIONRECORD (
        id                    BIGINT       NOT NULL AUTO_INCREMENT,
        qty                   DOUBLE,
        doseDescription       VARCHAR(500),
        administeredAt        DATETIME,
        comments              VARCHAR(500),
        stockDeducted         BIT          NOT NULL DEFAULT 0,
        retired               BIT          NOT NULL DEFAULT 0,
        retiredAt             DATETIME,
        retireComments        VARCHAR(500),
        createdAt             DATETIME,
        status                VARCHAR(50),
        patientEncounter_id   BIGINT,
        prescription_id       BIGINT,
        item_id               BIGINT,
        itemBatch_id          BIGINT,
        administeredBy_id     BIGINT,
        department_id         BIGINT,
        stockDeductionBill_id BIGINT,
        creater_id            BIGINT,
        retirer_id            BIGINT,
        PRIMARY KEY (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
    ',
    'SELECT ''MEDICATIONADMINISTRATIONRECORD already exists — skipping'' AS info'
);

PREPARE stmt FROM @sql_create;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'Migration v2.13.0 complete' AS status;
