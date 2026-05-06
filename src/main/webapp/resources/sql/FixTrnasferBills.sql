-- Set date range
SET @fromDate = '2025-04-01 00:00:00';
SET @toDate   = '2025-04-30 23:59:59';

-- 1. Insert new BillItemFinanceDetails for missing ones
INSERT INTO billitemfinancedetails (
    LINENETRATE,
    LINEGROSSRATE,
    GROSSRATE,
    LINENETTOTAL,
    NETTOTAL,
    LINEGROSSTOTAL,
    GROSSTOTAL,
    LINECOSTRATE,
    LINECOST,
    QUANTITY,
    QUANTITYBYUNITS,
    VALUEATCOSTRATE,
    VALUEATPURCHASERATE,
    VALUEATRETAILRATE,
    RETAILSALERATE,
    CREATEDAT
)
SELECT 
    bi.NETRATE,
    bi.RATE,
    bi.RATE,
    bi.NETVALUE,
    bi.NETVALUE,
    bi.GROSSVALUE,
    bi.GROSSVALUE,
    ib.COSTRATE,
    ib.COSTRATE * pbi.QTY,
    pbi.QTY,
    pbi.QTY,
    ib.COSTRATE * pbi.QTY,
    ib.PURCHASERATE * pbi.QTY,
    ib.RETAILRATE * pbi.QTY,
    ib.RETAILRATE,
    NOW()
FROM billitem bi
JOIN bill b ON bi.BILL_ID = b.ID
JOIN pharmaceuticalbillitem pbi ON pbi.BILLITEM_ID = bi.ID
JOIN itembatch ib ON ib.ID = pbi.ITEMBATCH_ID
LEFT JOIN billitemfinancedetails fd ON bi.BILLITEMFINANCEDETAILS_ID = fd.ID
WHERE b.BILLTYPE IN ('PharmacyTransferIssue', 'PharmacyTransferReceive')
  AND bi.RETIRED = FALSE
  AND (fd.ID IS NULL OR fd.LINENETTOTAL IS NULL OR fd.LINENETTOTAL = 0)
  AND b.CREATEDAT BETWEEN @fromDate AND @toDate;

-- 2. Update billitems to link to their new BillItemFinanceDetails
UPDATE billitem bi
JOIN bill b ON bi.BILL_ID = b.ID
LEFT JOIN billitemfinancedetails fd ON bi.BILLITEMFINANCEDETAILS_ID = fd.ID
JOIN pharmaceuticalbillitem pbi ON pbi.BILLITEM_ID = bi.ID
JOIN itembatch ib ON ib.ID = pbi.ITEMBATCH_ID
SET bi.BILLITEMFINANCEDETAILS_ID = (
    SELECT MAX(nfd.ID) 
    FROM billitemfinancedetails nfd
    WHERE nfd.CREATEDAT = (SELECT MAX(CREATEDAT) 
                           FROM billitemfinancedetails 
                           WHERE nfd.RETAILSALERATE = ib.RETAILSALERATE)
)
WHERE b.BILLTYPE IN ('PharmacyTransferIssue', 'PharmacyTransferReceive')
  AND bi.RETIRED = FALSE
  AND (fd.ID IS NULL OR fd.LINENETTOTAL IS NULL OR fd.LINENETTOTAL = 0)
  AND b.CREATEDAT BETWEEN @fromDate AND @toDate;

-- 3. Insert BillFinanceDetails for bills in date range
INSERT INTO billfinancedetails (
    NETTOTAL,
    GROSSTOTAL,
    TOTALCOSTVALUE,
    TOTALPURCHASEVALUE,
    TOTALRETAILSALEVALUE,
    CREATEDAT
)
SELECT 
    SUM(fd.NETTOTAL),
    SUM(fd.GROSSTOTAL),
    SUM(fd.VALUEATCOSTRATE),
    SUM(fd.VALUEATPURCHASERATE),
    SUM(fd.VALUEATRETAILRATE),
    NOW()
FROM bill b
JOIN billitem bi ON bi.BILL_ID = b.ID
JOIN billitemfinancedetails fd ON fd.ID = bi.BILLITEMFINANCEDETAILS_ID
WHERE b.BILLTYPE IN ('PharmacyTransferIssue', 'PharmacyTransferReceive')
  AND bi.RETIRED = FALSE
  AND b.CREATEDAT BETWEEN @fromDate AND @toDate
GROUP BY b.ID;

-- 4. Update bills to point to their BillFinanceDetails
UPDATE bill b
JOIN billfinancedetails bfd ON bfd.CREATEDAT = (
    SELECT MAX(CREATEDAT) 
    FROM billfinancedetails 
    WHERE CREATEDAT BETWEEN @fromDate AND @toDate
)
SET b.BILLFINANCEDETAILS_ID = bfd.ID
WHERE b.BILLTYPE IN ('PharmacyTransferIssue', 'PharmacyTransferReceive')
  AND b.CREATEDAT BETWEEN @fromDate AND @toDate;
