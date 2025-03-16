-- Step 1: Set database name and start date
SET @database_name = 'mp';
SET @start_date = '2024-05-01 00:00:00';

-- Step 2: Retrieve the first bill ID after the given start date
SET @query = CONCAT('SELECT id INTO @bill_id FROM ', @database_name, '.bill WHERE createdAt >= "', @start_date, '" ORDER BY createdAt ASC LIMIT 1;');
PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 3: Check retrieved bill_id
SELECT @bill_id;

-- Step 4: Disable foreign key constraints
SET FOREIGN_KEY_CHECKS = 0;

-- Step 5: Get table sizes before deletion
SELECT TABLE_NAME, ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024, 2) AS size_mb
FROM information_schema.tables
WHERE TABLE_SCHEMA = @database_name
AND TABLE_NAME IN ('adjustmentbillitem', 'agenthistory', 'agentreferencebook', 'agentsfees', 'appemail', 'appointmentactivity', 
'bill', 'billcomponent', 'billentry', 'billfee', 'billfeepayment', 'billitem', 'billnumber', 'billsession', 'capturecomponent', 
'cashbookentry', 'cashtransaction', 'cashtransactionhistory', 'categoryitem', 'clinicalfindingvalue', 'componentasignment', 
'detailedfinancialbill', 'drawerentry', 'encountercomponent', 'encountercreditcompany', 'fee', 'feechange', 'feevalue', 
'fingerprintrecord', 'fingerprintrecordhistory', 'itemusage', 'loan', 'logins', 'message', 'notification', 'patient', 'patientdeposit', 
'patientdeposithistory', 'patientencounter', 'patientflag', 'patientinvestigation', 'patientitem', 'patientreport', 
'patientreportitemvalue', 'patientroom', 'patientsample', 'patientsamplecomponant', 'patientsessioninstanceactivity', 
'payeetaxrange', 'payment', 'paymentgatewaytransaction', 'paymentmethodvalue', 'phdate', 'prescription', 'prescriptiontemplate', 
'prescriptiontemplate_prescriptiontemplate', 'roster', 'servicesessioninstance', 'sessioninstance', 'sessioninstanceactivity', 
'sms', 'staffleaveentitle', 'staffpaysheetcomponent', 'stockhistory', 'stockvarientbillitem', 'token', 'triggersubscription', 'upload');

-- Step 6: Define all tables to be cleaned
SET @tables = 'adjustmentbillitem, agenthistory, agentreferencebook, agentsfees, appemail, appointmentactivity, 
bill, billcomponent, billentry, billfee, billfeepayment, billitem, billnumber, billsession, capturecomponent, 
cashbookentry, cashtransaction, cashtransactionhistory, categoryitem, clinicalfindingvalue, componentasignment, 
detailedfinancialbill, drawerentry, encountercomponent, encountercreditcompany, fee, feechange, feevalue, 
fingerprintrecord, fingerprintrecordhistory, itemusage, loan, logins, message, notification, patient, patientdeposit, 
patientdeposithistory, patientencounter, patientflag, patientinvestigation, patientitem, patientreport, 
patientreportitemvalue, patientroom, patientsample, patientsamplecomponant, patientsessioninstanceactivity, 
payeetaxrange, payment, paymentgatewaytransaction, paymentmethodvalue, phdate, prescription, prescriptiontemplate, 
prescriptiontemplate_prescriptiontemplate, roster, servicesessioninstance, sessioninstance, sessioninstanceactivity, 
sms, staffleaveentitle, staffpaysheetcomponent, stockhistory, stockvarientbillitem, token, triggersubscription, upload';

