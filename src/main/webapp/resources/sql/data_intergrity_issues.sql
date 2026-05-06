  Query 1: Check PharmaceuticalBillItem structure for batch info
  -- Check if PharmaceuticalBillItem has direct batch reference
  SELECT
      pbi.ID,
      pbi.BILLITEM_ID,
      pbi.STOCK_ID,
      pbi.ITEMBATCH_ID,
      bi.ITEMBATCH_ID as BillItemBatchId
  FROM PHARMACEUTICALBILLITEM pbi
  LEFT JOIN BILLITEM bi ON pbi.BILLITEM_ID = bi.ID
  WHERE pbi.ID = 1929585
  LIMIT 1;

  Query 2: Analyze the affected records by bill type
  -- See which bill types are affected
  SELECT
      b.BILLTYPE,
      b.BILLTYPEATOMIC,
      COUNT(*) as AffectedCount
  FROM STOCKHISTORY sh
  JOIN PHARMACEUTICALBILLITEM pbi ON sh.PBITEM_ID = pbi.ID
  LEFT JOIN STOCK s ON pbi.STOCK_ID = s.ID
  JOIN BILLITEM bi ON pbi.BILLITEM_ID = bi.ID
  JOIN BILL b ON bi.BILL_ID = b.ID
  WHERE sh.CREATEDAT >= '2025-09-01'
    AND pbi.STOCK_ID IS NOT NULL
    AND s.ID IS NULL
  GROUP BY b.BILLTYPE, b.BILLTYPEATOMIC
  ORDER BY AffectedCount DESC;

  Query 3: Check if BillItem has batch information
  -- Can we get batch from BillItem?
  SELECT
      bi.ID as BillItemId,
      bi.ITEMBATCH_ID,
      ib.BATCHNO
  FROM BILLITEM bi
  LEFT JOIN ITEMBATCH ib ON bi.ITEMBATCH_ID = ib.ID
  WHERE bi.ID = (SELECT BILLITEM_ID FROM PHARMACEUTICALBILLITEM WHERE ID = 1929585);
