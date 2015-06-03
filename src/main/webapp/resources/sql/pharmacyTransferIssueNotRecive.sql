select bi.`ID` from billitem bi join bill b on bi.`BILL_ID`=b.`ID` 
join billitem rbi on bi.`REFERANCEBILLITEM_ID`=rbi.`ID` WHERE b.`BILLTYPE`='PharmacyTransferIssue' and
 b.`DTYPE`='BilledBill'
 and rbi.`ID` is not null and bi.`CREATER_ID` is not null
 and (b.`INSID` is null or b.`DEPTID` is null);

-- select rbi.`BILL_ID` from billitem bi join bill b on bi.`BILL_ID`=b.`ID` 
-- join billitem rbi on bi.`REFERANCEBILLITEM_ID`=rbi.`ID` WHERE b.`BILLTYPE`='PharmacyTransferIssue' and
--  b.`DTYPE`='BilledBill'
--  and rbi.`ID` is not null and bi.`CREATER_ID` is not null
--  and (b.`INSID` is null or b.`DEPTID` is null);

-- SELECT b.`INSID`,b.`DEPTID` FROM bill b WHERE b.`ID`=8601642;
-- SELECT b.`INSID`,b.`DEPTID` FROM bill b WHERE b.`ID`=8601235;

-- SELECT * FROM pharmaceuticalbillitem phi WHERE phi.`BILLITEM_ID` in(
-- select bi.`ID` from billitem bi join bill b on bi.`BILL_ID`=b.`ID` 
-- join billitem rbi on bi.`REFERANCEBILLITEM_ID`=rbi.`ID` WHERE b.`BILLTYPE`='PharmacyTransferIssue' and
--  b.`DTYPE`='BilledBill'
--  and rbi.`ID` is not null and bi.`CREATER_ID` is not null
--  and (b.`INSID` is null or b.`DEPTID` is null)
-- );