-- Step 7: Generate delete queries dynamically
SELECT GROUP_CONCAT(CONCAT('DELETE FROM ', @database_name, '.', table_name, ' WHERE id < ', @bill_id, ';') SEPARATOR ' ')
INTO @query
FROM (SELECT SUBSTRING_INDEX(SUBSTRING_INDEX(@tables, ', ', n), ', ', -1) AS table_name
      FROM (SELECT 1 n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 
            UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10 UNION ALL SELECT 11 UNION ALL SELECT 12 
            UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15 UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 
            UNION ALL SELECT 19 UNION ALL SELECT 20 UNION ALL SELECT 21 UNION ALL SELECT 22 UNION ALL SELECT 23 UNION ALL SELECT 24 
            UNION ALL SELECT 25 UNION ALL SELECT 26 UNION ALL SELECT 27 UNION ALL SELECT 28 UNION ALL SELECT 29 UNION ALL SELECT 30 
            UNION ALL SELECT 31 UNION ALL SELECT 32 UNION ALL SELECT 33 UNION ALL SELECT 34 UNION ALL SELECT 35 UNION ALL SELECT 36 
            UNION ALL SELECT 37 UNION ALL SELECT 38 UNION ALL SELECT 39 UNION ALL SELECT 40 UNION ALL SELECT 41 UNION ALL SELECT 42 
            UNION ALL SELECT 43 UNION ALL SELECT 44 UNION ALL SELECT 45 UNION ALL SELECT 46 UNION ALL SELECT 47 UNION ALL SELECT 48 
            UNION ALL SELECT 49 UNION ALL SELECT 50 UNION ALL SELECT 51 UNION ALL SELECT 52 UNION ALL SELECT 53 UNION ALL SELECT 54 
            UNION ALL SELECT 55 UNION ALL SELECT 56 UNION ALL SELECT 57 UNION ALL SELECT 58 UNION ALL SELECT 59 UNION ALL SELECT 60 
            UNION ALL SELECT 61 UNION ALL SELECT 62 UNION ALL SELECT 63 UNION ALL SELECT 64 UNION ALL SELECT 65 UNION ALL SELECT 66 
            UNION ALL SELECT 67 UNION ALL SELECT 68) AS numbers 
      WHERE n <= LENGTH(@tables) - LENGTH(REPLACE(@tables, ', ', '')) + 1) AS derived_table;

-- Step 8: Execute the delete statements
PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 9: Get table sizes after deletion
SELECT TABLE_NAME, ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024, 2) AS size_mb
FROM information_schema.tables
WHERE TABLE_SCHEMA = @database_name
AND TABLE_NAME IN ('adjustmentbillitem', 'agenthistory', 'agentreferencebook', 'agentsfees', 'appemail', 'appointmentactivity', 
'bill', 'billcomponent', 'billentry', 'billfee', 'billfeepayment', 'billitem', 'billnumber', 'billsession', 'capturecomponent', 
'cashbookentry', 'cashtransaction', 'cashtransactionhistory', 'categoryitem', 'clinicalfindingvalue', 'componentasignment', 
'detailedfinancialbill', 'drawerentry', 'encountercomponent', 'encountercreditcompany', 'fee', 'feechange', 'feevalue', 
'fingerprintrecord', 'fingerprintrecordhistory', 'itemusage', 'loan', 'logins', 'message', 'notification', 'patient', 'patientdeposit', 
'patientdeposithistory', 'patientencounter', 'patientflag', 'patientinvestigation', 'patientitem', 'patientreport', 
'patientreportitemvalue', 'patientroom', 'patientsample', 'patientsamplecomponant', 'patientsessioninstanceactivity', 
'payeetaxrange', 'payment', 'paymentgatewaytransaction', 'paymentmethodvalue', 'phdate', 'prescription', 'prescriptiontemplate', 
'prescriptiontemplate_prescriptiontemplate', 'roster', 'servicesessioninstance', 'sessioninstance', 'sessioninstanceactivity', 
'sms', 'staffleaveentitle', 'staffpaysheetcomponent', 'stockhistory', 'stockvarientbillitem', 'token', 'triggersubscription', 'upload');

-- Step 10: Re-enable foreign key constraints
SET FOREIGN_KEY_CHECKS = 1;
