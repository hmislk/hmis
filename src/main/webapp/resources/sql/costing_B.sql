SELECT
  -- Identifiers
  `ID`,
  `DTYPE`,
  `CREATEDAT`,

  -- Departments
  `FROMDEPARTMENT_ID`,
  `TODEPARTMENT_ID`,
  `DEPARTMENT_ID`,

  -- Institutions
  `FROMINSTITUTION_ID`,
  `TOINSTITUTION_ID`,
  `INSTITUTION_ID`,

  -- Bill Types
  `BILLTYPEATOMIC`,
  `BILLTYPE`,

  -- Financial Components
  `DISCOUNT`,
  `TAX`,
  `EXPENSETOTAL`,

  -- Totals
  `NETTOTAL`,
  `TOTAL`,

  -- References
  `BILLFINANCEDETAILS_ID`
FROM `BILL`
ORDER BY `ID` DESC
LIMIT 10;
