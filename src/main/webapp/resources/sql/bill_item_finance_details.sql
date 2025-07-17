SELECT 
    b.billtypeatomic,
    b.deptid, 
    i.dtype AS item_dtype,
    i.name AS item_name,
    bif.`TOTALCOST`

FROM billitemfinancedetails bif
LEFT JOIN billitem bi ON bif.id = bi.billitemfinancedetails_id
LEFT JOIN bill b ON bi.bill_id = b.id
LEFT JOIN item i ON bi.item_id = i.id
ORDER BY bif.id DESC
LIMIT 11;
