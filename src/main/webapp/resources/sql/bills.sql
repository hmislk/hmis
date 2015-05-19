select `ID`,`TOTAL`,`GRANTTOTAL`,`GRNNETTOTAL`,`NETTOTAL`,`BALANCE`,`PAIDAMOUNT`,`BALANCE`
 from bill 
where billtype='OpdBill'
order by `ID` desc limit 10;
