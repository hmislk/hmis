select 
-- *
-- f.`ID`,
p.`ID`,
p.`NAME`,
ss.`STARTINGTIME`,
f.`RECORDTIMESTAMP`,
f.`APPROVEDAT`
from fingerprintrecord f 
join item ss on f.`SERVICESESSION_ID`=ss.`ID`
join staff s on ss.`STAFF_ID`=s.`ID` 
join person p on s.`PERSON_ID`=p.`ID` 
WHERE f.`DTYPE`='ArrivalRecord' 
-- and p.`ID`=159 krishantha jayasekara
-- and p.`ID`=605242 Nirukshan Jayaweer
and p.`NAME` like '%Nirukshan%'
-- and ss.`STARTINGTIME`='1970-01-01 06:30:00.0'
and (ss.`STARTINGTIME`='1970-01-01 16:00:00.0' or ss.`STARTINGTIME`='1970-01-01 16:30:00.0')
order by  ss.`STARTINGTIME` ,f.`RECORDTIMESTAMP`
;