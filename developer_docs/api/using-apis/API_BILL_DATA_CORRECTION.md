# Bill Data Correction API

## Overview

This API provides a controlled way to correct already-saved bill-related financial data after human approval.

- **Endpoint**: `PATCH /api/bill_data_correction`
- **Auth Header**: `Finance: <API_KEY>`
- **Purpose**: Correct historical data in bill records without direct database access.

> **Important:** This endpoint can only **update** existing records. It **cannot create** a missing
> `BillFinanceDetails` row. If you need to create missing BFDs for historical
> `PHARMACY_STOCK_ADJUSTMENT` or `PHARMACY_RETAIL_RATE_ADJUSTMENT` bills, use
> `POST /api/pharmacy/backfill_bfd` instead (see
> `developer_docs/pharmacy/f15-bfd-backfill-guide.md`).

## Current Related Endpoints

Before applying corrections, use these read-only endpoints to identify the exact records:

- `GET /api/pharmacy_f15_report`
- `GET /api/costing_data/bills_by_type`
- `GET /api/costing_data/by_bill_id/{billId}`

Then apply updates using:

- `PATCH /api/bill_data_correction` — update existing BFD/bill/item fields
- `POST /api/bill_data_correction/create_bill_item` — **create** a single missing `BillItem` on an already-saved bill
- `POST /api/bill_data_correction/create_bill_fee` — **create** a single missing `BillFee` on an already-saved `BillItem`
- `POST /api/pharmacy/backfill_bfd` — **create** missing BFDs for historical adjustment bills

## Creating a Missing Bill Item

Use this only to repair a bill that is missing a `BillItem` it should have (e.g. a refund bill whose
item never got persisted due to a bug in the saving code). It does **not** recompute or touch the
bill's totals, finance details, or payments — the values you supply must already be consistent with
the bill's existing `netTotal`/`total`.

- **Endpoint**: `POST /api/bill_data_correction/create_bill_item`
- **Auth Header**: `Finance: <API_KEY>`

### Request Body

```json
{
  "billId": 5214531,
  "itemId": 61881,
  "referenceBillItemId": 5232165,
  "qty": -1.0,
  "rate": 1740.0,
  "netRate": 1740.0,
  "grossValue": -1740.0,
  "netValue": -1740.0,
  "auditComment": "Refund bill item never persisted due to saveRefundBill() bug — recreated to match original bill item 5232165",
  "approvedBy": "Dr. Smith"
}
```

- `billId` (required) — the bill the item should belong to.
- `itemId` (required) — the `Item` id (service/investigation/drug) for the new bill item.
- `referenceBillItemId` (optional) — the original bill item this one refunds/corresponds to. When
  given, `referenceBill` is set to that item's bill automatically, and the request is rejected with
  `409` if the target bill already has a non-retired item referencing the same source item (prevents
  duplicate recreation).
- `qty`, `rate`, `netRate`, `grossValue`, `netValue` — plain numeric fields copied onto the new
  `BillItem` as-is.
- `auditComment`, `approvedBy` — mandatory, as with `PATCH /api/bill_data_correction`.

### Response (Success)

```json
{
  "status": "success",
  "code": 200,
  "data": {
    "targetType": "BILL_ITEM_CREATE",
    "billId": 5214531,
    "createdBillItemId": 5240001,
    "newValues": {"billItemId": 5240001, "itemId": 61881, "referenceBillItemId": 5232165, "qty": -1.0, "rate": 1740.0, "netRate": 1740.0, "grossValue": -1740.0, "netValue": -1740.0},
    "auditComment": "...",
    "approvedBy": "Dr. Smith",
    "correctedAt": "2026-09-01 18:00:00",
    "correctedByApiUser": "admin_user"
  }
}
```

As with `PATCH`, the correction is appended to the bill's `comments` for audit.

## Creating a Missing Bill Fee

Use this only to repair a `BillItem` that is missing a `BillFee` it should have — e.g. a refund item
that was itself backfilled with `create_bill_item` above, but the corresponding fee was never
recreated, so reports that join from `BillFee` (such as the QuickBooks Daily Return report) still
don't see it. It does **not** recompute or touch the bill's totals, finance details, or payments.

