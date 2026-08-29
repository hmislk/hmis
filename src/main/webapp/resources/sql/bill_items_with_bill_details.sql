SELECT 
    b.billtypeatomic, 
    b.deptid,
    i.dtype AS item_dtype,
    i.name AS item_name,
    bi.*
FROM billitem bi
LEFT JOIN bill b ON bi.bill_id = b.id
LEFT JOIN item i ON bi.item_id = i.id
ORDER BY bi.id DESC
LIMIT 20;
