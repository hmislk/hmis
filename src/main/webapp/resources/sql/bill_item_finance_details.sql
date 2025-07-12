SELECT 
    b.billtypeatomic,
    b.deptid,
    bif.*
FROM billitemfinancedetails bif
LEFT JOIN billitem bi ON bif.id = bi.billitemfinancedetails_id
LEFT JOIN bill b ON bi.bill_id = b.id
ORDER BY bif.id DESC
LIMIT 10;
