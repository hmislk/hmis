select `ID`, `DTYPE`, `BILLTYPEATOMIC`, `DEPTID`, `NETTOTAL` , `PAYMENTMETHOD`, `DEPARTMENT_ID`, `INSTITUTION_ID`, `FROMINSTITUTION_ID`, `TOINSTITUTION_ID`
from bill 
order by `ID` desc limit 10;
select * from billitemfinancedetails order by id desc limit 1;

select * from stockhistory order by id desc limit 50;

