# Timed Items API

Master data for **timed services** — inward charges billed by how long something ran
(oxygen, ICU time, cardiac monitoring, nebulisation) — and the tiered fee slots that price
them.

This API manages *configuration*. Recording or billing actual usage against a patient is a
different surface entirely and is not covered here.

- Base path: `/api/timed-items`
- Auth: `Finance: <api-key>` header on every request
- Entities: `TimedItem` (a service), `TimedItemFee` (one fee slot), `TimedItemCategory`

Consumed by the inward timed service page
(`/inward/inward_timed_service_consume.xhtml`) and configured in the UI at
`/inward/inward_timed_item.xhtml` / `inward_timed_item_fee.xhtml`.

## Response envelope

```json
{ "status": "success", "code": 200, "data": { } }
{ "status": "error",   "code": 400, "message": "..." }
```

---

## How fee slots work

A timed item carries an ordered list of fee slots. `sortOrder` is the **slot position**:
slot 1 prices the first block of the stay, slot 2 the second, and so on. The last slot with
`repeating = true` continues to price every block beyond the list.

`durationHours` is the length of one block and `durationUnit` says what that length is
counted in:

| durationUnit | Meaning |
|---|---|
| `ONE_TIME` | Charge the fee once for the whole service, regardless of duration |
| `MINUTE` | `durationHours` counts minutes |
| `HOUR` | `durationHours` counts hours — **the default** when the field is omitted |
| `DAY` | `durationHours` counts days |

`durationHours` keeps its historical name for database compatibility; with a unit attached
it is really "duration value".

### Slot rules

These are enforced identically here and on the fee page — an identical payload is accepted
or rejected the same way on both surfaces.

| Rule | Behaviour |
|---|---|
| `sortOrder` omitted or `0` | Auto-assigned to the next free slot (highest in use + 1) |
| `sortOrder` less than 1 | `400 — Slot Order must be 1 or greater.` |
| `sortOrder` already used by another live fee on the same item | `400 — Slot Order must be unique per service.` |
| `durationHours <= 0` on any unit except `ONE_TIME` | `400 — Duration must be greater than 0 for a <unit> fee.` |

Duplicate or zero slot orders are rejected because the billing code orders fees by
`sortOrder` and then picks a tier **by position**. A collision makes that ordering
ambiguous, so the stay could be priced at the wrong tier with nothing visibly wrong.

---

## Timed items

### Search

```
GET /api/timed-items/search
```

| Param | Notes |
|---|---|
| `query` | Matched against name and code, case-insensitively, as a substring |
| `departmentType` | `DepartmentType` enum, e.g. `Inward`, `Theatre` |
| `inwardChargeType` | `InwardChargeType` enum, e.g. `OxygenCharges`, `Nebulisation` |
| `categoryId` | `TimedItemCategory` id |
| `departmentId` | Department id |
| `institutionId` | Institution id |
| `inactive` | `true` / `false` |
| `includeRetired` | `true` to include retired items. Default `false` |
| `limit` | Page size, 1–100. Default 30 |
| `offset` | Rows to skip. Default 0 |

```json
{
  "status": "success",
  "code": 200,
  "data": {
    "items": [
      {
        "id": 13838979,
        "name": "Oxygen Therapy",
        "code": "OXYTEST",
        "departmentType": "Inward",
        "inwardChargeType": "OxygenCharges",
        "categoryId": 1667,
        "categoryName": "Respiratory Care",
        "total": 500.0,
        "inactive": false,
        "retired": false
      }
    ],
    "total": 137,
    "limit": 30,
    "offset": 0
  }
}
```

`total` is the number of rows matching the filters, ignoring `limit`/`offset` — use it to
decide whether another page remains.

### Get one

```
GET /api/timed-items/{id}
GET /api/timed-items/{id}?includeRetired=true
```

Returns the item with its fee list. A retired item is `404` unless `includeRetired=true`,
which also includes retired fees in the response.

### Create

```
POST /api/timed-items
```

```json
{
  "name": "Oxygen Therapy",
  "departmentType": "Inward",
  "inwardChargeType": "OxygenCharges",
  "code": "OXY",
  "departmentId": 486,
  "institutionId": 2,
  "categoryId": 1667,
  "inactive": false
}
```

