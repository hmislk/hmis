SET FOREIGN_KEY_CHECKS=0;

DELETE FROM agenthistory;
DELETE FROM agentreferencebook;
DELETE FROM agentsfees;
DELETE FROM allowedpaymentmethod;
DELETE FROM appointment;
DELETE FROM area;
DELETE FROM bankaccount;
DELETE FROM batchbill;
DELETE FROM bill;
DELETE FROM billcomponent;
DELETE FROM billentry;
DELETE FROM billfee;
DELETE FROM billfeepayment;
DELETE FROM billitem ;
DELETE FROM billnumber ;
DELETE FROM billsession ;
DELETE FROM cashierdrawer;
DELETE FROM cashtransaction;
DELETE FROM cashtransactionhistory;
-- DELETE FROM category; ok
DELETE FROM drawer;
DELETE FROM encountercomponent;
DELETE FROM fee;
DELETE FROM feechange;
DELETE FROM fingerprintrecord;
DELETE FROM fingerprintrecordhistory;
DELETE FROM form;
-- DELETE FROM formitemvalue;
DELETE FROM inwardpriceadjustment;
DELETE FROM issueratemargins;
-- DELETE FROM item;
DELETE FROM itembatch;
-- DELETE FROM itemforitem;
DELETE FROM itemsdistributors;
DELETE FROM logins;
DELETE FROM medicalpackageitem;
DELETE FROM patient;
DELETE FROM patientencounter;
DELETE FROM patientflag;
DELETE FROM patientinvestigation;
DELETE FROM patientitem;
DELETE FROM patientreport;
DELETE FROM patientreportitemvalue;
DELETE FROM patientroom;
DELETE FROM payment;
DELETE FROM packageitem;
DELETE FROM paymentscheme;
DELETE FROM paysheetcomponent;
-- DELETE FROM person;
DELETE FROM personinstitution;
DELETE FROM pharmaceuticalbillitem;
DELETE FROM phdate;
DELETE FROM pricematrix;
DELETE FROM relationship;
DELETE FROM reorder;
-- DELETE FROM reportitem;
DELETE FROM roomfacilitycharge;
DELETE FROM roster;
DELETE FROM salarycycle;
DELETE FROM salaryhold;
DELETE FROM servicesession;
DELETE FROM servicesessionleave;
DELETE FROM shift;
DELETE FROM sms;
DELETE FROM staff;
DELETE FROM staffbasics;
DELETE FROM staffdesignation;
DELETE FROM staffemployeestatus;
DELETE FROM staffemployment;
DELETE FROM staffgrade;
DELETE FROM staffhistory;
DELETE FROM staffleave;
DELETE FROM staffleaveentitle;
DELETE FROM staffloan;
DELETE FROM staffpaysheetcomponent;
DELETE FROM staffsalary;
DELETE FROM staffsalarycomponant;
DELETE FROM staffshift;
DELETE FROM staffshifthistory;
DELETE FROM staffstaffcategory;
DELETE FROM staffworkday;
DELETE FROM staffworkingdepartment;
DELETE FROM stock;
DELETE FROM stockhistory;
DELETE FROM stockvarientbillitem;
DELETE FROM userpreference;
DELETE FROM userstock;
DELETE FROM userstockcontainer;
DELETE FROM webuserpaymentscheme;

-- special Delete commands
DELETE FROM webuser WHERE `ID`!=22279302 and `ID`!=3151;
DELETE FROM webuserprivilege WHERE `RETIRED`=true;
DELETE FROM webuserprivilege WHERE `WEBUSER_ID`!=22279302 and `WEBUSER_ID`!=3151;
DELETE FROM webuserdepartment WHERE `RETIRED`=true;
DELETE FROM webuserdepartment WHERE `WEBUSER_ID`!=22279302 and `WEBUSER_ID`!=3151;
DELETE FROM department WHERE `RETIRED`=true;
DELETE FROM department WHERE `ID`!=14638;
DELETE FROM institution WHERE `ID`!=5764;
DELETE FROM category WHERE `RETIRED`=true;
DELETE FROM category WHERE 
`DTYPE`='TimedItemCategory' or `DTYPE`='StoreItemCategory' or `DTYPE`='StaffCategory' or 
`DTYPE`='SessionNumberGenerator' or `DTYPE`='ServiceSubCategory' or `DTYPE`='ServiceCategory' or 
`DTYPE`='RoomCategory' or `DTYPE`='Room' or `DTYPE`='MembershipScheme' or 
`DTYPE`='FormFormat' or `DTYPE`='ConsumableCategory' or `DTYPE`='AssetCategory' or 
`DTYPE`='Make' or `DTYPE`='Machine' or `NAME`='';
DELETE FROM category WHERE `DTYPE`='PharmaceuticalItemCategory' and 
(`NAME`!='Capsule' and `NAME`!='Spray' and `NAME`!='Application' and `NAME`!='Bulk' and `NAME`!='Cream' and 
`NAME`!='Drops' and `NAME`!='Injection' and `NAME`!='Surgical' and `NAME`!='Syrup' and `NAME`!='Syrup' ) ORDER BY `NAME`;

DELETE FROM item WHERE `DTYPE`='BillExpense' or `DTYPE`='InwardService' or `DTYPE`='MedicalPackage'
or `DTYPE`='Packege' or `DTYPE`='ServiceSession' or `DTYPE`='Service' or `DTYPE`='TimedItem';

SET FOREIGN_KEY_CHECKS=1;
