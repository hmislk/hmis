-- select count(s.`CREATEDAT`),s.`CREATEDAT` from staffshift s where s.`ROSTER_ID` is null group BY s.`CREATEDAT`;
-- select s.`ID`,s.`CREATEDAT`,p.`NAME` from staffshift s join webuser w on s.`CREATER_ID`=w.`ID` join person p on w.`WEBUSERPERSON_ID`=p.`ID`  where s.`ROSTER_ID` is null;
SELECT * FROM billitem WHERE billitem.`BILL_ID` in(
select bill.`ID` from bill join department on bill.`TODEPARTMENT_ID`=department.`ID` 
WHERE department.`NAME`='Oxygen Unit ' and bill.`CREATEDAT` between '2015-05-01 00:00:00.0' and '2015-06-16 23:59:59.0' 
and bill.`BILLTYPE`='StoreIssue'
);
