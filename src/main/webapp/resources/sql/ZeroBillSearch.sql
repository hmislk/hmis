select distinct `BILLTYPE`, count(*)  , `CREATER_ID`
from bill 
where `NETTOTAL` =0.0
and `BILLDATE`  > '2020-01-01'
and (`BILLTYPE` ='PharmacySale' or `BILLTYPE` = 'PharmacyPre')
group by `BILLTYPE`, `CREATER_ID`
order by count(*) desc;