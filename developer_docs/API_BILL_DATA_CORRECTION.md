# Bill Data Correction API

## Overview

This API provides a controlled way to correct already-saved bill-related financial data after human approval.

- **Endpoint**: `PATCH /api/bill_data_correction`
- **Auth Header**: `Finance: <API_KEY>`
- **Purpose**: Correct historical data in bill records without direct database access.

## Current Related Endpoints

Before applying corrections, use these read-only endpoints to identify the exact records:

- `GET /api/pharmacy_f15_report`
- `GET /api/costing_data/bills_by_type`
- `GET /api/costing_data/by_bill_id/{billId}`

Then apply updates using:

- `PATCH /api/bill_data_correction`

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
- `totalBillValue` = `|netTotal|` + `billExpensesNotConsideredForCosting`

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
