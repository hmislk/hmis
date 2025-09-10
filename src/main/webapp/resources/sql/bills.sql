select `ID`, `BALANCE`, `DTYPE`, `BILLTYPEATOMIC`, `DEPTID`,  `FULLYISSUED`, `FROMINSTITUTION_ID` , `TOINSTITUTION_ID`, `DEPARTMENT_ID`, `FROMDEPARTMENT_ID` , `TODEPARTMENT_ID`, `CREATEDAT`, `CREATER_ID`
from bill 
order by `ID` desc limit 10;

select * from stockhistory order by id desc limit 50;

