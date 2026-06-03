-- Migration v2.10.0: Backfill registrationSource from selfRegistered and drop selfRegistered column
-- Issue: hmislk/hmis#21199
--
-- Background:
--   Patient.selfRegistered (Boolean) was superseded by Patient.registrationSource
--   (PatientRegistrationSource enum) introduced in #21181. Patients that were
--   self-registered via the patient portal had selfRegistered=true; those rows now
--   need registrationSource='ONLINE_SELF'. Once migrated the column is dropped.
--
-- Idempotent: UPDATE only touches rows where registrationSource is not yet set.
--             DROP COLUMN uses IF EXISTS so re-running after the column is gone is safe.

SELECT 'Migration v2.10.0 - Backfill registrationSource from selfRegistered' AS status;

-- Step 1: backfill ONLINE_SELF for all formerly self-registered patients
UPDATE patient
SET registrationSource = 'ONLINE_SELF'
WHERE selfRegistered = 1
  AND (registrationSource IS NULL OR registrationSource = '');

SELECT CONCAT('Rows backfilled to ONLINE_SELF: ', ROW_COUNT()) AS backfill_status;

-- Step 2: drop the now-redundant selfRegistered column
ALTER TABLE patient DROP COLUMN IF EXISTS selfRegistered;

SELECT 'Migration v2.10.0 completed' AS final_status;
