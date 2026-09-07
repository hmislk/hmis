SELECT
  -- Identifiers
  `ID`,
  `CREATEDAT`,

  -- References
  `BILL_ID`,
  `ITEM_ID`,
  `BILLITEMFINANCEDETAILS_ID`,

  -- Quantity
  `QTY`,

  -- Rates
  `RATE`,
  `NETRATE`,

  -- Values
  `GROSSVALUE`,
  `NETVALUE`,

  -- Status
  `RETIRED`
FROM `BILLITEM`
ORDER BY `ID` DESC
LIMIT 10;
