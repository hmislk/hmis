# Item Mappings API

Which items a **department**, an **institution**, or an **outside-charge site** may bill —
the `ItemMapping` entity. Previously reachable only through three admin pages:

- `admin/items/manage_department_item_mappings.xhtml`
- `admin/items/manage_institution_item_mappings.xhtml`
- `admin/items/manage_outside_charge_item_mappings.xhtml`

Backs the `ITEMS_MAPPED_TO_LOGGED_DEPARTMENT` / `..._INSTITUTION` item-listing strategies
(theatre surgery service bill, OPD/Inward/Collecting-Centre mapped item lists) — a hospital
that wants a curated item list for a department depends on these mappings existing.

- Base path: `/api/item-mappings`
- Auth: `Finance: <api-key>` header on every request
- Entity: `ItemMapping` (`item`, `department` **or** `institution` + `outsideChargeMapping`,
  `retired`, `creater`/`createdAt`, `retirer`/`retiredAt`)

## Response envelope

```json
{ "status": "success", "code": 200, "data": { } }
{ "status": "error",   "code": 400, "message": "..." }
```

---

## The three mapping kinds

`ItemMapping` has no separate "site" table. All three kinds live on the same row:

| Kind | How it's stored |
|---|---|
| Department mapping | `department` set, `institution` null |
| Institution mapping | `institution` set, `outsideChargeMapping = false` |
| Outside-charge mapping | `institution` set (the charge site), `outsideChargeMapping = true` |

A request identifies the target with exactly one of `departmentId`, `institutionId`, or
`outsideChargeSiteId` — providing zero or more than one is a `400`.

---

## GET /api/item-mappings/search

List current mappings — the audit/diff primitive that did not exist before this API.

Query parameters (all optional, combine freely):

| Param | Meaning |
|---|---|
| `departmentId` | Only mappings for this department |
| `institutionId` | Only mappings for this institution (excludes outside-charge rows) |
| `outsideChargeSiteId` | Only outside-charge mappings for this institution |
| `itemId` | Only mappings for this item |
| `query` | Substring match (case-insensitive) on the mapped item's name |
| `limit` | Max rows returned, default 30, clamped to 1–200 |

At most one of `departmentId` / `institutionId` / `outsideChargeSiteId` may be given — `400`
if more than one is present. Only non-retired mappings are returned.

```bash
curl -s -H "Finance: <key>" \
  "http://localhost:9090/rh/api/item-mappings/search?departmentId=101&limit=50"
```

```json
{
  "status": "success",
  "code": 200,
  "data": [
    {
      "id": 501,
      "retired": false,
      "outsideChargeMapping": false,
      "createdAt": "2026-09-01 10:15:00",
      "item": { "id": 3001, "name": "Full Blood Count" },
      "department": { "id": 101, "name": "Cashier" }
    }
  ]
}
```

---

## POST /api/item-mappings

Map one item to exactly one target.

```json
{ "itemId": 3001, "departmentId": 101 }
```

(or `"institutionId"`, or `"outsideChargeSiteId"` instead of `"departmentId"`)

Behaviour, matching `ItemMappingController`'s `addAllSelectedItemsTo...()` methods exactly:

| Situation | Result |
|---|---|
| No mapping exists for this item+target | `201`, new row created, `status: "success"` |
| An **active** mapping already exists | `200`, `status: "already_exists"`, existing row returned, no new row |
| A **soft-retired** mapping exists for this exact item+target | `200`, `status: "reactivated"` — that same row is un-retired (`retired=false`) in place, **not** duplicated |

This mirrors the admin-page behavior: re-adding an item that was previously removed from a
department's mapping reactivates the original row (preserving its original `creater`/
`createdAt`) rather than creating a second row for the same pair.

```bash
curl -s -H "Finance: <key>" -H "Content-Type: application/json" \
  -X POST "http://localhost:9090/rh/api/item-mappings" \
  -d '{"itemId": 3001, "departmentId": 101}'
```

---

## POST /api/item-mappings/bulk

Map many items to one target in a single call — mirrors how the admin pages actually work
(`addAllSelectedItemsToDepartment()` etc. already operate on a list of selected items).

```json
{ "itemIds": [3001, 3002, 3003], "departmentId": 101 }
```

Never fails the whole batch on one bad id. Each `itemId` gets its own outcome:

| Outcome | Meaning |
|---|---|
| `created` | New mapping row created |
| `reactivated` | A previously soft-retired mapping for this item+target was un-retired in place |
| `already_mapped` | An active mapping already exists for this item+target |
| `item_not_found` | No `Item` exists with that id |
| `invalid_item_id` | The array entry wasn't a valid numeric id |

```bash
curl -s -H "Finance: <key>" -H "Content-Type: application/json" \
  -X POST "http://localhost:9090/rh/api/item-mappings/bulk" \
  -d '{"itemIds": [3001, 3002, 9999999], "departmentId": 101}'
```

```json
{
  "status": "success",
  "code": 200,
  "data": [
    { "itemId": 3001, "outcome": "created", "id": 601 },
    { "itemId": 3002, "outcome": "already_mapped", "id": 588 },
    { "itemId": 9999999, "outcome": "item_not_found" }
  ]
}
```

Re-running the same bulk call is safe (idempotent) — items already mapped simply report
`already_mapped` again instead of creating duplicates.

---

## DELETE /api/item-mappings/{id}

Soft-retires one mapping (`retired = true`, `retirer`, `retiredAt` set) — matches
`removeSelectedItemMappingForDepartment()` / `...ForInstitution()` / `...ForOutsideChargeSite()`.

**Never a hard delete.** The row stays in the table so history/audit is preserved, and
re-mapping the same item+target later reactivates this same row (see the POST/bulk
behavior above) instead of creating a new one.

```bash
curl -s -H "Finance: <key>" -X DELETE "http://localhost:9090/rh/api/item-mappings/601"
```

Calling DELETE on an already-retired mapping is a no-op that returns success (idempotent).

---

## Notes for callers

- The target Item's own `retired`/`inactive` state is **not** checked before creating a
  mapping — this matches the existing `ItemMappingController` behavior exactly, which never
  validates the mapped item's own status either.
- The target Department/Institution's own `retired` state is likewise not checked, for the
  same reason (parity with the admin pages, not an oversight).
- `outsideChargeSiteId` and `institutionId` both resolve against the `Institution` entity —
  there is no separate outside-charge-site table. Use `outsideChargeSiteId` when you want the
  mapping flagged `outsideChargeMapping = true` (Add Outside Charges / Mode B, issue #23250);
  use `institutionId` for a plain institution-scoped mapping. The two are mutually exclusive
  and produce different, non-overlapping result sets in `GET /search`.
