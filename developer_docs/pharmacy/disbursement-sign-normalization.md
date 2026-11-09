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

Why the two lists are opposite
- The two lists above are different layers, not a contradiction. Bill/Item totals follow the money view of the bill (an issue is positive, a receive negative). Finance values follow the stock view (stock out is negative, stock in positive).
- So `PHARMACY_RECEIVE` is negative in Bill/Item totals but positive in finance values, and `PHARMACY_RECEIVE_CANCELLED` is the reverse. Both match `isBillAndItemTotalsPositive` and `isFinanceValueNegative` in `DataAdministrationController`.

Report display: Transfer Receive reports
- Transfer Receive by Bill, by Bill Item and Summary use the stock-in view: `PHARMACY_RECEIVE` is positive and `PHARMACY_RECEIVE_CANCELLED` (any CANCELLATION or REFUND category atomic) is negative, so the totals net.
- Stored signs on older receive bills are mixed, so take the absolute value first and then apply the sign from the BillTypeAtomic. Never sign by stored value.
- `PHARMACY_RECEIVE_PRE` is an unapproved receive request that is later replaced by the `PHARMACY_RECEIVE` bill. These reports exclude it, or the receive would be counted twice.
- The Summary uses the same rules and columns as Transfer Receive by Bill (`b.retired = false`, `BillFinanceDetails` values); only the sign and the `PHARMACY_RECEIVE_PRE` exclusion are added. Its Transfer Value reads `lineNetTotal`, which receive bills leave empty (#25387).

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

