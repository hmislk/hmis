select `ID`, `CREATEDAT`,  `DTYPE`, `BILLTYPE`, `BILLTYPEATOMIC`, `DEPTID`,`DEPARTMENT_ID`, `INSTITUTION_ID`, `NETTOTAL` , `PAYMENTMETHOD`, `REFERENCEBILL_ID`,  `DEPARTMENT_ID`, `INSTITUTION_ID`, `FROMINSTITUTION_ID`, `TOINSTITUTION_ID`, `BILLEDBILL_ID`, `REFERENCEBILL_ID`
from BILL 
order by `ID` desc limit 10;

SELECT ID, BILL_ID, PAIDVALUE, PAYMENTMETHOD FROM PAYMENT where BILL_ID in (1945936 , 1945941);

-- select * from billitem order by id desc limit 10;
-- select * from billitemfinancedetails order by id desc limit 10;
-- select `ID`, `QTY`, `FREEQTY` from pharmaceuticalbillitem order by id desc limit 10;
-- 
-- select * from stockhistory order by id desc limit 50;
-- 
