-- Check pharmaceutical bill items created on 9th July 2025
SELECT `ID`, `QTY`, `RETAILRATE`, `PURCHASERATE`, `COSTRATE`,  `CREATEDAT`,
       `PURCHASERATEPACK`, `RETAILRATEPACK`, `COSTRATEPACK`, 
       `PURCHASEVALUE`, `RETAILVALUE`, `COSTVALUE`,  
       `STOCK_ID`, `ITEMBATCH_ID`, `BILLITEM_ID` 
FROM pharmaceuticalbillitem
WHERE `ID` >= 3312025
ORDER BY id DESC
limit 10;
select `ID`, `CREATEDAT` from billitem
where `ID` = 4289413;