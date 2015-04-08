Select staff.RETIRED, staff.DTYPE, staff.code, staff.DATELEFT , staff.`PERSON_ID`,
 person.NAME, staff.`EMPLOYEESTATUS`,staff.`INSTITUTION_ID`
from staff inner join person on staff.`PERSON_ID` = person.`ID` 
where staff.ID=61498;
