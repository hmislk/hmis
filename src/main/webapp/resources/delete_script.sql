SET FOREIGN_KEY_CHECKS=0;
DELETE from agentsfees where `ID` > 0;
DELETE FROM appointment where `ID` > 0;
delete from bankaccount where `ID` > 0;
delete from batchbill where `ID`>0;
delete from bill where `ID` > 0;
delete from billcomponent WHERE `ID` > 0;
delete from billentry where `ID` > 0;
delete from billfee where `ID` > 0;
delete from billitem where `ID` > 0;
delete from billsession where `ID`>0;
delete from clinicalfindingvalue where `ID`>0;
delete from dayshift WHERE `ID` > 0;
delete from encountercomponent WHERE `ID`>0;
delete from fingerprintrecord WHERE `ID` > 0;
delete from hrmvariables WHERE `ID`>0;
delete from hrmvariables_payeetaxrange WHERE `HrmVariables_ID` > 0 ;
-- delete from investigationitemvalue WHERE `ID`>0;
delete from investigationreportitemvalue WHERE `ID`>0;
delete from inwardpriceadjustment WHERE `ID` > 10;
delete FROM itembatch WHERE `ID`> 0;
delete from itemforitem WHERE `ID` > 0;
delete from itempackage WHERE `ID`>0;
delete from itemscategories WHERE `ID`>0;
delete FROM itemsdistributors WHERE `ID` > 0;
 delete from ixcal WHERE `ID`>0;
DELETE from loan WHERE `ID` > 0;
delete from logins WHERE `ID`> 0;
delete from mapping WHERE `ID`>0;
delete from medicalpackageitem WHERE `ID`>0;
delete from packageitem WHERE `ID` > 0;
delete from patient WHERE `ID` > 0;
delete from patientencounter WHERE `ID` > 0;
delete from patientflag WHERE `ID` > 0;
delete from patientinvestigation where `ID` > 0;
delete from patientitem WHERE `ID` > 0;
delete from patientreport WHERE `ID` > 0;
delete from patientreportitemvalue WHERE `ID`>0;
delete from patientroom where `ID` > 0;
delete from payeetaxrange WHERE `ID` > 0;
delete from payment WHERE `ID` > 0;
delete from paysheetcomponent WHERE `ID` > 0;
delete from person where `ID`> 7000;
delete from pharmaceuticalbillitem WHERE `ID` > 0;
delete from phdate WHERE `ID` > 0;
delete from price WHERE `ID` >0;
delete from reorder WHERE `ID` > 0;
delete from roomfacilitycharge WHERE `ID` > 0;
delete from roster WHERE `ID` > 0;
delete from salarycycle  WHERE `ID` > 0;
delete from salaryhold  WHERE `ID` > 0;
delete from servicesession  WHERE `ID` > 0;
delete from servicesessionleave WHERE `ID` > 0;
delete from shift WHERE `ID` > 0;
delete from shiftpreference WHERE `ID` > 0;
delete from staffbasics WHERE `ID` > 0;
delete from staffdesignation WHERE `ID` > 0;
delete from staffemployeestatus  WHERE `ID` > 0;
delete from staffemployment  WHERE `ID` > 0;
delete from staffgrade  WHERE `ID` > 0;
delete from staffgroup WHERE `ID`>0;
delete from staffhistory  WHERE `ID` > 0;
delete from staffleave  WHERE `ID` > 0;
delete from staffloan  WHERE `ID` > 0;
delete from staffpaysheetcomponent WHERE `ID` > 0;
delete from staffsalary WHERE `ID`>0;
delete from staffsalarycomponant  WHERE `ID` > 0;;
delete from staffshift WHERE `ID` > 0;
delete from staffstaffcategory WHERE `ID` > 0;
delete from staffworkday WHERE `ID` > 0;
delete from staffworkingdepartment  WHERE `ID` > 0;
delete from stock  WHERE `ID` > 0;
delete from stockhistory WHERE `ID` > 0;
delete from stockvarientbillitem  WHERE `ID` > 0;
delete from userstock  WHERE `ID` > 0;
delete from userstockcontainer WHERE `ID` > 0;
delete from vtminvmp WHERE `ID` > 0;
delete from vtmsvmps  WHERE `ID` > 0;
delete from webtheme WHERE `ID` > 7000;
delete from webuser WHERE `ID` > 7000;

SET FOREIGN_KEY_CHECKS=1;






























