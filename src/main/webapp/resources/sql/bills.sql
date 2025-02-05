select `ID`, 
`COMPLETED`,
`BILLEDBILL_ID`,
`REFERENCEBILL_ID`,
 `DTYPE`,
`BILLCLASSTYPE`,
`BILLTYPE`, 
`BILLTYPEATOMIC`, 
`REFERENCENUMBER`,
`PAIDAMOUNT`, 
`SETTLEDAMOUNTBYPATIENT`, `SETTLEDAMOUNTBYSPONSOR`, `PAIDAT`,
`DEPTID`, 
`COMMENTS`,
 `CREATEDAT`,
`TOTAL`,
`GRANTTOTAL`,
`GRNNETTOTAL`,
`NETTOTAL`,
`PAIDAMOUNT`,
`BALANCE`, 
`DEPARTMENT_ID`,
 `INSTITUTION_ID`,
 `PAYMENTMETHOD`,
 `REFERENCEBILL_ID`,
`FORWARDREFERENCEBILL_ID`,
`BACKWARDREFERENCEBILL_ID`,
`CREDITCOMPANY_ID`
from bill 
order by `ID` desc limit 10;
