select `ID`, `BILLFINANCEDETAILS_ID`, `BILLTYPEATOMIC`, `DEPTID` from bill order by `ID` desc limit 10;
select `ID`, `BILL_ID`, `BILLITEMFINANCEDETAILS_ID` from billitem order by `ID` desc limit 10;
select `ID` from billitemfinancedetails order by `ID` desc limit 10;
select `ID`, `BILLITEM_ID` from pharmaceuticalbillitem order by `ID` desc limit 10;
select `ID` from billfinancedetails order by `ID` desc limit 10;