select `ID`, `CREATEDAT`, `DEPTID`, `DTYPE`, `BILLTYPEATOMIC`,  `NETTOTAL`,  `DEPARTMENT_ID`, `CREATEDAT`,`BILLDATE` , `REFERENCEBILL_ID`
from bill
order by id desc 
limit 10;