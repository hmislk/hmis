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

# 4. Verify capabilities
curl -s "$BASE/api/capabilities" | python -m json.tool
```
