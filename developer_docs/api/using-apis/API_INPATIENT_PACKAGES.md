# Inpatient Packages API

Manage fixed-price Inpatient Package headers (per Admission Type + Room Category) and their
component items (services, timed items, professional-fee roles, outside charges, pharmacy
items). Distinct from the Admission Charges API, which manages additive routine charges rather
than a bundled package price.

**Base path:** `/api/inpatient-packages`
**Auth:** `Finance` header (API key)

## Data shape

```json
{
  "id": 12,
  "name": "Normal Delivery Package",
  "admissionTypeId": 3,
  "roomCategoryId": 5,
  "includedRoomDurationHours": 48,
  "fixedRoomCharge": 5000,
  "chargeTypeAmounts": { "RoomCharges": 5000, "NursingCharges": 1000 },
  "totalPrice": 8000,
  "items": [
    { "id": 101, "componentType": "SERVICE", "itemId": 4521, "qty": 1, "fixedPrice": 2000 },
    { "id": 102, "componentType": "PROFESSIONAL_FEE_ROLE", "roleLabel": "Visiting Consultant", "qty": 1, "fixedPrice": 3000 }
  ]
}
```

`fixedRoomCharge` and `totalPrice` are always server-computed — `fixedRoomCharge` from
`chargeTypeAmounts["RoomCharges"]`, `totalPrice` from the sum of `chargeTypeAmounts` plus every
non-retired item's `fixedPrice`. Any value sent for either field is ignored.

`componentType` is one of `SERVICE`, `TIMED_ITEM`, `PROFESSIONAL_FEE_ROLE`, `OUTSIDE_CHARGE`,
`PHARMACY_ITEM`. Every type except `PROFESSIONAL_FEE_ROLE` requires `itemId`;
`PROFESSIONAL_FEE_ROLE` requires `specialityId` or `roleLabel`.

## Endpoints

### `POST /inpatient-packages`
Create a full package (header + items) in one call. Body: `name`, `admissionTypeId`,
`roomCategoryId` required; `includedRoomDurationHours`, `chargeTypeAmounts`, `items` optional
(omitted `items` creates a package with no components). Returns 201 with the created package.

### `GET /inpatient-packages?admissionTypeId=&roomCategoryId=`
List non-retired packages with their items, optionally filtered.

### `GET /inpatient-packages/{id}`
Fetch one package with its items.

### `PUT /inpatient-packages/{id}`
Full update. Header fields (`name`, `admissionTypeId`, `roomCategoryId`,
`includedRoomDurationHours`, `chargeTypeAmounts`) are always overwritten from the body — this is
not a partial update for the header, so a field you omit resets to its default (`0` for
`includedRoomDurationHours`, empty for `chargeTypeAmounts`, which also changes the
server-computed `fixedRoomCharge`/`totalPrice`). Always send the complete header, not just the
field you're changing. `items` behaves differently depending on whether the key is present:
- **Key absent** — existing components are left untouched (header-only update).
- **Key present** (an array, possibly `[]`) — full-replace diff: an item with an `id` updates
  that component, an item without an `id` creates a new one, and any existing component whose
  `id` is missing from the array is soft-retired. `items: []` retires every component.

### `POST /inpatient-packages/{id}/retire`
Soft-retire the whole package. Body (optional): `{"retireComments": "..."}`. There is currently
no restore/un-retire endpoint for this API — retiring a package removes it from every
GET/LIST response this API offers, and undoing it requires direct database access.

## Example: create a package with two components

```bash
curl -s -H "Finance: <key>" -H "Content-Type: application/json" \
  -X POST "http://localhost:9090/rh/api/inpatient-packages" \
  -d '{
    "name": "Normal Delivery Package",
    "admissionTypeId": 3,
    "roomCategoryId": 5,
    "includedRoomDurationHours": 48,
    "chargeTypeAmounts": {"RoomCharges": 5000, "NursingCharges": 1000},
    "items": [
      {"componentType": "SERVICE", "itemId": 4521, "qty": 1, "fixedPrice": 2000},
      {"componentType": "PROFESSIONAL_FEE_ROLE", "roleLabel": "Visiting Consultant", "qty": 1, "fixedPrice": 3000}
    ]
  }' | python -m json.tool
```

## Example: header-only rename (items untouched)

Note this still repeats every header field — PUT has no partial-header mode, so
`includedRoomDurationHours` is included here to avoid resetting it to 0.

```bash
curl -s -H "Finance: <key>" -H "Content-Type: application/json" \
  -X PUT "http://localhost:9090/rh/api/inpatient-packages/12" \
  -d '{"name": "Normal Delivery Package (Updated)", "admissionTypeId": 3, "roomCategoryId": 5,
       "includedRoomDurationHours": 48,
       "chargeTypeAmounts": {"RoomCharges": 5000, "NursingCharges": 1000}}' \
  | python -m json.tool
```
