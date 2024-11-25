select `ID`,`TOTAL`,`GRANTTOTAL`,`GRNNETTOTAL`,`NETTOTAL`,`BALANCE`,`PAIDAMOUNT`,`BALANCE`, `DEPARTMENT_ID`, `INSTITUTION_ID`
 from bill 
where billtype='OpdBill'
order by `ID` desc limit 10;
