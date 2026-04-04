# Finance Legacy API Documentation

## Overview
The Finance Legacy API provides bill query endpoints for retrieving billing data from the HMIS system. This is the **original** finance REST API (`/api/finance`) and returns Bill entities serialised directly to JSON.

For richer financial data (including finance details, pharmaceutical item details, and payment breakdowns), prefer the newer [`/api/costing_data`](API_COSTING_DATA.md) endpoints. Use this legacy API when you need:
- All bills or bill items for today (no parameters)
- Bills filtered by a single date or a date range
- Bills filtered by `BillType` category (not available in `/api/costing_data`)

## Authentication
All endpoints require API Key-based authentication using the `Finance` header.

```
Finance: <your-api-key>
```

## Base URL
```
/api/finance
```

## Date Formats
| Context | Format | Example |
|---------|--------|---------|
| Single date path parameter | `dd-MM-yyyy` | `04-04-2026` |
| Date-range path parameter | `dd-MM-yyyy-hh:mm:ss` | `01-04-2026-00:00:00` |

## Endpoints

### 1. Get All Bills (Today)
Returns all non-retired bills created today.

**Endpoint:** `GET /api/finance/bill`

**Authentication:** Required

**Example:**
```bash
curl -X GET http://localhost:8080/hmis/api/finance/bill \
  -H 'Finance: your-api-key-here'
```

---

### 2. Get Bills by Date
Returns all non-retired bills created on the specified date (full day, midnight-to-midnight).

**Endpoint:** `GET /api/finance/bill/{date}`

**Path Parameters:**
- `date` (string, required): Date in `dd-MM-yyyy` format

**Example:**
```bash
curl -X GET http://localhost:8080/hmis/api/finance/bill/04-04-2026 \
  -H 'Finance: your-api-key-here'
```

---

### 3. Get Bills by Date Range
Returns all non-retired bills created within the specified date-time range (inclusive).

**Endpoint:** `GET /api/finance/bill/{from}/{to}`

**Path Parameters:**
- `from` (string, required): Start datetime in `dd-MM-yyyy-hh:mm:ss` format
- `to` (string, required): End datetime in `dd-MM-yyyy-hh:mm:ss` format

**Example:**
```bash
curl -X GET 'http://localhost:8080/hmis/api/finance/bill/01-04-2026-00:00:00/04-04-2026-23:59:59' \
  -H 'Finance: your-api-key-here'
```

---

### 4. Get Bill Items (Today)
Returns all non-retired bills **with their line items** created today.

**Endpoint:** `GET /api/finance/bill_item`

**Authentication:** Required

**Example:**
```bash
curl -X GET http://localhost:8080/hmis/api/finance/bill_item \
  -H 'Finance: your-api-key-here'
```

---

### 5. Get Bill Items by Date
Returns all non-retired bills with line items created on the specified date.

**Endpoint:** `GET /api/finance/bill_item/{date}`

**Path Parameters:**
- `date` (string, required): Date in `dd-MM-yyyy` format

**Example:**
```bash
curl -X GET http://localhost:8080/hmis/api/finance/bill_item/04-04-2026 \
  -H 'Finance: your-api-key-here'
```

---

### 6. Get Bill Items by Date Range
Returns all non-retired bills with line items created within the specified date-time range.

**Endpoint:** `GET /api/finance/bill_item/{from}/{to}`

**Path Parameters:**
- `from` (string, required): Start datetime in `dd-MM-yyyy-hh:mm:ss` format
- `to` (string, required): End datetime in `dd-MM-yyyy-hh:mm:ss` format

**Example:**
```bash
curl -X GET 'http://localhost:8080/hmis/api/finance/bill_item/01-04-2026-00:00:00/04-04-2026-23:59:59' \
  -H 'Finance: your-api-key-here'
```

---

### 7. Get Bill Items by Category (All Dates)
Returns all non-retired bills with line items for the specified `BillType` category (today by default — the underlying query defaults to today when no dates are supplied).

**Endpoint:** `GET /api/finance/bill_item_cat/{bill_category}`

**Path Parameters:**
- `bill_category` (string, required): A valid `BillType` enum name (case-sensitive)

**Example:**
```bash
curl -X GET http://localhost:8080/hmis/api/finance/bill_item_cat/OpdBill \
  -H 'Finance: your-api-key-here'
```

---

