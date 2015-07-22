SELECT bfp.`ID`,pe.`NAME`,b.`INSID`,b.`DEPTID`,b.`BILLTYPE`,bfp.`CREATEDAT`,d.`NAME`,i.`NAME`
 FROM billfeepayment bfp 
join department d on bfp.`DEPARTMENT_ID`=d.`ID`
join institution i on bfp.`INSTITUTION_ID`=i.`ID`
join webuser w on bfp.`CREATER_ID`=w.`ID`
join person pe on w.`WEBUSERPERSON_ID`=pe.`ID`
join billfee bf on bfp.`BILLFEE_ID`=bf.`ID`
join bill b on bf.`BILL_ID`=b.`ID`
WHERE bfp.`PAYMENT_ID`=10361567;
