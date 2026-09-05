# QuickBooks Integration API Documentation

## Overview
The QuickBooks (QB) Integration API provides read-only endpoints to export HMIS financial transaction data in a format compatible with QuickBooks. Finance staff use these endpoints to synchronise invoices, GRN records, payment data, and journal entries from HMIS into QuickBooks.

All endpoints follow an incremental-sync pattern: they accept the ID of the last successfully synced record and a start date, then return the next batch of records from that point forward (up to 2 500 records per request).

## Authentication
All endpoints require API Key-based authentication using the `Finance` header.

**Header:**
```
Finance: <your-api-key>
```

## Base URL
```
/api/qb
```

## Path Parameter Reference

| Parameter | Type | Format | Description |
|-----------|------|--------|-------------|
| `institution_code` | String | — | Institution code as configured in HMIS |
| `last_invoice_id` | Long | numeric | ID of the last synced invoice/bill (use 1 on first sync) |
| `last_grn_id` | Long | numeric | ID of the last synced GRN record (use 1 on first sync) |
| `last_return_grn_id` | Long | numeric | ID of the last synced GRN return / write-off / journal (use 1 on first sync) |
| `last_payment_id` | Long | numeric | ID of the last synced payment (use 1 on first sync) |
| `last_return_payment_id` | Long | numeric | ID of the last synced payment return (use 1 on first sync) |
| `last_date` | String | yyyy-MM-dd | Earliest date to include records from (e.g. `2025-01-01`) |

## Endpoints

### 1. Get Last Invoice ID
Returns the highest bill ID that exists on or after `last_date` for the given institution. Use this to determine the starting point before calling the list endpoints.

**Endpoint:** `GET /api/qb/last_invoice_id/{institution_code}/{last_date}`

**Example Request:**
```bash
curl -X GET \
  'http://localhost:8080/hmis/api/qb/last_invoice_id/MAIN/2025-01-01' \
  -H 'Finance: your-api-key-here'
```

**Response:**
```json
{
  "lastInvoiceId": "3500000",
  "status": { "code": 200, "type": "success" }
}
```

---

### 2. Credit Invoice List (`cInvList`)
Returns cash-paid invoices (PharmacySale, ChannelPaid, OpdBill, ChannelCash, PharmacyWholeSale) with IDs greater than `last_invoice_id` and dated on or after `last_date`. Maximum 2 500 records per call.

**Endpoint:** `GET /api/qb/cInvList/{institution_code}/{last_invoice_id}/{last_date}`

**Example Request:**
```bash
curl -X GET \
  'http://localhost:8080/hmis/api/qb/cInvList/MAIN/3500000/2025-01-01' \
  -H 'Finance: your-api-key-here'
```

**Response:**
```json
{
  "cInvList": [
    {
      "header": {
        "invoiceDate": "2025-01-15",
        "invoiceNo": "PHARM-3500001",
        "bankAcc": "Cash Pharmacy",
        "customerName": "Cash",
        "soldTo": "John Smith",
        "payMethod": "Cash",
        "rep_name": ""
      },
      "grid": [
        {
          "item": "Paracetamol 500mg",
          "qty": 10,
          "amount": 150.00,
          "invClass": "Pharmacy",
          "invType": "PharmacySale"
        }
      ]
    }
  ],
  "lastId": 3502500,
  "status": { "code": 200, "type": "success" }
}
```

**Usage note:** Use the returned `lastId` as `last_invoice_id` in your next call to continue paginating.

---

### 3. Invoice List (`invList`)
Returns credit-paid outpatient invoices and all inpatient final bills with IDs greater than `last_invoice_id` and dated on or after `last_date`. Maximum 2 500 records per call.

**Endpoint:** `GET /api/qb/invList/{institution_code}/{last_invoice_id}/{last_date}`

**Example Request:**
```bash
curl -X GET \
  'http://localhost:8080/hmis/api/qb/invList/MAIN/3500000/2025-01-01' \
  -H 'Finance: your-api-key-here'
```

