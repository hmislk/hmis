-- Check the latest bill item data for pharmacy transaction verification
-- This query retrieves the most recent bill item with key financial fields
SELECT id, `BILL_ID`, ITEM_ID,  `CREATEDAT`, `BILLITEMFINANCEDETAILS_ID`, `NETRATE`, `RATE`, `QTY`, `NETVALUE`, `GROSSVALUE`, RETIRED FROM BILLITEM order by id desc LIMIT 10;

-- Check the latest bill item finance details for pharmacy transactions
-- This query fetches the most recent 2 records to compare financial calculations
select `ID`,  `CREATEDAT`,  `LINENETRATE`, `LINEGROSSTOTAL`, `GROSSRATE`, `GROSSTOTAL`, `LINEGROSSRATE`, `BILLCOSTRATE`,  `TOTALCOSTRATE`, `LINECOSTRATE`, `LINECOST`, `BILLCOST`, `TOTALCOST`, `QUANTITY`, `QUANTITYBYUNITS`,
 `LINECOSTRATE`, COSTRATE, PURCHASERATE, RETAILSALERATE , `VALUEATCOSTRATE`, `VALUEATPURCHASERATE`, `VALUEATRETAILRATE` from BILLITEMFINANCEDETAILS order by id desc limit 10;

-- Check the latest pharmaceutical bill item data
-- This query verifies pharmacy-specific item information including rates and stock references
select
 `CREATEDAT`, 
`CREATER_ID`,
 `ID`,
 `BILLITEM_ID`,
  `QTY`,
 `RETAILRATE`,
 `PURCHASERATE`,
 `COSTRATE` ,

 `PURCHASEVALUE`, 
`RETAILVALUE`, 
`COSTVALUE`
 from PHARMACEUTICALBILLITEM order by id desc limit 10;


-- Check the latest bill header information
-- This query retrieves the most recent bill with key totals and transaction type
select `ID` , `DTYPE`, `CREATEDAT`,  FROMDEPARTMENT_ID, TODEPARTMENT_ID,   `BILLTYPEATOMIC`, `BILLTYPE`,  `NETTOTAL`, `TOTAL`, `SALEVALUE` , `DEPARTMENT_ID`, `BILLFINANCEDETAILS_ID` from BILL order by id desc limit 10;

-- Check the latest bill finance details summary
-- This query fetches bill-level financial totals for verification
select `ID`,  `NETTOTAL`, `GROSSTOTAL`, `TOTALCOSTVALUE`, `TOTALPURCHASEVALUE`, `TOTALRETAILSALEVALUE` from BILLFINANCEDETAILS order by id desc limit 10;