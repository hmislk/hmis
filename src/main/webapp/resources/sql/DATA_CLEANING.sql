-- Step 1: Set database name and start date
SET @database_name = 'mpdatacleaning';
SET @start_date = '2024-05-01 00:00:00';

-- Step 2: Retrieve the first bill ID after the given start date
SET @bill_id = (SELECT id FROM mpdatacleaning.bill WHERE createdAt >= @start_date ORDER BY createdAt ASC LIMIT 1);

-- Step 3: Check retrieved bill_id
SELECT @bill_id;

-- Step 4: Disable foreign key constraints
SET FOREIGN_KEY_CHECKS = 0;

-- Step 5: Get table sizes before deletion
SELECT TABLE_NAME, ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024, 2) AS size_mb
FROM information_schema.tables
WHERE TABLE_SCHEMA = @database_name;

-- Step 6: Execute DELETE statements for each table manually
DELETE FROM mpdatacleaning.adjustmentbillitem WHERE id < @bill_id;
DELETE FROM mpdatacleaning.agenthistory WHERE id < @bill_id;
DELETE FROM mpdatacleaning.agentreferencebook WHERE id < @bill_id;
DELETE FROM mpdatacleaning.agentsfees WHERE id < @bill_id;
DELETE FROM mpdatacleaning.appemail WHERE id < @bill_id;
DELETE FROM mpdatacleaning.appointmentactivity WHERE id < @bill_id;
DELETE FROM mpdatacleaning.bill WHERE id < @bill_id;
DELETE FROM mpdatacleaning.billcomponent WHERE id < @bill_id;
DELETE FROM mpdatacleaning.billentry WHERE id < @bill_id;
DELETE FROM mpdatacleaning.billfee WHERE id < @bill_id;
DELETE FROM mpdatacleaning.billfeepayment WHERE id < @bill_id;
DELETE FROM mpdatacleaning.billitem WHERE id < @bill_id;
DELETE FROM mpdatacleaning.billnumber WHERE id < @bill_id;
DELETE FROM mpdatacleaning.billsession WHERE id < @bill_id;
DELETE FROM mpdatacleaning.capturecomponent WHERE id < @bill_id;
DELETE FROM mpdatacleaning.cashbookentry WHERE id < @bill_id;
DELETE FROM mpdatacleaning.cashtransaction WHERE id < @bill_id;
DELETE FROM mpdatacleaning.cashtransactionhistory WHERE id < @bill_id;
DELETE FROM mpdatacleaning.categoryitem WHERE id < @bill_id;
DELETE FROM mpdatacleaning.clinicalfindingvalue WHERE id < @bill_id;
DELETE FROM mpdatacleaning.componentasignment WHERE id < @bill_id;
DELETE FROM mpdatacleaning.detailedfinancialbill WHERE id < @bill_id;
DELETE FROM mpdatacleaning.drawerentry WHERE id < @bill_id;
DELETE FROM mpdatacleaning.encountercomponent WHERE id < @bill_id;
DELETE FROM mpdatacleaning.encountercreditcompany WHERE id < @bill_id;
DELETE FROM mpdatacleaning.fee WHERE id < @bill_id;
DELETE FROM mpdatacleaning.feechange WHERE id < @bill_id;
DELETE FROM mpdatacleaning.feevalue WHERE id < @bill_id;
DELETE FROM mpdatacleaning.fingerprintrecord WHERE id < @bill_id;
DELETE FROM mpdatacleaning.fingerprintrecordhistory WHERE id < @bill_id;
DELETE FROM mpdatacleaning.itemusage WHERE id < @bill_id;
DELETE FROM mpdatacleaning.loan WHERE id < @bill_id;
DELETE FROM mpdatacleaning.logins WHERE id < @bill_id;
DELETE FROM mpdatacleaning.message WHERE id < @bill_id;
DELETE FROM mpdatacleaning.notification WHERE id < @bill_id;
DELETE FROM mpdatacleaning.patient WHERE id < @bill_id;
DELETE FROM mpdatacleaning.patientdeposit WHERE id < @bill_id;
DELETE FROM mpdatacleaning.patientdeposithistory WHERE id < @bill_id;
DELETE FROM mpdatacleaning.patientencounter WHERE id < @bill_id;
DELETE FROM mpdatacleaning.patientflag WHERE id < @bill_id;
DELETE FROM mpdatacleaning.patientinvestigation WHERE id < @bill_id;
DELETE FROM mpdatacleaning.patientitem WHERE id < @bill_id;
DELETE FROM mpdatacleaning.patientreport WHERE id < @bill_id;
DELETE FROM mpdatacleaning.patientreportitemvalue WHERE id < @bill_id;
DELETE FROM mpdatacleaning.patientroom WHERE id < @bill_id;
DELETE FROM mpdatacleaning.patientsample WHERE id < @bill_id;
DELETE FROM mpdatacleaning.patientsamplecomponant WHERE id < @bill_id;
DELETE FROM mpdatacleaning.patientsessioninstanceactivity WHERE id < @bill_id;
DELETE FROM mpdatacleaning.payeetaxrange WHERE id < @bill_id;
DELETE FROM mpdatacleaning.payment WHERE id < @bill_id;
DELETE FROM mpdatacleaning.paymentgatewaytransaction WHERE id < @bill_id;
DELETE FROM mpdatacleaning.paymentmethodvalue WHERE id < @bill_id;
DELETE FROM mpdatacleaning.phdate WHERE id < @bill_id;
DELETE FROM mpdatacleaning.prescription WHERE id < @bill_id;
DELETE FROM mpdatacleaning.prescriptiontemplate WHERE id < @bill_id;
DELETE FROM mpdatacleaning.prescriptiontemplate_prescriptiontemplate WHERE id < @bill_id;
DELETE FROM mpdatacleaning.roster WHERE id < @bill_id;
DELETE FROM mpdatacleaning.servicesessioninstance WHERE id < @bill_id;
DELETE FROM mpdatacleaning.sessioninstance WHERE id < @bill_id;
DELETE FROM mpdatacleaning.sessioninstanceactivity WHERE id < @bill_id;
DELETE FROM mpdatacleaning.sms WHERE id < @bill_id;
DELETE FROM mpdatacleaning.staffleaveentitle WHERE id < @bill_id;
DELETE FROM mpdatacleaning.staffpaysheetcomponent WHERE id < @bill_id;
DELETE FROM mpdatacleaning.stockhistory WHERE id < @bill_id;
DELETE FROM mpdatacleaning.stockvarientbillitem WHERE id < @bill_id;
DELETE FROM mpdatacleaning.token WHERE id < @bill_id;
DELETE FROM mpdatacleaning.triggersubscription WHERE id < @bill_id;
DELETE FROM mpdatacleaning.upload WHERE id < @bill_id;

-- Step 7: Get table sizes after deletion
SELECT TABLE_NAME, ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024, 2) AS size_mb
FROM information_schema.tables
WHERE TABLE_SCHEMA = @database_name;

-- Step 8: Re-enable foreign key constraints
SET FOREIGN_KEY_CHECKS = 1;
