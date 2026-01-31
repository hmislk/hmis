-- Check the latest bill finance details summary
-- This query fetches bill-level financial totals for verification
select `ID`,  `NETTOTAL`, `GROSSTOTAL`, `TOTALCOSTVALUE`, `TOTALPURCHASEVALUE`, `TOTALRETAILSALEVALUE` from BILLFINANCEDETAILS order by id desc limit 10;