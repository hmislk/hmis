SELECT 
    b.billtypeatomic,
    b.deptid,
    bf.*
FROM billfinancedetails bf
LEFT JOIN bill b ON b.billfinancedetails_id = bf.id
ORDER BY bf.id DESC
LIMIT 10;