**Response:**
```json
{
  "invList": [
    {
      "header": {
        "invoiceDate": "2025-01-15",
        "invoiceNo": "OPD-3500100",
        "bankAcc": "Credit Company Pharmacy",
        "customerName": "ABC Insurance",
        "soldTo": "Jane Doe",
        "payMethod": "Credit",
        "rep_name": ""
      },
      "grid": [
        {
          "item": "Consultation",
          "qty": 1,
          "amount": 2000.00,
          "invClass": "OPD",
          "invType": "OpdBill"
        }
      ]
    }
  ],
  "lastId": 3502500,
  "status": { "code": 200, "type": "success" }
}
```

---

### 4. Sales Returns List (`salesRetList`)
Returns sales return bills (voided / returned invoices) with IDs greater than `last_invoice_id` and dated on or after `last_date`.

**Endpoint:** `GET /api/qb/salesRetList/{institution_code}/{last_invoice_id}/{last_date}`

**Example Request:**
```bash
curl -X GET \
  'http://localhost:8080/hmis/api/qb/salesRetList/MAIN/3500000/2025-01-01' \
  -H 'Finance: your-api-key-here'
```

**Response:**
```json
{
  "salesRetList": [ ... ],
  "lastId": 3502500,
  "status": { "code": 200, "type": "success" }
}
```

---

### 5. GRN List (`grnList`)
Returns pharmacy Goods Received Note (purchase) bills with IDs greater than `last_grn_id` and dated on or after `last_date`.

**Endpoint:** `GET /api/qb/grnList/{institution_code}/{last_grn_id}/{last_date}`

**Example Request:**
```bash
curl -X GET \
  'http://localhost:8080/hmis/api/qb/grnList/MAIN/1000000/2025-01-01' \
  -H 'Finance: your-api-key-here'
```

**Response:**
```json
{
  "grnList": [ ... ],
  "lastId": 1002500,
  "status": { "code": 200, "type": "success" }
}
```

---

### 6. GRN Returns List (`grnRetList`)
Returns GRN return records (goods returned to supplier) with IDs greater than `last_return_grn_id` and dated on or after `last_date`.

**Endpoint:** `GET /api/qb/grnRetList/{institution_code}/{last_return_grn_id}/{last_date}`

**Example Request:**
```bash
curl -X GET \
  'http://localhost:8080/hmis/api/qb/grnRetList/MAIN/1000000/2025-01-01' \
  -H 'Finance: your-api-key-here'
```

**Response:**
```json
{
  "grnRetList": [ ... ],
  "lastId": 1002500,
  "status": { "code": 200, "type": "success" }
}
```

---

### 7. Write-off Corrections List (`wcList`)
Returns write-off and stock correction entries with IDs greater than `last_return_grn_id` and dated on or after `last_date`.

**Endpoint:** `GET /api/qb/wcList/{institution_code}/{last_return_grn_id}/{last_date}`

**Example Request:**
```bash
curl -X GET \
  'http://localhost:8080/hmis/api/qb/wcList/MAIN/1000000/2025-01-01' \
  -H 'Finance: your-api-key-here'
```

**Response:**
```json
{
  "wcList": [ ... ],
  "lastId": 1002500,
  "status": { "code": 200, "type": "success" }
}
```

---

### 8. Journal Entries List (`jurList`)
Returns journal entry records with IDs greater than `last_return_grn_id` and dated on or after `last_date`.

**Endpoint:** `GET /api/qb/jurList/{institution_code}/{last_return_grn_id}/{last_date}`

**Example Request:**
```bash
curl -X GET \
  'http://localhost:8080/hmis/api/qb/jurList/MAIN/1000000/2025-01-01' \
  -H 'Finance: your-api-key-here'
```

**Response:**
```json
{
  "jurList": [ ... ],
  "lastId": 1002500,
  "status": { "code": 200, "type": "success" }
}
```

---

