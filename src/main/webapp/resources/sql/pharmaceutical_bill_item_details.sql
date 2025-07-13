SELECT 
    b.billtypeatomic, 
    b.deptid,
    i.dtype AS item_dtype,
    i.name AS item_name,
    pbi.* 
FROM pharmaceuticalbillitem pbi
LEFT JOIN billitem bi ON pbi.billitem_id = bi.id
LEFT JOIN bill b ON bi.bill_id = b.id
LEFT JOIN item i ON bi.item_id = i.id
ORDER BY pbi.id DESC
LIMIT 30;
