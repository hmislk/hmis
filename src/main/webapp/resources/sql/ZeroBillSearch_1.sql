select `ID`, `INSID`, `DEPTID`, `BILLTYPE`, `BILLDATE`, `BILLTIME`, `DEPARTMENT_ID`,`CREATER_ID`
from bill 
where `NETTOTAL` =0.0
and `BILLDATE`  > '2020-01-01'
and (`BILLTYPE` ='PharmacySale' or `BILLTYPE` = 'PharmacyPre')
order by id desc 
limit 10;