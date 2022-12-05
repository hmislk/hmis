select id, BILLTYPE from BILL order by id desc limit 1;
SELECT * FROM BILLITEM where bill_id=25453352 order by id desc LIMIT 100;
select id, BILLTYPE from BILL where id =6783433;
SELECT id, ITEM_ID FROM BILLITEM where id=6783434 order by id desc LIMIT 100;
SELECT * FROM BILLFEE where BILLITEM_ID=6783434 order by id desc LIMIT 100;
select name, STAFF_ID from ITEM where id=6767021;
select 

-- SELECT id, CREATEDAT, PAYMENTMETHOD, DTYPE, BILLTYPE, TOTAL, BALANCE, PATIENTENCOUNTER_ID, INSTITUTION_ID FROM BILL order by id desc LIMIT 10;
-- SELECT * FROM BILLITEM where id=25417215 order by id desc LIMIT 100;
-- SELECT * FROM ITEM where id=25417215 order by id desc LIMIT 100;
-- SELECT * FROM BILLFEE where id=25417215 order by id desc LIMIT 100;
-- 
-- select * from PATIENTENCOUNTER where id =25417202;

