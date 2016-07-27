SELECT b.`INSID`,b.`DTYPE`,b.`RETIRED`,b.`PAIDAMOUNT`,f.`FEETYPE`,b.`REFUNDED`,b.`CANCELLED`,
(bf.`FEEVALUE`-bf.`PAIDVALUE`),b.`BILLTYPE`,pe.`NAME`,b.`APPOINTMENTAT`,ss.`NAME`,ss.`RETIRED`,os.`NAME`,os.`RETIRED`,bs.`ABSENT`,os.`REFUNDABLE`
 FROM billfee bf 
-- join department d on bfp.`DEPARTMENT_ID`=d.`ID`
-- join institution i on bfp.`INSTITUTION_ID`=i.`ID`
-- join webuser w on bfp.`CREATER_ID`=w.`ID`
-- join person pe on w.`WEBUSERPERSON_ID`=pe.`ID`
join bill b on bf.`BILL_ID`=b.`ID`
join fee f on bf.`FEE_ID`=f.`ID`
join staff s on bf.`STAFF_ID`=s.`ID`
join person pe on s.`PERSON_ID`=pe.`ID`
join billsession bs on b.`SINGLEBILLSESSION_ID`=bs.`ID`
join item ss on bs.`SERVICESESSION_ID`=ss.`ID`
join item os on ss.`ORIGINATINGSESSION_ID`=os.`ID`

WHERE b.`DEPTID`='CACH/1931' or b.`INSID`='RHCH/3341' or b.`INSID`='RHCH/18550' or b.`INSID`='RHCH/19804';

