select *
from BILL
where id = 28035145
order by id desc limit 5;
select * from BILLFEE where BILL_ID = 28035145;
select * from BILLITEM where BILL_ID = 28035145;
select * from ITEM where id=6116143 or id=6128499;
