SELECT 
    b.billtypeatomic, 
    b.deptid,
    pbi.* 
FROM pharmaceuticalbillitem pbi
LEFT JOIN billitem bi ON pbi.billitem_id = bi.id
LEFT JOIN bill b ON bi.bill_id = b.id
ORDER BY pbi.id DESC
LIMIT 100;
