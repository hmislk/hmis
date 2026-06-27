# Pharmacy Discount API

Manages `PaymentSchemeDiscount` rows — per-category discount percentages applied during pharmacy
billing for a given payment scheme.

**Base path:** `/api/pharmacy/discounts`  
**Auth:** `Finance` header with an active, non-expired API key.

---

## Endpoints

### GET `/api/pharmacy/discounts`

List non-retired discount rows.

**Query params** (all optional):

| Param | Description |
|-------|-------------|
| `paymentSchemeId` | Filter by PaymentScheme id |
| `paymentSchemeName` | Filter by PaymentScheme name (partial, case-insensitive) |
| `billType` | Filter by BillType enum (e.g. `PharmacySale`) |
| `limit` | Max rows to return (default 200) |

---

### POST `/api/pharmacy/discounts`

Create a single discount row.

**Body:**

```json
{
  "paymentSchemeId": 12,
  "discountPercent": 5.0,
  "billType": "PharmacySale",
  "categoryId": 34,
  "paymentMethod": "Cash"
}
```

`discountPercent` is required. `billType` defaults to `PharmacySale` when omitted.
Supply `paymentSchemeId` or `paymentSchemeName` to identify the scheme.

---

### POST `/api/pharmacy/discounts/bulk`

**Primary use case.** Idempotent bulk upsert: sets the same `discountPercent` across **all**
non-retired `PharmaceuticalItemCategory` rows for the given payment scheme.

- If a matching non-retired row already exists for `(category, paymentScheme, billType, paymentMethod)`,
  its `discountPercent` is updated.
- If no matching row exists, a new one is created.
- Re-running with the same parameters is safe — produces `created=0, updated=N`.

**Body:**

```json
{
  "paymentSchemeName": "Multiple",
  "paymentMethod": "Cash",
  "discountPercent": 5.0
}
```

`paymentMethod` is **required** — the runtime lookup always filters by the sale's payment method, so
rows with `null` paymentMethod are never matched. Valid values: `Cash`, `Card`, `Credit`,
`MultiplePaymentMethods`, `Staff`, etc.

Or by scheme id:

```json
{
  "paymentSchemeId": 12,
  "paymentMethod": "Credit",
  "discountPercent": 5.0,
  "billType": "PharmacySale"
}
```

**Response:**

```json
{
  "status": "success",
  "code": 200,
  "data": {
    "paymentSchemeId": 12,
    "paymentSchemeName": "Multiple",
    "billType": "PharmacySale",
    "discountPercent": 5.0,
    "created": 47,
    "updated": 0,
    "total": 47
  }
}
```

---

### PUT `/api/pharmacy/discounts/{id}`

Update the `discountPercent` on an existing row.

**Body:**

```json
{
  "discountPercent": 7.5
}
```

---

### DELETE `/api/pharmacy/discounts/{id}`

Soft-retire a discount row (sets `retired=true`).

---

## Read path

Rows created by this API are consumed by `PriceMatrixController.fetchPaymentSchemeDiscount(…)` /
`getPaymentSchemeDiscount(…)` during pharmacy billing. The resolution order is:
Item → Category → ParentCategory → Department. Category-level rows (created by `/bulk`) are the
second step in that chain.

---

## Verification

```bash
# List all discounts for a scheme
curl -s -H "Finance: <key>" \
  "http://localhost:9080/rh/api/pharmacy/discounts?paymentSchemeName=Multiple"

# Bulk create 5% across all pharmacy categories
curl -s -X POST -H "Finance: <key>" -H "Content-Type: application/json" \
  -d '{"paymentSchemeName":"Multiple","discountPercent":5.0}' \
  http://localhost:9080/rh/api/pharmacy/discounts/bulk

# Check DB
# SELECT id, category_id, paymentScheme_id, discountPercent, DTYPE
#   FROM PriceMatrix WHERE DTYPE='PaymentSchemeDiscount';

# Re-run to confirm idempotency (created=0, updated=N)
```