### 9. Customer Payment List (`cusPayList`)
Returns customer payment records (bill sessions / receipts) with IDs greater than `last_payment_id` and dated on or after `last_date`.

**Endpoint:** `GET /api/qb/cusPayList/{institution_code}/{last_payment_id}/{last_date}`

**Example Request:**
```bash
curl -X GET \
  'http://localhost:8080/hmis/api/qb/cusPayList/MAIN/500000/2025-01-01' \
  -H 'Finance: your-api-key-here'
```

**Response:**
```json
{
  "cusPayList": [ ... ],
  "lastId": 502500,
  "status": { "code": 200, "type": "success" }
}
```

---

### 10. Payment Returns (`paymentreturn`)
Returns payment return records (refunds) with IDs greater than `last_return_payment_id` for the given institution. Unlike the other endpoints, this one does not filter by date.

**Endpoint:** `GET /api/qb/paymentreturn/{institution_code}/{last_return_payment_id}`

**Example Request:**
```bash
curl -X GET \
  'http://localhost:8080/hmis/api/qb/paymentreturn/MAIN/100000' \
  -H 'Finance: your-api-key-here'
```

**Response:**
```json
{
  "paymentreturnList": [ ... ],
  "lastId": 102500,
  "status": { "code": 200, "type": "success" }
}
```

---

## Response Invoice Object Structure

Invoice and sales-return records share the same two-part structure.

### `header` object
| Field | Type | Description |
|-------|------|-------------|
| `invoiceDate` | String (yyyy-MM-dd) | Date the bill was created |
| `invoiceNo` | String | Composite identifier: `{deptId}-{billId}` |
| `bankAcc` | String | Payment method label + department name |
| `customerName` | String | Credit company, staff name, or payment method in-hand label |
| `soldTo` | String | Patient name (or "Customer" if unknown) |
| `payMethod` | String | Payment method label |
| `rep_name` | String | Representative name (currently always empty string) |

### `grid` array items
| Field | Type | Description |
|-------|------|-------------|
| `item` | String | Item / service name |
| `qty` | Double | Absolute quantity |
| `amount` | Double | Absolute net value |
| `invClass` | String | Department name (QB class) |
| `invType` | String | Bill type label |

---

## Error Responses

### 401 Unauthorized — invalid API key
```json
{ "code": 401, "type": "error", "message": "Not a valid key." }
```

### 401 Unauthorized — invalid institution
```json
{ "code": 401, "type": "error", "message": "Not a valid institution code." }
```

### 401 Unauthorized — invalid path parameter
```json
{ "code": 401, "type": "error", "message": "Not a valid path parameter." }
```

### 400 Bad Request — unrecognised parameter
```json
{ "code": 400, "type": "error", "message": "Parameter name is not recognized." }
```

---

## Incremental Sync Workflow

The typical integration loop for each data type is:

1. **Bootstrap** — Call `GET /api/qb/last_invoice_id/{code}/{start_date}` to get the starting ID.
2. **Paginate** — Call the appropriate list endpoint with the returned `lastId` and your start date.
3. **Process** — Import the returned records into QuickBooks.
4. **Advance** — Use the `lastId` from the response as the `last_invoice_id` for the next call.
5. **Repeat** — Continue until the returned list is empty or smaller than the maximum batch size (2 500).

```
last_id = get_last_invoice_id(institution_code, start_date)

while True:
    result = get_cinv_list(institution_code, last_id, start_date)
    if result["cInvList"] is empty:
        break
    import_to_quickbooks(result["cInvList"])
    last_id = result["lastId"]
```

---

## Notes

- All date fields in responses use the format `yyyy-MM-dd`.
- All monetary values are returned as `Double`.
- Records are returned in ascending ID order.
- Maximum batch size is **2 500 records** per request.
- Use `1` as the initial ID on the very first sync.
- The `last_date` parameter acts as a lower-bound date filter; records before this date are excluded.
- The `paymentreturn` endpoint has no date filter — it returns all returns after the given ID.