### 8. Get Bill Items by Category and Date
Returns bills with line items for the specified category on the specified date.

**Endpoint:** `GET /api/finance/bill_item_cat/{date}/{bill_category}`

**Path Parameters:**
- `date` (string, required): Date in `dd-MM-yyyy` format
- `bill_category` (string, required): A valid `BillType` enum name (case-sensitive)

**Example:**
```bash
curl -X GET http://localhost:8080/hmis/api/finance/bill_item_cat/04-04-2026/PharmacySale \
  -H 'Finance: your-api-key-here'
```

---

### 9. Get Bill Items by Category and Date Range
Returns bills with line items for the specified category within the date-time range.

**Endpoint:** `GET /api/finance/bill_item_cat/{from}/{to}/{bill_category}`

**Path Parameters:**
- `from` (string, required): Start datetime in `dd-MM-yyyy-hh:mm:ss` format
- `to` (string, required): End datetime in `dd-MM-yyyy-hh:mm:ss` format
- `bill_category` (string, required): A valid `BillType` enum name (case-sensitive)

**Example:**
```bash
curl -X GET 'http://localhost:8080/hmis/api/finance/bill_item_cat/01-04-2026-00:00:00/04-04-2026-23:59:59/InwardFinalBill' \
  -H 'Finance: your-api-key-here'
```

---

## Common BillType Values

| BillType | Description |
|----------|-------------|
| `OpdBill` | Outpatient department bill |
| `InwardFinalBill` | Inpatient final bill |
| `InwardProvisionalBill` | Inpatient provisional bill |
| `PharmacySale` | Pharmacy retail sale |
| `PharmacyIssue` | Pharmacy issue (internal transfer) |
| `LabBill` | Laboratory bill |
| `AdmissionBill` | Patient admission bill |
| `PaymentBill` | Professional fee payment bill |
| `PatientPaymentReceiveBill` | Patient deposit received |
| `PatientPaymentRefundBill` | Patient deposit refunded |

For the full list of `BillType` values see `com.divudi.core.data.BillType`.

---

## Response Format

### Success (bill list, no items)
```json
{
  "status": { "code": 200, "type": "success" },
  "data": [
    {
      "id": 123456,
      "bill_id_1": "INS-001",
      "bill_id_2": "DEPT-001",
      "bill_date": "2026-04-04",
      "bill_time": "10:30:00",
      "bill_categoty": "OpdBill",
      "type": "BilledBill",
      "gross_total": 1500.00,
      "discount": 50.00,
      "net_total": 1450.00,
      "payment_method": "Cash",
      "institution": "Main Hospital",
      "department": "OPD",
      "created_at": "2026-04-04 10:30:00",
      "created_user": "Dr. Smith"
    }
  ]
}
```

### Success (bill list with items)
```json
{
  "status": { "code": 200, "type": "success" },
  "data": [
    {
      "id": 123456,
      "bill_date": "2026-04-04",
      "bill_items": [
        {
          "Id": 789,
          "item": "Paracetamol 500mg",
          "item_type": "Drug",
          "Qty": 10.0,
          "Rate": 5.00,
          "NetRate": 5.00,
          "GrossValue": 50.00,
          "NetValue": 50.00,
          "Refunded": false
        }
      ],
      "gross_total": 50.00,
      "discount": 0.0,
      "net_total": 50.00
    }
  ]
}
```

### Error Responses
```json
{ "code": 401, "type": "error", "message": "Not a valid key." }
{ "code": 400, "type": "error", "message": "No Data." }
{ "code": 401, "type": "error", "message": "Not a valid path parameter." }
```

---

## Comparison with `/api/costing_data`

| Feature | `/api/finance` (legacy) | `/api/costing_data` (modern) |
|---------|------------------------|------------------------------|
| Response model | Bill entity fields | Rich DTOs (finance details, pharmaceutical details, payments) |
| Filter by category | Yes (`bill_item_cat/*`) | No |
| Filter by bill number | No | Yes (`bill?number=...`) |
| Filter by bill ID | No | Yes (`by_bill_id/{id}`) |
| Payment breakdown | Single `payment_method` field | Full `payments` array |
| Finance cost details | No | Yes (cost, purchase, retail rates) |
| Pharmaceutical details | No | Yes |

Use `/api/finance` when you need category-based filtering. Use `/api/costing_data` when you need detailed financial or pharmaceutical cost breakdown.
