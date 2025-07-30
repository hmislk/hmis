-- Check the latest bill item data for pharmacy transaction verification
-- This query retrieves the most recent bill item with key financial fields
SELECT id, `NETRATE`, `RATE`, `QTY`, `NETVALUE`, `GROSSVALUE` FROM billitem order by id desc LIMIT 1;

-- Check the latest bill item finance details for pharmacy transactions
-- This query fetches the most recent 2 records to compare financial calculations
select `ID`, `LINENETRATE`, `LINEGROSSTOTAL`, `GROSSRATE`, `GROSSTOTAL`, `LINEGROSSRATE`, `LINECOST`, `QUANTITY`, `QUANTITYBYUNITS`,
 `LINECOSTRATE`, `VALUEATCOSTRATE`, `VALUEATPURCHASERATE`, `VALUEATRETAILRATE` from billitemfinancedetails order by id desc limit 2;

-- Check the latest pharmaceutical bill item data
-- This query verifies pharmacy-specific item information including rates and stock references
select `ID`, `QTY`, `RETAILRATE`, `PURCHASERATE`, `PURCHASERATEPACK`, `RETAILRATEPACK`, `PURCHASEVALUE`, `RETAILVALUE`, `STOCK_ID`, `ITEMBATCH_ID` from pharmaceuticalbillitem order by id desc limit 1;

-- Check the latest bill header information
-- This query retrieves the most recent bill with key totals and transaction type
select `ID` , `CREATEDAT`, `BILLTYPEATOMIC`,  `NETTOTAL`, `TOTAL`, `SALEVALUE` from Bill order by id desc limit 1;

-- Check the latest bill finance details summary
-- This query fetches bill-level financial totals for verification (note: TOTALCOSTVALUE appears twice, may be intentional for debugging)
select `ID`, `NETTOTAL`, `GROSSTOTAL`, `TOTALCOSTVALUE`, `TOTALCOSTVALUE`, `TOTALPURCHASEVALUE`, `TOTALRETAILSALEVALUE` from billfinancedetails order by id desc limit 1;