select `ID`, `DTYPE`, `BILLTYPEATOMIC`, `DEPTID`, `NETTOTAL` , `PAYMENTMETHOD`, `REFERENCEBILL_ID`,  `DEPARTMENT_ID`, `INSTITUTION_ID`, `FROMINSTITUTION_ID`, `TOINSTITUTION_ID`
from bill 
order by `ID` desc limit 10;
select * from billitemfinancedetails order by id desc limit 1;
select `ID`, `QTY`, `FREEQTY` from pharmaceuticalbillitem order by id desc limit 10;

select * from stockhistory order by id desc limit 50;