The `fee`, `department`, `institution`, and `patient` on the new row are copied from the
`referenceBillFee` you supply — this mirrors what the normal refund code path
(`BillReturnController`) does when it creates a refund `BillFee` by copying and inverting the
original — so you only need to provide the (already-inverted) `feeValue`/`feeGrossValue`.

- **Endpoint**: `POST /api/bill_data_correction/create_bill_fee`
- **Auth Header**: `Finance: <API_KEY>`

### Request Body

```json
{
  "billItemId": 5251032,
  "referenceBillFeeId": 5217123,
  "feeValue": -1740.0,
  "feeGrossValue": -1740.0,
  "auditComment": "Backfill missing BillFee for refund bill RHDOM/R/26/60715 — BillItem 5251032 was recreated via create_bill_item after issue #23408, but its BillFee was never backfilled, leaving the QB Daily Return report (#23436) unable to see the refund",
  "approvedBy": "Dr. Smith"
}
```

- `billItemId` (required) — the `BillItem` the fee should be attached to.
- `referenceBillFeeId` (required) — the original `BillFee` this one refunds/corrects. Its `fee`,
  `department`, `institution`, and `patient` are copied onto the new row; the request is rejected
  with `409` if the target bill item already has a non-retired fee referencing the same source fee
  (prevents duplicate recreation).
- `feeValue`, `feeGrossValue` — plain numeric fields copied onto the new `BillFee` as-is (already
  inverted/signed as appropriate for a refund or cancellation).
- `auditComment`, `approvedBy` — mandatory, as with `PATCH /api/bill_data_correction`.

### Response (Success)

```json
{
  "status": "success",
  "code": 200,
  "data": {
    "targetType": "BILL_FEE_CREATE",
    "billItemId": 5251032,
    "createdBillFeeId": 5300001,
    "newValues": {"billFeeId": 5300001, "billItemId": 5251032, "referenceBillFeeId": 5217123, "feeId": 123085, "feeValue": -1740.0, "feeGrossValue": -1740.0},
    "auditComment": "...",
    "approvedBy": "Dr. Smith",
    "correctedAt": "2026-09-03 12:00:00",
    "correctedByApiUser": "admin_user"
  }
}
```

As with `PATCH`, the correction is appended to the bill's `comments` for audit.

## Request Body

```json
{
  "targetType": "BILL_FINANCE_DETAILS",
  "targetId": 987654,
  "fields": {
    "totalRetailSaleValue": -62709.88,
    "totalCostValue": 0.0
  },
  "auditComment": "F15 discrepancy correction — approved by Dr. Smith on 2026-02-20",
  "approvedBy": "Dr. Smith"
}
```

## Supported `targetType`

| targetType | Entity | Editable fields |
|---|---|---|
| `BILL` | Bill | `netTotal`, `grossTotal`, `comments` |
| `BILL_ITEM` | BillItem | `qty`, `rate`, `grossValue`, `netValue`, `discount` |
| `BILL_FINANCE_DETAILS` | BillFinanceDetails | `totalRetailSaleValue`, `totalCostValue`, `totalPurchaseValue`, `netTotal`, `grossTotal`, `billExpensesConsideredForCosting`, `billExpensesNotConsideredForCosting`, `totalBillValue` |
| `BILL_FEES` | BillFee | `feeValue`, `grossValue` |
| `BILL_ITEM_FINANCE_DETAILS` | BillItemFinanceDetails | `valueAtRetailRate`, `valueAtCostRate`, `costRate`, `retailSaleRate` |
| `PHARMACEUTICAL_BILL_ITEM` | PharmaceuticalBillItem | `qty`, `retailRate`, `costRate`, `retailValue`, `costValue` |

## BILL_FINANCE_DETAILS Field Reference

### Existing fields

| Field | Type | Description |
|---|---|---|
| `netTotal` | BigDecimal | Net total after all deductions and costing expenses added |
| `grossTotal` | BigDecimal | Gross total of line items |
| `totalCostValue` | BigDecimal | Total value at cost rate across all bill items |
| `totalPurchaseValue` | BigDecimal | Total value at purchase rate across all bill items |
| `totalRetailSaleValue` | BigDecimal | Total value at retail sale rate across all bill items |

### New fields (added 2026-02-23)

