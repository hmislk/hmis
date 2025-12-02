-- Check the latest bill item data for pharmacy transaction verification
-- This query retrieves the most recent bill item with key financial fields
SELECT 
  id, 
  `BILL_ID`, 
  ITEM_ID, 
  `BILLITEMFINANCEDETAILS_ID`, 
  `CREATEDAT`, 
  RETIRED, 
  `NETRATE`, 
  `RATE`, 
  `QTY`, 
  `NETVALUE`, 
  `GROSSVALUE` 
FROM BILLITEM 
ORDER BY id DESC 
LIMIT 10;

-- Check the latest bill item finance details for pharmacy transactions
-- This query fetches the most recent 2 records to compare financial calculations
SELECT 
  `ID`, 
  `CREATEDAT`, 
  `LINENETRATE`, 
  `GROSSRATE`, 
  `LINEGROSSRATE`, 
  `BILLCOSTRATE`, 
  `TOTALCOSTRATE`, 
  `LINECOSTRATE`, 
  COSTRATE, 
  PURCHASERATE, 
  RETAILSALERATE, 
  `LINEGROSSTOTAL`, 
  `GROSSTOTAL`, 
  `LINECOST`, 
  `BILLCOST`, 
  `TOTALCOST`, 
  `VALUEATCOSTRATE`, 
  `VALUEATPURCHASERATE`, 
  `VALUEATRETAILRATE`, 
  `QUANTITY`, 
  `QUANTITYBYUNITS` 
FROM BILLITEMFINANCEDETAILS 
ORDER BY id DESC 
LIMIT 10;

-- Check the latest pharmaceutical bill item data
-- This query verifies pharmacy-specific item information including rates and stock references
SELECT 
  `ID`, 
  `BILLITEM_ID`, 
  `CREATER_ID`, 
  `CREATEDAT`, 
  `RETAILRATE`, 
  `PURCHASERATE`, 
  `COSTRATE`, 
  `QTY`, 
  `RETAILVALUE`, 
  `PURCHASEVALUE`, 
  `COSTVALUE` , 
STOCK_ID, 
ITEMBATCH_ID
FROM PHARMACEUTICALBILLITEM 
ORDER BY id DESC 
LIMIT 10;

-- Check the latest bill header information
-- This query retrieves the most recent bill with key totals and transaction type
SELECT 
  `ID`, 
  FROMDEPARTMENT_ID, 
  TODEPARTMENT_ID, 
  `DEPARTMENT_ID`, 
  `BILLFINANCEDETAILS_ID`, 
  `DTYPE`, 
  `BILLTYPEATOMIC`, 
  `BILLTYPE`, 
  `CREATEDAT`, 
  `NETTOTAL`, 
  `TOTAL`, 
  `SALEVALUE` 
FROM BILL 
ORDER BY id DESC 
LIMIT 10;

-- Check the latest bill finance details summary
-- This query fetches bill-level financial totals for verification
SELECT 
  `ID`, 
  `NETTOTAL`, 
  `GROSSTOTAL`, 
  `TOTALCOSTVALUE`, 
  `TOTALPURCHASEVALUE`, 
  `TOTALRETAILSALEVALUE` 
FROM BILLFINANCEDETAILS 
ORDER BY id DESC 
LIMIT 10;