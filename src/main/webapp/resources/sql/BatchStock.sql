select item.`NAME`, itembatch.`BATCHNO` ,itembatch.`ITEM_ID`, stock.`STOCK` from stock 
join itembatch on stock.`ITEMBATCH_ID` = itembatch.`ID` 
join item on itembatch.`ITEM_ID` = item.`ID`
where `STOCK`=420;