select `ID`, `BILLTYPEATOMIC`, `DEPTID`,  `DEPARTMENT_ID`,  `FROMDEPARTMENT_ID` , `TODEPARTMENT_ID`
from bill 
order by `ID` desc limit 10;
select * from billitemfinancedetails order by id desc limit 1;
select `ID`, `QTY`, `FREEQTY` from pharmaceuticalbillitem order by id desc limit 10;

select * from stockhistory order by id desc limit 50;

