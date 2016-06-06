select `ID`,`CREATEDAT`,`NETTOTAL`,`VAT` from  bill WHERE `VAT` is null and `CREATEDAT` between "2016-05-01 00:00:00" and "2016-06-01 00:00:00";
-- update bill set vat=0  WHERE `VAT` is null and `CREATEDAT` between "2016-04-01 00:00:00" and "2016-05-01 00:00:00";

