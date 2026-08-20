# Inward Room Management API

REST endpoints for managing inward room master data: room categories, rooms, and room facility charges (fee configurations).

**Authentication:** `Finance` header (API key)

---

## 1. Room Categories — `/api/inward/room-categories`

### GET — List

```
GET /api/inward/room-categories?query=&size=
```

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| query | string | — | Name search (case-insensitive) |
| size | int | 200 | Max results (1–1000) |

**Response 200:**
```json
{
  "status": "success",
  "code": 200,
  "data": [
    { "id": 1, "name": "General Ward", "code": "GW", "description": null, "retired": false }
  ]
}
```

### GET — Fetch by ID

```
GET /api/inward/room-categories/{id}
```

Returns a single room category or 404 if not found / retired.

### POST — Create

```
POST /api/inward/room-categories
Content-Type: application/json
```

```json
{ "name": "General Ward", "code": "GW", "description": "Standard ward rooms" }
```

| Field | Required | Description |
|-------|----------|-------------|
| name | Yes | Category name (must be unique) |
| code | No | Short code |
| description | No | Description |

**Response 201** — created record.
**Response 409** — `{ "status": "already_exists", "id": 5 }` if name already in use.

### PUT — Update

```
PUT /api/inward/room-categories/{id}
Content-Type: application/json
```

All fields optional. Only supplied fields are updated.

### DELETE — Soft-retire

```
DELETE /api/inward/room-categories/{id}?retireComments=reason
```

---

## 2. Rooms — `/api/inward/rooms`

### GET — List

```
GET /api/inward/rooms?query=&roomCategoryId=&size=
```

| Param | Type | Description |
|-------|------|-------------|
| query | string | Name search |
| roomCategoryId | long | Filter by room category ID |
| size | int | Max results (default 200) |

**Response 200:**
```json
{
  "status": "success",
  "code": 200,
  "data": [
    {
      "id": 10,
      "name": "Room 101",
      "code": "R101",
      "description": null,
      "filled": false,
      "retired": false,
      "roomCategory": { "id": 1, "name": "General Ward" }
    }
  ]
}
```

### GET — Fetch by ID

```
GET /api/inward/rooms/{id}
```

Returns a single room or 404 if not found / retired.

### POST — Create

```json
{
  "name": "Room 101",
  "code": "R101",
  "roomCategoryId": 1,
  "filled": false
}
```

| Field | Required | Description |
|-------|----------|-------------|
| name | Yes | Room name (must be unique) |
| code | No | Short code |
| description | No | Description |
| roomCategoryId | No | ID of the parent RoomCategory |
| filled | No | `true` if room is under construction/unavailable |

### PUT — Update

```
PUT /api/inward/rooms/{id}
```

All fields optional. `roomCategoryId: null` clears the category.

### DELETE — Soft-retire

```
DELETE /api/inward/rooms/{id}?retireComments=reason
```

---

## 3. Room Facility Charges — `/api/inward/room-facility-charges`

### GET — List

```
GET /api/inward/room-facility-charges?query=&roomId=&roomCategoryId=&size=
```

| Param | Type | Description |
|-------|------|-------------|
| query | string | Name search |
| roomId | long | Filter by room ID |
| roomCategoryId | long | Filter by room category ID |
| size | int | Max results (default 200) |

**Response 200:**
```json
{
  "status": "success",
  "code": 200,
  "data": [
    {
      "id": 20,
      "name": "General Ward - Cash",
      "roomCharge": 1500.0,
      "maintananceCharge": 200.0,
      "linenCharge": 100.0,
      "nursingCharge": 500.0,
      "moCharge": 300.0,
      "moChargeForAfterDuration": 150.0,
      "adminstrationCharge": 50.0,
      "medicalCareCharge": 0.0,
      "retired": false,
      "room": { "id": 10, "name": "Room 101" },
      "roomCategory": { "id": 1, "name": "General Ward" },
      "department": { "id": 3, "name": "Inward" },
      "timedItemFee": {
        "id": 5,
        "durationHours": 24.0,
        "overShootHours": 6.0,
        "durationDaysForMoCharge": 0
      }
    }
  ]
}
```

### GET — Fetch by ID

```
GET /api/inward/room-facility-charges/{id}
```

Returns a single room facility charge or 404 if not found / retired.

### POST — Create

```json
{
  "name": "General Ward - Cash",
  "roomId": 10,
  "roomCategoryId": 1,
  "departmentId": 3,
  "roomCharge": 1500.0,
  "maintananceCharge": 200.0,
  "linenCharge": 100.0,
  "nursingCharge": 500.0,
  "moCharge": 300.0,
  "moChargeForAfterDuration": 150.0,
  "adminstrationCharge": 50.0,
  "medicalCareCharge": 0.0,
  "timedItemFeeDurationHours": 24.0,
  "timedItemFeeOverShootHours": 6.0,
  "timedItemFeeDurationDaysForMoCharge": 0
}
```

| Field | Required | Description |
|-------|----------|-------------|
| name | Yes | Charge config name |
| roomId | No | Room ID |
| roomCategoryId | No | Room category ID |
| departmentId | No | Department ID |
| roomCharge | No | Room charge per block (default 0) |
| maintananceCharge | No | Maintenance charge per block |
| linenCharge | No | Linen charge per day |
| nursingCharge | No | Nursing charge per block |
| moCharge | No | MO charge per block |
| moChargeForAfterDuration | No | MO charge for after-duration |
| adminstrationCharge | No | Administration charge per block |
| medicalCareCharge | No | Medical care charge per block |
| timedItemFeeDurationHours | No | Block duration in hours |
| timedItemFeeOverShootHours | No | Over-shoot hours for last block |
| timedItemFeeDurationDaysForMoCharge | No | Duration days for MO charge calculation |

