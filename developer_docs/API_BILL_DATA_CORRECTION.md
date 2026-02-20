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
| `BILL_FINANCE_DETAILS` | BillFinanceDetails | `totalRetailSaleValue`, `totalCostValue`, `totalPurchaseValue`, `netTotal`, `grossTotal` |
| `BILL_FEES` | BillFee | `feeValue`, `grossValue`, `netValue` |
| `BILL_ITEM_FINANCE_DETAILS` | BillItemFinanceDetails | `valueAtRetailRate`, `valueAtCostRate`, `costRate`, `retailSaleRate` |
| `PHARMACEUTICAL_BILL_ITEM` | PharmaceuticalBillItem | `qty`, `retailRate`, `costRate`, `retailValue`, `costValue` |

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

## Error Response

```json
{
  "status": "error",
  "code": 400,
  "message": "Field 'unknownField' is not allowed for BILL_FINANCE_DETAILS"
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
