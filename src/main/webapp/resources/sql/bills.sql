select `ID`, `CREATEDAT`, `BILLTYPE`, `BILLTYPEATOMIC`,  `TOTAL`,`GRANTTOTAL`,`GRNNETTOTAL`,`NETTOTAL`,`BALANCE`,`PAIDAMOUNT`,`BALANCE`, `DEPARTMENT_ID`, `INSTITUTION_ID`, `PAYMENTMETHOD`
from bill 
order by `ID` desc limit 10;
