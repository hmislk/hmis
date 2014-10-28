SELECT `FEE`,`FFEE` FROM fee WHERE `DTYPE` = "ItemFee" and `FEE` != `FFEE`;
-- UPDATE fee SET `FFEE`=`FEE` Where `ID`>0 and `DTYPE` = "ItemFee";