These fields were added to support the **Total Bill Value** feature on GRN prints and must be
backfilled for all historical GRN bills using the API.

| Field | Type | Description |
|---|---|---|
| `billExpensesConsideredForCosting` | BigDecimal | Sum of bill expenses marked as "Considered for Costing". These are included in item cost rate calculations. |
| `billExpensesNotConsideredForCosting` | BigDecimal | Sum of bill expenses marked as "NOT Considered for Costing". These are overhead costs that do not affect item cost rates. Always stored as a positive value. |
| `totalBillValue` | BigDecimal | **Total Bill Value** = `netTotal` + `billExpensesNotConsideredForCosting`. Represents the actual total cash outflow to the supplier. Always stored as a positive value. |

### Backfilling historical GRNs

For GRN bills saved before 2026-02-23, these three fields will be `null`. Use the PATCH endpoint to
set them. You can calculate the values from the parent bill:

- `billExpensesConsideredForCosting` = from `bill.expensesTotalConsideredForCosting`
- `billExpensesNotConsideredForCosting` = from `bill.expensesTotalNotConsideredForCosting`
- `totalBillValue` = `bfd.netTotal` + `billExpensesNotConsideredForCosting`
  (Note: `bfd.netTotal` is stored as a positive value; use the BFD's netTotal, not the Bill's netTotal which may be negative)

Example for a GRN where Net Total = 22,675 and non-costing expenses = 500:

```bash
curl -s -X PATCH "$BASE_URL/api/bill_data_correction" \
  -H "Finance: $FINANCE_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "targetType": "BILL_FINANCE_DETAILS",
    "targetId": 56789,
    "fields": {
      "billExpensesConsideredForCosting": 600.00,
      "billExpensesNotConsideredForCosting": 500.00,
      "totalBillValue": 23175.00
    },
    "auditComment": "Backfill totalBillValue for GRN 9927624 — new field added 2026-02-23",
    "approvedBy": "Dr. Buddhika"
  }'
```

## Validation Rules

- `targetType`, `targetId`, and `fields` are mandatory.
- `auditComment` is mandatory.
- `approvedBy` is mandatory.
- Unknown fields for the selected `targetType` are rejected with `400`.
- API key must be valid and mapped to an active user.

## Response (Success)

```json
{
  "status": "success",
  "code": 200,
  "data": {
    "targetType": "BILL_FINANCE_DETAILS",
    "targetId": 987654,
    "previousValues": {
      "totalRetailSaleValue": -62422.21
    },
    "newValues": {
      "totalRetailSaleValue": -62709.88
    },
    "auditComment": "F15 discrepancy correction — approved by Dr. Smith on 2026-02-20",
    "approvedBy": "Dr. Smith",
    "correctedAt": "2026-02-20 14:35:00",
    "correctedByApiUser": "admin_user"
  }
}
```

## Error Responses

The API layer maps `IllegalArgumentException` from missing entities (e.g., `targetId` not found)
to `404 Not Found`, while true validation problems (unknown fields, empty payloads, etc.) remain
`400 Bad Request` errors.

### 400 – Validation Failure (Example)

```json
{
  "status": "error",
  "code": 400,
  "message": "Field 'unknownField' is not allowed for BILL_FINANCE_DETAILS"
}
```

### 401 – Invalid or Missing API Key

```json
{
  "status": "error",
  "code": 401,
  "message": "Invalid or missing API key"
}
```

### 404 – Target Entity Not Found

```json
{
  "status": "error",
  "code": 404,
  "message": "Bill not found for id 123456"
}
```

### 500 – Unexpected Server Error

```json
{
  "status": "error",
  "code": 500,
  "message": "Internal server error"
}
```

## Example cURL

```bash
curl -s -X PATCH "$BASE_URL/api/bill_data_correction" \
  -H "Finance: $FINANCE_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "targetType": "BILL_FINANCE_DETAILS",
    "targetId": 987654,
    "fields": {
      "totalRetailSaleValue": -62709.88
    },
    "auditComment": "F15 discrepancy correction - approved by Dr. Smith on 2026-02-20",
    "approvedBy": "Dr. Smith"
  }'
```

## Audit Trail

Every correction is appended to the parent bill's `comments` including:

- correction timestamp
- target type and id
- API user
- approver
- audit comment
- previous values and new values
