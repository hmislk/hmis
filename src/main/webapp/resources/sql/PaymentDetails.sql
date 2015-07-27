select p.`ID`,pe.`NAME`,b.`INSID`,b.`DEPTID`,p.`PAIDVALUE`,b.`BILLTYPE`,p.`PAYMENTMETHOD`,p.`CREATEDAT`,d.`NAME`,i.`NAME`
from payment p join department d on p.`DEPARTMENT_ID`=d.`ID`
join institution i on p.`INSTITUTION_ID`=i.`ID`
join webuser w on p.`CREATER_ID`=w.`ID`
join person pe on w.`WEBUSERPERSON_ID`=pe.`ID`
join bill b on p.`BILL_ID`=b.`ID`;

