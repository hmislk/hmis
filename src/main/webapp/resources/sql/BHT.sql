select `BHTNO`, person.`NAME`,patientencounter.`DATEOFADMISSION`
from patientencounter 
join patient on patientencounter.`PATIENT_ID` = patient.`ID` 
join person on patient.`PERSON_ID` = person.`ID`
where person.`NAME` like '%Hewa%'
order by patientencounter.`ID` desc