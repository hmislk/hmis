select id,name,`CREATEDAT`,`RETIRED`,`RETIREDAT` from Person where name like '%Pushpakumara%';
select staff.id,STAFF.`PERSON_ID` from staff where staff.`PERSON_ID` IN (1206, 5886, 6146, 6147, 12768, 13014, 27669, 35711, 36998, 61457, 83709, 97384, 113945);
