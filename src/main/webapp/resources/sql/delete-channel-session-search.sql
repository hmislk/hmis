select id,`NAME`,`TITLE`,`CREATEDAT`,`RETIRED`,`RETIREDAT` from person where `NAME` like "%indika liya%";
select id,`DTYPE`,`RETIRED`,`CREATEDAT`,`RETIREDAT`,`RETIRER_ID` from Staff 
where `PERSON_ID` in (1926,31860,92270);
select `ID`,`NAME`,`CREATEDAT`,`RETIRED`,`RETIREDAT`,`RETIRER_ID`,`DTYPE`,`STAFF_ID`,`DEPARTMENT_ID` 
from Item where `STAFF_ID`in (96656,1427781);