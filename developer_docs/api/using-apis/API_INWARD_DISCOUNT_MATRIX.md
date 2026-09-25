# Inward Discount Matrix API

Manages `InwardDiscountMatrix` rows (a `PriceMatrix` subclass). Each row gives a discount % for
inpatient bills, keyed by discount scheme, department, category, admission type, BHT type
(payment method) and credit company. This API backs the same data as the admin pages
*Inward → Administration → Inward Discount Matrix – Services and Investigations / Pharmacy*.

**Base path:** `/api/inward-discount-matrix`
**Auth:** `Finance` header with an active, non-expired API key.
**Source:** `src/main/java/com/divudi/ws/inward/InwardDiscountMatrixApi.java`

---

## How a row is matched

At billing time the lookup walks Item → Category → Parent Category → Department → global. At each
level a row for the admission's discount scheme wins over a row with no scheme. A **blank**
department, admission type, BHT type or credit company on a row means *"all"*.

**Do not use a blank category to mean "all pharmacy items".** A row with a scheme and a blank
category is the global fallback for *both* pharmacy and services/investigations. To give a flat
discount on every medicine, create one row per pharmaceutical category. The bulk POST below exists
for exactly this.

## Endpoints

| Method | Path | Purpose |
|---|---|---|
| GET | `?scope=service\|pharmacy&departmentId=&categoryId=&admissionTypeId=&paymentSchemeId=&paymentMethod=&creditCompanyId=&limit=` | List active rows |
| GET | `/{id}` | One row |
| POST | *(base)* | Create one row, or many rows with `categoryIds` |
| PUT | `/{id}` | Update a row (`scope` required when changing `categoryId`) |
| DELETE | `/{id}?retireComments=` | Soft-retire |
| GET | `/admission-types/search`, `/payment-schemes/search`, `/pharmaceutical-item-categories/search`, `/payment-methods`, `/credit-companies/search` | Resolve names to ids |

## POST — single row

```json
{
  "scope": "pharmacy",
  "paymentSchemeId": 12,
  "categoryId": 345,
  "discountPercent": 2.5
}
```

`201` with the created row. `409` with `"status": "already_exists"` and the existing `id` if an
active row with the identical combination (department, category, admission type, payment method,
scheme, credit company, null-safe) exists.

## POST — bulk across categories

Pass `categoryIds` (array) **instead of** `categoryId`. All the other fields apply to every row.

```json
{
  "scope": "pharmacy",
  "paymentSchemeId": 12,
  "categoryIds": [345, 346, 347],
  "discountPercent": 2.5
}
```

- Every id is validated first (it must exist, not be retired, and match `scope`). If any id fails,
  the call returns `400` and **nothing** is created.
- Categories that already have an identical active row are skipped, not duplicated.
- Response: `201` if at least one row was created, `200` if every category was skipped.

```json
{
  "status": "success",
  "code": 200,
  "data": {
    "createdCount": 2,
    "skippedCount": 1,
    "created": [ { "id": 9001, "...": "..." }, { "id": 9002, "...": "..." } ],
    "skipped": [ { "categoryId": 345, "existingId": 8123 } ]
  }
}
```

Sending both `categoryId` and `categoryIds` returns `400`.

If a row fails to save partway through, the call returns `500` with the rows already `created`
(each row commits on its own). Re-sending the same request is safe: those rows are skipped.

To list pharmaceutical category ids, use
`GET /api/inward-discount-matrix/pharmaceutical-item-categories/search?limit=200`. **`limit` is
capped at 200**, so if `data` comes back with exactly 200 entries there may be more. Narrow the
search with `query=` (e.g. by first letter) and combine the results before bulk-creating. After
creating, check the count with `GET /api/inward-discount-matrix?scope=pharmacy&paymentSchemeId=…&limit=…`.

## Services & investigations (`scope=service`)

The same bulk POST works with `scope=service`. `categoryIds` may mix ServiceCategory,
ServiceSubCategory and InvestigationCategory ids, and any other type returns `400` with nothing
written. For a category discount to reach a bill, the item must have a category and both
`Item.discountAllowed` and the fee's `discountAllowed` must be true:

- `POST /api/services/items/bulk-discount-allowed` `{"itemType":"Investigation","discountAllowed":true}`
- `POST /api/services/fees/bulk-margin` `{"itemType":"Investigation","discountAllowed":true}`
- `POST /api/investigations/copy-legacy-category` (dry run) / `?apply=true`: copies the deprecated
  `Investigation.investigationCategory` into `category` where `category` is blank. Until then,
  `Investigation.getCategory()` falls back to the legacy field, so the discount lookup already
  sees it.

## Room charges (`scope=room`)

Room charge discounts (room, linen, maintenance, nursing, MO, administration, medical care) use
`scope=room`. `categoryId(s)` are not accepted.

```json
{
  "scope": "room",
  "paymentSchemeId": 12,
  "inwardChargeTypes": ["RoomCharges"],
  "roomCategoryIds": [678, 679],
  "discountPercent": 30
}
```

- `inwardChargeType` or `inwardChargeTypes[]` is required and must be one of `RoomCharges`,
  `LinenCharges`, `MaintainCharges`, `NursingCharges`, `MOCharges`, `AdministrationCharge`,
  `MedicalCareICU`.
- `roomCategoryId` or `roomCategoryIds[]` is optional. Omit it for a row that applies to every
  room. When a room is charged, a row for its own category wins over an all-rooms row.
- One row is created per charge type × room category, and existing identical rows are skipped.
  It always returns the bulk response. Room category ids come from `GET /api/inward/room-categories`.
- `GET ?scope=room[&inwardChargeType=][&roomCategoryId=]` lists room-charge rows. `scope=service`
  and `scope=pharmacy` no longer include room-charge rows.