### PUT — Update

```
PUT /api/inward/room-facility-charges/{id}
```

All fields optional. Only supplied fields are updated.

### DELETE — Soft-retire

```
DELETE /api/inward/room-facility-charges/{id}?retireComments=reason
```

---

## 4. Room Facility Timed Items — `/api/inward/room-facility-charges/{id}/timed-items`

Manages the list of individual `TimedItem` services attached to a room facility charge (issue
#23147), so each is auto-billed based on duration of stay alongside the fixed room charges. This
is distinct from the `timedItemFee` block-duration/overshoot config on the parent charge (§3
above) — that config controls *how* time-based billing is calculated; this list controls *which*
items get billed that way.

### GET — List attached timed items

```
GET /api/inward/room-facility-charges/{id}/timed-items
```

`{id}` is the `RoomFacilityCharge` id. Returns 404 if not found / retired.

**Response 200:**
```json
{
  "status": "success",
  "code": 200,
  "data": [
    {
      "id": 7,
      "timedItem": { "id": 42, "name": "ICU Bed Charge" },
      "createdAt": "2026-08-20 10:15:00",
      "retired": false
    }
  ]
}
```

### POST — Attach a timed item

```
POST /api/inward/room-facility-charges/{id}/timed-items
Content-Type: application/json
```

```json
{ "timedItemId": 42 }
```

| Field | Required | Description |
|-------|----------|-------------|
| timedItemId | Yes | ID of the `TimedItem` to attach |

**Response 201** — created attachment record (same shape as the GET list rows).
**Response 409** — already attached (an active attachment for this `TimedItem` already exists on
this room facility charge).
**Response 400** — `timedItemId` missing, or the `TimedItem` doesn't exist / is retired.

### DELETE — Soft-retire an attachment

```
DELETE /api/inward/room-facility-charges/{id}/timed-items/{linkId}?retireComments=reason
```

Retires the attachment (`{linkId}`), not the underlying `TimedItem`. Returns 404 if the
attachment doesn't belong to `{id}` or doesn't exist; 400 if already retired.

---

## Bed-board SVG (issue #21592)

A room is a **leaf** in the bed-board hierarchy (Site → Building → Floor → Unit →
Room), so it stores only one drawing:

- **`svgChildView`** — the small shape showing how the room looks as a tile
  *inside its parent's* canvas, on the shared `viewBox="0 0 1000 600"` grid.
  (Rooms have no `svgParentView` — you do not navigate *into* a room.)

`svgChildView` is accepted on the normal room `POST`/`PUT` bodies and returned by
the room `GET`. SVG is stored **verbatim** (create/update → read back identical);
it is sanitised at render time on the bed board. See the wiki page
[Inpatient — Bed Board](https://github.com/hmislk/hmis/wiki/Inpatient-Bed-Board)
for authoring guidance and copy-paste examples.

### GET `/api/inward/rooms/{id}/svg` — Read just the drawing

```json
{ "status": "success", "code": 200,
  "data": { "id": 10, "name": "Room 101", "svgChildView": "<svg ...>" } }
```

### PUT `/api/inward/rooms/{id}/svg` — Set just the drawing

Only `svgChildView` is changed when present; pass an empty string to clear it.

```bash
curl -s -H "Finance: $KEY" -H "Content-Type: application/json" \
  -X PUT "$BASE/api/inward/rooms/10/svg" \
  -d '{"svgChildView":"<svg viewBox=\"0 0 1000 600\"><rect x=\"100\" y=\"100\" width=\"200\" height=\"150\"/></svg>"}'
```

---

## Typical Workflow

```bash
KEY="your-api-key"
BASE="http://localhost:8080/rh"

# 1. Create a room category
curl -s -H "Finance: $KEY" -H "Content-Type: application/json" \
  -X POST "$BASE/api/inward/room-categories" \
  -d '{"name":"General Ward"}' | python -m json.tool

# 2. Create a room in that category (use ID from step 1)
curl -s -H "Finance: $KEY" -H "Content-Type: application/json" \
  -X POST "$BASE/api/inward/rooms" \
  -d '{"name":"Room 101","roomCategoryId":1,"filled":false}' | python -m json.tool

# 3. Create a fee config for that room (use IDs from above)
curl -s -H "Finance: $KEY" -H "Content-Type: application/json" \
  -X POST "$BASE/api/inward/room-facility-charges" \
  -d '{"name":"Room 101 - Cash","roomId":10,"roomCategoryId":1,"roomCharge":1500,"timedItemFeeDurationHours":24}' | python -m json.tool

# 4. Attach a Timed Item to that charge (use the charge id from step 3, TimedItem id from /api/timed-items)
curl -s -H "Finance: $KEY" -H "Content-Type: application/json" \
  -X POST "$BASE/api/inward/room-facility-charges/20/timed-items" \
  -d '{"timedItemId":42}' | python -m json.tool

# 5. Verify capabilities
curl -s "$BASE/api/capabilities" | python -m json.tool
```
