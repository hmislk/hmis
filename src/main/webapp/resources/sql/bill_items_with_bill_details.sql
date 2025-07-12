SELECT 
    b.billtypeatomic, 
    b.deptid,
    bi.* 
FROM billitem bi
LEFT JOIN bill b ON bi.bill_id = b.id
ORDER BY bi.id DESC
LIMIT 10;
