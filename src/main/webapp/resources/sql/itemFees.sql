Select id from bill where `DEPTID` = 'ChannelingCHANN/63';
select id, `FEE_ID` from billfee where `BILL_ID`=2655800;
select id,`ITEM_ID`,`RETIRED` from fee where id in (2600265,2600266,2600268);
select id,`NAME`,`RETIRED`,`DTYPE`,`CREATEDAT`,`RETIRER_ID`, `EDITEDAT`,`STAFF_ID` from item where id = 2756730;
select id,`NAME`,`RETIRED`,`DTYPE`,`CREATEDAT`,`RETIRER_ID`, `EDITEDAT`,`STAFF_ID` from item where `STAFF_ID` = 2597840;

-- select `ID`,`CREATEDAT`, `NAME`,`FEETYPE`,`FEE`,`ITEM_ID`,`RETIREDAT`,`RETIRED` from fee  order by `CREATEDAT` desc limit 200;
-- update fee
-- set `RETIRED` = false
-- where `RETIRED` is null;
-- select id,`NAME`,`DTYPE`,`RETIRED` from item where id = 2829168;
-- select * from billitem where `ITEM_ID` = 2600255;