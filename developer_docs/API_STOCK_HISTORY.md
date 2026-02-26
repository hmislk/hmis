# Stock History API Documentation

## Overview
The Stock History API provides transaction-level stock movement records captured by `StockHistory`. It can be used to verify quantity/value movements after GRNs, issues, retail sales, and adjustments without direct database access.

## Authentication
All requests require API key authentication via the `Finance` header.

**Header:**
```text
Finance: <your-api-key>
```

## Base URL
```text
/api/stock_history
```

## Endpoint

### Get Stock History Records
**Endpoint:** `GET /api/stock_history`

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| itemId | Long | No | Filter by item ID |
| departmentId | Long | No | Filter by department ID |
| billId | Long | No | Filter by related bill ID |
| fromDate | String | No | Start timestamp (`yyyy-MM-dd HH:mm:ss`) |
| toDate | String | No | End timestamp (`yyyy-MM-dd HH:mm:ss`) |
| historyType | String | No | Filter by `HistoryType` enum value |
| limit | Integer | No | Max rows (default: 100) |

**Example Request:**
```bash
curl -X GET \
  'http://localhost:8080/hmis/api/stock_history?billId=9602168&limit=20' \
  -H 'Finance: your-api-key-here'
```

**Example Response:**
```json
{
  "status": "success",
  "code": 200,
  "data": [
    {
      "id": 12345,
      "createdAt": "2026-02-25 08:22:55",
      "stockAt": "2026-02-25 00:00:00",
      "historyType": "Issue",
      "itemId": 117323,
      "itemName": "Item Name",
      "batchNo": "BATCH001",
      "expiryDate": "2027-01-31 00:00:00",
      "departmentId": 485,
      "departmentName": "Main Pharmacy",
      "billId": 9602168,
      "billNumber": "MPPHTI/796",
      "billTypeAtomic": "PHARMACY_DIRECT_ISSUE",
      "transactionQty": -10.0,
      "stockQty": 40.0,
      "institutionBatchQty": 40.0,
      "totalBatchQty": 40.0,
      "itemStock": 40.0,
      "institutionItemStock": 40.0,
      "totalItemStock": 40.0,
      "retailRate": 12.48,
      "purchaseRate": 10.0,
      "costRate": 10.0202,
      "stockSaleValue": 499.2,
      "stockPurchaseValue": 400.0,
      "stockCostValue": 400.808,
      "itemStockValueAtSaleRate": 499.2,
      "itemStockValueAtPurchaseRate": 400.0,
      "itemStockValueAtCostRate": 400.808,
      "institutionBatchStockValueAtSaleRate": 499.2,
      "institutionBatchStockValueAtPurchaseRate": 400.0,
      "institutionBatchStockValueAtCostRate": 400.808,
      "totalBatchStockValueAtSaleRate": 499.2,
      "totalBatchStockValueAtPurchaseRate": 400.0,
      "totalBatchStockValueAtCostRate": 400.808
    }
  ]
}
```

## Notes
- Results are ordered by latest records first (`id DESC`).
- `limit` defaults to 100 if omitted or invalid (<= 0). Maximum allowed value is 1000.
- Date parsing requires exact format: `yyyy-MM-dd HH:mm:ss`.
- `historyType` must match an enum value in `HistoryType`.
