# Pharmacy Disbursement Sign Normalization

Overview
- Clarifies sign conventions for pharmacy disbursement/transfer/disposal flows to avoid ambiguous instructions like “get absolute value first and then negate”.
- Documents the mapping used by `DataAdministrationController` helpers:
  - `isBillAndItemTotalsPositive(BillTypeAtomic)` – expected sign for Bill/BillItem gross/net totals.
  - `isFinanceValueNegative(BillTypeAtomic)` – expected sign for finance values (purchase/retail/cost).

Definitions
- Bill/Item Totals: gross/net totals at Bill and BillItem scopes.
- Finance Values: purchase/retail/cost value aggregations at BillFinanceDetails/BillItemFinanceDetails/PharmaceuticalBillItem.

Sign Rules by Flow (BillTypeAtomic)
- Positive totals: values are shown/stored as positive numbers.
- Negative totals: values are shown/stored as negative numbers (e.g., returns/refunds as outflows vs inflows depending on domain policy).

Bill/Item Totals (gross/net)
- Positive: `PHARMACY_ISSUE`, `PHARMACY_DIRECT_ISSUE`, `PHARMACY_DISPOSAL_ISSUE`, `PHARMACY_RECEIVE_CANCELLED`.
- Negative: `PHARMACY_ISSUE_CANCELLED`, `PHARMACY_ISSUE_RETURN`, `PHARMACY_DIRECT_ISSUE_CANCELLED`, `PHARMACY_DISPOSAL_ISSUE_CANCELLED`, `PHARMACY_DISPOSAL_ISSUE_RETURN`, `PHARMACY_RECEIVE`, `PHARMACY_RECEIVE_PRE`.

Finance Values (purchase/retail/cost)
- Negative (stock-out / issue flows): `PHARMACY_ISSUE`, `PHARMACY_DIRECT_ISSUE`, `PHARMACY_DISPOSAL_ISSUE`, `PHARMACY_RECEIVE_CANCELLED`.
- Positive (receive/returns/refunds): `PHARMACY_ISSUE_CANCELLED`, `PHARMACY_ISSUE_RETURN`, `PHARMACY_DIRECT_ISSUE_CANCELLED`, `PHARMACY_DISPOSAL_ISSUE_CANCELLED`, `PHARMACY_DISPOSAL_ISSUE_RETURN`, `PHARMACY_RECEIVE`, `PHARMACY_RECEIVE_PRE`.

Implementation Pattern
1) Normalize with absolute value
2) Apply sign based on mapping above

Example
```
BigDecimal applySign(BigDecimal v, boolean negative) {
  if (v == null) return null;
  BigDecimal abs = v.abs();
  return negative ? abs.negate() : abs;
}
```

Consistency Across Layers
- BillFinanceDetails gross/net header totals MUST follow the same sign convention as Bill and BillItem totals.
- Do NOT force all header totals positive; derive sign from `isBillAndItemTotalsPositive(...)`.

Display vs Storage
- UI may display absolute values for readability (e.g., reports), but persisted/calculated values must follow the above conventions.

