# Inward Price Matrix API

Manages `InwardPriceAdjustment` rows (a `PriceMatrix` subclass) — margin and discount entries
applied to Inward bills for a given billing department and category, optionally narrowed by
payment method, price band, admission type, room category, and credit company.

**Base path:** `/api/price-matrix/inward`
**Auth:** `Finance` header with an active, non-expired API key.
**Source:** `src/main/java/com/divudi/ws/inward/PriceMatrixInwardApi.java`

---

## Key field: `departmentId` means Billing Department, not Item Department

`departmentId` refers to the **billing/ward department** the charge applies to (e.g. "Cardiology
Ward", "ICU") — **not** an item/stock department (like a pharmacy store).

Evidence:

- `PriceMatrix` has a `department` field (`ManyToOne Department`) **and a separate `item` field**
  (`PriceMatrix.java`). Inward price adjustments only use `department`; `item` is used by other
  matrix types (e.g. pharmacy).
- The Inward admin UI (`inward_price_adjustment_room_category.xhtml`) labels this field
  "Department" with the placeholder **"Select the Room Department"**, backed by
  `departmentController.completeDept`.
- The API sets `entry.setInstitution(department.getInstitution())` on create — confirming this is
  the billing-side department the entry's institution is derived from.

When integrating, resolve `departmentId` from the ward/room department the bill is raised
against, not from any item or store hierarchy.

---

## Room Category (Room Facility Category)

`roomCategoryId` narrows the margin/discount entry to a specific **Room Category** (e.g. "General
Ward", "Semi-Private", "ICU"), backed by `RoomCategory` — a subclass of `Category`
(`com.divudi.core.entity.inward.RoomCategory extends com.divudi.core.entity.Category`) managed via
`RoomCategoryFacade` and `/api/inward/room-categories` (see `API_INWARD_ROOM.md`).

This is the same field already used by the existing "Room Category" price-adjustment screen
(`inward_price_adjustment_room_category.xhtml`), now exposed through the REST API. It is
independent of `categoryId` (the general service/billing category) — a row can set either,
both, or neither, and `roomCategory` participates in duplicate-combination matching the same way
`admissionType`/`creditCompany` do.

---

## Authentication

All endpoints require a valid API key in the `Finance` header, resolved via `ApiKeyController`.
Returns `401` if the key is missing, unknown, retired, expired, or the linked `WebUser` is
retired/not activated.

```
GET /api/price-matrix/inward
Finance: <api-key>
```

---

## Endpoints

### GET `/api/price-matrix/inward`

List non-retired entries, ordered by department name, category name, then `fromPrice`.

**Query params** (all optional):

| Param | Type | Description |
|-------|------|-------------|
| `departmentId` | Long | Filter by billing department |
| `categoryId` | Long | Filter by category |
| `roomCategoryId` | Long | Filter by room category |
| `paymentMethod` | String (enum) | Filter by `PaymentMethod` name (e.g. `Cash`, `Card`, `Credit`) |
| `limit` | int | Max rows, default 50, clamped 1–1000 |

---

### GET `/api/price-matrix/inward/{id}`

Fetch one entry. `404` if not found, retired, or not an `InwardPriceAdjustment`.

---

### POST `/api/price-matrix/inward`

Create a new entry.

**Body:**

```json
{
  "departmentId": 12,
  "categoryId": 45,
  "margin": 10.0,
  "paymentMethod": "Card",
  "discountPercent": 5.0,
  "fromPrice": 0,
  "toPrice": 50000,
  "admissionTypeId": 2,
  "roomCategoryId": 3,
  "creditCompanyId": 7
}
```

| Field | Required | Notes |
|-------|----------|-------|
| `departmentId` | Yes | Billing/ward department. Institution is inherited from it. |
| `categoryId` | Yes | Target category |
| `margin` | Yes | Must be finite |
| `paymentMethod` | No | Omit/blank = applies to all payment methods |
| `discountPercent` | No | Default `0` |
| `fromPrice` | No | Default `0` |
| `toPrice` | No | Default `9999999999`; must be `> fromPrice` |
| `admissionTypeId` | No | Restrict to an admission type |
| `roomCategoryId` | No | Restrict to a room category (see above) |
| `creditCompanyId` | No | Restrict to a credit company (`Institution`) |

**Behavior:**

- Validates department/category/admissionType/creditCompany exist and are not retired.
- Rejects `fromPrice >= toPrice` with `400`.
- Duplicate detection (see below) — returns `409` instead of creating a conflicting row.
- On success: `201` with the created entry, logs `PRICE_MATRIX_CREATED` audit event.

---

### PUT `/api/price-matrix/inward/{id}`

Partial update — only fields present in the body are changed (`containsKey` check).

- Setting `paymentMethod`, `admissionTypeId`, `roomCategoryId`, or `creditCompanyId` to `null`
  clears that association. `departmentId`/`categoryId` cannot be nulled.
- Re-validates `fromPrice < toPrice` and re-runs duplicate detection (excluding self) — `409` on
  conflict.
- Logs `PRICE_MATRIX_UPDATED` with before/after snapshots.
- `404` if not found, retired, or not an `InwardPriceAdjustment`.

---

### DELETE `/api/price-matrix/inward/{id}?retireComments=`

Soft-retires the entry (`retired=true`, `retiredAt`, `retirer`). Never physically deletes.

- `400` if already retired.
- `404` if not found or not an `InwardPriceAdjustment`.
- Logs `PRICE_MATRIX_RETIRED`.
- Response: `{ "id": ..., "retired": true }`

---

## Response DTO shape

Returned by list/get/create/update:

```json
{
  "id": 101,
  "departmentId": 12,
  "departmentName": "Cardiology Ward",
  "categoryId": 45,
  "categoryName": "Room Charges",
  "paymentMethod": "Card",
  "margin": 10.0,
  "discountPercent": 5.0,
  "fromPrice": 0.0,
  "toPrice": 50000.0,
  "admissionTypeId": 2,
  "admissionTypeName": "Emergency",
  "roomCategoryId": 3,
  "roomCategoryName": "General Ward",
  "creditCompanyId": 7,
  "creditCompanyName": "ABC Insurance",
  "retired": false,
  "createdAt": "2026-08-10 09:15:00"
}
```

---

## Duplicate detection

Before create/update, the API searches for another **active** entry with the exact same
combination of: `department`, `category`, `paymentMethod`, `fromPrice`, `toPrice`,
`creditCompany`, `admissionType`, `roomCategory` (nulls matched via `IS NULL`). If found:

```json
HTTP 409
{
  "status": "already_exists",
  "code": 409,
  "message": "An active price adjustment entry with the same combination already exists.",
  "id": 87
}
```

---

## Standard response envelope

**Success**

```json
{ "status": "success", "code": 200, "data": { ... } }
```

**Error**

```json
{ "status": "error", "code": 400, "message": "departmentId is required" }
```

| Code | Meaning |
|------|---------|
| 200 | Successful GET/PUT/DELETE |
| 201 | Entry created |
| 400 | Validation error (bad JSON, missing/invalid field, `fromPrice >= toPrice`, invalid enum, already-retired on delete) |
| 401 | Missing/invalid API key |
| 404 | Not found / not an `InwardPriceAdjustment` / retired |
| 409 | Duplicate active combination |
| 500 | Unexpected server error |

---

## Related components

| Component | Role |
|-----------|------|
| `PriceMatrix` | Base entity shared across matrix types (has both `department` and `item` fields) |
| `InwardPriceAdjustment` | Inward subclass: margin, discount, price band, admissionType, roomCategory, creditCompany |
| `RoomCategory` | `Category` subclass representing a room/ward category (e.g. General Ward, ICU) |
| `PriceMatrixFacade` | JPA persistence access |
| `RoomCategoryFacade` | Lookup/validation for `roomCategoryId` |
| `AuditService` | Before/after audit logging on create/update/retire |
| `ApiKeyController` | Resolves/validates the `Finance` header API key |

Sibling APIs in `com.divudi.ws.inward` worth reviewing: `InwardDiscountMatrixApi.java`,
`InwardPriceAdjustmentApi.java`.

---

## Verification

```bash
# List entries for a department
curl -s -H "Finance: <key>" \
  "http://localhost:9080/rh/api/price-matrix/inward?departmentId=12"

# Create an entry
curl -s -X POST -H "Finance: <key>" -H "Content-Type: application/json" \
  -d '{"departmentId":12,"categoryId":45,"margin":10.0}' \
  http://localhost:9080/rh/api/price-matrix/inward

# Retire an entry
curl -s -X DELETE -H "Finance: <key>" \
  "http://localhost:9080/rh/api/price-matrix/inward/101?retireComments=Superseded"

# Check DB
# SELECT id, department_id, category_id, margin, discountPercent, fromPrice, toPrice, DTYPE
#   FROM PriceMatrix WHERE DTYPE='InwardPriceAdjustment';
```
