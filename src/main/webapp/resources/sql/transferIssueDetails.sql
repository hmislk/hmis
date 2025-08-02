SELECT 
    b.DEPTID                AS deptId,
    b.CREATEDAT             AS createdAt,
    it.NAME                 AS itemName,
    it.CODE                 AS itemCode,
    bi.QTY                  AS qty,
    ib.COSTRATE             AS costRate,
    bfd.VALUEATCOSTRATE     AS costValue,
    p.RETAILRATE            AS retailRate,
    bfd.VALUEATRETAILRATE   AS retailValue,
    p.PURCHASERATE          AS purchaseRate,
    bfd.VALUEATPURCHASERATE AS purchaseValue,
    bfd.LINEGROSSRATE       AS transferRate,
    bfd.LINEGROSSTOTAL      AS transferValue
FROM billitem bi
INNER JOIN bill b 
    ON bi.BILL_ID = b.ID
LEFT JOIN pharmaceuticalbillitem p 
    ON bi.ID = p.BILLITEM_ID
LEFT JOIN itembatch ib 
    ON p.ITEMBATCH_ID = ib.ID
LEFT JOIN billitemfinancedetails bfd 
    ON bi.BILLITEMFINANCEDETAILS_ID = bfd.ID
LEFT JOIN item it 
    ON bi.ITEM_ID = it.ID
WHERE b.BILLTYPE = 'PharmacyTransferIssue'
  AND (b.RETIRED = FALSE OR b.RETIRED IS NULL)
  AND (bi.RETIRED = FALSE OR bi.RETIRED IS NULL)
  AND b.CREATEDAT BETWEEN '2025-07-01' AND '2025-07-31'
ORDER BY bi.ID;