`name`, `departmentType` and `inwardChargeType` are required. `code` is generated from the
name if omitted.

### Update, retire, restore

```
PUT    /api/timed-items/{id}                    all fields optional
DELETE /api/timed-items/{id}?retireComments=... soft-retire
PATCH  /api/timed-items/{id}/restore            un-retire
PATCH  /api/timed-items/{id}/activate           inactive = false
PATCH  /api/timed-items/{id}/deactivate         inactive = true
```

`DELETE` never removes a row — it sets `retired = true`. `restore` clears that along with
the retirer, timestamp and comments. Fees that were already retired before the item was
retired stay retired: restoring a service does not resurrect slot configuration that may
have been removed deliberately.

Restoring something that is not retired returns `409`.

**Retire vs deactivate:** deactivate hides a service from selection but leaves it
configured and reportable; retire removes it from the catalogue. Prefer deactivate for
"we're not offering this at the moment".

---

## Fee slots

### List

```
GET /api/timed-items/{id}/fees
GET /api/timed-items/{id}/fees?includeRetired=true
```

Ordered by `sortOrder`, then id.

### Add one

```
POST /api/timed-items/{id}/fees
```

```json
{
  "name": "First hour",
  "fee": 500,
  "ffee": 750,
  "durationHours": 1,
  "durationUnit": "HOUR",
  "overShootHours": 0,
  "sortOrder": 1,
  "repeating": false
}
```

`name` is required. `durationHours` must be greater than 0 unless `durationUnit` is
`ONE_TIME`. `ffee` (foreigner fee) defaults to `fee`. `durationUnit` defaults to `HOUR`.

### Replace the whole list

```
PUT /api/timed-items/{id}/fees
```

```json
{
  "fees": [
    { "name": "First hour",  "fee": 500, "durationHours": 1, "durationUnit": "HOUR", "sortOrder": 1 },
    { "id": 13523704, "name": "Second hour", "fee": 400, "durationHours": 1, "sortOrder": 2 },
    { "name": "Thereafter", "fee": 300, "durationHours": 1, "sortOrder": 3, "repeating": true }
  ]
}
```

Prefer this over a run of `POST`s when configuring a tiered service:

- The whole set is validated before anything is written, so a bad slot in the middle cannot
  leave the service half-configured.
- Slot-order uniqueness is checked across the complete list rather than one row at a time.
- The parent item's total is recalculated once instead of once per call.

A row **with** an `id` updates that existing slot; a row **without** one adds a slot. Any
live slot whose id is absent from the array is retired. Send `{"fees": []}` to clear every
slot.

### Update one, retire, restore

```
PUT    /api/timed-items/{id}/fees/{feeId}          all fields optional
DELETE /api/timed-items/{id}/fees/{feeId}          soft-retire
PATCH  /api/timed-items/{id}/fees/{feeId}/restore  un-retire
```

`PUT` validates the fee **as it will be stored**, not just the fields that arrived: a
payload that only switches the unit can still leave a zero-length block, and one that only
moves the slot order can still collide with a sibling.

Restoring a fee needs a live parent — restore the item first. If the slot it used to occupy
was taken while it was retired, the restore is rejected with the uniqueness error; move the
occupying slot first.

---

## Categories

`TimedItemCategory` groups services in the UI.

```
GET    /api/timed-items/categories?query=&limit=
GET    /api/timed-items/categories/{id}
POST   /api/timed-items/categories            body: { "name": "...", "code": "..." }
PUT    /api/timed-items/categories/{id}
DELETE /api/timed-items/categories/{id}?retireComments=...
```

`POST` with a name already in use returns `409` with
`{"status": "already_exists", "id": ..., "name": "..."}`.

---

## Status codes

| Code | When |
|---|---|
| 400 | Validation failure — slot order, duration, bad enum value, malformed JSON or query param |
| 401 | Missing, unknown or expired `Finance` key |
| 404 | Item or fee does not exist, or is retired and `includeRetired` was not set |
| 409 | Restoring something that is not retired; duplicate category name |

---

## Related

- [API_INWARD_ROOM.md](API_INWARD_ROOM.md) — room facility charges use a `TimedItemFee` for
  their billing block and accept the same `durationUnit` as
  `timedItemFeeDurationUnit`
- AI chat tool: `manage_timed_items` (see `AnthropicApiService`)
