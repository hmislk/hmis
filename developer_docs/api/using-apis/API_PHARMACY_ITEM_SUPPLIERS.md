# Pharmacy Item Suppliers API

Links items (AMPs) to suppliers (Dealers) — the `ItemsDistributors` entity. Previously reachable only through the GUI page:

- `pharmacy/pharmacy_items_distributors.xhtml` (**Pharmacy → Pharmaceutical Management → Institutions → Item Suppliers**)

When onboarding supplier price lists containing hundreds of items, linking them one-by-one via the GUI takes hours, and direct SQL bypasses audit logging and validation. This API provides validated, audited single and bulk item-supplier linking.

- Base path: `/api/pharmacy/item_suppliers`
- Auth: `Finance: <api-key>` header on every request
- Entity: `ItemsDistributors` (`institution` [Dealer], `item` [AMP], `orderNo`, `retired`, `creater`/`createdAt`, `retirer`/`retiredAt`, `retireComments`)

---

## Response Envelope

Standard HMIS JSON response envelopes:

```json
{
  "status": "success",
  "code": 200,
  "data": { }
}
```

Error response:

```json
{
  "status": "error",
  "code": 400,
  "message": "Supplier must be a Dealer institution (got: Agency)"
}
```

Conflict response (HTTP 409):

```json
{
  "status": "error",
  "code": 409,
  "message": "Active link already exists for supplier 100 and item 200",
  "data": {
    "id": 501,
    "supplierId": 100,
    "supplierName": "ABC Pharmaceuticals",
    "itemId": 200,
    "itemName": "Paracetamol 500mg Tablet",
    "itemCode": "PARA500",
    "orderNo": 0,
    "retired": false,
    "createdAt": "2026-11-03 22:30:00",
    "createrId": 10,
    "createrName": "admin"
  }
}
```

---

## GET /api/pharmacy/item_suppliers

List active (non-retired) item-supplier links.

### Query Parameters

| Parameter | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `supplierId` | Long | Optional | Filter by supplier (Institution ID) |
| `itemId` | Long | Optional | Filter by item (Item ID) |
| `limit` | Integer | Optional | Max records to return (default: `50`, max: `500`) |
| `offset` | Integer | Optional | Offset for pagination (default: `0`) |

### Example Request

```bash
curl -s -H "Finance: <api-key>" \
  "http://localhost:8080/hmis/api/pharmacy/item_suppliers?supplierId=100&limit=20"
```

### Example Response

```json
{
  "status": "success",
  "code": 200,
  "data": [
    {
      "id": 501,
      "supplierId": 100,
      "supplierName": "ABC Pharmaceuticals",
      "itemId": 200,
      "itemName": "Paracetamol 500mg Tablet",
      "itemCode": "PARA500",
      "orderNo": 1,
      "retired": false,
      "createdAt": "2026-11-03 22:30:00",
      "createrId": 10,
      "createrName": "admin"
    }
  ]
}
```

---

## GET /api/pharmacy/item_suppliers/{id}

Get a single item-supplier link by ID.

### Example Request

```bash
curl -s -H "Finance: <api-key>" \
  "http://localhost:8080/hmis/api/pharmacy/item_suppliers/501"
```

---

## POST /api/pharmacy/item_suppliers

Link one item to a Dealer supplier.

### Request Body

```json
{
  "supplierId": 100,
  "itemId": 200,
  "orderNo": 1
}
```

### Validation & Behavior

| Situation | HTTP Status | Outcome |
| :--- | :--- | :--- |
| Valid link created | `201 Created` | Link created, audit logged, returns created link data |
| Active link already exists | `409 Conflict` | Returns existing link in `data` with 409 status |
| Supplier or item not found | `404 Not Found` | Error message indicating missing entity |
| Supplier is retired or not a Dealer | `400 Bad Request` | Rejects non-Dealer or retired supplier |
| Item is retired or not an Amp/Ampp | `400 Bad Request` | Rejects non-pharmaceutical (must be Amp or Ampp) or retired item |
| Missing `supplierId` or `itemId` | `400 Bad Request` | Validation error message |

---

## POST /api/pharmacy/item_suppliers/bulk

Link multiple items to suppliers in one batch call. Each entry that fails business validation (e.g. non-existent or retired entity, duplicate link), and each entry with no usable ID (a non-object row such as `null`, or a missing or `null` `supplierId`/`itemId`), receives its own row-level outcome, allowing valid entries in the batch to succeed. A malformed ID (e.g. a fractional number like `301.5` or a string like `"bad"`), or a body that is not valid JSON, rejects the entire batch with HTTP 400 with no row outcomes and nothing is linked. An unexpected persistence error rolls back the whole batch and returns HTTP 500 without row outcomes. Maximum 500 entries per bulk request.

### Request Formats

Supports three standard request formats:

1. **Array of objects**:
```json
[
  { "supplierId": 100, "itemId": 200 },
  { "supplierId": 100, "itemId": 300 }
]
```

2. **Object with `links` or `items` array**:
```json
{
  "links": [
    { "supplierId": 100, "itemId": 200 },
    { "supplierId": 100, "itemId": 300 }
  ]
}
```

3. **Single supplier fan-out with `itemIds` array**:
```json
{
  "supplierId": 100,
  "itemIds": [200, 300, 400]
}
```

### Example Response

```json
{
  "status": "success",
  "code": 200,
  "data": [
    {
      "supplierId": 100,
      "itemId": 200,
      "outcome": "created",
      "code": 201,
      "id": 501,
      "data": { ... }
    },
    {
      "supplierId": 100,
      "itemId": 300,
      "outcome": "already_exists",
      "code": 409,
      "id": 480,
      "message": "Active link already exists for supplier 100 and item 300"
    },
    {
      "supplierId": 100,
      "itemId": 999,
      "outcome": "error",
      "code": 404,
      "message": "Item not found with ID: 999"
    }
  ]
}
```

---

## DELETE /api/pharmacy/item_suppliers/{id}

Soft-retires an item-supplier link. Never performs a hard delete from `itemsdistributors`.

### Query Parameters

| Parameter | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `retireComments` | String | Optional | Reason for retiring the link (default: `"Retired via API"`) |

### Example Request

```bash
curl -s -X DELETE -H "Finance: <api-key>" \
  "http://localhost:8080/hmis/api/pharmacy/item_suppliers/501?retireComments=Supplier+no+longer+distributes+this+brand"
```

### Example Response

```json
{
  "status": "success",
  "code": 200,
  "data": {
    "id": 501,
    "supplierId": 100,
    "supplierName": "ABC Pharmaceuticals",
    "itemId": 200,
    "itemName": "Paracetamol 500mg Tablet",
    "itemCode": "PARA500",
    "retired": true,
    "retireComments": "Supplier no longer distributes this brand",
    "retiredAt": "2026-11-03 22:35:00",
    "retirerId": 10,
    "retirerName": "admin"
  }
}
```
