-- select * from patientsample where id = 30122;
select insid from bill where id = (select bill_id from patientsample where id = 39870);
